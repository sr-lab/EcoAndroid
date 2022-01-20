package leaks.analysis;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

/**
 * Implements a very basic intra-procedural resource leak analysis (of Android resources)
 * A resource is leaked when it is acquired, but never released
 * Does not account for branches (if resource==null) and so has a lot of false positives
 * Outdated, needs refactoring - use FlowSets, use Resource
 */
public class BasicResourceLeakAnalysis extends ForwardFlowAnalysis<Unit, Set<Local>> {
    private Set<Local> results;

    public BasicResourceLeakAnalysis(DirectedGraph<Unit> dg) {
        super(dg);
        results = new HashSet<Local>();

        doAnalysis();

        //gathers all resources leaked when the analysis ends
        //a resource is leaked (intra-procedurally) when in a return stmt
        //there is a fact with that resource
        Iterator i = dg.iterator();
        while (i.hasNext()) {
            Unit u = (Unit) i.next();
            if (u instanceof ReturnStmt) {
                results.addAll(getFlowAfter(u));
            }
        }

    }

    public Set<Local> getResults() {
        return Collections.unmodifiableSet(results);
    }

    /* Assumes there is no "switching" between vars (e.g. a=x.acquire, b=a, b=x.release)
     * (does IR take care of that already?)
     */
    protected void flowThrough(Set<Local> in, Unit u, Set<Local> out) {
        String cursorClass = "android.database.sqlite.SQLiteDatabase";
        String cursorClass2 = "android.database.Cursor";
        String cursorAcquire = "rawQuery";
        String cursorRelease = "close";

        //out = new HashSet<Local>(in);
        out.addAll(in);

        if (u instanceof IfStmt) {
            IfStmt stmt = (IfStmt) u;
            System.out.println("");
        }

        //unit must be an assignment, either acquire or release op
        if (u instanceof AssignStmt) {
            AssignStmt stmt = (AssignStmt) u;
            Value rhs = stmt.getRightOp();

            if (rhs instanceof InvokeExpr) {
                InvokeExpr expr = (InvokeExpr) rhs;
                SootMethod meth = expr.getMethod();

                //we are seeing an acquire op, must add local to the facts
                if (meth.getDeclaringClass().getName().equals(cursorClass) &&
                        meth.getName().equals(cursorAcquire)) {
                    Value lhs = stmt.getLeftOp();
                    if (lhs instanceof Local) {
                        Local local = (Local) lhs;
                        out.add(local);
                        //System.out.println("ACQUIRE CURSOR");
                    }
                }
            }
        }

        //we are seeing a resource being released, must remove local from facts
        else if (u instanceof InvokeStmt) {
            InvokeStmt stmt = (InvokeStmt) u;
            SootMethod meth = stmt.getInvokeExpr().getMethod();

            if (meth.getDeclaringClass().getName().equals(cursorClass2) &&
                    meth.getName().equals(cursorRelease)) {
                Value value = stmt.getInvokeExprBox().getValue();

                if (value instanceof InterfaceInvokeExpr) {
                    InterfaceInvokeExpr expr = (InterfaceInvokeExpr) value;
                    Value value2 = expr.getBase();

                    if (value2 instanceof Local) {
                        Local local = (Local) value2;
                        out.remove(local);
                    }
                }
            }

        }
    }

    protected Set<Local> newInitialFlow() {
        return new HashSet<Local>();
    }

    protected Set<Local> entryInitialFlow() { return new HashSet<Local>(); }

    protected void merge(Set<Local> in1, Set<Local> in2, Set<Local> out) {
        out.addAll(in1);
        out.addAll(in2);
    }

    protected void copy(Set<Local> src, Set<Local> dest) {
        dest = new HashSet<Local>(src);
    }
}