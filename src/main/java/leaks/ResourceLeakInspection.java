package leaks;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.*;
import leaks.results.Leak;
import leaks.results.ResultsIntellij;
import org.jetbrains.annotations.NotNull;

public class ResourceLeakInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethod(PsiMethod method) {
                ResultsIntellij results = ServiceManager.getService(holder.getProject(), ResultsIntellij.class);

                if (results.hasLeak(method)) {
                    PsiIdentifier id = method.getNameIdentifier();
                    for (Leak l : results.getLeaks(method)) {
                        String description = "EcoAndroid: Leaked " +
                                l.getResource() +
                                " declared on " +
                                l.getDeclaredClassName() + "." +
                                l.getDeclaredMethodName();
                        holder.registerProblem(id, description);
                    }
                }
            }
        };
    }
}
