package ReduceSize;

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

public class GZIPCompressionQuickFix implements LocalQuickFix {

    private final String QUICK_FIX_NAME = "EcoAndroid: Reduce Size Energy Pattern - gzip compression before receiving data";
    protected PsiMethodCallExpression psiGetInputStream = null;

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {

        // o psiElement é o psiOpenConnection
        PsiElement psiElement = descriptor.getPsiElement();
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
        PsiFile psiFile = psiClass.getContainingFile();
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);
        PsiElementFactory factory = PsiElementFactory.getInstance(project);
        try {
            PsiLocalVariable localVariable = PsiTreeUtil.getParentOfType(psiElement, PsiLocalVariable.class);
            PsiAssignmentExpression assignmentExpression = PsiTreeUtil.getParentOfType(psiElement, PsiAssignmentExpression.class);
            String name = "";
            String nameReader = "";
            String newString = "";
            if(localVariable != null) {
                name = localVariable.getNameIdentifier().getText();
                PsiStatement statement = factory.createStatementFromText(name + ".setRequestProperty(\"Accept-Encoding\", \"gzip\"); \n", psiClass);
                localVariable.getParent().getParent().addAfter(statement, localVariable.getParent());
            }
            else if(assignmentExpression != null) {
                name = assignmentExpression.getLExpression().getText();
                PsiStatement statement = factory.createStatementFromText(name + ".setRequestProperty(\"Accept-Encoding\", \"gzip\"); \n", psiClass);
                assignmentExpression.getParent().getParent().addAfter(statement, assignmentExpression.getParent());
            }

            localVariable = PsiTreeUtil.getParentOfType(psiGetInputStream, PsiLocalVariable.class);
            assignmentExpression = PsiTreeUtil.getParentOfType(psiGetInputStream, PsiAssignmentExpression.class);
            if(localVariable != null) {
                nameReader = localVariable.getNameIdentifier().getText();
                newString = localVariable.getInitializer().getText();
                PsiExpression parameter = factory.createExpressionFromText("new java.util.zip.GZIPInputStream(" + psiGetInputStream.getText() + ")", psiClass);
                psiGetInputStream.replace(parameter);
                String newString2 = "";
                newString2 = localVariable.getInitializer().getText();
                PsiIfStatement ifStatement = (PsiIfStatement) factory.createStatementFromText("if (\"gzip\".equals(" + name +".getContentEncoding())) \n" +
                        " { \n " + nameReader + " = " + newString2 + ";\n }  " +
                        "else { \n " + nameReader + " = " + newString + ";\n  }", psiClass);
                PsiStatement reader = factory.createStatementFromText(localVariable.getType().getCanonicalText() + " " + nameReader + " = null; ", psiClass);
                localVariable.getParent().getParent().addBefore(reader, localVariable.getParent());
                localVariable.replace(ifStatement);

            }
            else if(assignmentExpression != null) {
                nameReader = assignmentExpression.getLExpression().getText();
                newString = assignmentExpression.getRExpression().getText();
                PsiExpression parameter = factory.createExpressionFromText("new java.util.zip.GZIPInputStream(" + psiGetInputStream.getText() + ")", psiClass);
                psiGetInputStream.replace(parameter);
                String newString2 = "";
                newString2 = assignmentExpression.getLExpression().getText();
                PsiIfStatement ifStatement = (PsiIfStatement) factory.createStatementFromText("if (\"gzip\".equals(" + name +".getContentEncoding())) \n" +
                        " { \n " + nameReader + " = " + newString2 + ";\n }  " +
                        "else { \n " + nameReader + " = " + newString + ";\n  }", psiClass);
                PsiStatement reader = factory.createStatementFromText(assignmentExpression.getLExpression().getType().getCanonicalText() + " " + nameReader + " = null; ", psiClass);
                assignmentExpression.getParent().getParent().addBefore(reader, assignmentExpression.getParent());
                assignmentExpression.replace(ifStatement);
            }
            JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
            javaCodeStyleManager.shortenClassReferences(psiClass);

            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: REDUCE SIZE ENERGY PATTERN APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* The goal is to use gzip compression before receiving data\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Application changed java file \"" + psiClass.getContainingFile().getName() + "\"\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "*/", psiClass.getContainingFile());
            psiMethod.getParent().addBefore(comment, psiMethod);

        } catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: REDUCE SIZE ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.getParent().addBefore(comment, psiMethod);
        }
    }
}
