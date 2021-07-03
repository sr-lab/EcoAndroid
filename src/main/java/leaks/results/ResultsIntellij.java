package leaks.results;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import soot.SootMethod;
import soot.Type;

import java.util.*;

// TODO For now, only querying inter procedural results!
public class ResultsIntellij implements IResults {
    private final Project project;

    private final Set<Leak> rawResults = new HashSet<>();
    private final Map<PsiMethod, Set<Leak>> results = new HashMap<>();
    private final Map<PsiMethod, Set<Leak>> interProcResults = new HashMap<>();
    private final Map<PsiMethod, Set<Leak>> intraProcResults = new HashMap<>();

    public ResultsIntellij(Project project) {
        this.project = project;
    }

    @Override
    public void add(Leak leak, AnalysisType type) {
        rawResults.add(leak);
        // TODO declMethod or leakedMethod?
        Optional<PsiMethod> psiMethodOfLeakedMethod = findPsiMethodOfSootMethod(leak.getLeakedMethod());
        if (psiMethodOfLeakedMethod.isPresent()) {
            results.computeIfAbsent(psiMethodOfLeakedMethod.get(), k -> new HashSet<>()).add(leak);
            switch (type) {
                case INTER:
                    interProcResults.computeIfAbsent(psiMethodOfLeakedMethod.get(), k -> new HashSet<>()).add(leak);
                    break;
                case INTRA:
                    intraProcResults.computeIfAbsent(psiMethodOfLeakedMethod.get(), k -> new HashSet<>()).add(leak);
                    break;
            }
        }
    }

    public boolean hasLeak(PsiMethod method) {
        return interProcResults.containsKey(method);
    }

    public Set<Leak> getLeaks(PsiMethod method) {
        return interProcResults.get(method);
    }

    @Override
    public void clearAll() {
        rawResults.clear();
        results.clear();
        interProcResults.clear();
        intraProcResults.clear();
    }

    /**
     * Returns the equivalent PsiMethod of the given SootMethod
     * @param sootMethod
     * @return
     */
    private Optional<PsiMethod> findPsiMethodOfSootMethod(SootMethod sootMethod) {
        Project project = this.project;
        return ApplicationManager.getApplication().runReadAction((Computable<Optional<PsiMethod>>) () -> {
            PsiMethod psiMethodOfLeak = null;

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
                    psiMethodOfLeak = psiMethod;
                }
            }

            return Optional.ofNullable(psiMethodOfLeak);
        });
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
