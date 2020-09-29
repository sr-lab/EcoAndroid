package ReduceSize;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

public class GZIPCompressionInspection extends LocalInspectionTool {

    private GZIPCompressionQuickFix gzipCompressionQuickFix = new GZIPCompressionQuickFix();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            private final String DESCRIPTION_TEMPLATE_REDUCE_SIZE = "EcoAndroid: Reduce Size Energy Pattern";

            private PsiMethodCallExpression psiOpenConnection = null;
            private PsiMethodCallExpression psiGetInputStream = null;

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                if(psiOpenConnection != null && psiGetInputStream != null) {
                    PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiOpenConnection, PsiMethod.class);
                    Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfAnyType(psiMethod.getBody(), PsiMethodCallExpression.class);
                    methodCalls.removeIf(el -> !(el.getMethodExpression().getReferenceName().equals("setRequestProperty")));
                    if(methodCalls.size() == 0) {
                        PsiMethodCallExpression aux = psiOpenConnection;
                        gzipCompressionQuickFix.psiGetInputStream = psiGetInputStream;
                        // cleaning for next inspection
                        psiOpenConnection = null;
                        psiGetInputStream = null;
                        holder.registerProblem(aux, DESCRIPTION_TEMPLATE_REDUCE_SIZE, gzipCompressionQuickFix);
                        return;
                    }

                }
                else if(expression.getMethodExpression().getReferenceName().equals("getInputStream")) {
                    PsiMethod psiMethodResolved = expression.resolveMethod();
                    PsiClass psiHttpURlConnection = JavaPsiFacade.getInstance(holder.getProject()).findClass("java.net.URLConnection", GlobalSearchScope.allScope(holder.getProject()));
                    if(!psiMethodResolved.getContainingClass().equals(psiHttpURlConnection)) { return; }
                    psiGetInputStream = expression;
                }
                else if(expression.getMethodExpression().getReferenceName().equals("openConnection")) {
                    PsiMethod psiMethodResolved = expression.resolveMethod();
                    PsiClass psiHttpURlConnection = JavaPsiFacade.getInstance(holder.getProject()).findClass("java.net.URL", GlobalSearchScope.allScope(holder.getProject()));
                    if(!psiMethodResolved.getContainingClass().equals(psiHttpURlConnection)) { return; }
                    psiOpenConnection = expression;
                }
            }
        };
    }
}
