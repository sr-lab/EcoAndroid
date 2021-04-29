package leaks;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;

import java.util.*;

/**
 * Implements a intra-procedural resource leak analysis (of Android resources)
 * A resource is leaked when it is acquired, but never released.
 * Takes into consideration branching when a resource is not acquired (r==null)
 */
public class RLAnalysis extends ForwardBranchedFlowAnalysis<FlowSet<Local>> {
    protected final FlowSet<Local> emptySet;
    //the results used during analysis, using Set instead of FlowSet causes bugs
    private final FlowSet<Local> results;
    //the results but in a Set
    private final Set<Local> setResults;
    private final UnitGraph graph;

    public RLAnalysis(UnitGraph dg) {
        super(dg);

        graph = dg;
        emptySet = new ArraySparseSet<>();
        results = new ArraySparseSet<>();
        setResults = new HashSet<>();

        if (hasResources()) {
            doAnalysis();
            for (Local resource : results) {
                setResults.add(resource);
            }
        }
    }

    public FlowSet<Local> getFlowSetResults() { return results; }

    public Set<Local> getResults() { return setResults; }

    /**
     * Helper function to remove facts from FlowSets
     * @param setList the List of FlowSet
     * @param local the element to remove
     */
    private void removeFromSets(List<FlowSet<Local>> setList, Local local) {
        Iterator<FlowSet<Local>> setIt = setList.iterator();
        while (setIt.hasNext()) {
            FlowSet<Local> branchSet = setIt.next();
            branchSet.remove(local);
        }
    }

    /**
     * Helper function to add facts to FlowSets
     * @param setList the List of FlowSet
     * @param local the element to add
     */
    private void addToSets(List<FlowSet<Local>> setList, Local local) {
        Iterator<FlowSet<Local>> setIt = setList.iterator();
        while (setIt.hasNext()) {
            FlowSet<Local> fallSet = setIt.next();
            fallSet.add(local);
        }
    }

    protected void flowThrough(FlowSet<Local> in, Unit unit, List<FlowSet<Local>> fallOut, List<FlowSet<Local>> branchOut) {
        //System.out.println("falls: " + u.fallsThrough() + " | branches: " + u.branches());

        /* Because return stmt ends the control flow, they don't have fallOut nor branchOut sets
         * (although Soot has an OUT set for return stmts in ForwardFlowAnalysis)
         * So, to store our results, we directly insert the IN set's contents of return stmts
         * into our result set (simulating an OUT set for return stmts)
         */
        if (unit instanceof ReturnStmt || unit instanceof ReturnVoidStmt) {
            results.union(in);
            return;
        }

        {
            Iterator<FlowSet<Local>> fallOutIt = fallOut.iterator();
            while (fallOutIt.hasNext()) {
                FlowSet<Local> fallSet = fallOutIt.next();
                fallSet.union(in);
            }
            Iterator<FlowSet<Local>> branchOutIt = branchOut.iterator();
            while (branchOutIt.hasNext()) {
                FlowSet<Local> branchSet = branchOutIt.next();
                branchSet.union(in);
            }
        }

        /* if stmts are important, e.g.:
                r = null;
                try {
                    ...
                    r = acquire()
                } catch (Exception) {
                    ...
                } finally {
                    if (r != null) {
                        r.release()
                    }
                }
         * to see if the resource was acquired
         * We need to have this in mind - branches might require different facts
         * In this case there isn't a resource leak because a check is being done
         */
        if (unit instanceof IfStmt) {
            IfStmt stmt = (IfStmt) unit;
            ConditionExpr cond = (ConditionExpr) stmt.getCondition();
            Value lhs = cond.getOp1();

            if (lhs instanceof Local) {
                Local local = (Local) lhs;

                //sanity check
                //releasing resource only makes sense if it was acquired (= is in IN set)
                if (in.contains(local)) {
                    String condSymbol = cond.getSymbol();
                    Value rhs = cond.getOp2();

                    if (rhs instanceof NullConstant) {
                        if (condSymbol.equals(" == ")) {
                            removeFromSets(branchOut, local);
                        } else if (condSymbol.equals(" != ")) {
                            removeFromSets(fallOut, local);
                        }
                    }
                }
            }
        } // end if stmt check

        else if (unit instanceof AssignStmt) {
            AssignStmt stmt = (AssignStmt) unit;
            Value rhs = stmt.getRightOp();

            if (rhs instanceof InvokeExpr) {
                InvokeExpr expr = (InvokeExpr) rhs;
                SootMethod meth = expr.getMethod();

                //we are seeing an acquire op, must add local to the facts
                for (Resource resource : Resource.values()) {
                    if (resource.isBeingAcquired(meth.getName(), meth.getDeclaringClass().getName())) {
                        Value lhs = stmt.getLeftOp();

                        if (lhs instanceof Local) {
                            Local local = (Local) lhs;

                            if (unit.fallsThrough()) {
                                addToSets(fallOut, local);
                            } if (unit.branches()) {
                                addToSets(branchOut, local);
                            }
                        }
                        // only one resource can be acquired in a stmt
                        break;
                    }
                }
            }
        } // end assign stmt check

        //TODO if resources can be released in more than one type of stmt, this might increase perf
        //if (!in.isEmpty()) {
        else if (unit instanceof InvokeStmt) {
            InvokeStmt stmt = (InvokeStmt) unit;
            SootMethod meth = stmt.getInvokeExpr().getMethod();

            for (Resource resource : Resource.values()) {
                if (resource.isBeingReleased(meth.getName(), meth.getDeclaringClass().getName())) {
                    Value value = stmt.getInvokeExprBox().getValue();

                    if (value instanceof InterfaceInvokeExpr) {
                        InterfaceInvokeExpr expr = (InterfaceInvokeExpr) value;
                        Value value2 = expr.getBase();

                        if (value2 instanceof Local) {
                            Local local = (Local) value2;

                            // sanity check
                            // releasing resource only makes sense if it was acquired (= is in IN set)
                            if (in.contains(local)) {
                                if (unit.fallsThrough()) {
                                    removeFromSets(fallOut, local);
                                } if (unit.branches()) {
                                    removeFromSets(branchOut, local);
                                }
                            }
                        }
                    }
                    // only one resource can be released in a stmt
                    break;
                }
            }
        } // end invoke stmt check
    }

    /**
     * Checks if method has resource that can be leaked.
     * Used to increase analysis time performance
     * @returns true if current method has resources that can be leaked, false otherwise
     */
    private boolean hasResources() {
        for (Local local : graph.getBody().getLocals()) {
            for (Resource resource : Resource.values()) {
                if (local.getType().toString().equals(resource.acquireClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected FlowSet<Local> newInitialFlow() {
        return emptySet.emptySet();
    }

    protected FlowSet<Local> entryInitialFlow() {
        return emptySet.emptySet();
    }

    protected void merge(FlowSet<Local> in1, FlowSet<Local> in2, FlowSet<Local> out) {
        in1.union(in2, out);
    }

    protected void copy(FlowSet<Local> src, FlowSet<Local> dest) {
        src.copy(dest);
    }

    // *** NOTE: this is here because ForwardBranchedFlowAnalysis does
    // *** not handle exceptional control flow properly in the
    // *** dataflow analysis. this should be removed when
    // *** ForwardBranchedFlowAnalysis is fixed.
    protected boolean treatTrapHandlersAsEntries() {
        return true;
    }
}