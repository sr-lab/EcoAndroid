package leaks;

import com.android.tools.r8.code.ReturnVoid;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import soot.*;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.toolkits.scalar.Pair;
import vasco.DataFlowSolution;

import java.util.*;

public final class ResultsProcessor implements IResultsProcessor {
    private final Project project;
    private final Set<PsiFile> affectedFiles = new HashSet<>();
    private final ResultsProvider results;

    public ResultsProcessor(Project project) {
        this.project = project;
        this.results = ServiceManager.getService(project, ResultsProvider.class);
    }

    public void visit(RLAnalysis analysis) {
        SootMethod analyzedMethod = analysis.getAnalyzedMethod();
        Set<Local> analysisResults = analysis.getResults();
            for (Local local : analysisResults) {
                for (Resource resource : Resource.values()) {
                    if (resource.isIntraProcedural() &&
                            resource.getType().equals(local.getType().toString())) {
                        Optional<ResourceLeak> leak = processMethodResults(analyzedMethod, resource);
                        leak.ifPresent(results::addResult);
                        break;
                    }
                }
            }
    }

    public void visit(VascoRLAnalysis analysis) {
        Set<SootMethod> analysisMethods = analysis.getMethods();
        DataFlowSolution<Unit,Map<FieldInfo, Pair<Local, Boolean>>> solution = analysis.getMeetOverValidPathsSolution();
        for (SootMethod method : analysisMethods) {
            // Check if a resource is leaked in problematic callbacks
            // These callbacks are not taken into account by Soot's CG -
            // the activity can terminate unexpectedly, creating a resource leak
            if (method.getName().matches("onStop|onPause")) {
                Unit returnUnit = method.getActiveBody().getUnits().getLast();
                HashMap<FieldInfo, Pair<Local, Boolean>> facts =
                        (HashMap<FieldInfo, Pair<Local, Boolean>>) solution.getValueAfter(returnUnit);
                for (Map.Entry<FieldInfo, Pair<Local, Boolean>> entry : facts.entrySet()) {
                    if (entry.getValue().getO2() == true) {
                        Optional<ResourceLeak> leak = processMethodResults(method, entry.getKey().getResource());
                        leak.ifPresent(results::addResult);
                    }
                }
            }
        }
    }

    @Override
    public void visit(AllInOneRLAnalysis analysis) {
        Set<SootMethod> analysisMethods = analysis.getMethods();
        DataFlowSolution<Unit,Map<ResourceInfo, FactState>> solution = analysis.getMeetOverValidPathsSolution();
        for (SootMethod method : analysisMethods) {
            // Check if a resource is leaked in problematic callbacks
            // These callbacks are not taken into account by Soot's CG:
            // the activity can terminate unexpectedly in these, creating a resource leak
            if (method.getName().matches("onStop|onPause")) {
                Unit returnUnit = method.getActiveBody().getUnits().getLast();
                HashMap<ResourceInfo, FactState> facts =
                        (HashMap<ResourceInfo, FactState>) solution.getValueAfter(returnUnit);
                for (Map.Entry<ResourceInfo, FactState> entry : facts.entrySet()) {
                    if (entry.getValue().isAcquired()) {
                        Optional<ResourceLeak> leak = processMethodResults(entry.getKey().getDeclaringMethod(), entry.getKey().getResource());
                        leak.ifPresent(results::addResult);
                    }
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
                                    Optional<ResourceLeak> leak = processMethodResults(fact.getO1().getDeclaringMethod(), fact.getO1().getResource());
                                    if (leak.isPresent()) {
                                        results.addResult(leak.get());
                                    }
                                } else {
                                    Optional<ResourceLeak> leak = processMethodResults(fact.getO1().getDeclaringMethod(), fact.getO1().getResource());
                                    if (leak.isPresent()) {
                                        if (leak.get().getSootMethod().equals(m)) {
                                            results.addResult(leak.get());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes a resource leak information and stores it
     * @param sootMethod SootMethod where the resource was leaked
     * <code>sootMethod</code>
     * @param leakedResource Resource that was leaked
     * @return Optional of a ResourceLeak, with a ResourceLeak if the leak was successfully stored
     */
    private Optional<ResourceLeak> processMethodResults(SootMethod sootMethod, Resource leakedResource) {
        Project project = this.project;
        return ApplicationManager.getApplication().runReadAction((Computable<Optional<ResourceLeak>>) () -> {
            ResourceLeak leak = null;

            String methodClass = sootMethod.getDeclaringClass().getName();
            if (methodClass.startsWith("android.")) {
                return Optional.empty();
            }

            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(methodClass, GlobalSearchScope.allScope(project));
            if (psiClass == null) {
                return Optional.empty();
            }

            String methodName = sootMethod.getName();
            PsiMethod[] possiblePsiMethods = psiClass.findMethodsByName(methodName, true);
            for (PsiMethod psiMethod : possiblePsiMethods) {
                if (areSootPsiMethodsEquivalent(sootMethod, psiMethod)) {
                    leak = new ResourceLeak(sootMethod, psiMethod, leakedResource);
                    return Optional.of(leak);
                }
            }
            affectedFiles.add(psiClass.getContainingFile());

            return Optional.ofNullable(leak);
        });
    }

    // Under testing
    public void runCodeInspection() {
        ResourceLeakInspection inspection = new ResourceLeakInspection();
        InspectionManager inspectionManager = InspectionManager.getInstance(project);
        for (PsiFile file : affectedFiles) {
            inspection.checkFile(file, inspectionManager, true);
        }
    }

    /**
     * Checks if a SootMethod and a PsiMethod are equivalent (i.e. the PsiMethod represents the SootMethod, and vice-versa)
     * @param sootMethod
     * @param psiMethod
     * @return true if the <code>psiMethod</code> is the PSI representation of the <code>sootMethod</code>
     */
    private boolean areSootPsiMethodsEquivalent(SootMethod sootMethod, PsiMethod psiMethod) {
        String sootMethodName = sootMethod.getName();
        String psiMethodName = psiMethod.getName();
        if (!sootMethodName.equals(psiMethodName)) {
            return false;
        }
        System.out.println("(d) passed name test");

        String sootMethodClassName = sootMethod.getDeclaringClass().getName();
        String psiMethodClassName = psiMethod.getContainingClass().getQualifiedName();
        if (!sootMethodClassName.equals(psiMethodClassName)) {
            return false;
        }
        System.out.println("(d) passed class test");

        int sootMethodNumberParams = sootMethod.getParameterCount();
        int psiMethodNumberParams = psiMethod.getParameterList().getParametersCount();
        if (sootMethodNumberParams != psiMethodNumberParams) {
            return false;
        }
        System.out.println("(d) passed #type test");

        Iterator<Type> sootMethodParamTypeIterator = sootMethod.getParameterTypes().iterator();
        Iterator<PsiParameter> psiMethodsParamIterator = Arrays.stream(psiMethod.getParameterList().getParameters()).iterator();

        /* For a soot and psi method to be equal, their name, class, and param types need to be the same
         * (this is because of java overloading)
         * The string representation of Soot and PSI types differ in some types
         * (e.g. java.lang.Class vs java.lang.Class<T>) -> TODO not yet accounted!!!!
         */
        while (sootMethodParamTypeIterator.hasNext() && psiMethodsParamIterator.hasNext()) {
            Type sootParamType = sootMethodParamTypeIterator.next();
            PsiParameter psiParameter = psiMethodsParamIterator.next();
            PsiType psiParamType = psiParameter.getType();

            // TODO fix (use string similarity?)
            if (!psiParamType.equalsToText(sootParamType.toString())) {
                return false;
            }

        }
        System.out.println("(d) passed types test");

        return true;
    }
}