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
    private static final String QUICK_FIX_NAME = "EcoAndroid: Apply pattern Cache [Adding cache mechanism to SSL Session]";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }


    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) problemDescriptor.getPsiElement();
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiMethod.class);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiClass.class);
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiFile psiFile = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiFile.class);

        PsiElement psiElement = psiMethod;
        if(psiElement == null) {
            psiElement = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiStatement.class);
        }

        try {
            //get the name of the variable of type SSLContext
            String variableName = psiMethodCallExpression.getMethodExpression().getQualifier().getText();

            PsiStatement sslSessionContext = factory.createStatementFromText("javax.net.ssl.SSLSessionContext sslSessionContext = " + variableName + ".getServerSessionContext();\n", psiClass);
            PsiStatement sessionCacheSize = factory.createStatementFromText("int sessionCacheSize = sslSessionContext.getSessionCacheSize();\n", psiClass);
            PsiStatement ifStatement = factory.createStatementFromText("if (sessionCacheSize > 0) {\n" +
                            "\t // EcoAndroid: the next line makes the cache size of an SSL Session unlimited \n" +
                            "\t sslSessionContext.setSessionCacheSize(0); \n }", psiClass);

            PsiStatement psiStatement = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiStatement.class);
            psiStatement.getParent().addAfter(ifStatement, psiStatement);
            psiStatement.getParent().addAfter(sessionCacheSize, psiStatement);
            psiStatement.getParent().addAfter(sslSessionContext, psiStatement);

            JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
            javaCodeStyleManager.shortenClassReferences(psiClass);

            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiElement.getNode()))
                    + "* EcoAndroid: CACHE ENERGY PATTERN \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiElement.getNode()))
                    + "* Setting the cache to having no limit \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiElement.getNode()))
                    + "* Application changed file \"" + psiFile.getName() + "\". \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiElement.getNode()))
                    + "*/", psiFile);
            psiElement.addBefore(comment, psiElement.getFirstChild());
        }catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiElement.getNode()))
                    + "* EcoAndroid: CACHE ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiElement.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiElement.getNode()))
                    +"*/", psiFile);
            psiElement.addBefore(comment, psiElement.getFirstChild());
        }



    }
}
