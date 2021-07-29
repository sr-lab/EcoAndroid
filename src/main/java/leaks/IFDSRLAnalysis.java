package leaks;

import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IDESolver;
import heros.solver.IFDSSolver;
import leaks.results.IAnalysisVisitor;
import leaks.results.IResults;
import soot.*;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.scalar.Pair;

import java.util.Collections;
import java.util.Set;

/**
 * Wrapper class for {@link IFDSResourceLeak}.
 * @see IFDSResourceLeak
 */
public class IFDSRLAnalysis implements IAnalysis{

    private IFDSSolver<Unit, Pair<ResourceInfo, Local>, SootMethod, InterproceduralCFG<Unit, SootMethod>> solver;
    private IDESolver<Unit, Pair<ResourceInfo, Local>, SootMethod, Set<Pair<ResourceInfo, Local>>, InterproceduralCFG<Unit, SootMethod>> solver2;

    public void doAnalysis(boolean debug) {
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();

        IFDSTabulationProblem<Unit, Pair<ResourceInfo, Local>,
                SootMethod,
                InterproceduralCFG<Unit, SootMethod>> problem = new IFDSResourceLeak(icfg);

        this.solver = new IFDSSolver<>(problem);
        solver.solve();

        /*
        IDETabulationProblem<Unit, Pair<ResourceInfo, Local>,
                SootMethod, Set<Pair<ResourceInfo, Local>>,
                InterproceduralCFG<Unit, SootMethod>> problem2 = new IDEResourceLeak(icfg);

        this.solver2 = new IDESolver<>(problem2);
        solver2.solve();
         */

        if (debug) {
            for (SootClass c : Scene.v().getApplicationClasses()) {
                for (SootMethod m : c.getMethods()) {
                    if (m.hasActiveBody()) {
                        System.out.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                        System.out.println(m.getName() + " | " + c.getName());
                        for (Unit stmt : m.getActiveBody().getUnits()) {
                            System.out.print(stmt + "\n   |-- ");
                            //Map<Pair<ResourceInfo, Set<Local>>, Set<Pair<ResourceInfo, Set<Local>>>> res = solver2.resultsAt(stmt);
                            Set<Pair<ResourceInfo, Local>> res = solver.ifdsResultsAt(stmt);
                            System.out.println(res);
                        }
                    }
                }
            }
        }
    }

    public Set<Pair<ResourceInfo, Local>> getResultsAtStmt(Unit stmt) {
        return Collections.unmodifiableSet(solver.ifdsResultsAt(stmt));
    }

    @Override
    public void accept(IAnalysisVisitor visitor, IResults storage) {
        visitor.visit(this, storage);
    }
}
