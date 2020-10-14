package Cache.URLCaching;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class URLCachingInspection extends LocalInspectionTool {

    private final URLCachingQuickFix urlCachingQuickFix = new URLCachingQuickFix();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_URLCACHING = "EcoAndroid: Cache [Adding caching mechanism to URL Connection]";


            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                if(!(expression.getMethodExpression().getReferenceName().equals("openConnection"))) { return; }

                PsiMethod psiMethodResolved = expression.resolveMethod();
                PsiClass psiSSLContextClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("java.net.URL", GlobalSearchScope.allScope(holder.getProject()));
                if(!psiMethodResolved.getContainingClass().equals(psiSSLContextClass)) { return ; }

                // TODO check at more levels
                PsiMethod psiMethod = (PsiMethod) PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                Collection<PsiMethodCallExpression> methodCallExpressionsCollection = PsiTreeUtil.findChildrenOfType(psiMethod, PsiMethodCallExpression.class);

                methodCallExpressionsCollection.removeIf(el -> !(el.getMethodExpression().getReferenceName().contains("getHeaderFieldDate")));
                methodCallExpressionsCollection.removeIf(el -> !(el.getArgumentList().getExpressions().length > 0));
                methodCallExpressionsCollection.removeIf(el -> !(el.getArgumentList().getExpressions()[0].getText().equals("\"Last-Modified\"")));
                if(methodCallExpressionsCollection.size() > 0 ) { return; }

                if(PsiTreeUtil.getParentOfType(expression, PsiReturnStatement.class) != null) { return; }
                if(PsiTreeUtil.getParentOfType(expression, PsiReferenceExpression.class) != null) { return; }

                holder.registerProblem(expression, DESCRIPTION_TEMPLATE_URLCACHING, urlCachingQuickFix);




            }
        };
    }
}
