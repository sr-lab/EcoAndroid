import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

public class DynamicRetryDelayInspection extends LocalInspectionTool {

    //instantiates the quickFix that changes wait time from constant to dynamic
    private DynamicWaitTimeQuickFix drdQuickFix;

    // create UI panel
    public JComponent createOptionPanel() {
        return new JPanel(new FlowLayout(FlowLayout.LEFT));
    }

    // creates visitor
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            /**
             *  This string defines the short message shown to a user signaling the inspection
             *  found a problem. It reuses a string from the inspections bundle.
             */
            @NonNls
            private final String DESCRIPTION_TEMPLATE_DYNAMIC_WAIT_TIME = "Refactor4Green:" + " Dynamic Retry Delay Dynamic Wait Time";


            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {

                /*
                 * FIRST PHASE: CHECK IF THE METHOD CALL IS FOR A THREAD.SLEEP & FIND THE VARIABLE USED TO CALCULATE THE TIME TO SLEEP THE THREAD
                 */
                if(!(expression.getMethodExpression().getCanonicalText().equals("Thread.sleep"))) { return; }
                PsiExpression argument =  expression.getArgumentList().getExpressions()[0];
                PsiMethod psiMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                // Thread.sleep apenas tem 1 arg que é o tempo, no entanto este pode ser uma expressão. eu quero a ref
                Collection<PsiReferenceExpression> childrenOfAnyType = PsiTreeUtil.findChildrenOfAnyType(argument, PsiReferenceExpression.class);
                //TODO: neste momento, só com variaveis
                if(childrenOfAnyType.size() == 0) { return; }
                PsiReferenceExpression timeVariable = childrenOfAnyType.iterator().next();

                /*
                 *  SECOND PHASE: CHECK THE WHERE THE VARIABLE COMES FROM: LOCAL VARIABLE OR PARAMETER
                 *  OPTION 1: VALUE IS FROM A LOCAL VARIABLE
                 */
                //NOTE: SINCE ITS CONSTANTLY INSPECTING, IF THE USER SWITCHES FROM LOCAL VARIABLE TO PARAMETER OR VICE VERSA, IT NEEDS TO CLEAN THE COLLECTION IN THE QUICK FIX
                if(timeVariable.resolve() instanceof PsiLocalVariable) {
                    drdQuickFix = new DynamicWaitTimeQuickFix();
                    //NOTE: GET ALL ASSIGNMENTS OF THE VARIABLE
                    Collection<PsiAssignmentExpression> assignmentExpressions = PsiTreeUtil.collectElementsOfType(psiMethod.getBody(), PsiAssignmentExpression.class);
                    Predicate<PsiAssignmentExpression> predicate = a -> !((a.getLExpression().getReference().getCanonicalText().equals(timeVariable.getReference().getCanonicalText()))
                                                                    && (a.getRExpression() instanceof PsiLiteralExpression)
                                                                    && (PsiUtilBase.compareElementsByPosition(a, expression) < 0));
                    assignmentExpressions.removeIf(predicate);
                    Iterator<PsiAssignmentExpression> iterator = assignmentExpressions.iterator();
                    if(iterator.hasNext()) {
                        drdQuickFix.setReference(timeVariable);
                        holder.registerProblem(timeVariable, DESCRIPTION_TEMPLATE_DYNAMIC_WAIT_TIME, drdQuickFix);
                        return;
                    }
                }
                /*
                 *  SECOND PHASE: CHECK THE WHERE THE VARIABLE COMES FROM: LOCAL VARIABLE OR PARAMETER
                 *  OPTION 2: VALUE IS FROM A PARAMETER
                 */
                if(timeVariable.resolve() instanceof PsiParameter) {
                    drdQuickFix = new DynamicWaitTimeQuickFix();
                    PsiParameter parameter = (PsiParameter) timeVariable.resolve();
                    PsiMethod method = (PsiMethod) parameter.getDeclarationScope();
                    int index = method.getParameterList().getParameterIndex(parameter);

                    Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(psiMethod.getBody(), PsiMethodCallExpression.class);
                    //NOTE: GET ALL METHOD CALLS USED WITH THIS VARIABLE
                    Predicate<PsiMethodCallExpression> predicateMethodCall = a -> !((a.getMethodExpression().getCanonicalText().equals(method.getName())));
                    methodsCalls.removeIf(predicateMethodCall);
                    Iterator<PsiMethodCallExpression> iterator = methodsCalls.iterator();
                    while(iterator.hasNext()) {
                        PsiMethodCallExpression currentMethodCall = iterator.next();
                        PsiReferenceExpression ref = (PsiReferenceExpression) currentMethodCall.getArgumentList().getExpressions()[index];
                        Collection<PsiAssignmentExpression> assignmentExpressions = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiAssignmentExpression.class);
                        Predicate<PsiAssignmentExpression> predicateAssignment = a -> !((a.getLExpression().getReference().getCanonicalText().equals(ref.getReference().getCanonicalText()))
                                                                            && (a.getRExpression() instanceof PsiLiteralExpression)
                                                                            && (PsiUtilBase.compareElementsByPosition(a, currentMethodCall) < 0));
                        assignmentExpressions.removeIf(predicateAssignment);
                        if(assignmentExpressions.size() > 0) {
                            //NOTE: ADD EVERY REFERENCE THAT IS USED TO CALL THE FUNCTION
                            drdQuickFix.setReference(ref);
                        }
                    }
                    if(drdQuickFix.psiReferenceExpressions.size() > 0)
                        holder.registerProblem(timeVariable, DESCRIPTION_TEMPLATE_DYNAMIC_WAIT_TIME, drdQuickFix);
                    return;
                }
            }

        };
    }
}
