package Cache.CheckMetadata;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.CacheUpdateRunner;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

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
        PsiClass containingClass = psiMethod.getContainingClass();

        Iterator<PsiLocalVariable> iterator = intentVariables.iterator();
        String ifStatement = "if ( ";
        PsiCodeBlock declarationsBlock = factory.createCodeBlock();
        while (iterator.hasNext()) {
            PsiLocalVariable currentLocalVariable = iterator.next();
            //create the variable that will store the last value
            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) factory.createStatementFromText("public static "
                    + currentLocalVariable.getType().getCanonicalText()
                    + " last" + currentLocalVariable.getName() + " = null;", psiMethod);
            declarationsBlock.add(declarationStatement);
            String name = currentLocalVariable.getName();
            ifStatement += "last" + name + " == " + currentLocalVariable.getInitializer().getText() + " && ";
            updateStatements.add(factory.createStatementFromText("last" + name + " = " + currentLocalVariable.getInitializer().getText() + ";", containingClass));
        }
        declarationsBlock.getLBrace().delete();
        declarationsBlock.getRBrace().delete();
        containingClass.addAfter(declarationsBlock, containingClass.getLBrace());

        String newMethodBody = "private void updateValues(android.content.Intent intent) {";
        Iterator<PsiStatement> iteratorStatements = updateStatements.iterator();
        while(iteratorStatements.hasNext()) {
            PsiStatement currentStatement = iteratorStatements.next();
            newMethodBody += currentStatement.getText() + "\n";
        }
        newMethodBody += "}";
        PsiMethod updateMethod = factory.createMethodFromText(newMethodBody, null);
        containingClass.add(updateMethod);
        PsiStatement methodCallStatement = factory.createStatementFromText("updateValues(intent);", containingClass);
        psiMethod.getBody().addAfter(methodCallStatement, psiMethod.getBody().getLBrace());

        ifStatement = ifStatement.substring(0, ifStatement.length() - 4);
        //TODO: um exemplo para fazer return;
        ifStatement += ") { return; } ";
        PsiStatement statementFromText = factory.createStatementFromText(ifStatement, containingClass);
        psiMethod.getBody().addAfter(statementFromText, psiMethod.getBody().getLBrace());

        //TODO: dar \n entre os vars
        containingClass = (PsiClass) CodeStyleManager.getInstance(project).reformat(containingClass);

    }

}
