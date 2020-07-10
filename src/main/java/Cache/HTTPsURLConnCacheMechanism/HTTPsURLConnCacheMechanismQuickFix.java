package Cache.HTTPsURLConnCacheMechanism;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.formatting.Indent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class HTTPsURLConnCacheMechanismQuickFix implements LocalQuickFix {
    private static final String QUICK_FIX_NAME = "Refactor4Green: Cache - SSL Session Cached";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) descriptor.getPsiElement();
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiMethod.class);
        PsiClass psiClass = psiMethod.getContainingClass();
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiFile psiFile = psiClass.getContainingFile();

        try {



            String lastUpdateTimeName = "lastUpdateTime";
            if(!(PsiUtil.isVariableNameUnique(lastUpdateTimeName, psiClass))) {
                int counterAux = 2;
                while (!PsiUtil.isVariableNameUnique(lastUpdateTimeName + counterAux, psiClass))
                    counterAux++;
                lastUpdateTimeName = lastUpdateTimeName + counterAux;
            }
            PsiStatement lastUpdateTime;
            if(psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                lastUpdateTime = factory.createStatementFromText("static long " + lastUpdateTimeName +  " = 0;", null);
            }else {
                lastUpdateTime = factory.createStatementFromText("long " + lastUpdateTimeName +  " = 0;", null);
            }
            psiClass.addAfter(lastUpdateTime, psiClass.getLBrace());

            String variableName = retrieveVariableName(psiMethodCallExpression);

            PsiStatement currentTimeStatement = factory.createStatementFromText("long currentTime = java.lang.System.currentTimeMillis();\n", psiClass);
            PsiStatement lastModifiedStatement = factory.createStatementFromText("long lastModified = " + variableName + ".getHeaderFieldDate(\"Last-Modified\", currentTime);\n", psiClass);
            PsiIfStatement ifStatement = (PsiIfStatement) factory.createStatementFromText("if (lastModified < " + lastUpdateTimeName + " ) \n" +
                    " { \n //TODO: Skip Update \n }  else { \n" + lastUpdateTimeName + "+= lastModified; }", psiClass);

            PsiStatement psiStatement = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiStatement.class);
            psiStatement.getParent().addAfter(ifStatement, psiStatement);
            psiStatement.getParent().addAfter(lastModifiedStatement, psiStatement);
            psiStatement.getParent().addAfter(currentTimeStatement, psiStatement);

            JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
            javaCodeStyleManager.shortenClassReferences(psiClass);

            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Refactor4Green: CACHE ENERGY PATTERN APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Only process update when something changed. The application inserts annotations in the source code in order for the developer to know where to place the code\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Application changed java file \"" + psiClass.getContainingFile().getName() + "\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "*/", psiClass.getContainingFile());
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
        } catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Refactor4Green: CACHE ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
        }
    }

    String retrieveVariableName(PsiExpression expression) {
        if(expression.getContext() instanceof PsiAssignmentExpression) {
            PsiAssignmentExpression psiAssignmentExpression = (PsiAssignmentExpression) expression.getContext();
            return psiAssignmentExpression.getLExpression().getText();
        }
        else if(expression.getContext() instanceof PsiLocalVariable){
            PsiLocalVariable psiLocalVariable = (PsiLocalVariable) expression.getContext();
            return psiLocalVariable.getNameIdentifier().getText();

        }
        else if(expression.getContext() instanceof PsiTypeCastExpression) {
            PsiTypeCastExpression psiTypeCastExpression = (PsiTypeCastExpression) expression.getContext();
            return retrieveVariableName(psiTypeCastExpression);
        }
        return "";
    }
}
