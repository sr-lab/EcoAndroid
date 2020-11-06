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
import java.util.Iterator;

public class GZIPCompressionInspection extends LocalInspectionTool {

    private GZIPCompressionQuickFix gzipCompressionQuickFix;

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            private final String DESCRIPTION_TEMPLATE_REDUCE_SIZE = "EcoAndroid: Reduce Size [GZIP Compression]";

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                PsiMethodCallExpression psiGetInputStream;
                if(!(expression.getMethodExpression().getReferenceName().equals("getInputStream"))) { return; }

                PsiMethod psiMethodResolved = expression.resolveMethod();
                PsiClass psiHttpURlConnection = JavaPsiFacade.getInstance(holder.getProject()).findClass("java.net.URLConnection", GlobalSearchScope.allScope(holder.getProject()));
                if(!psiMethodResolved.getContainingClass().equals(psiHttpURlConnection)) { return; }
                psiGetInputStream = expression;
                PsiElement urlConnectionVariable = psiGetInputStream.getMethodExpression().getQualifier();

                // ja associado a uma var
                if(urlConnectionVariable instanceof PsiReferenceExpression) {
                    PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) urlConnectionVariable;
                    PsiLocalVariable psiLocalVariable = (PsiLocalVariable) psiReferenceExpression.resolve();

                    // check if the request property for gzip exists with the variable
                    PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiLocalVariable, PsiMethod.class);
                    Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfAnyType(psiMethod.getBody(), PsiMethodCallExpression.class);
                    methodCalls.removeIf(el -> !(el.getMethodExpression().getQualifier() != null
                            && el.getMethodExpression().getQualifier().getText().equals(urlConnectionVariable.getText())
                            && el.getMethodExpression().getReferenceName().equals("setRequestProperty")
                            && el.getArgumentList().getExpressionCount() == 2
                            && el.getArgumentList().getExpressions()[1].getText().equals("\"gzip\"")));
                    if(!methodCalls.isEmpty()) { return; }

                    methodCalls = PsiTreeUtil.findChildrenOfAnyType(psiMethod.getBody(), PsiMethodCallExpression.class);
                    methodCalls.removeIf(el -> !(el.getMethodExpression().getReferenceName().equals("openConnection")));
                    if(methodCalls.isEmpty()) { return; }
                    PsiMethodCallExpression psiFinalMethodCall = (PsiMethodCallExpression) methodCalls.toArray()[0];
                    if(methodCalls.size() >= 2) {
                        // look for the closet one
                        int distance = 0;
                        for (PsiMethodCallExpression methodCall : methodCalls) {
                            int distanceAux = PsiUtilBase.compareElementsByPosition(methodCall, psiGetInputStream);
                            if(distanceAux < distance) {
                                distance = distanceAux;
                                psiFinalMethodCall = methodCall;
                            }
                        }
                    }

                    PsiResourceList tryStatement1 = PsiTreeUtil.getParentOfType(psiFinalMethodCall, PsiResourceList.class);
                    PsiResourceList tryStatement2 = PsiTreeUtil.getParentOfType(psiGetInputStream, PsiResourceList.class);
                    if(tryStatement1 != null || tryStatement2 != null) { return; }

                    gzipCompressionQuickFix = new GZIPCompressionQuickFix();
                    gzipCompressionQuickFix.psiGetInputStream = psiGetInputStream;
                    holder.registerProblem(psiFinalMethodCall, DESCRIPTION_TEMPLATE_REDUCE_SIZE, gzipCompressionQuickFix);
                    return;
                }
                // é do tipo openConnection().getIntputStream()
                else if (urlConnectionVariable instanceof PsiMethodCallExpression){
                    PsiMethod psiMethod = PsiTreeUtil.getParentOfType(urlConnectionVariable, PsiMethod.class);
                    Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfAnyType(psiMethod.getBody(), PsiMethodCallExpression.class);
                    methodCalls = PsiTreeUtil.findChildrenOfAnyType(psiMethod.getBody(), PsiMethodCallExpression.class);
                    methodCalls.removeIf(el -> !(el.getMethodExpression().getReferenceName().equals("openConnection")));
                    if(methodCalls.isEmpty()) { return; }
                    PsiMethodCallExpression psiFinalMethodCall = methodCalls.iterator().next();
                    PsiResourceList tryStatement1 = PsiTreeUtil.getParentOfType(psiFinalMethodCall, PsiResourceList.class);
                    PsiResourceList tryStatement2 = PsiTreeUtil.getParentOfType(psiGetInputStream, PsiResourceList.class);
                    if(tryStatement1 != null || tryStatement2 != null) { return; }

                    gzipCompressionQuickFix = new GZIPCompressionQuickFix();
                    gzipCompressionQuickFix.psiGetInputStream = psiGetInputStream;
                    holder.registerProblem(psiFinalMethodCall, DESCRIPTION_TEMPLATE_REDUCE_SIZE, gzipCompressionQuickFix);
                }
            };
        };
    }
}
