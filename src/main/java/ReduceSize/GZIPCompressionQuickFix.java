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

    private final String QUICK_FIX_NAME = "EcoAndroid: Apply pattern Reduce Size [GZIP Compression]";
    protected PsiMethodCallExpression psiGetInputStream = null;
    protected boolean tryWithResources = false;

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
            // check if there is a variable for openConnection
            PsiLocalVariable localVariable = null;
            PsiAssignmentExpression assignmentExpression = null;
            if(!(psiElement instanceof PsiLocalVariable || psiElement instanceof PsiAssignmentExpression)) {
                localVariable = PsiTreeUtil.getParentOfType(psiElement, PsiLocalVariable.class);
                assignmentExpression = PsiTreeUtil.getParentOfType(psiElement, PsiAssignmentExpression.class);
            }
            if(psiElement instanceof PsiLocalVariable) { localVariable = (PsiLocalVariable) psiElement; }
            else if(psiElement instanceof PsiAssignmentExpression) { assignmentExpression = (PsiAssignmentExpression) psiElement; }
            String nameOpenConnection = "";
            // o openConnection esta numa local variable
            if(localVariable != null) {

                // é preciso criar a var e adicionar
                String variableType = localVariable.getType().getPresentableText();
                if(!(variableType.equals("HttpURLConnection") || variableType.equals("URLConnection")
                        || variableType.equals("JarURLConnection") || variableType.equals("HttpsURLConnection"))) {
                    PsiStatement urlConnectionDeclaration = factory.createStatementFromText("java.net.URLConnection  urlConnection = " + psiElement.getText() + ";\n", psiFile);
                    localVariable.getParent().getParent().addBefore(urlConnectionDeclaration, localVariable.getParent());
                    PsiExpression urlConnectionParameter = factory.createExpressionFromText("urlConnection", psiFile);
                    psiElement.replace(urlConnectionParameter);
                    nameOpenConnection = "urlConnection";
                    PsiStatement statement = factory.createStatementFromText(nameOpenConnection + ".setRequestProperty(\"Accept-Encoding\", \"gzip\"); \n",
                            psiClass);
                    if(tryWithResources) { localVariable.getParent().addBefore(statement, localVariable); }
                    else { localVariable.getParent().getParent().addBefore(statement, localVariable.getParent()); }
                }
                else {
                    nameOpenConnection = localVariable.getNameIdentifier().getText();
                    PsiStatement statement = factory.createStatementFromText(nameOpenConnection + ".setRequestProperty(\"Accept-Encoding\", \"gzip\"); \n",
                            psiClass);
                    if(tryWithResources) {
                        PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(localVariable, PsiTryStatement.class);
                        tryStatement.getParent().addBefore(statement, tryStatement);
                    }
                    else { localVariable.getParent().getParent().addAfter(statement, localVariable.getParent()); }
                }
            }
            else if (assignmentExpression != null) {
                String variableType = assignmentExpression.getLExpression().getType().getPresentableText();
                if(!(variableType.equals("HttpURLConnection") || variableType.equals("URLConnection")
                        || variableType.equals("JarURLConnection") || variableType.equals("HttpsURLConnection"))) {
                    PsiStatement urlConnectionDeclaration = factory.createStatementFromText(variableType + " " + psiElement.getText() + ";\n", psiFile);
                    assignmentExpression.getParent().getParent().addBefore(urlConnectionDeclaration, assignmentExpression.getParent());
                    PsiExpression urlConnectionParameter = factory.createExpressionFromText("urlConnection", psiFile);
                    psiElement.replace(urlConnectionParameter);
                    nameOpenConnection = "urlConnection";
                    PsiStatement statement = factory.createStatementFromText(nameOpenConnection + ".setRequestProperty(\"Accept-Encoding\", \"gzip\"); \n",
                            psiClass);
                    assignmentExpression.getParent().addBefore(statement, assignmentExpression);
                }
                else {
                    nameOpenConnection = assignmentExpression.getLExpression().getText();
                    PsiStatement statement = factory.createStatementFromText(nameOpenConnection + ".setRequestProperty(\"Accept-Encoding\", \"gzip\"); \n",
                            psiClass);
                    assignmentExpression.getParent().getParent().addAfter(statement, assignmentExpression.getParent());
                }
            }

            localVariable  = null;
            assignmentExpression = null;
            if(!(psiGetInputStream instanceof PsiLocalVariable || psiGetInputStream instanceof PsiAssignmentExpression)) {
                localVariable = PsiTreeUtil.getParentOfType(psiGetInputStream, PsiLocalVariable.class);
                assignmentExpression = PsiTreeUtil.getParentOfType(psiGetInputStream, PsiAssignmentExpression.class);
            }
            if(psiGetInputStream instanceof PsiLocalVariable) { localVariable = (PsiLocalVariable) psiGetInputStream; }
            else if(psiGetInputStream instanceof PsiAssignmentExpression) { assignmentExpression = (PsiAssignmentExpression) psiGetInputStream; }
            String inputStreamString = psiGetInputStream.getText();
            String variableName = "";
            if(localVariable != null) {
                variableName = localVariable.getNameIdentifier().getText();
                String initializer = localVariable.getInitializer().getText();
                PsiStatement newLocalVariable = factory.createStatementFromText(localVariable.getType().getCanonicalText() + " " + variableName + ";\n", psiFile);
                String declarationWithGZIP = initializer.replace(inputStreamString,"new java.util.zip.GZIPInputStream(" + inputStreamString + ")");
                PsiIfStatement ifStatement = (PsiIfStatement) factory.createStatementFromText("if (\"gzip\".equals(" + nameOpenConnection +".getContentEncoding())) \n" +
                        " { \n " + variableName + " = " + declarationWithGZIP + ";\n }  " +
                        "else { \n " + variableName + " = " + initializer + ";\n  }", psiClass);
                if(tryWithResources) {
                    PsiTryStatement tryStatement = PsiTreeUtil.getParentOfType(localVariable, PsiTryStatement.class);
                    tryStatement.getParent().addBefore(ifStatement, tryStatement);
                }
                else { localVariable.getParent().getParent().addAfter(ifStatement, localVariable.getParent()); }
                localVariable.replace(newLocalVariable);
            }
            else if (assignmentExpression != null )  {
                variableName = assignmentExpression.getLExpression().getText();
                String initializer = assignmentExpression.getRExpression().getText();
                String declarationWithGZIP = initializer.replace(inputStreamString,"new java.util.zip.GZIPInputStream(" + inputStreamString + ")");
                PsiIfStatement ifStatement = (PsiIfStatement) factory.createStatementFromText("if (\"gzip\".equals(" + nameOpenConnection +".getContentEncoding())) \n" +
                        " { \n " + variableName + " = " + declarationWithGZIP + ";\n }  " +
                        "else { \n " + variableName + " = " + initializer + ";\n  }", psiClass);
                assignmentExpression.getParent().getParent().addAfter(ifStatement, assignmentExpression.getParent());
                assignmentExpression.delete();
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
             psiMethod.addBefore(comment, psiMethod.getFirstChild());

        } catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: REDUCE SIZE ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
             psiMethod.addBefore(comment, psiMethod.getFirstChild());
        }
    }
}
