package leaks;

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

public class IFDSResourceLeak
        extends DefaultJimpleIFDSTabulationProblem<Pair<ResourceInfo, Local>, InterproceduralCFG<Unit, SootMethod>> {

    InterproceduralCFG<Unit, SootMethod> icfg;
    public IFDSResourceLeak(InterproceduralCFG<Unit, SootMethod> icfg) {
        super(icfg);
        this.icfg = icfg;
    }

    @Override
    public FlowFunctions<Unit, Pair<ResourceInfo, Local>, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, Pair<ResourceInfo, Local>, SootMethod>() {
            @Override
            public FlowFunction<Pair<ResourceInfo, Local>> getNormalFlowFunction(final Unit curr, Unit succ) {
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

                    // Case where we are dealing with a class member resource

                    // The trick here is to look ahead - we first check for any resource declaration,
                    // what we do depends on the next unit (succ):
                    // -> if its an invoke stmt, then it can be acquired or released
                    // -> if not, then the resource is simply being declared and will be
                    //    used for something else
                    if (rhs instanceof InstanceFieldRef) {
                        InstanceFieldRef fieldRef = (InstanceFieldRef) stmt.getRightOp();
                        SootField field = fieldRef.getField();
                        Local lhs = (Local) stmt.getLeftOp();

                        for (Resource r : Resource.values()) {
                            if (r.isBeingDeclared(field.getType().toString())) {

                                // An invoke stmt on a local representing a resource is usually preceded by
                                // an assignment of the resource to the same local:
                                // $r1 = r0.<resource>                       => assign stmt
                                // $virtualinvoke r1.<resource: acquire()>   => invoke stmt
                                if (succ instanceof InvokeStmt) {
                                    InvokeStmt invokeStmt = (InvokeStmt) succ;
                                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                                    if (invokeExpr instanceof VirtualInvokeExpr) {
                                        VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
                                        SootMethod invokedMethod = virtualInvokeExpr.getMethod();
                                        Value base = virtualInvokeExpr.getBase();

                                        if (base instanceof Local) {
                                            Local local = (Local) base;

                                            return new FlowFunction<Pair<ResourceInfo, Local>>() {
                                                @Override
                                                public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                                    SootMethod debug = icfg.getMethodOf(curr);
                                                    if (!equalsZeroValue(source)) {
                                                        if (r.isBeingReleased(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())
                                                        && source.getO1().getName().equals(field.getName())) {
                                                            return Collections.emptySet();
                                                        }
                                                        //return Collections.singleton(source);
                                                    } else if (r.isBeingAcquired(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {
                                                        LinkedHashSet<Pair<ResourceInfo, Local>> res = new LinkedHashSet<>();
                                                        res.add(new Pair<>(
                                                                new ResourceInfo(field.getName(), r, field.getDeclaringClass(), icfg.getMethodOf(curr)),
                                                                lhs));
                                                        return res;
                                                    }
                                                    //return Collections.emptySet();
                                                    return Collections.singleton(source);
                                                }
                                            };

                                        }
                                    }
                                // The resource is only being assigned and not acquired
                                // This information is crucial for dealing with if stmts
                                } else {
                                    return new FlowFunction<Pair<ResourceInfo, Local>>() {
                                        @Override
                                        public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
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

                // We need to check if stmts because there can be paths
                // where the resource is null or not acquired
                // For those paths we need to kill facts regarding those resources
                else if (curr instanceof IfStmt) {
                    IfStmt stmt = (IfStmt) curr;
                    ConditionExpr cond = (ConditionExpr) stmt.getCondition();
                    Value lhs = cond.getOp1();

                    if (lhs instanceof Local) {
                        Local local = (Local) lhs;

                        // Case for an if stmt where it is being checked
                        // if a resource is being held via the proper operation
                        // e.g. wakelock's isHeld()

                        // Check if the variable seen was the result
                        // of a isHeld operation
                        for (Unit pred : icfg.getPredsOf(curr)) {
                            if (pred instanceof AssignStmt) {
                                AssignStmt assignStmt = (AssignStmt) pred;

                                if (assignStmt.getLeftOp() instanceof Local) {
                                    Local predLhs = (Local) assignStmt.getLeftOp();
                                    //TODO performance -  check for boolean local
                                    if (predLhs.equivTo(lhs)) { // Sanity check :)
                                        if (assignStmt.getRightOp() instanceof VirtualInvokeExpr) {
                                            VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr) assignStmt.getRightOp();
                                            SootMethod invokedMethod = invokeExpr.getMethod();
                                            Value base = invokeExpr.getBase();

                                            for (Resource r : Resource.values()) {
                                                if (r.isCheckedIfItsHeld(base.getType().toString(), invokedMethod.getName())) {
                                                    return new FlowFunction<Pair<ResourceInfo, Local>>() {
                                                        @Override
                                                        public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                                            // The if will branch and its a about a seen resource
                                                            if (stmt.getTarget().equals(succ) && source.getO2().equivTo(base)) {
                                                                String condSymbol = cond.getSymbol();
                                                                Value rhs = cond.getOp2();

                                                                // We are branching, and the resource is not held,
                                                                // so we remove facts related to that resource
                                                                if (rhs instanceof IntConstant) {
                                                                    if (condSymbol.equals(" == ")
                                                                    && ((IntConstant) rhs).value == 0) {
                                                                        return Collections.emptySet();
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

                        // Case where we are simply dealing with a check to
                        // see if the resource is null
                        return new FlowFunction<Pair<ResourceInfo, Local>>() {
                            @Override
                            public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                SootMethod debug = icfg.getMethodOf(curr);
                                SootMethod debug2 = icfg.getMethodOf(succ);

                                // The if will branch and its a about a seen resource
                                if (stmt.getTarget().equals(succ) && source.getO2().equivTo(local)) {
                                    String condSymbol = cond.getSymbol();
                                    Value rhs = cond.getOp2();

                                    // We are branching and the resource is "null",
                                    // so we remove facts related to that resource
                                    if (rhs instanceof NullConstant) {
                                        if (condSymbol.equals(" == ")) {
                                            return Collections.emptySet();
                                        }
                                    }
                                }
                                return Collections.singleton(source);
                            }
                        };
                    }
                } // End if stmt check

                return Identity.v();
            }

            @Override
            public FlowFunction<Pair<ResourceInfo, Local>> getCallFlowFunction(Unit callStmt,
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

                Stmt stmt = (Stmt) callStmt;
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                final List<Value> args = invokeExpr.getArgs();

                final List<Local> localArguments = new ArrayList<Local>(args.size());
                for (Value value : args) {
                    if (value instanceof Local) {
                        localArguments.add((Local) value);
                    } else {
                        localArguments.add(null);
                    }
                }

                return new FlowFunction<Pair<ResourceInfo, Local>>() {
                    @Override
                    public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                        // Case where a resource is being passed by reference to a function
                        // We assume that a resource being passed by reference was declared in
                        // a function, and therefore is NOT a class member! (these can be used
                        // freely and do not need to be passed by reference!)
                        if (!destinationMethod.getName().equals("<clinit>")
                                && !destinationMethod.getSubSignature().equals("void run()")) {
                            if (localArguments.contains(source.getO2()) && !source.getO1().isClassMember()) {
                                int paramIndex = args.indexOf(source.getO2());
                                Pair<ResourceInfo, Local> pair = new Pair<ResourceInfo, Local>(
                                        source.getO1(),
                                        destinationMethod.retrieveActiveBody().getParameterLocal(paramIndex));
                                return Collections.singleton(pair);
                            }

                        // We assume the resources are contained in their classes,
                        // either if they are a member of the class, or a resource declared
                        // in a method being passed by reference
                        // For that reason, it is safe to remove facts regarding a resource not
                        // in its class
                        } else if (!equalsZeroValue(source) && source.getO1().isClassMember()) {
                            String destMethodClassName = destinationMethod.getDeclaringClass().getName();
                            String sourceDeclClassName = source.getO1().getDeclaringClass().getName();
                            if (destMethodClassName.equals(sourceDeclClassName)) {
                                return Collections.singleton(source);
                            }
                        }
                        return Collections.emptySet();
                    }
                };
            }

            @Override
            public FlowFunction<Pair<ResourceInfo, Local>> getReturnFlowFunction(final Unit callSite,
                                                                                 SootMethod calleeMethod, final Unit exitStmt, Unit returnSite) {
                System.out.println("-------------------------------------\n"
                        + "returnFlow: "
                        + icfg.getMethodOf(callSite).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(callSite).getName() + "\n"
                        + callSite);

                // TODO take into account class member getting out of their class (redo callFlow)
                if (exitStmt instanceof ReturnStmt || exitStmt instanceof ReturnVoidStmt || exitStmt instanceof ThrowStmt) {
                    return new FlowFunction<Pair<ResourceInfo, Local>>() {
                        @Override
                        public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                            if (!source.getO1().isClassMember()) {
                                return Collections.emptySet();
                            }
                            return Collections.singleton(source);
                        }
                    };
                }
                return Identity.v();
            }

            @Override
            public FlowFunction<Pair<ResourceInfo, Local>> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                System.out.println("-------------------------------------\n"
                        + "callToReturnFlow: "
                        + icfg.getMethodOf(callSite).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(callSite).getName() + "\n"
                        + callSite + "\n"
                        + returnSite);

                if (callSite.toString().equals("$z0 = r0.<org.connectbot.ConsoleActivity: boolean forcedOrientation>")) {
                    List<Unit> preds = icfg.getPredsOf(callSite);
                    System.out.print("");
                }

                if (callSite.toString().equals("$r6 = virtualinvoke $r3.<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQuery(java.lang.String,java.lang.String[])>($r5, null)")) {
                    System.out.print("");
                }

                // Case where we are dealing with a resource acquired in a method,
                // not a class member.
                if (callSite instanceof AssignStmt) {
                    AssignStmt stmt = (AssignStmt) callSite;
                    Value rhs = stmt.getRightOp();

                    if (rhs instanceof VirtualInvokeExpr) {
                        VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr) rhs;
                        SootMethod invokedMethod = invokeExpr.getMethod();

                        if (stmt.getLeftOp() instanceof Local) {
                            Local lhs = (Local) stmt.getLeftOp();

                            for (Resource r : Resource.values()) {
                                if (r.isBeingAcquired(invokedMethod.getName(), invokedMethod.getDeclaringClass().toString())) {
                                    return new FlowFunction<Pair<ResourceInfo, Local>>() {
                                        @Override
                                        public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                            SootMethod debug = icfg.getMethodOf(callSite);

                                            if (equalsZeroValue(source)) {
                                                SootMethod callSiteMethod = icfg.getMethodOf(callSite);
                                                SootClass callSiteClass = callSiteMethod.getDeclaringClass();
                                                Set<Pair<ResourceInfo, Local>> res = new LinkedHashSet<>();
                                                Pair<ResourceInfo, Local> fact = new Pair<>(
                                                        new ResourceInfo(lhs.getName(), r, callSiteClass, callSiteMethod, false),
                                                        lhs);
                                                res.add(fact);
                                                return res;
                                                //return Collections.singleton(source);
                                            } else {
                                                return Collections.singleton(source);
                                            }
                                        }
                                    };
                                }
                            }
                        }
                    }
                } else if (callSite instanceof InvokeStmt) {
                    InvokeStmt invokeStmt = (InvokeStmt) callSite;
                    SootMethod invokedMethod = invokeStmt.getInvokeExpr().getMethod();

                    for (Resource r : Resource.values()) {
                        if (r.isBeingReleased(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {

                            if (invokeStmt.getInvokeExpr() instanceof InterfaceInvokeExpr) {
                                InterfaceInvokeExpr expr = (InterfaceInvokeExpr) invokeStmt.getInvokeExpr() ;
                                Value value = expr.getBase();

                                if (value instanceof Local) {
                                    Local local = (Local) value;

                                    return new FlowFunction<Pair<ResourceInfo, Local>>() {
                                        @Override
                                        public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                            if (source.getO2().equivTo(local)) {
                                                return Collections.emptySet();
                                            } else {
                                                return Collections.singleton(source);
                                            }
                                        }
                                    };
                                }
                            }
                        }
                    }
                }

                return Identity.v();
            }
        };
    }

    public Map<Unit, Set<Pair<ResourceInfo, Local>>> initialSeeds() {
        return DefaultSeeds.make(Collections.singleton(Scene.v().getMainMethod().getActiveBody().getUnits().getFirst()),
                zeroValue());
    }

    public Pair<ResourceInfo, Local> createZeroValue() {
        return new Pair<ResourceInfo, Local>(
                new ResourceInfo("<<zero>>", null, null, null),
                new JimpleLocal("<<zero>>", NullType.v()
                ));
    }

    private boolean equalsZeroValue(Pair<ResourceInfo, Local> fact) {
        return fact.getO1().getName().equals("<<zero>>");
    }
}
