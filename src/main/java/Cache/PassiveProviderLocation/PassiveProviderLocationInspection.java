package Cache.PassiveProviderLocation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PassiveProviderLocationInspection extends LocalInspectionTool {

    private final PassiveProviderLocationQuickFix passiveProviderLocationQuickFix = new PassiveProviderLocationQuickFix();
    private final PassiveProviderLocationInfoWarningQuickFix passiveProviderLocationInfoWarningQuickFix = new PassiveProviderLocationInfoWarningQuickFix();


    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_PASSIVE_PROVIDER = "EcoAndroid: Cache [Switching to PASSIVE_PROVIDER]";
            private final String DESCRIPTION_TEMPLATE_PASSIVE_PROVIDER_INFO_WARNING = "EcoAndroid: Cache [Possible switch to PASSIVE_PROVIDER]";


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
                String parText = expression.getArgumentList().getExpressions()[0].getText();
                if(parText.equals("LocationManager.PASSIVE_PROVIDER")) { return; }

                // look for Object.requireNonNull
                if(checkExplicitWishForProvider(PsiTreeUtil.getParentOfType(expression, PsiMethod.class),expression, parText, false)) {
                    holder.registerProblem(expression.getArgumentList().getExpressions()[0], DESCRIPTION_TEMPLATE_PASSIVE_PROVIDER_INFO_WARNING, passiveProviderLocationInfoWarningQuickFix);
                    return;
                }

                // se está na condicao do if
                PsiIfStatement ifStatement = PsiTreeUtil.getParentOfType(expression, PsiIfStatement.class);
                if(ifStatement != null) {
                    if(ifStatement.getCondition() instanceof PsiMethodCallExpression) {
                        if(checkExplicitWishForProvider(((PsiMethodCallExpression) ifStatement.getCondition()).resolveMethod(),ifStatement.getCondition() ,parText, true)) {
                            holder.registerProblem(expression.getArgumentList().getExpressions()[0], DESCRIPTION_TEMPLATE_PASSIVE_PROVIDER_INFO_WARNING, passiveProviderLocationInfoWarningQuickFix);
                            return;
                        }
                    }
                    else if (ifStatement.getCondition() instanceof PsiReferenceExpression && ifStatement.getCondition().getText().equals("flagEcoAndroid")) {
                        return;
                    }
                }
                holder.registerProblem(expression.getArgumentList().getExpressions()[0], DESCRIPTION_TEMPLATE_PASSIVE_PROVIDER, passiveProviderLocationQuickFix);
            }

            boolean checkExplicitWishForProvider(PsiMethod psiMethod, PsiElement expression, String parText, boolean flag) {
                for (PsiElement psiElement : psiMethod.getBody().getStatements()) {
                    Collection<PsiMethodCallExpression> childrenMethodCalls = PsiTreeUtil.findChildrenOfType(psiElement, PsiMethodCallExpression.class);
                    childrenMethodCalls.removeIf(el -> !(el.getMethodExpression().getText().endsWith("isProviderEnabled")
                            && el.getArgumentList().getExpressionCount() > 0
                            && el.getArgumentList().getExpressions()[0].getText().equals(parText)));
                    if(childrenMethodCalls.size() > 0) {
                        if(flag) { return true; }
                        PsiMethodCallExpression next = childrenMethodCalls.iterator().next();
                        if(PsiUtilBase.compareElementsByPosition(expression, next) > 0) { return true; }
                    }
                }
                return false;
            }
        };
    }
}
