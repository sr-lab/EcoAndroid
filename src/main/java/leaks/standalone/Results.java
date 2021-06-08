package leaks.standalone;

import leaks.*;
import soot.*;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Results implements IResultsProcessor {
    private final Map<SootMethod, Set<Resource>> results = new HashMap<>();

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
        File file = new File("/home/ricardo/batch/" + fileName + ".csv");
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

    @Override
    public void visit(RLAnalysis analysis) {
        SootMethod analyzedMethod = analysis.getAnalyzedMethod();
        Set<Local> analysisResults = analysis.getResults();
        for (Local local : analysisResults) {
            for (Resource resource : Resource.values()) {
                if (resource.isIntraProcedural() &&
                        resource.getType().equals(local.getType().toString())) {
                    results.computeIfAbsent(analyzedMethod, k -> new HashSet<>()).add(resource);
                    break;
                }
            }
        }
    }

    @Override
    public void visit(IFDSRLAnalysis analysis) {
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (m.hasActiveBody()) {
                    for (Unit stmt : m.getActiveBody().getUnits()) {
                        if (stmt instanceof ReturnStmt || stmt instanceof ReturnVoidStmt) {
                            Set<Pair<ResourceInfo, Set<Local>>> res = analysis.getResultsAtStmt(stmt);
                            for (Pair<ResourceInfo, Set<Local>> fact : res) {
                                // TODO and cases where we have class members but we are not in an activity??
                                if (fact.getO1().isClassMember() && m.getName().matches("onStop|onPause")) {
                                    results.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                } else if (fact.getO1().getDeclaringMethod().equals(m)){
                                    results.computeIfAbsent(fact.getO1().getDeclaringMethod(), k-> new HashSet<>()).add(fact.getO1().getResource());
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Override
    public void visit(VascoRLAnalysis analysis) { /* unused */ }

    @Override
    public void visit(AllInOneRLAnalysis allInOneRLAnalysis) { /* unused */ }

}
