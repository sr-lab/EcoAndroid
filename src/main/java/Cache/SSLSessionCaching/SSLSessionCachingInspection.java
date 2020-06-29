package Cache.SSLSessionCaching;

import Cache.CheckMetadata.CheckMetadataQuickFix;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class SSLSessionCachingInspection extends LocalInspectionTool {

    private SSLSessionCachingQuickFix sslSessionCachingQuickFix = new SSLSessionCachingQuickFix();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_SSL_SESSION_CACHE = "Refactor4Green: Cache - SSL Session Cached";


            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                // isto da sslContext (que é tipo do objecto que está a ser chamado)
                expression.getMethodExpression().getQualifierExpression();

                if(!(expression.getMethodExpression().getReferenceName().equals("init"))) { return; }

                PsiMethod psiMethodResolved = expression.resolveMethod();
                PsiClass psiSSLContextClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("javax.net.ssl.SSLContext", GlobalSearchScope.allScope(holder.getProject()));
                if(!psiMethodResolved.getContainingClass().equals(psiSSLContextClass)) { return ; }

                // TODO check at more levels
                PsiMethod psiMethod = (PsiMethod) PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                Collection<PsiMethodCallExpression> methodCallExpressionsCollection = PsiTreeUtil.findChildrenOfType(psiMethod, PsiMethodCallExpression.class);
                methodCallExpressionsCollection.removeIf(el -> !(el.getMethodExpression().getReferenceName().contains("setSessionCacheSize")));

                if(methodCallExpressionsCollection.size() > 0 ) { return; }

                holder.registerProblem(expression, DESCRIPTION_TEMPLATE_SSL_SESSION_CACHE, sslSessionCachingQuickFix);


            }
        };
    }
}
