package Cache.SSLSessionCaching;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class SSLSessionCachingQuickFix implements LocalQuickFix {
    private static final String QUICK_FIX_NAME = "Refactor4Green: Cache - SSL Session Cached";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }


    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(problemDescriptor.getPsiElement(), PsiMethod.class);
        PsiClass psiClass = psiMethod.getContainingClass();
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiMethodCallExpression psiHighlightedMethodCall = (PsiMethodCallExpression) problemDescriptor.getPsiElement();

        String newCodeBlockString = "{ javax.net.ssl.SSLSessionContext sslSessionContext = context.getServerSessionContext();\n"
                + " int sessionCacheSize = sslSessionContext.getSessionCacheSize();\n"
                + " if (sessionCacheSize > 0) {\n"
                    + " \t sslSessionContext.setSessionCacheSize(0);\n"
                + "  }\n"
                + " }";
        PsiCodeBlock newCodeBlock = factory.createCodeBlockFromText(newCodeBlockString, psiClass);
        newCodeBlock.getLBrace().delete();
        newCodeBlock.getRBrace().delete();

        psiMethod.getBody().addAfter(newCodeBlock,psiHighlightedMethodCall.getParent().getNextSibling());

        JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        javaCodeStyleManager.shortenClassReferences(psiClass);


    }
}
