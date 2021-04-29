package leaks;

import com.intellij.psi.PsiMethod;
import soot.SootMethod;

public class ResourceLeakInfo {
    private String methodName;
    private String className;
    private SootMethod sootMethod;
    private PsiMethod psiMethod;

    public ResourceLeakInfo(SootMethod sootMethod, PsiMethod psiMethod) {
        //we use soot's method info, but this is validated by the results processor
        this.methodName = sootMethod.getName();
        this.className = sootMethod.getDeclaringClass().getName();
        this.sootMethod = sootMethod;
        this.psiMethod = psiMethod;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public PsiMethod getPsiMethod() {
        return psiMethod;
    }

    public SootMethod getSootMethod() {
        return sootMethod;
    }

    /*
    public Set<PsiElement> getResourcesPsiElement() {
        return Collections.unmodifiableSet(resourcesPsiElement);
    }
     */


}
