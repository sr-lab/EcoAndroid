package resourceleaks;

import com.intellij.psi.PsiMethod;
import soot.Local;
import soot.SootMethod;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ResourceLeakInfo {
    private String methodName;
    private String className;
    private SootMethod sootMethod;
    private PsiMethod psiMethod;
    private Set<Local> resourcesLeaked;

    public ResourceLeakInfo(SootMethod sootMethod, PsiMethod psiMethod, Set<Local> resourcesLeaked) {
        //we use soot's method info, but this is validated by the results processor
        this.methodName = sootMethod.getName();
        this.className = sootMethod.getDeclaringClass().getName();
        this.sootMethod = sootMethod;
        this.psiMethod = psiMethod;
        this.resourcesLeaked = new HashSet<Local>(resourcesLeaked);
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
