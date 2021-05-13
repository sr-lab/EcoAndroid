package leaks;

import java.util.HashMap;
import java.util.Map;

import soot.*;
import soot.jimple.*;
import soot.toolkits.scalar.Pair;
import vasco.Context;
import vasco.ForwardInterProceduralAnalysis;
import vasco.ProgramRepresentation;
import vasco.soot.DefaultJimpleRepresentation;

/**
 * An inter-procedural resource leak analysis.
 */
public class VascoRLAnalysis
        extends ForwardInterProceduralAnalysis<SootMethod, Unit, Map<FieldInfo, Pair<Local, Boolean>>>
        implements IAnalysis {

    public VascoRLAnalysis() {
        super();
        verbose = true;

        //doAnalysis();
    }

    @Override
    public Map<FieldInfo, Pair<Local, Boolean>> normalFlowFunction(Context<SootMethod, Unit, Map<FieldInfo, Pair<Local, Boolean>>> context, Unit unit,
                                                   Map<FieldInfo, Pair<Local, Boolean>> inValue) {
        System.out.println("normal:" + context.getMethod().getDeclaringClass().getName()+"."+context.getMethod().getName());
        System.out.println(unit);

        if (context.getMethod().getDeclaringClass().getName().equals("org.connectbot.ConsoleActivity") &&
                (context.getMethod().getName().equals("onStart") || context.getMethod().getName().equals("onStop"))) {
            Map<FieldInfo, Pair<Local, Boolean>> entryValue = context.getEntryValue();
            System.out.println("");
        }

        Map<FieldInfo, Pair<Local, Boolean>> out = copy(inValue);


        if (unit instanceof ReturnStmt || unit instanceof ReturnVoidStmt) {
            // Clean up any tagged resource that were declared but not acquired
            for (Map.Entry<FieldInfo, Pair<Local, Boolean>> entry : out.entrySet()) {
                if (entry.getValue().getO2() == false) {
                    out.remove(entry.getKey());
                }
            }
        } // End check return stmt

        // $rx = r0.<Resource r> (assignment from InstanceFieldRef to Local)
        else if (unit instanceof AssignStmt) {
            AssignStmt stmt = (AssignStmt) unit;
            Value rhs = stmt.getRightOp();

            if (rhs instanceof InstanceFieldRef) {
                InstanceFieldRef fieldRef = (InstanceFieldRef) stmt.getRightOp();
                SootField field = fieldRef.getField();

                for (Resource r : Resource.values()) {
                    if (r.isBeingDeclared(field.getType().toString())) {

                        // FIX casts without check
                        // We create new facts because each node needs to have its own info,
                        // changing it could damage the analysis
                        Pair<Local, Boolean> state;
                        FieldInfo fieldInfo = new FieldInfo(field.getName(), field.getDeclaringClass().getName(), field.getType().toString(), r);
                        if (inValue.containsKey(fieldInfo)) { // This specific resource has already been seen, keep info about acquired state
                            state = new Pair<>((Local) stmt.getLeftOp(), inValue.get(fieldInfo).getO2());
                        } else { // First time seeing this resource, so it is not yet acquired
                            state = new Pair<>((Local) stmt.getLeftOp(), false);
                        }
                        out.put(fieldInfo, state);
                        break;
                    }
                }
            }
        } // End invoke stmt check

        // Similar to intra-procedural check, but because we don't have branched analysis
        // we do a naive check: if there are release operations after an 'if' to see if a resource
        // is null, we then assume the developer released the resource correctly
        else if (unit instanceof IfStmt) {
            IfStmt stmt = (IfStmt) unit;
            ConditionExpr cond = (ConditionExpr) stmt.getCondition();
            Value lhs = cond.getOp1();

            if (lhs instanceof Local) {
                Local local = (Local) lhs;

                for (Map.Entry<FieldInfo, Pair<Local, Boolean>> entry : out.entrySet()) {
                    //FIX "missing" context check, local check might be enough (they are uniquely id)
                    if (entry.getValue().getO1().equivTo(local)
                            && existsReleaseOpInIfFlow(context, unit, entry.getKey().getResource())) {
                        out.remove(entry.getKey());
                    }
                }
            }
        } // End if stmt check

        return out;
    }

    @Override
    public Map<FieldInfo, Pair<Local, Boolean>> callEntryFlowFunction(
            Context<SootMethod, Unit, Map<FieldInfo, Pair<Local, Boolean>>> context,
            SootMethod calledMethod, Unit unit, Map<FieldInfo, Pair<Local, Boolean>> inValue) {

        System.out.println("callEntry:" + context.getMethod().getDeclaringClass().getName()+"."+context.getMethod().getName());
        System.out.println(unit);

        if (context.getMethod().getDeclaringClass().getName().equals("org.connectbot.ConsoleActivity") &&
                (context.getMethod().getName().equals("onStart") || context.getMethod().getName().equals("onStop"))) {
            Map<FieldInfo, Pair<Local, Boolean>> entryValue = context.getEntryValue();
            System.out.print("");
        }

        if (unit.toString().equals("virtualinvoke $r0.<org.connectbot.ConsoleActivity: void onPause()>()")) {
            if (context.getMethod().getName().equals("dummyMainMethod_org_connectbot_ConsoleActivity")) {
                System.out.print("");
            }
        }

        Map<FieldInfo, Pair<Local, Boolean>> out = copy(inValue);
        return out;
    }

    @Override
    public Map<FieldInfo, Pair<Local, Boolean>> callExitFlowFunction(
            Context<SootMethod, Unit, Map<FieldInfo, Pair<Local, Boolean>>> context, SootMethod calledMethod,
            Unit unit, Map<FieldInfo, Pair<Local, Boolean>> exitValue) {
        System.out.println("callExit:" + context.getMethod().getDeclaringClass().getName()+"."+context.getMethod().getName());
        System.out.println(unit);

        if (context.getMethod().getDeclaringClass().getName().equals("org.connectbot.ConsoleActivity") &&
                (context.getMethod().getName().equals("onPause"))) {
            Map<FieldInfo, Pair<Local, Boolean>> entryValue = context.getEntryValue();
            System.out.println("");
        }

        if (context.getMethod().getName().equals("dummyMainMethod_org_connectbot_ConsoleActivity")) {
            if (unit.toString().equals("virtualinvoke $r0.<org.connectbot.ConsoleActivity: void onPause()>()")
            || unit.toString().equals("virtualinvoke $r0.<org.connectbot.ConsoleActivity: void onResume()>()")) {
                System.out.println("");
            }
        }

        Map<FieldInfo, Pair<Local, Boolean>> out = copy(exitValue);

        /*
        // TODO dar OUT ao que tiver no return do invoke unit
        if (unit instanceof InvokeStmt) {
            InvokeStmt stmt = (InvokeStmt) unit;
            InvokeExpr invokeExpr = stmt.getInvokeExpr();

            if (invokeExpr instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
                SootMethod invokedMethod = virtualInvokeExpr.getMethod();

                if (invokedMethod.hasActiveBody()) {
                    Unit returnStmt = invokedMethod.getActiveBody().getUnits().getLast();
                    out = copy(getMeetOverValidPathsSolution().getValueAfter(unit));
                }
            }
        }

         */

        return out;

        /*
        // Initialise result to an empty value
        Map<Local, Constant> afterCallValue = topValue();
        // Only propagate constants for return values
        if (unit instanceof AssignStmt) {
            Value lhsOp = ((AssignStmt) unit).getLeftOp();
            assign((Local) lhsOp, RETURN_LOCAL, exitValue, afterCallValue);
        }
        // Return the map with the returned value's constant
        return afterCallValue;

         */
    }

    @Override
    public Map<FieldInfo, Pair<Local, Boolean>> callLocalFlowFunction(
            Context<SootMethod, Unit, Map<FieldInfo, Pair<Local, Boolean>>> context,
            Unit unit, Map<FieldInfo, Pair<Local, Boolean>> inValue) {
        System.out.println("callLocal:" + context.getMethod().getDeclaringClass().getName()+"."+context.getMethod().getName());
        System.out.println(unit);

        if (context.getMethod().getDeclaringClass().getName().equals("org.connectbot.ConsoleActivity") &&
                (context.getMethod().getName().equals("onStart") || context.getMethod().getName().equals("onStop"))) {
            Map<FieldInfo, Pair<Local, Boolean>> entryValue = context.getEntryValue();
            System.out.println("");
        }

        if (context.getMethod().getName().equals("dummyMainMethod_org_connectbot_ConsoleActivity")) {
            if (unit.toString().equals("virtualinvoke $r0.<org.connectbot.ConsoleActivity: void onPause()>()")) {
                System.out.println("");
            }
        }

        Map<FieldInfo, Pair<Local, Boolean>> out = copy(inValue);

        if (unit instanceof InvokeStmt) {
            InvokeStmt stmt = (InvokeStmt) unit;
            InvokeExpr invokeExpr = stmt.getInvokeExpr();

            if (invokeExpr instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
                SootMethod invokedMethod = virtualInvokeExpr.getMethod();
                Value base = virtualInvokeExpr.getBase();

                if (base instanceof Local) {
                    Local local = (Local) base;

                    if (invokedMethod.getName().matches("onPause|onStart|onStop|onDestroy|onRestart")) {
                        Unit u = invokedMethod.getActiveBody().getUnits().getLast();
                        HashMap<FieldInfo, Pair<Local, Boolean>> after =
                                (HashMap<FieldInfo, Pair<Local, Boolean>>) this.getMeetOverValidPathsSolution().getValueAfter(u);
                        HashMap<FieldInfo, Pair<Local, Boolean>> before =
                                (HashMap<FieldInfo, Pair<Local, Boolean>>) this.getMeetOverValidPathsSolution().getValueBefore(u);
                        out = new HashMap<>(after);
                    }

                    for (Resource r : Resource.values()) {
                        if (r.isBeingAcquired(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {
                            for (Map.Entry<FieldInfo, Pair<Local, Boolean>> entry : out.entrySet()) {
                                //FIX "missing" context check, local check might be enough (they are uniquely id)
                                if (entry.getValue().getO1().equivTo(local)) {
                                    Pair<Local, Boolean> newState = new Pair<>(local, true);
                                    out.put(entry.getKey(), newState);
                                    break;
                                }
                            }
                            break;
                        } else if (r.isBeingReleased(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {
                            for (Map.Entry<FieldInfo, Pair<Local, Boolean>> entry : out.entrySet()) {
                                //FIX "missing" context check, local check might be enough (they are uniquely id)
                                if (entry.getValue().getO1().equivTo(local)) {
                                    out.remove(entry.getKey());
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        /*
        // Initialise result to the input
        Map<Local, Constant> afterCallValue = copy(inValue);
        // Remove information for return value (as it's value will flow from the call)
        if (unit instanceof AssignStmt) {
            Value lhsOp = ((AssignStmt) unit).getLeftOp();
            afterCallValue.remove(lhsOp);
        }
        // Rest of the map remains the same
        return afterCallValue;
         */

        return out;
    }

    // TODO FIX else return probably cutting off paths
    private boolean existsReleaseOpInIfFlow(Context<SootMethod, Unit, Map<FieldInfo, Pair<Local, Boolean>>> context,
                                            Unit unit, Resource resource) {
        for (Unit u : context.getControlFlowGraph().getSuccsOf(unit)) {
            if (u instanceof InvokeStmt) {
                InvokeStmt stmt = (InvokeStmt) u;
                InvokeExpr invokeExpr = stmt.getInvokeExpr();

                if (invokeExpr instanceof VirtualInvokeExpr) {
                    VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
                    SootMethod invokedMethod = virtualInvokeExpr.getMethod();
                    Value base = virtualInvokeExpr.getBase();

                    if (resource.isBeingReleased(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {
                        return true;
                    }

                }
            } else {
                return existsReleaseOpInIfFlow(context, u, resource);
            }
        }

        return false;
    }

    @Override
    public Map<FieldInfo, Pair<Local, Boolean>> boundaryValue(SootMethod method) {
        return topValue();
    }

    @Override
    public Map<FieldInfo, Pair<Local, Boolean>> copy(Map<FieldInfo, Pair<Local, Boolean>> src) {
        return new HashMap<FieldInfo, Pair<Local, Boolean>>(src);
    }

    @Override
    public Map<FieldInfo, Pair<Local, Boolean>> meet(Map<FieldInfo, Pair<Local, Boolean>> op1, Map<FieldInfo, Pair<Local, Boolean>> op2) {
        Map<FieldInfo, Pair<Local, Boolean>> result;
        // Add all from one map
        result = new HashMap<FieldInfo, Pair<Local, Boolean>>(op1);
        // Take care of entries in second map
        for (FieldInfo x : op2.keySet()) {
            if (op1.containsKey(x)) {
                Pair<Local, Boolean> p1 = op1.get(x);
                Pair<Local, Boolean> p2 = op2.get(x);

                // The resource is leaked, it is added as leaked
                if (p2.getO2() == true) {
                    result.put(x, p2);
                } else {
                    result.put(x, p1);
                }
            } else {
                result.put(x, op2.get(x));
            }
        }

        return result;
    }

    /**
     * Returns an empty map.
     */
    @Override
    public Map<FieldInfo, Pair<Local, Boolean>> topValue() {
        return new HashMap<FieldInfo, Pair<Local, Boolean>>();
    }

    /**
     * Returns a default jimple representation.
     * @see DefaultJimpleRepresentation
     */
    @Override
    public ProgramRepresentation<SootMethod, Unit> programRepresentation() {
        return DefaultJimpleRepresentation.v();
    }

    @Override
    public void accept(IResultsProcessor resultsProcessor) {
        resultsProcessor.visit(this);
    }
}

