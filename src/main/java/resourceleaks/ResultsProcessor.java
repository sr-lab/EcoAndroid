package resourceleaks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import soot.*;

import java.util.*;

public final class ResultsProcessor {
    private final Project project;
    // TODO account for multiple resources leaked in method
    private Map<String, ResourceLeakInfo> results = new HashMap<String, ResourceLeakInfo>();

    public ResultsProcessor(Project project) {
        this.project = project;
    }

    /**
     * Processes a resource leak information and stores it
     * @param sootMethod SootMethod where the resource was leaked
     * @param possiblePsiMethods array of PsiMethod that possible contain the PSI representation of the
     * <code>sootMethod</code>
     * @return true if the leak was stored, false otherwise
     */
    public boolean processMethodResults(SootMethod sootMethod, PsiMethod[] possiblePsiMethods) {
        for (PsiMethod psiMethod : possiblePsiMethods) {
            if (areSootPsiMethodsEquivalent(sootMethod, psiMethod)) {
                ResourceLeakInfo info = new ResourceLeakInfo(sootMethod, psiMethod);
                results.put(sootMethod.getName(), info);
                return true;
            }
        }
        return false;
    }

    //under test
    public boolean processMethodResults(SootMethod sootMethod) {
        //final boolean[] wasProcessed = new boolean[1];
        //needed to read PSI tree because we are not in the UI thread
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>)() -> {
            String methodName = sootMethod.getName();
            String methodClass = sootMethod.getClass().getName();
            PsiClass psiClass = JavaPsiFacade.getInstance(this.project).findClass(methodClass, GlobalSearchScope.allScope(this.project));
            PsiMethod[] possiblePsiMethods = psiClass.findMethodsByName(methodName, true);
            for (PsiMethod psiMethod : possiblePsiMethods) {
                if (areSootPsiMethodsEquivalent(sootMethod, psiMethod)) {
                    ResourceLeakInfo info = new ResourceLeakInfo(sootMethod, psiMethod);
                    results.put(sootMethod.getName(), info);
                    //wasProcessed[0] = true;
                    return true;
                }
            }
            //wasProcessed[0] = false;
            return false;
        });
        //return wasProcessed[0];
    }



    public boolean hasResourceLeaked(SootMethod method) {
        String methodName = method.getName();
        if (results.containsKey(methodName)) {
            return results.get(methodName).getSootMethod().equals(method);
        }
        return false;
    }

    public boolean hasResourceLeaked(PsiMethod method) {
        String methodName = method.getName();
        if (results.containsKey(methodName)) {
            return results.get(methodName).getPsiMethod().equals(method);
        }
        return false;
    }

    /**
     * Checks if a SootMethod and a PsiMethod are equivalent (i.e. the PsiMethod represents the SootMethod, and vice-versa)
     * @param sootMethod
     * @param psiMethod
     * @return true if the <code>psiMethod</code> is the PSI representation of the <code>sootMethod</code>
     */
    public boolean areSootPsiMethodsEquivalent(SootMethod sootMethod, PsiMethod psiMethod) {
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

        Iterator sootMethodParamTypeIterator = sootMethod.getParameterTypes().iterator();
        Iterator psiMethodsParamIterator = Arrays.stream(psiMethod.getParameterList().getParameters()).iterator();

        /* For a soot and psi method to be equal, their name, class, and param types need to be the same
         * (this is because of java overloading)
         * The string representation of Soot and PSI types differ in some types
         * (e.g. java.lang.Class vs java.lang.Class<T>) -> TODO not yet accounted!!!!
         */
        while (sootMethodParamTypeIterator.hasNext() && psiMethodsParamIterator.hasNext()) {
            Type sootParamType = (Type) sootMethodParamTypeIterator.next();
            PsiParameter psiParameter = (PsiParameter) psiMethodsParamIterator.next();
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