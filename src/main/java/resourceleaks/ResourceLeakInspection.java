package resourceleaks;

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
                ResultsProcessor processor = ServiceManager.getService(holder.getProject(), ResultsProcessor.class);
                if (processor.hasResourceLeaked(method)) {
                    holder.registerProblem(method, DESCRIPTION);
                }
            }
        };
    }
}
