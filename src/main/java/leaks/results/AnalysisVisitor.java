package leaks.results;

import leaks.*;
import soot.*;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.scalar.Pair;

import java.util.*;

public class AnalysisVisitor implements IAnalysisVisitor{

    private static AnalysisVisitor INSTANCE;

    private AnalysisVisitor() {
    }

    public static AnalysisVisitor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AnalysisVisitor();
        }
        return INSTANCE;
    }
    @Override
    public void visit(RLAnalysis analysis, IResults results) {
        SootMethod analyzedMethod = analysis.getAnalyzedMethod();
        Set<Local> analysisResults = analysis.getResults();
        for (Local local : analysisResults) {
            for (Resource resource : Resource.values()) {
                if (resource.isIntraProcedural() &&
                        resource.getType().equals(local.getType().toString())) {
                    Leak leak = new Leak(resource, analyzedMethod, analyzedMethod, -1);
                    results.add(leak, IResults.AnalysisType.INTRA);
                    break;
                }
            }
        }
    }

    @Override
    public void visit(IFDSRLAnalysis analysis, IResults results) {
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
        Map<SootMethod, Unit> possibleLeaksLocation = collectPossibleLeaksLocation(analysis);
        Map<SootMethod, Unit> leaksLocation = processPossibleLeaks(possibleLeaksLocation, icfg, analysis);
        for (Map.Entry<SootMethod, Unit> entry : leaksLocation.entrySet()) {
            Set<Pair<ResourceInfo, Local>> facts = analysis.getResultsAtStmt(entry.getValue());
            for (Pair<ResourceInfo, Local> fact : facts) {
                Leak leak = new Leak(fact.getO1().getResource(), entry.getKey(),
                        fact.getO1().getDeclaringMethod(), entry.getValue().getJavaSourceStartLineNumber());
                results.add(leak, IResults.AnalysisType.INTER);
            }
        }
    }

    private Map<SootMethod, Unit> collectPossibleLeaksLocation(IFDSRLAnalysis analysis) {
        // TODO and cases where we have class members but we are not in an activity??
        Map<SootMethod, Unit> out = new HashMap<>();
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (m.hasActiveBody()) {
                    for (Unit stmt : m.getActiveBody().getUnits()) {
                        Set<Pair<ResourceInfo, Local>> res = analysis.getResultsAtStmt(stmt);
                        if (stmt instanceof ReturnStmt) {
                            ReturnStmt returnStmt = (ReturnStmt) stmt;

                            if (returnStmt.getOp() instanceof Local) {
                                Local local = (Local) returnStmt.getOp();

                                for (Pair<ResourceInfo, Local> fact : res) {
                                    // If a fact contains a local that is being returned by this statement, it could be
                                    // a resource that would be used by other functions. So, to prevent false positives,
                                    // we ignore this case. If there is indeed a leak on this resource, it will be
                                    // caught in a later return stmt
                                    if (fact.getO1().isClassMember() && m.getName().matches("onStop|onPause")
                                            || !equalLocals(local, fact.getO2())) {
                                        out.put(m, stmt);
                                    }
                                }
                            }
                        } else if (stmt instanceof ReturnVoidStmt) {
                            for (Pair<ResourceInfo, Local> fact : res) {
                                if (fact.getO1().isClassMember() && m.getName().matches("onStop|onPause")) {
                                    out.put(m, stmt);
                                }
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private Map<SootMethod, Unit> processPossibleLeaks(Map<SootMethod, Unit> possibleLeaksLocation,
                                               JimpleBasedInterproceduralCFG icfg, IFDSRLAnalysis analysis) {

        Map<SootMethod, Unit> leaksLocation = new HashMap<>(possibleLeaksLocation);
        List<SootMethod> locationsToRemove = new ArrayList<>();

        for (Map.Entry<SootMethod, Unit> entry : possibleLeaksLocation.entrySet()) {
            for (Pair<ResourceInfo, Local> fact : analysis.getResultsAtStmt(entry.getValue())) {
                boolean leakedInCallerMethod = false;
                Collection<Unit> callers = icfg.getCallersOf(entry.getKey());
                List<Boolean> callersUseResource = new ArrayList<>();
                for (Unit caller : callers) {
                    SootMethod callerMethod = icfg.getMethodOf(caller);
                    boolean callerUsesResource = methodUsesResource(callerMethod, fact.getO1(), analysis);
                    callersUseResource.add(callerUsesResource);
                    if (possibleLeaksLocation.containsKey(callerMethod) && callerUsesResource) {
                        for (Pair<ResourceInfo, Local> callerFact : analysis.getResultsAtStmt(possibleLeaksLocation.get(callerMethod))) {
                            if (callerFact.getO1().equals(fact.getO1())) {
                                leakedInCallerMethod = true;
                            }
                        }
                    }
                }
                if (!leakedInCallerMethod && callersUseResource.stream().anyMatch(b -> b)) {
                    //leaksLocation.remove(entry.getKey());
                    locationsToRemove.add(entry.getKey());
                }
            }
        }

        for (SootMethod m : locationsToRemove) {
            leaksLocation.remove(m);
        }

        return leaksLocation;
    }

    private boolean methodUsesResource(SootMethod method, ResourceInfo resourceInfo, IFDSRLAnalysis analysis) {
        if (method.hasActiveBody()) {
            Body body = method.retrieveActiveBody();
            for (Unit u : body.getUnits()) {
                for (Pair<ResourceInfo, Local> fact : analysis.getResultsAtStmt(u)) {
                    if (fact.getO1().equals(resourceInfo)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean equalLocals(Local l1, Local l2) {
        return l1.equivTo(l2) || (l1.getName().matches(l2.getName())) && (l2).getName().startsWith("RESOURCE_");
    }
}
