package leaks;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1997 - 2013 Eric Bodden and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Identity;

import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.toolkits.scalar.Pair;

public class IFDSTest
        extends DefaultJimpleIFDSTabulationProblem<Pair<FactId, Local>, InterproceduralCFG<Unit, SootMethod>> {

    InterproceduralCFG<Unit, SootMethod> icfg;
    public IFDSTest(InterproceduralCFG<Unit, SootMethod> icfg) {
        super(icfg);
        this.icfg = icfg;
    }

    @Override
    public FlowFunctions<Unit, Pair<FactId, Local>, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, Pair<FactId, Local>, SootMethod>() {
            @Override
            public FlowFunction<Pair<FactId, Local>> getNormalFlowFunction(final Unit curr, Unit succ) {
                System.out.println("-------------------------------------\n"
                        + "normalFlow: "
                        + icfg.getMethodOf(curr).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(curr).getName() + "\n"
                        + curr);

                if (curr.toString().equals("$z0 = r0.<org.connectbot.ConsoleActivity: boolean forcedOrientation>")
                || curr.toString().equals("$z0 = virtualinvoke $r2.<android.os.PowerManager$WakeLock: boolean isHeld()>()")) {
                    List<Unit> pred = icfg.getPredsOf(curr);
                    System.out.println("");
                }


                if (curr instanceof AssignStmt) {
                    AssignStmt stmt = (AssignStmt) curr;
                    Value rhs = stmt.getRightOp();

                    if (rhs instanceof InstanceFieldRef) {
                        InstanceFieldRef fieldRef = (InstanceFieldRef) stmt.getRightOp();
                        SootField field = fieldRef.getField();

                        Local lhs = (Local) stmt.getLeftOp();

                        for (Resource r : Resource.values()) {
                            if (r.isBeingDeclared(field.getType().toString())) {

                                // An invoke stmt on a local representing a resource is usually preceded by
                                // an assignment of the resource to the same local:
                                // $r1 = r0.<resource>
                                // $virtualinvoke r1.<resource: acquire()>
                                if (succ instanceof InvokeStmt) {
                                    InvokeStmt invokeStmt = (InvokeStmt) succ;
                                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                                    if (invokeExpr instanceof VirtualInvokeExpr) {
                                        VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
                                        SootMethod invokedMethod = virtualInvokeExpr.getMethod();
                                        Value base = virtualInvokeExpr.getBase();

                                        if (base instanceof Local) {
                                            Local local = (Local) base;

                                            return new FlowFunction<Pair<FactId, Local>>() {
                                                @Override
                                                public Set<Pair<FactId, Local>> computeTargets(Pair<FactId, Local> source) {
                                                    SootMethod debug = icfg.getMethodOf(curr);
                                                    if (!equalsZeroValue(source)) {
                                                        // If resource being released in succ
                                                        if (r.isBeingReleased(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())
                                                        && source.getO1().getName().equals(field.getName())) {
                                                            return Collections.emptySet();
                                                        }
                                                        //return Collections.singleton(source);
                                                    } else if (r.isBeingAcquired(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {
                                                        LinkedHashSet<Pair<FactId, Local>> res = new LinkedHashSet<>();
                                                        res.add(new Pair<>(
                                                                new FactId(field.getName(), r, field.getDeclaringClass(), icfg.getMethodOf(curr)),
                                                                lhs));
                                                        return res;
                                                    }
                                                    return Collections.emptySet();
                                                    //return Collections.singleton(source);
                                                }
                                            };

                                        }
                                    }
                                // The resource is only being assigned and not acquired
                                } else {
                                    return new FlowFunction<Pair<FactId, Local>>() {
                                        @Override
                                        public Set<Pair<FactId, Local>> computeTargets(Pair<FactId, Local> source) {
                                            SootMethod debug = icfg.getMethodOf(curr);
                                            // We only care about tracking acquired resources
                                            if (source.getO1().getName().equals(field.getName())  //) {
                                                    && !lhs.equivTo(source.getO2())) {
                                                // Update the "current" local because of If check
                                                return Collections.singleton(
                                                        new Pair<>(source.getO1(), lhs));
                                            } else {
                                                return Collections.singleton(source);
                                                //experimental
                                                //return Collections.emptySet();
                                            }
                                        }
                                    };
                                }


                            }
                        }
                    }
                } // End assign stmt check

                else if (curr instanceof IfStmt) {
                    IfStmt stmt = (IfStmt) curr;
                    ConditionExpr cond = (ConditionExpr) stmt.getCondition();
                    Value lhs = cond.getOp1();

                    if (lhs instanceof Local) {
                        Local local = (Local) lhs;

                        // In case we are in an "if" a resource is held via
                        // its proper "isHeld" method
                        for (Unit pred : icfg.getPredsOf(curr)) {
                            if (pred instanceof AssignStmt) {
                                AssignStmt assignStmt = (AssignStmt) pred;

                                if (assignStmt.getLeftOp() instanceof Local) {
                                    Local predLhs = (Local) assignStmt.getLeftOp();
                                    //TODO perf check for boolean local
                                    if (predLhs.equivTo(lhs)) { // Sanity check :)
                                        if (assignStmt.getRightOp() instanceof VirtualInvokeExpr) {
                                            VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr) assignStmt.getRightOp();
                                            SootMethod invokedMethod = invokeExpr.getMethod();
                                            Value base = invokeExpr.getBase();

                                            for (Resource r : Resource.values()) {
                                                if (r.isCheckedIfItsHeld(base.getType().toString(), invokedMethod.getName())) {
                                                    return new FlowFunction<Pair<FactId, Local>>() {
                                                        @Override
                                                        public Set<Pair<FactId, Local>> computeTargets(Pair<FactId, Local> source) {
                                                            if (source.getO2().equivTo(base)) {
                                                                // The if will branch and its a about a seen resource
                                                                if (stmt.getTarget().equals(succ)) {
                                                                    String condSymbol = cond.getSymbol();
                                                                    Value rhs = cond.getOp2();

                                                                    // We are branching, and the resource is "null",
                                                                    // so we remove facts related to that resource
                                                                    if (rhs instanceof IntConstant) {
                                                                        if (condSymbol.equals(" == ")
                                                                        && ((IntConstant) rhs).value == 0) {
                                                                            return Collections.emptySet();
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            return Collections.singleton(source);
                                                        }
                                                    };
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return new FlowFunction<Pair<FactId, Local>>() {
                            @Override
                            public Set<Pair<FactId, Local>> computeTargets(Pair<FactId, Local> source) {
                                SootMethod debug = icfg.getMethodOf(curr);
                                SootMethod debug2 = icfg.getMethodOf(succ);

                                if (source.getO2().equivTo(local)) {
                                    // The if will branch and its a about a seen resource
                                    if (stmt.getTarget().equals(succ)) {
                                        String condSymbol = cond.getSymbol();
                                        Value rhs = cond.getOp2();

                                        // We are branching, and the resource is "null",
                                        // so we remove facts related to that resource
                                        if (rhs instanceof NullConstant) {
                                            if (condSymbol.equals(" == ")) {
                                                return Collections.emptySet();
                                            }
                                        }
                                    }
                                }
                                return Collections.singleton(source);
                            }
                        };

                    }
                }

                return Identity.v();
            }

            @Override
            public FlowFunction<Pair<FactId, Local>> getCallFlowFunction(Unit callStmt,
                                                                              final SootMethod destinationMethod) {
                System.out.println("-------------------------------------\n"
                        + "callFlow: "
                        + icfg.getMethodOf(callStmt).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(callStmt).getName() + "\n"
                        + callStmt);

                if (callStmt.toString().equals("$z0 = r0.<org.connectbot.ConsoleActivity: boolean forcedOrientation>")) {
                    List<Unit> preds = icfg.getPredsOf(callStmt);
                    System.out.print("");
                }

                return new FlowFunction<Pair<FactId, Local>>() {
                    @Override
                    public Set<Pair<FactId, Local>> computeTargets(Pair<FactId, Local> source) {
                        if (!equalsZeroValue(source)) {
                            String destMethodClassName = destinationMethod.getDeclaringClass().getName();
                            String sourceDeclClassName = source.getO1().getDeclaringClass().getName();
                            if (!destMethodClassName.equals(sourceDeclClassName)) {
                                return Collections.emptySet();
                            }
                        }
                        return Collections.singleton(source);
                    }
                };

                //return Identity.v();
            }

            @Override
            public FlowFunction<Pair<FactId, Local>> getReturnFlowFunction(final Unit callSite,
                                                                                SootMethod calleeMethod, final Unit exitStmt, Unit returnSite) {
                System.out.println("-------------------------------------\n"
                        + "returnFlow: "
                        + icfg.getMethodOf(callSite).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(callSite).getName() + "\n"
                        + callSite);



                /*
                if (exitStmt instanceof ReturnStmt || exitStmt instanceof ReturnVoidStmt) {
                    return new FlowFunction<Pair<FactId, Local>>() {
                        @Override
                        public Set<Pair<FactId, Local>> computeTargets(Pair<FactId, Local> source) {
                            if (!equalsZeroValue(source)) {
                                return Collections.emptySet();
                            }

                            return Collections.singleton(source);
                        }
;                    };
                }

                 */

                return Identity.v();
            }

            @Override
            public FlowFunction<Pair<FactId, Local>> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                System.out.println("-------------------------------------\n"
                        + "callToReturnFlow: "
                        + icfg.getMethodOf(callSite).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(callSite).getName() + "\n"
                        + callSite);

                if (callSite.toString().equals("$z0 = r0.<org.connectbot.ConsoleActivity: boolean forcedOrientation>")) {
                    List<Unit> preds = icfg.getPredsOf(callSite);
                    System.out.print("");
                }

                /*

                if (callSite instanceof InvokeStmt) {
                    InvokeStmt stmt = (InvokeStmt) callSite;
                    InvokeExpr invokeExpr = stmt.getInvokeExpr();

                    if (invokeExpr instanceof VirtualInvokeExpr) {
                        VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
                        SootMethod invokedMethod = virtualInvokeExpr.getMethod();
                        Value base = virtualInvokeExpr.getBase();

                        if (base instanceof Local) {
                            Local local = (Local) base;

                            return new FlowFunction<Pair<FactId, Local>>() {
                                @Override
                                public Set<Pair<FactId, Local>> computeTargets(Pair<FactId, Local> source) {
                                    LinkedHashSet<Pair<FactId, Local>> res = new LinkedHashSet<>();
                                    SootMethod m = icfg.getMethodOf(returnSite);

                                    for (Resource r : Resource.values()) {
                                        if (r.isBeingAcquired(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {


                                        } else if (r.isBeingReleased(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {

                                        }
                                    }
                                    return Collections.emptySet();

                                    //return res;
                                }
                            };
                        }
                    }
                }

                 */

                return Identity.v();
            }
        };
    }

    public Map<Unit, Set<Pair<FactId, Local>>> initialSeeds() {
        return DefaultSeeds.make(Collections.singleton(Scene.v().getMainMethod().getActiveBody().getUnits().getFirst()),
                zeroValue());
    }

    public Pair<FactId, Local> createZeroValue() {
        return new Pair<FactId, Local>(
                new FactId("<<zero>>", null, null, null),
                new JimpleLocal("<<zero>>", NullType.v()
                ));
    }

    private boolean equalsZeroValue(Pair<FactId, Local> fact) {
        return fact.getO1().getName().equals("<<zero>>");
    }

    private boolean existsReleaseOpInIfFlow(Unit unit, Resource resource) {

        for (Unit u : icfg.getSuccsOf(unit)) {
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
                // else if (unit instanceof ReturnStmt) return false
            } else {
                return existsReleaseOpInIfFlow(u, resource);
            }
        }
        // return existsReleaseOpInIfFlow(context, u, resource);
        return false;
    }

    private boolean wasLocalAssignedToInstaceField(Local local, SootMethod method) {
        /*
        Body b = method.retrieveActiveBody();
        List<ValueBox> defs = b.getDefBoxes();
        for (ValueBox box : defs) {
            if (box.canContainValue(local)) {
                if (box.getValue() instanceof AssignStmt) {

                }
            }
        }
         */
        return false;
    }
}
