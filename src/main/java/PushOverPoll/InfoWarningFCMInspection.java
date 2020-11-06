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

    private static final String DESCRIPTION_TEMPLATE_INFOWARNING_FCM_PUSHOVERPOLL = "EcoAndroid: Push Over Poll [InfoWarning about FCM]";
    InfoWarningFCMQuickFix infoWarningGCMQuickFix = new InfoWarningFCMQuickFix();
    /*
     * TODO EcoAndroid
     * PUSH OVER POLL ENERGY PATTERN INFO WARNING
     * An alternative to a polling service is a to use push notifications
     * One way to implement them is to use Firebase Cloud Messaging
     * FCM uses an API and works with Android Studio 1.4 or higher with Gradle projects
     * If you wish to know more about this topic, read the following information:
     * https://firebase.google.com/docs/cloud-messaging/android/client
     */
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
