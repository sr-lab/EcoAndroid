package Cache.PassiveProviderLocation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PassiveProviderLocationInspection extends LocalInspectionTool {

    private final PassiveProviderLocationQuickFix passiveProviderLocationQuickFix = new PassiveProviderLocationQuickFix();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_PASSIVE_PROVIDER = "Refactor4Green: Cache - Switching to PASSIVE_PROVIDER";

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                if(!(expression.getMethodExpression().getReferenceName().equals("requestLocationUpdates")))
                    return;

                // check if the its calling the method we want
                PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) expression.getMethodExpression().getQualifier().getReference();
                PsiClass psiClass = JavaPsiFacade.getInstance(holder.getProject()).findClass(psiReferenceExpression.getType().getCanonicalText(), GlobalSearchScope.allScope(holder.getProject()));
                PsiClass psiLocationManager = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.location.LocationManager", GlobalSearchScope.allScope(holder.getProject()));
                if(!(psiClass.equals(psiLocationManager) || psiClass.isInheritor(psiLocationManager, true)))
                    return;

                // check if the class passes the right arg or not
                if(expression.getArgumentList().getExpressions()[0].getText().equals("LocationManager.PASSIVE_PROVIDER"))
                    return;

                holder.registerProblem(expression, DESCRIPTION_TEMPLATE_PASSIVE_PROVIDER, passiveProviderLocationQuickFix);
            }
        };
    }
}
