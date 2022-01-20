package leaks.analysis;

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Identity;

import java.util.*;

import heros.flowfunc.KillAll;
import leaks.Resource;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.toolkits.scalar.Pair;

/**
 * Implementation of resource leak detection using IFDS.
 * Dataflow facts are represented as a pair of {@link ResourceInfo} and a Soot's Local.
 * @see ResourceInfo
 * @see IFDSRLAnalysis
 */
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

            // The getNormalFlowFunction takes care of the flow for a normal statement, i.e., a statement
            // that is neither a call nor exit statement.
            // In our analysis, we use it to:
            // (1) Handle when class-member resources are acquired or used;
            // (2) Handle 'if' statements where it checks whether a resource is being held, or is null.
            @Override
            public FlowFunction<Pair<ResourceInfo, Local>> getNormalFlowFunction(final Unit curr, Unit succ) {
                System.out.println("-------------------------------------\n"
                        + "normalFlow: "
                        + icfg.getMethodOf(curr).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(curr).getName() + "\n"
                        + curr);

                if (curr instanceof AssignStmt) {
                    AssignStmt stmt = (AssignStmt) curr;
                    Value rhs = stmt.getRightOp();

                    // Case (1) where we are dealing with a class-member resource

                    // The trick here is to look ahead - first check for any resource declaration,
                    // and the next step depends on the "next" unit in analysis (succ):
                    // - if its an invoke stmt, then the resource could be acquired or released;
                    // - if not, then the resource is simply being declared and will be
                    //   used for something else.
                    if (rhs instanceof InstanceFieldRef) {
                        InstanceFieldRef fieldRef = (InstanceFieldRef) stmt.getRightOp();
                        SootField field = fieldRef.getField();
                        Local lhs = (Local) stmt.getLeftOp();

                        for (Resource r : Resource.values()) {
                            if (r.isBeingDeclared(field.getType().toString())) {

                                // An invoke stmt on a local representing a resource is usually preceded by
                                // an assignment of the resource to the same local, e.g.:
                                // $r1 = r0.<resource>                       => assign stmt
                                // $virtualinvoke r1.<resource: acquire()>   => invoke stmt
                                if (succ instanceof InvokeStmt) {
                                    InvokeStmt invokeStmt = (InvokeStmt) succ;
                                    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                                    if (invokeExpr instanceof InstanceInvokeExpr) {
                                        InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                                        SootMethod invokedMethod = instanceInvokeExpr.getMethod();
                                        Value base = instanceInvokeExpr.getBase();

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
                                                    } else if (r.isBeingAcquired(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {
                                                        LinkedHashSet<Pair<ResourceInfo, Local>> res = new LinkedHashSet<>();
                                                        res.add(new Pair<>(
                                                                new ResourceInfo(field.getName(), r, field.getDeclaringClass(), icfg.getMethodOf(curr), true),
                                                                lhs));
                                                        return res;
                                                    }
                                                    return Collections.singleton(source);
                                                }
                                            };

                                        }
                                    }
                                // The resource is only being assigned and not acquired.
                                // (Other resources do not need to be checked for this!
                                // We handle this type of aliasing before analysis because
                                // it works, and it is simpler.)
                                // This information is crucial for dealing with 'if' stmts.
                                } else {
                                    return new FlowFunction<Pair<ResourceInfo, Local>>() {
                                        @Override
                                        public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                            SootMethod debug = icfg.getMethodOf(curr);
                                            // We only care about tracking acquired resources
                                            if (!equalsZeroValue(source)) {
                                                if (source.getO1().getName().equals(field.getName())
                                                        && !equalLocals(source.getO2(), lhs)) {
                                                    // Update the "current" local because of 'if' statements
                                                    return Collections.singleton(
                                                            new Pair<>(source.getO1(), lhs));
                                                } else {
                                                    return Collections.emptySet();
                                                }
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

                // Case (2) where we handle 'if' statements.
                // We handle first (a) when there is a check to see if the resource is being held and
                // second (b) when a resource is being checked for null.

                // This is necessary to flow information about the state of the resource
                // (e.g. if we branch out because the resource is null, but we have
                // a fact that says it was acquired, we need to kill that fact).
                else if (curr instanceof IfStmt) {
                    IfStmt stmt = (IfStmt) curr;
                    ConditionExpr cond = (ConditionExpr) stmt.getCondition();
                    Value lhs = cond.getOp1();

                    if (lhs instanceof Local) {
                        Local local = (Local) lhs;

                        // Case (a) for an if statement where it is being checked
                        // if a resource is being held via the proper operation
                        // (e.g. wakelock's isHeld())

                        // This "hack" allows to see if the variable used in the if statement
                        // resulted in the use of a resource's isHeld operation.
                        // We look for the predecessores of the if statement to search for the
                        // assign statement related to the ifs statement's variable.
                        for (Unit pred : icfg.getPredsOf(curr)) {
                            if (pred instanceof AssignStmt) {
                                AssignStmt assignStmt = (AssignStmt) pred;

                                if (assignStmt.getLeftOp() instanceof Local) {
                                    Local predLhs = (Local) assignStmt.getLeftOp();
                                    //TODO performance -  check for boolean local
                                    if (predLhs.equivTo(lhs)) { // Sanity check :)
                                        if (assignStmt.getRightOp() instanceof InstanceInvokeExpr) {
                                            InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) assignStmt.getRightOp();
                                            SootMethod invokedMethod = invokeExpr.getMethod();
                                            Value base = invokeExpr.getBase();

                                            for (Resource r : Resource.values()) {
                                                if (r.isCheckedIfItsHeld(base.getType().toString(), invokedMethod.getName())) {
                                                    return new FlowFunction<Pair<ResourceInfo, Local>>() {
                                                        @Override
                                                        public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                                            String condSymbol = cond.getSymbol();
                                                            Value rhs = cond.getOp2();
                                                            // TODO check logic again
                                                            // The if will branch and its about a seen resource
                                                            if (stmt.getTarget().equals(succ) && equalLocals(source.getO2(), (Local) base)) {
                                                                // We are branching, and the resource is not held,
                                                                // so we remove facts related to that resource
                                                                if (rhs instanceof IntConstant) {
                                                                    if (condSymbol.equals(" != ")
                                                                    && ((IntConstant) rhs).value == 0) {
                                                                        return Collections.emptySet();
                                                                    }
                                                                }
                                                            // The if will not branch and its about a seen resource
                                                            } else if (equalLocals(source.getO2(), (Local) base)) {
                                                                // We are not branching, and the resource is held,
                                                                // so we remove facts related to that resource
                                                                if (rhs instanceof IntConstant) {
                                                                    if (condSymbol.equals(" == ")
                                                                            && ((IntConstant) rhs).value == 0) {
                                                                        return Collections.emptySet();
                                                                    }
                                                                }
                                                            }
                                                            // The other 2 cases (resource is held, branch; resource is not held, not branch)
                                                            // fall in this category: we just need to remove the fact
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

                        // Case (b) where we are simply dealing with a check to
                        // see if the resource is null.
                        return new FlowFunction<Pair<ResourceInfo, Local>>() {
                            @Override
                            public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                String condSymbol = cond.getSymbol();
                                Value rhs = cond.getOp2();

                                // The if will branch and its about a seen resource
                                if (stmt.getTarget().equals(succ) && equalLocals(source.getO2(), local)) {
                                    // We are branching and the resource is "null",
                                    // so we remove facts related to that resource
                                    if (rhs instanceof NullConstant) {
                                        if (condSymbol.equals(" == ")) {
                                            return Collections.emptySet();
                                        }
                                    }
                                // The if will not branch and its about a seen resource
                                } else if (equalLocals(source.getO2(), local)) {
                                    // We are not branching and the resource is "not null",
                                    // so we remove facts related to that resource
                                    if (rhs instanceof NullConstant) {
                                        if (condSymbol.equals(" != ")) {
                                            return Collections.emptySet();
                                        }
                                    }
                                }
                                // The other 2 cases (resource null, branch; resource null, not branch)
                                // fall in this category: we just need to remove the fact
                                return Collections.singleton(source);
                            }
                        };
                    }
                }

                return Identity.v();
            }

            // The getCallFlowFunction takes care of handling call statements.
            // In our analysis, we use it to:
            // (1) Handle the passing of references to the arguments of a call;
            // (2) Handle the flow of facts of class-member resources
            @Override
            public FlowFunction<Pair<ResourceInfo, Local>> getCallFlowFunction(Unit callStmt,
                                                                               final SootMethod destinationMethod) {
                System.out.println("-------------------------------------\n"
                        + "callFlow: "
                        + icfg.getMethodOf(callStmt).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(callStmt).getName() + "\n"
                        + callStmt);

                Stmt stmt = (Stmt) callStmt;
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                final List<Value> args = invokeExpr.getArgs();

                final List<Local> localArguments = new ArrayList<>(args.size());
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
                        // Case (1) where a resource is being passed by reference to a function.
                        // We need to update the current Local of the fact with its new representation
                        // in the function called.
                        // We assume that a resource being passed by reference was declared in
                        // a function, and therefore is NOT a class member! (these can be used
                        // freely and do not need to be passed by reference!)
                        if (!destinationMethod.getName().equals("<clinit>")
                                && !destinationMethod.getSubSignature().equals("void run()")) {
                            if (localArguments.contains(source.getO2()) && !source.getO1().isClassMember()) {

                                // Ignore QueryMap calls
                                if (destinationMethod.getDeclaringClass().getName().contains("QueryMap")) {
                                    return Collections.emptySet();
                                }

                                /*
                                ResourceInfo updatedInfo = source.getO1();

                                if (destinationMethod.getDeclaringClass().getName().contains("QueryMap")) {
                                    updatedInfo = new ResourceInfo(
                                            "&QUERYMAP", source.getO1().getResource(),
                                            source.getO1().getDeclaringClass(), source.getO1().getDeclaringMethod());
                                }

                                 */

                                int paramIndex = args.indexOf(source.getO2());
                                Pair<ResourceInfo, Local> pair = new Pair<>(
                                        //updatedInfo,
                                        source.getO1(),
                                        destinationMethod.retrieveActiveBody().getParameterLocal(paramIndex));
                                return Collections.singleton(pair);
                            }
                        }
                        // Case (2) where class-member resources can flow freely through function calls.
                        if (source.getO1().isClassMember()) {
                            return Collections.singleton(source);
                        }

                        return Collections.emptySet();
                    }
                };
            }

            // The getReturnFlowFunction takes care of the flow from an exit of a method.
            // In our analysis, we use it to:
            // (1) Handle returning resources from a function;
            // (2) Handles flow of non-class-member resources
            // (in conjunction with getCallToReturnFlowFunction).
            @Override
            public FlowFunction<Pair<ResourceInfo, Local>> getReturnFlowFunction(final Unit callSite,
                                                                                 SootMethod calleeMethod, final Unit exitStmt, Unit returnSite) {
                System.out.println("-------------------------------------\n"
                        + "returnFlow: "
                        + icfg.getMethodOf(callSite).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(callSite).getName() + "\n"
                        + callSite + "\n"
                        + exitStmt);


                return new FlowFunction<Pair<ResourceInfo, Local>>() {
                    @Override
                    public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {

                        if (callSite.toString().equals("specialinvoke r10.<info.guardianproject.otr.app.im.provider.Imps$ProviderSettings$QueryMap: void <init>(android.database.Cursor,android.content.ContentResolver,long,boolean,android.os.Handler)>($r9, $r4, $l1, 0, null)")) {
                            System.out.println("ZZZ");
                        }

                        // Case (1) where a resource is acquired in a method and then returned to its callee.
                        if (callSite instanceof AssignStmt && exitStmt instanceof ReturnStmt) {
                            AssignStmt assignStmt = (AssignStmt) callSite;
                            ReturnStmt returnStmt = (ReturnStmt) exitStmt;

                            if (assignStmt.getLeftOp() instanceof Local && returnStmt.getOp() instanceof Local) {
                                Local assignStmtLocal = (Local) assignStmt.getLeftOp();
                                Local returnStmtLocal = (Local) returnStmt.getOp();

                                // We identify that a resource was acquired and returned
                                // by a method, and so we update its Local.
                                if (!equalsZeroValue(source)) {
                                    if (source.getO1().getResource().getType().equals(assignStmtLocal.getType().toString())
                                            && equalLocals(returnStmtLocal, source.getO2())) {
                                        return Collections.singleton(new Pair<>(source.getO1(), assignStmtLocal));
                                    }
                                    return Collections.emptySet();
                                }
                                return Collections.singleton(source);
                            }
                        }

                        if (source.getO1().isClassMember()) {
                            return Collections.singleton(source);

                        // Case (2) where a resource is passed by reference.
                        // If a resource is passed to the function, we eliminate
                        // the facts that flow through the callToReturnFlow,
                        // while letting pass the facts from the returnFlow.
                        // This helps separate logic of the flow of facts while
                        // reducing the number of false positives.
                        } else {
                            Stmt stmt = (Stmt) callSite;
                            InvokeExpr invokeExpr = stmt.getInvokeExpr();
                            final List<Value> args = invokeExpr.getArgs();

                            final List<Local> localArguments = new ArrayList<>(args.size());
                            for (Value value : args) {
                                if (value instanceof Local) {
                                    localArguments.add((Local) value);
                                } else {
                                    localArguments.add(null);
                                }
                            }
                            SootMethod calledMethod = icfg.getMethodOf(exitStmt);
                            List<Local> calledMethodParamLocals = calledMethod.retrieveActiveBody().getParameterLocals();
                            Local param = null;

                            // TODO refactor to function and refactor code :) (problem with contains and aliasing)
                            for (Local l : calledMethodParamLocals) {
                                if (equalLocals(l, source.getO2())) {
                                    param = l;
                                }
                            }

                            if (param != null) {
                                int paramIndex;
                                paramIndex = calledMethodParamLocals.indexOf(param);
                                Local calleeLocal = localArguments.get(paramIndex);
                                Pair<ResourceInfo, Local> fact = new Pair<>(
                                        source.getO1(),
                                        calleeLocal
                                );
                                return Collections.singleton(fact);
                            }
                        }
                        return Collections.emptySet();
                    }
                };
            }

            // The getCallToReturnFlowFunction takes care of the flow from a call site to
            // a successor statement just right after the call (making it best suited
            // to propagate information that does NOT concern the callee - a job for
            // getCallFlowFunction).
            // In our analysis, we use it to:
            // (1) Handle the acquiring of non-class-member resources
            // (2) Handles flow of non-class-member resources
            // (in conjunction with getReturnFlowFunction).
            @Override
            public FlowFunction<Pair<ResourceInfo, Local>> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                System.out.println("-------------------------------------\n"
                        + "callToReturnFlow: "
                        + icfg.getMethodOf(callSite).getDeclaringClass().getShortName() + "."
                        + icfg.getMethodOf(callSite).getName() + "\n"
                        + callSite + "\n"
                        + returnSite);

                if (callSite instanceof AssignStmt) {
                    AssignStmt stmt = (AssignStmt) callSite;
                    Value rhs = stmt.getRightOp();

                    if (rhs instanceof InvokeExpr) {
                        InvokeExpr invokeExpr = (InvokeExpr) rhs;
                        SootMethod invokedMethod = invokeExpr.getMethod();

                        if (stmt.getLeftOp() instanceof Local) {
                            Local lhs = (Local) stmt.getLeftOp();

                            return new FlowFunction<Pair<ResourceInfo, Local>>() {
                                @Override
                                public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                                    // Handles case (2) where a resource is passed by ref.
                                    // If a resource is passed to the function, we eliminate
                                    // the facts that flow through the getCallToReturnFlowFunction
                                    // while letting pass the facts from the getReturnFlowFunction.
                                    final List<Value> args = invokeExpr.getArgs();
                                    final List<Local> localArguments = new ArrayList<>(args.size());
                                    for (Value v : args) {
                                        if (v instanceof Local) {
                                            localArguments.add((Local) v);
                                        } else {
                                            localArguments.add(null);
                                        }
                                    }
                                    // TODO Might exist problem related to aliased Local's (RESOURCE_X)
                                    if (localArguments.contains(source.getO2())) {
                                        return Collections.emptySet();
                                    }

                                    // Handles case (1) where a resource is being acquired via an assign statement.
                                    for (Resource r : Resource.values()) {
                                        if (r.isBeingAcquired(invokedMethod.getName(), invokedMethod.getDeclaringClass().toString())) {
                                            SootMethod callSiteMethod = icfg.getMethodOf(callSite);
                                            if (equalsZeroValue(source) && !callSiteMethod.getName().equals("<init>")) {
                                                SootClass callSiteClass = callSiteMethod.getDeclaringClass();
                                                Set<Pair<ResourceInfo, Local>> res = new LinkedHashSet<>();
                                                Pair<ResourceInfo, Local> fact = new Pair<>(
                                                        new ResourceInfo(lhs.getName(), r, callSiteClass, callSiteMethod),
                                                        lhs);
                                                res.add(fact);
                                                return res;
                                            }
                                        }
                                    }
                                    return Collections.singleton(source);
                                }
                            };
                        }
                    }
                } else if (callSite instanceof InvokeStmt) {
                    InvokeStmt invokeStmt = (InvokeStmt) callSite;
                    SootMethod invokedMethod = invokeStmt.getInvokeExpr().getMethod();

                    if (invokedMethod.getDeclaringClass().getName().contains("QueryMap")) {
                        return KillAll.v();
                    }

                    return new FlowFunction<Pair<ResourceInfo, Local>>() {
                        @Override
                        public Set<Pair<ResourceInfo, Local>> computeTargets(Pair<ResourceInfo, Local> source) {
                            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                            /*
                            if (callSite.toString().equals("specialinvoke r10.<info.guardianproject.otr.app.im.provider.Imps$ProviderSettings$QueryMap: void <init>(android.database.Cursor,android.content.ContentResolver,long,boolean,android.os.Handler)>($r9, $r4, $l1, 0, null)")) {
                                System.out.println("ZZZ");
                            }
                             */

                            // Handles case (2) where a resource is passed by ref. If a resource is passed
                            // to the function, we eliminate the facts that flow through the callToReturnFlow
                            // while letting pass the facts from the returnFlow.
                            final List<Value> args = invokeExpr.getArgs();
                            final List<Local> localArguments = new ArrayList<>(args.size());
                            for (Value v : args) {
                                if (v instanceof Local) {
                                    localArguments.add((Local) v);
                                } else {
                                    localArguments.add(null);
                                }
                            }

                            if (localArguments.contains(source.getO2())) {
                                return Collections.emptySet();
                            }

                            if (invokeExpr instanceof InstanceInvokeExpr) {
                                Value value = ((InstanceInvokeExpr) invokeExpr).getBase();
                                Local local = (Local) value;

                                // Handles case (1) where a resource is being acquired via an invoke statement.
                                for (Resource r : Resource.values()) {
                                    if (r.isBeingReleased(invokedMethod.getName(), invokedMethod.getDeclaringClass().getName())) {
                                        if (value instanceof Local) {
                                            if (equalLocals(source.getO2(), local)) {
                                                return Collections.emptySet();
                                            }
                                        }
                                    } else if (r.isBeingAcquired(invokedMethod.getName(), invokedMethod.getDeclaringClass().toString())) {
                                        // Edge case to not allow class-member resources beign acquired here.
                                        // Works using the same logic as if we were acquiring them, but backwards!:
                                        // We check the preds of the callsite to see if there is an assign stmt assiging
                                        // a InstanceFieldRef to the local.
                                        for (Unit pred : icfg.getPredsOf(callSite)) {
                                            if (pred instanceof AssignStmt) {
                                                AssignStmt stmt = (AssignStmt) pred;
                                                Value rhs = stmt.getRightOp();

                                                if (rhs instanceof InstanceFieldRef) {
                                                    return Collections.singleton(source);
                                                }
                                            }
                                        }

                                        SootMethod callSiteMethod = icfg.getMethodOf(callSite);
                                        if (equalsZeroValue(source) && !callSiteMethod.getName().equals("<init>")) {
                                            SootClass callSiteClass = callSiteMethod.getDeclaringClass();
                                            Set<Pair<ResourceInfo, Local>> res = new LinkedHashSet<>();
                                            Pair<ResourceInfo, Local> fact = new Pair<>(
                                                    new ResourceInfo(local.getName(), r, callSiteClass, callSiteMethod),
                                                    local);
                                            res.add(fact);
                                            return res;
                                        }
                                    }
                                }
                            }

                            return Collections.singleton(source);
                        }
                    };
                }

                return Identity.v();
            }
        };
    }

    /**
     * Helper function that simulates Local.equivTo(Local), but taking into account our aliasing processing.
     * @param l1
     * @param l2
     * @return
     */
    private boolean equalLocals(Local l1, Local l2) {
        return l1.equivTo(l2) || (l1.getName().matches(l2.getName())) && (l2).getName().startsWith("RESOURCE_");
    }

    public Map<Unit, Set<Pair<ResourceInfo, Local>>> initialSeeds() {
        return DefaultSeeds.make(Collections.singleton(Scene.v().getMainMethod().getActiveBody().getUnits().getFirst()),
                zeroValue());
    }

    public Pair<ResourceInfo, Local> createZeroValue() {
        return new Pair<>(
                new ResourceInfo("<<zero>>", null, null, null),
                new JimpleLocal("<<zero>>", NullType.v()));
    }

    private boolean equalsZeroValue(Pair<ResourceInfo, Local> fact) {
        return fact.getO1().getName().equals("<<zero>>");
    }
}
