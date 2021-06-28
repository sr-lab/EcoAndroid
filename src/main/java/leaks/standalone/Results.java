package leaks.standalone;

import leaks.*;
import soot.*;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Results implements IResultsProcessor {
    private final Map<SootMethod, Set<Resource>> results = new HashMap<>();

    private final Map<SootMethod, Set<Resource>> intraProcResults = new HashMap<>();
    private final Map<SootMethod, Set<Resource>> interProcResults = new HashMap<>();


    private static Results INSTANCE;

    private Results() {
    }

    public static Results getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Results();
        }
        return INSTANCE;
    }

    public void toCSV(String fileName) throws IOException {
        // TODO change paths
        writeResultsToFile(results, fileName, "all");
        writeResultsToFile(interProcResults, fileName, "inter");
        writeResultsToFile(intraProcResults, fileName, "intra");
    }

    private void writeResultsToFile( Map<SootMethod, Set<Resource>> results, String fileName, String type) throws IOException {
        File file = new File("/home/ricardo/batch/" + fileName + "_" + type + ".csv");
        FileWriter writer = new FileWriter(file);
        writer.write("methodName,className,resourceLeaked\n");
        for (Map.Entry<SootMethod, Set<Resource>> entry : results.entrySet()) {
            StringBuilder res = new StringBuilder();
            for (Resource r : entry.getValue()) {
                res.append(entry.getKey().getName()).append(",")
                        .append(entry.getKey().getDeclaringClass()).append(",")
                        .append(r).append("\n");
            }
            writer.write(res.toString());
        }
        writer.flush();
        writer.close();
    }

    private boolean equalLocals(Local l1, Local l2) {
        return l1.equivTo(l2) || (l1.getName().matches(l2.getName())) && (l2).getName().startsWith("RESOURCE_");
    }

    @Override
    public void visit(RLAnalysis analysis) {
        SootMethod analyzedMethod = analysis.getAnalyzedMethod();
        Set<Local> analysisResults = analysis.getResults();
        for (Local local : analysisResults) {
            for (Resource resource : Resource.values()) {
                if (resource.isIntraProcedural() &&
                        resource.getType().equals(local.getType().toString())) {
                    results.computeIfAbsent(analyzedMethod, k -> new HashSet<>()).add(resource);
                    intraProcResults.computeIfAbsent(analyzedMethod, k -> new HashSet<>()).add(resource);
                    break;
                }
            }
        }
    }

    @Override
    public void visit(IFDSRLAnalysis analysis) {
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (m.hasActiveBody()) {
                    for (Unit stmt : m.getActiveBody().getUnits()) {
                        Set<Pair<ResourceInfo, Local>> res = analysis.getResultsAtStmt(stmt);
                        // TODO refactor in accordance to changes in callReturn:
                        // TODO  - if facts in ReturnVoidStmt -> LEAK!
                        // TODO  - if facts in ReturnStmt -> need to check facts and return type
                        if (stmt instanceof ReturnStmt) {
                            ReturnStmt returnStmt = (ReturnStmt) stmt;
                            
                            if (returnStmt.getOp() instanceof Local) {
                                Local local = (Local) returnStmt.getOp();

                                for (Pair<ResourceInfo, Local> fact : res) {
                                    // TODO and cases where we have class members but we are not in an activity??
                                    if (fact.getO1().isClassMember() && m.getName().matches("onStop|onPause")
                                            || !equalLocals(local, fact.getO2())) {
                                        results.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                        interProcResults.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                    }
                                }
                            }
                        } else if (stmt instanceof ReturnVoidStmt) {
                            for (Pair<ResourceInfo, Local> fact : res) {
                                // TODO and cases where we have class members but we are not in an activity??
                                if (fact.getO1().isClassMember() && m.getName().matches("onStop|onPause")) {
                                    results.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                    interProcResults.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
        @Override
    public void visit(IFDSRLAnalysis analysis) {
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (m.hasActiveBody()) {
                    if (m.getName().equals("getWhiteboardState")) {
                        System.out.println("");
                    }
                    for (Unit stmt : m.getActiveBody().getUnits()) {
                        if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
                            List<Unit> preds = icfg.getPredsOf(stmt);
                            Set<Pair<ResourceInfo, Local>> res = analysis.getResultsAtStmt(stmt);
                            for (Pair<ResourceInfo, Local> fact : res) {
                                // TODO and cases where we have class members but we are not in an activity??
                                if (fact.getO1().isClassMember() && m.getName().matches("onStop|onPause")) {
                                    results.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                    interProcResults.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                } else if (fact.getO1().getDeclaringMethod().equals(m)){
                                    results.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                    interProcResults.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
     */

    @Override
    public void visit(VascoRLAnalysis analysis) { /* unused */ }

    @Override
    public void visit(AllInOneRLAnalysis allInOneRLAnalysis) { /* unused */ }

}
