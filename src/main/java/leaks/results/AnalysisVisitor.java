package leaks.results;

import leaks.*;
import leaks.analysis.IFDSRLAnalysis;
import leaks.analysis.RLAnalysis;
import leaks.analysis.ResourceInfo;
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
                if (//resource.isIntraProcedural() &&
                        resource.getType().equals(local.getType().toString())) {
                    Leak leak = new Leak(resource, analyzedMethod, analyzedMethod, false, -1);
                    results.add(leak, IResults.AnalysisType.INTRA);
                    break;
                }
            }
        }
    }

    @Override
    public void visit(IFDSRLAnalysis analysis, IResults results) {
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
        Map<Unit, SootMethod> possibleLeaksLocation = collectPossibleLeaksLocation(analysis);
        
        Map<Unit, Pair<ResourceInfo, Local>> leaks = processPossibleLeaks(possibleLeaksLocation, icfg, analysis);
        for (Map.Entry<Unit, Pair<ResourceInfo, Local>> entry : leaks.entrySet()) {
            Pair<ResourceInfo, Local> fact = entry.getValue();
            SootMethod leakedMethod = icfg.getMethodOf(entry.getKey());
            Leak leak = new Leak(fact.getO1().getResource(), leakedMethod,
                    fact.getO1().getDeclaringMethod(), fact.getO1().isClassMember(), leakedMethod.getJavaSourceStartLineNumber());
            results.add(leak, IResults.AnalysisType.INTER);
        }
    }

    private Map<Unit, SootMethod> collectPossibleLeaksLocation(IFDSRLAnalysis analysis) {
        Map<Unit, SootMethod> out = new HashMap<>();
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (m.getName().equals("getAvailableSites")) {
                    System.out.println("");
                }
                if (m.hasActiveBody()) {
                    for (Unit stmt : m.getActiveBody().getUnits()) {
                        Set<Pair<ResourceInfo, Local>> res = analysis.getResultsAtStmt(stmt);

                        if (stmt instanceof ReturnStmt) {
                            ReturnStmt returnStmt = (ReturnStmt) stmt;

                            if (returnStmt.getOp() instanceof Local) { //FIX! || returnStmt.getOp() instanceof NullConstant
                                Local local = (Local) returnStmt.getOp();

                                for (Pair<ResourceInfo, Local> fact : res) {
                                    // If a fact contains a local that is being returned by this statement, it could be
                                    // a resource that would be used by other functions. So, to prevent false positives,
                                    // we ignore this case. If there is indeed a leak on this resource, it will be
                                    // caught in a later return stmt
                                    if ((fact.getO1().isClassMember() && m.getName().matches("onStop|onPause"))
                                            || (!equalLocals(local, fact.getO2()) && !fact.getO1().isClassMember())) {
                                        out.put(stmt, m);
                                    }
                                }
                            }
                        } else if (stmt instanceof ReturnVoidStmt) {
                            for (Pair<ResourceInfo, Local> fact : res) {
                                if (fact.getO1().isClassMember() && m.getName().matches("onStop|onPause")
                                        || !fact.getO1().isClassMember()) {
                                    out.put(stmt, m);
                                }
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private Map<Unit, Pair<ResourceInfo, Local>> processPossibleLeaks(Map<Unit, SootMethod> possibleLeaksLocation,
                                                       JimpleBasedInterproceduralCFG icfg, IFDSRLAnalysis analysis) {

        Map<Unit, Pair<ResourceInfo, Local>> classMemberLeaks = new HashMap<>();
        Map<Unit, Pair<ResourceInfo, Local>> basicLeaks = new HashMap<>();

        for (Map.Entry<Unit, SootMethod> entry : possibleLeaksLocation.entrySet()) {
            Unit leakedUnit = entry.getKey();
            SootMethod leakedMethod = entry.getValue();

            for (Pair<ResourceInfo, Local> fact : analysis.getResultsAtStmt(leakedUnit)) {
                /*
                if (fact.getO1().getName().equals("&QUERYMAP")) {
                    continue;
                }
                 */

                 // Handle class-member resources
                 if (fact.getO1().isClassMember()) {
                     // For a class-member resource to be leaked: the resource must have been leaked in its suggested
                     // place to release and the resource has to be leaked in the same class where it was acquired
                     if (leakedMethod.getDeclaringClass().getName().equals(fact.getO1().getDeclaringClass().getName())
                            && leakedMethod.getName().equals(fact.getO1().getResource().getPlaceToRelease())) {
                         classMemberLeaks.put(leakedUnit, fact);
                     }
                 // Handle normal resources (that are declared in methods and can be passed by ref)
                 // Check if method was called, and if so, check if the caller has the resource leaked.
                 // We consider a leak only if that happens, as a way to reduce false positives.
                 // NOTE: This is not recursive. We only check for the callee at one level.
                 } else {
                     boolean leakedInCallerMethod = false;
                     Collection<Unit> callers = icfg.getCallersOf(leakedMethod);
                     List<Boolean> callersUseResource = new ArrayList<>();
                     for (Unit caller : callers) {
                         SootMethod callerMethod = icfg.getMethodOf(caller);
                         boolean callerUsesResource = methodUsesResource(callerMethod, fact.getO1(), analysis);
                         callersUseResource.add(callerUsesResource);
                         if (possibleLeaksLocation.containsKey(caller) && callerUsesResource) {
                             for (Pair<ResourceInfo, Local> callerFact : analysis.getResultsAtStmt(caller)) {
                                 if (callerFact.getO1().equals(fact.getO1())) {
                                     leakedInCallerMethod = true;
                                 }
                             }
                         }
                     }
                     if (leakedInCallerMethod || !callersUseResource.stream().anyMatch(b -> b)) {
                     //if (!leakedInCallerMethod && callersUseResource.stream().anyMatch(b -> b)) {
                         basicLeaks.put(leakedUnit, fact);
                     }
                 }
            }
        }

        /* Does not work, as the locals can change during analysis!
        // It is possible for a class member resource to be acquired as a basic resource.
        // In this case, the class member resource takes priority, and we eliminate the duplicate basic resource.
        Map<Unit, Pair<ResourceInfo, Local>> iterBasicLeaks = new HashMap<>(basicLeaks);
        for (Map.Entry<Unit, Pair<ResourceInfo, Local>> entryCML : classMemberLeaks.entrySet()) {
            if (entryCML.getValue().getO1().isClassMember()) {
                for (Map.Entry<Unit, Pair<ResourceInfo, Local>> entryBL : iterBasicLeaks.entrySet()) {
                    if (equalLocals(entryBL.getValue().getO2(), entryCML.getValue().getO2())) {
                        basicLeaks.remove(entryBL);
                    }
                }
            }
        }
         */

        Map<Unit, Pair<ResourceInfo, Local>> leaks = new HashMap<>(basicLeaks);
        leaks.putAll(classMemberLeaks);
        return leaks;
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
