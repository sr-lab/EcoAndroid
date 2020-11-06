package DynamicRetryDelay.DynamicWaitTime;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

public class DynamicWaitTimeInspection extends LocalInspectionTool {

    private DynamicWaitTimeQuickFix dynamicWaitTimeQuickFix;
    private final DynamicWaitTimeInfoWarningQuickFix dynamicWaitTimeInfoWarningQuickFix = new DynamicWaitTimeInfoWarningQuickFix();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            // the texts that appears when you pass the mouse through it wo/ clicking it
            @NonNls
            private final String DESCRIPTION_TEMPLATE_DYNAMIC_WAIT_TIME = "EcoAndroid: Dynamic Retry Delay [Dynamic Wait Time]";

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {

                if(!(expression.getMethodExpression().getCanonicalText().equals("Thread.sleep")))
                    return;
                PsiExpression argument =  expression.getArgumentList().getExpressions()[0];
                PsiMethod psiMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                // NOTE: THREAD.SLEEP ONLY HAS 1 ARG (WHICH IS TIME), HOWEVER THIS COULD BE AN EXPRESSION AND I WANT THE REFERENCE EXPRESSION
                PsiReferenceExpression timeVariable;
                if(!(argument instanceof PsiReferenceExpression)) {
                    Collection<PsiReferenceExpression> psiReferenceExpressions = PsiTreeUtil.findChildrenOfAnyType(argument, PsiReferenceExpression.class);
                    if(psiReferenceExpressions.size() == 0)
                        return;
                    timeVariable = psiReferenceExpressions.iterator().next();
                }
                else
                    timeVariable = (PsiReferenceExpression) argument;

                //NOTE: SINCE ITS CONSTANTLY INSPECTING, IF THE USER SWITCHES FROM LOCAL VARIABLE TO PARAMETER OR VICE VERSA, IT NEEDS TO CLEAN THE COLLECTION IN THE QUICK FIX
                if(timeVariable.resolve() instanceof PsiLocalVariable) {
                    dynamicWaitTimeQuickFix = new DynamicWaitTimeQuickFix();
                    //NOTE: GET ALL ASSIGNMENTS OF THE VARIABLE
                    Collection<PsiAssignmentExpression> assignmentExpressions = PsiTreeUtil.collectElementsOfType(psiMethod.getBody(), PsiAssignmentExpression.class);
                    Predicate<PsiAssignmentExpression> predicate = el -> !((el.getLExpression().getReference().getCanonicalText().equals(timeVariable.getReference().getCanonicalText()))
                            && (el.getOperationSign().getTokenType().equals(JavaTokenType.EQ))
                            && (el.getRExpression() instanceof PsiLiteralExpression)
                            && (PsiUtilBase.compareElementsByPosition(el, expression) < 0));
                    assignmentExpressions.removeIf(predicate);
                    Iterator<PsiAssignmentExpression> iterator = assignmentExpressions.iterator();
                    if(iterator.hasNext()) {
                        dynamicWaitTimeQuickFix.setReference(timeVariable);
                        holder.registerProblem(timeVariable, DESCRIPTION_TEMPLATE_DYNAMIC_WAIT_TIME, dynamicWaitTimeQuickFix, dynamicWaitTimeInfoWarningQuickFix);
                        return;
                    }
                }

                if(timeVariable.resolve() instanceof PsiParameter) {
                    dynamicWaitTimeQuickFix = new DynamicWaitTimeQuickFix();
                    PsiParameter parameter = (PsiParameter) timeVariable.resolve();
                    PsiMethod method = (PsiMethod) parameter.getDeclarationScope();
                    int index = method.getParameterList().getParameterIndex(parameter);

                    Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(psiMethod.getBody(), PsiMethodCallExpression.class);
                    //NOTE: GET ALL METHOD CALLS USED WITH THIS VARIABLE
                    Predicate<PsiMethodCallExpression> predicateMethodCall = el -> !((el.getMethodExpression().getCanonicalText().equals(method.getName())));
                    methodsCalls.removeIf(predicateMethodCall);
                    for (PsiMethodCallExpression currentMethodCall : methodsCalls) {
                        PsiReferenceExpression ref = (PsiReferenceExpression) currentMethodCall.getArgumentList().getExpressions()[index];
                        Collection<PsiAssignmentExpression> assignmentExpressions = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiAssignmentExpression.class);
                        Predicate<PsiAssignmentExpression> predicateAssignment = el -> !((el.getLExpression().getReference().getCanonicalText().equals(ref.getReference().getCanonicalText()))
                                && (el.getOperationSign().getTokenType().equals(JavaTokenType.EQ))
                                && (el.getRExpression() instanceof PsiLiteralExpression)
                                && (PsiUtilBase.compareElementsByPosition(el, currentMethodCall) < 0));
                        assignmentExpressions.removeIf(predicateAssignment);
                        //NOTE: ADD EVERY REFERENCE THAT IS USED TO CALL THE FUNCTION
                        if (assignmentExpressions.size() > 0)
                            dynamicWaitTimeQuickFix.setReference(ref);
                    }
                    if(dynamicWaitTimeQuickFix.psiReferenceExpressions.size() > 0) {
                        PsiComment[] comments = PsiTreeUtil.getChildrenOfType(psiMethod, PsiComment.class);
                        if(comments != null) {
                            for (PsiComment comment : comments) {
                                if(comment.getText().startsWith("/*\n             * TODO EcoAndroid")) {
                                    holder.registerProblem(timeVariable, DESCRIPTION_TEMPLATE_DYNAMIC_WAIT_TIME, dynamicWaitTimeQuickFix);
                                    return;
                                }
                            }
                        }
                        holder.registerProblem(timeVariable, DESCRIPTION_TEMPLATE_DYNAMIC_WAIT_TIME, dynamicWaitTimeQuickFix, dynamicWaitTimeInfoWarningQuickFix);
                    }
                }
            }
        };
    }
}
