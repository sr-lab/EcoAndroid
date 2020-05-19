package Cache.CheckMetadata;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class CheckMetadataQuickFix implements LocalQuickFix {
    private final String QUICK_FIX_NAME = "Refactor4Green: Cache - Check Metadata";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return QUICK_FIX_NAME;
    }

    ArrayList<PsiLocalVariable> intentVariables = new ArrayList<>();
    ArrayList<PsiStatement> updateStatements = new ArrayList<>();
    public void setIntentVariables(ArrayList<PsiLocalVariable> intentVariablesInspection) {
        intentVariables = intentVariablesInspection;
    }


    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        PsiElementFactory factory = PsiElementFactory.getInstance(project);
        PsiMethod psiMethod = (PsiMethod) ( problemDescriptor.getPsiElement()).getContext();
        PsiClass psiClass = psiMethod.getContainingClass();
        PsiFile psiFile = psiClass.getContainingFile();

        /*
         * ADDING COMMENT THAT SUMMARIZES CHANGES MADE TO THE CODE
         */
        PsiComment comment = factory.createCommentFromText("/* Refactor4Green: CACHE ENERGY PATTERN APPLIED \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "Whenever a request is received, checks if anything changes before using the data \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "Application changed java file \"" + psiClass.getContainingFile().getName() +
                "*/", psiClass.getContainingFile());
        psiMethod.addBefore(comment, psiMethod.getFirstChild());

        Iterator<PsiLocalVariable> iterator = intentVariables.iterator();
        String ifStatement = "if ( ";

        /*
         * FOR EVERY VARIABLE THAT IS CREATED FROM THE INTENT, CREATES A LAST+VARNAME, DELETES THE OLD VAR AND ADD IF
         */
        while (iterator.hasNext()) {
            PsiLocalVariable currentLocalVariable = iterator.next();
            //create the variable that will store the last value
            PsiCodeBlock declarationsBlock = factory.createCodeBlock();
            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) factory.createStatementFromText(currentLocalVariable.getType().getCanonicalText()
                    + " last" + currentLocalVariable.getName() + " = null;", psiMethod);
            declarationsBlock.add(declarationStatement);
            declarationsBlock.getLBrace().delete();
            declarationsBlock.getRBrace().delete();
            psiClass.addAfter(declarationsBlock, psiClass.getLBrace());
            String name = currentLocalVariable.getName();

            ifStatement += "last" + name + ".equals(" + currentLocalVariable.getInitializer().getText() + ") && ";
            updateStatements.add(factory.createStatementFromText("last" + name + " = " + currentLocalVariable.getInitializer().getText() + ";", psiClass));

            Collection<PsiReferenceExpression> references = PsiTreeUtil.collectElementsOfType(psiMethod, PsiReferenceExpression.class);
            references.removeIf(el -> !( el.getQualifiedName().equals(currentLocalVariable.getName())));
            references.forEach((el) -> { el.getChildren()[1].replace(factory.createIdentifier("last" + el.getQualifiedName())); });
            currentLocalVariable.getInitializer().getContext().delete();
        }

        String newMethodBody = "private void updateValues(android.content.Intent intent) {";
        Iterator<PsiStatement> iteratorStatements = updateStatements.iterator();
        while(iteratorStatements.hasNext()) {
            PsiStatement currentStatement = iteratorStatements.next();
            newMethodBody += currentStatement.getText();
        }
        newMethodBody += "}";
        PsiMethod updateMethod = factory.createMethodFromText(newMethodBody, null);
        psiClass.add(updateMethod);
        PsiStatement methodCallStatement = factory.createStatementFromText("updateValues(intent);", psiClass);
        psiMethod.getBody().addAfter(methodCallStatement, psiMethod.getBody().getLBrace());

        ifStatement = ifStatement.substring(0, ifStatement.length() - 4);
        //TODO: CHANGE THE LOG.INFO
        ifStatement += ") { return; } ";
        PsiStatement statementFromText = factory.createStatementFromText(ifStatement, psiClass);
        psiMethod.getBody().addAfter(statementFromText, psiMethod.getBody().getLBrace());

        psiClass = (PsiClass) CodeStyleManager.getInstance(project).reformat(psiClass);

    }

}