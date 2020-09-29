package Cache.SSLSessionCaching;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class SSLSessionCachingQuickFix implements LocalQuickFix {
    private static final String QUICK_FIX_NAME = "EcoAndroid: Cache - SSL session cached";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }


    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) problemDescriptor.getPsiElement();
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiMethod.class);
        PsiClass psiClass = psiMethod.getContainingClass();
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiFile psiFile = PsiTreeUtil.getParentOfType(psiMethod.getContainingClass(), PsiFile.class);

        try {
            //get the name of the variable of type SSLContext
            String variableName = psiMethodCallExpression.getMethodExpression().getQualifier().getText();

            PsiStatement sslSessionContext = factory.createStatementFromText("javax.net.ssl.SSLSessionContext sslSessionContext = " + variableName + ".getServerSessionContext();\n", psiClass);
            PsiStatement sessionCacheSize = factory.createStatementFromText("int sessionCacheSize = sslSessionContext.getSessionCacheSize();\n", psiClass);
            PsiStatement ifStatement = factory.createStatementFromText("if (sessionCacheSize > 0) {\n" +
                            "\t sslSessionContext.setSessionCacheSize(0); \n }"
                    , psiClass);

            PsiStatement psiStatement = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiStatement.class);
            psiStatement.getParent().addAfter(ifStatement, psiStatement);
            psiStatement.getParent().addAfter(sessionCacheSize, psiStatement);
            psiStatement.getParent().addAfter(sslSessionContext, psiStatement);

            JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
            javaCodeStyleManager.shortenClassReferences(psiClass);

            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: CACHE ENERGY PATTERN \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Increases cache size in a SSL Session \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Application changed file \"" + psiFile.getName() + "\" and xml file \"AndroidManifest.xml\". \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "*/", psiMethod.getContainingClass().getContainingFile());
            psiMethod.getParent().addBefore(comment, psiMethod);
        }catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: CACHE ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.getParent().addBefore(comment, psiMethod);
        }



    }
}
