package PushOverPoll;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class InfoWarningFCMInspection extends LocalInspectionTool {

    private static final String DESCRIPTION_TEMPLATE_INFOWARNING_FCM_PUSHOVERPOLL = "Refactor4Green: Push Over Poll - Info Warning FCM";
    InfoWarningFCMQuickFix infoWarningGCMQuickFix = new InfoWarningFCMQuickFix();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                if(!(expression.getMethodExpression().getReferenceName().equals("setRepeating"))) { return; }

                PsiMethod psiMethodResolved = expression.resolveMethod();
                PsiClass psiSSLContextClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.app.AlarmManager", GlobalSearchScope.allScope(holder.getProject()));
                if(!psiMethodResolved.getContainingClass().equals(psiSSLContextClass)) { return ; }

                holder.registerProblem(expression, DESCRIPTION_TEMPLATE_INFOWARNING_FCM_PUSHOVERPOLL, infoWarningGCMQuickFix);


            }
        };
    }
}
