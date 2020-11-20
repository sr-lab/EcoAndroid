package PushOverPoll;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class InfoWarningFCMInspection extends LocalInspectionTool {

    private static final String DESCRIPTION_TEMPLATE_INFOWARNING_FCM_PUSHOVERPOLL = "EcoAndroid: Push Over Poll [InfoWarning about FCM] can be applied";
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
                PsiMethod psiMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                PsiClass psiClass = psiMethod.getContainingClass();
                PsiClass psiSSLContextClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.app.AlarmManager", GlobalSearchScope.allScope(holder.getProject()));
                if(!psiMethodResolved.getContainingClass().equals(psiSSLContextClass)) { return ; }

                PsiComment[] comments = PsiTreeUtil.getChildrenOfType(psiMethod, PsiComment.class);
                if(comments != null) {
                    for (PsiComment comment : comments) {
                        if(comment.getText().startsWith("/*\n     * TODO EcoAndroid\n")) {
                            return;
                        }
                    }
                }

                holder.registerProblem(expression, DESCRIPTION_TEMPLATE_INFOWARNING_FCM_PUSHOVERPOLL, infoWarningGCMQuickFix);
            }
        };
    }
}
