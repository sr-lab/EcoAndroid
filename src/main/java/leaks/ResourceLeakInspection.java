package leaks;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ResourceLeakInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @NonNls
            private final String DESCRIPTION = "EcoAndroid: Resource Leak";

            @Override
            public void visitMethod(PsiMethod method) {
                ResultsProvider results = ServiceManager.getService(holder.getProject(), ResultsProvider.class);
                if (results.hasResourceLeaked(method)) {
                    PsiIdentifier id = method.getNameIdentifier();
                    holder.registerProblem(id, DESCRIPTION);
                }
            }
        };
    }
}
