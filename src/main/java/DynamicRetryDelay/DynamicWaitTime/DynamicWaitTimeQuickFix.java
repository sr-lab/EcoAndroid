package DynamicRetryDelay.DynamicWaitTime;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class DynamicWaitTimeQuickFix implements LocalQuickFix {

    // NOTE: NAME THAT APPEARS AS WARNINGS TO THE USER
    private final String QUICK_FIX_NAME = "EcoAndroid: Dynamic Retry Delay Energy Pattern - switching to a dynamic wait time between resource attempts case";
    ArrayList psiReferenceExpressions = new ArrayList();
    private String counterVariableName = "accessAttempts";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return QUICK_FIX_NAME;
    }

    public void setReference(PsiReferenceExpression ref) {
        if(!psiReferenceExpressions.contains(ref))
            psiReferenceExpressions.add(ref);
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        Iterator<PsiReferenceExpression> iterator = psiReferenceExpressions.iterator();

        if(!iterator.hasNext())
            return;

        PsiReferenceExpression psiReferenceExpression = iterator.next();
        PsiFile psiFile = PsiTreeUtil.getParentOfType(psiReferenceExpression, PsiFile.class);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiReferenceExpression, PsiClass.class);
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiReferenceExpression, PsiMethod.class);

        try {
            PsiElement psiElement = problemDescriptor.getPsiElement();
            if(!(PsiUtil.isVariableNameUnique(counterVariableName, psiElement))) {
                int counterAux = 2;
                while (!PsiUtil.isVariableNameUnique(counterVariableName + counterAux, psiElement))
                    counterAux++;
                counterVariableName = counterVariableName + counterAux;
            }

            PsiExpression initializer = factory.createExpressionFromText("0", null);
            PsiDeclarationStatement counterVariable = factory.createVariableDeclarationStatement(counterVariableName, PsiType.INT, initializer);
            psiClass.addAfter(counterVariable, psiClass.getLBrace());

            while(psiReferenceExpression != null) {
                PsiMethodCallExpression methodCallExpression  = PsiTreeUtil.getParentOfType(psiReferenceExpression, PsiMethodCallExpression.class);
                replaceWithIncrementOfNewVariable(psiMethod, psiReferenceExpression.getCanonicalText(), factory, methodCallExpression);

                //NOTE: THE SECOND ARG IS TO RESOLVE REFERENCES OF THE NEW ELEMENT
                PsiExpressionStatement statement = (PsiExpressionStatement) factory.createStatementFromText( psiReferenceExpression.getCanonicalText()
                        +  " = (" + psiReferenceExpression.getType().getCanonicalText() + ") (60.0 * (Math.pow(2.0, (double) " + counterVariableName +") - 1.0));", psiMethod);
                methodCallExpression.getParent().getParent().addBefore(statement, methodCallExpression.getParent());

                if(iterator.hasNext())
                    psiReferenceExpression = iterator.next();
                else
                    break;
            }

            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: DYNAMIC RETRY DELAY ENERGY PATTERN APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Switching the wait time from constant to dynamic \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Application changed java file \"" + psiClass.getContainingFile().getName() + "\"\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.getParent().addBefore(comment, psiMethod);
        } catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: DYNAMIC RETRY DELAY ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.getParent().addBefore(comment, psiMethod);
        }


    }


    private void replaceWithIncrementOfNewVariable(@NotNull PsiMethod psiMethod, @NotNull String identifier,PsiElementFactory factory, @NotNull PsiMethodCallExpression methodCallExpression) {
        Collection<PsiAssignmentExpression> assignmentExpressions = PsiTreeUtil.collectElementsOfType(psiMethod.getBody(), PsiAssignmentExpression.class);
        Iterator<PsiAssignmentExpression> iterator = assignmentExpressions.iterator();

        while(iterator.hasNext()) {
            PsiAssignmentExpression currentAssignmentExpression = iterator.next();
            if(currentAssignmentExpression.getLExpression().getReference().getCanonicalText().equals(identifier)) {
                //NOTE:  ONLY CHANGES IF ITS A LITERAL EXPRESSION. IF ITS DYNAMICALLY COMPUTED, NOTHING CHANGES
                if(currentAssignmentExpression.getRExpression() instanceof PsiLiteralExpression) {
                    if(PsiUtilBase.compareElementsByPosition(currentAssignmentExpression, methodCallExpression) < 0) {
                        PsiPostfixExpression statement = (PsiPostfixExpression) factory.createExpressionFromText(counterVariableName + "++", null);
                        currentAssignmentExpression.replace(statement);
                    }
                }
            }
        }
    }

}
