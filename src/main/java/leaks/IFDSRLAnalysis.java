package leaks;

import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import soot.*;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.scalar.Pair;

import java.util.Collections;
import java.util.Set;

public class IFDSRLAnalysis implements IAnalysis{

    private IFDSSolver<Unit, Pair<ResourceInfo, Set<Local>>,SootMethod, InterproceduralCFG<Unit, SootMethod>> solver;

    public void doAnalysis() {
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();

        IFDSTabulationProblem<Unit, Pair<ResourceInfo, Set<Local>>,
                SootMethod,
                InterproceduralCFG<Unit, SootMethod>> problem = new IFDSResourceLeak(icfg);

        this.solver = new IFDSSolver<>(problem);

        solver.solve();

        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (m.hasActiveBody()) {
                    System.out.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                    System.out.println(m.getName() + " | " + c.getName());
                    for (Unit stmt : m.getActiveBody().getUnits()) {
                        System.out.print(stmt+"\n   |-- ");
                        Set<Pair<ResourceInfo, Set<Local>>> res = solver.ifdsResultsAt(stmt);
                        System.out.println(res);
                    }
                }
            }
        }
    }

    public Set<Pair<ResourceInfo, Set<Local>>> getResultsAtStmt(Unit stmt) {
        return Collections.unmodifiableSet(solver.ifdsResultsAt(stmt));
    }

    @Override
    public void accept(IResultsProcessor resultsProcessor) {
        resultsProcessor.visit(this);
    }
}
