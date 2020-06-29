package Cache.CheckLayoutSize;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CheckLayoutSizeInspection extends LocalInspectionTool {

    private CheckLayoutSizeQuickFix checkLayoutSizeQuickFix;

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_CHECK_SIZE = "REFACTOR 4 GREEN - CHECK LAYOUT SIZE BEFORE RESETTING";

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                /*
                 *
                 *  FIRST PHASE - FIND WHERE THE METHOD SurfaceHolder.setFixedSize( x, y ) OR view.setSizeFromLayout()
                 *
                 */
                String methodCallExpressionString = expression.getMethodExpression().getCanonicalText();
                String[] methodCallExpressionStringArray = methodCallExpressionString.split("\\.");

                if(methodCallExpressionStringArray.length < 2) { return; }

                // TODO: THE SAME WITH MEASURE HEIGHT
                if(!methodCallExpressionStringArray[methodCallExpressionStringArray.length-1].equals("getMeasuredWidth")) { return; }

                if(expression.getContext() instanceof PsiBinaryExpression) { return; }

                /*
                 *
                 *  SECOND & THIRD PHASE - GO BACKWARDS TO WHERE THE SIZE IS RETRIEVED AND CHECK IF ITS BEING SEEN IF ITS 0 OR NOT
                 *
                 */
                // get method the call is inserted in
                PsiMethod psiMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                PsiStatement[] statements = psiMethod.getBody().getStatements();
                boolean isGoingToBeSet = false;
                for (PsiStatement statement : statements) {
                    Collection<PsiMethodCallExpression> methodCallExpressions = PsiTreeUtil.findChildrenOfType(statement, PsiMethodCallExpression.class);
                    methodCallExpressions.removeIf( el -> !(
                            el.getMethodExpression().getCanonicalText().split("\\.")[el.getMethodExpression().getCanonicalText().split("\\.").length-1].equals("setFixedSize")));
                    // means que ha uma chamada a setFixedSize
                    if(methodCallExpressions.size() > 0) {
                        PsiMethodCallExpression next = methodCallExpressions.iterator().next();
                        PsiMethod resolvedMethod = (PsiMethod) next.getMethodExpression().resolve();
                        PsiClass psiClass = resolvedMethod.getContainingClass();
                        PsiClass surfaceHolderClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.view.SurfaceHolder", GlobalSearchScope.allScope(holder.getProject()));
                        if(psiClass.equals(surfaceHolderClass) || hasClassInList(psiClass.getImplementsList(), surfaceHolderClass)) {
                            if(variableIsUsedInTheSettingOfTheSize(next, expression)) {
                                isGoingToBeSet = true;
                                break;
                            }
                        }
                    }
                }


                if(isGoingToBeSet) {
                    // TODO: MAKE SURE ITS CALLING THE METHOD OF THE CORRECT CLASS
                    Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(psiMethod.getBody(), PsiMethodCallExpression.class);
                    methodCalls.removeIf(el -> !(el.getMethodExpression().getCanonicalText().split("\\.")[el.getMethodExpression().getCanonicalText().split("\\.").length-1].equals("getMeasuredWidth")));
                    AtomicBoolean isChecked = new AtomicBoolean(false);
                    methodCalls.forEach( el -> {
                        if(el.getContext() instanceof PsiBinaryExpression) {
                            PsiBinaryExpression binaryExpression = (PsiBinaryExpression) el.getContext();
                            if(binaryExpression.getROperand().getText().equals("0")) {
                                isChecked.set(true);
                            }
                        }
                    });
                    if(!isChecked.get()) {
                        checkLayoutSizeQuickFix = new CheckLayoutSizeQuickFix();
                        holder.registerProblem(psiMethod.getNameIdentifier(), DESCRIPTION_TEMPLATE_CHECK_SIZE, checkLayoutSizeQuickFix);
                    }
                }
            }
            private boolean hasClassInList(PsiReferenceList list, PsiClass psiClass) {
                for (PsiElement child : list.getChildren()) {
                    if(child.equals(psiClass)) { return true; }
                }
                return false;
            }

            private boolean variableIsUsedInTheSettingOfTheSize(PsiMethodCallExpression updateSizeMethodCall, PsiMethodCallExpression retrieveSizeMethodCall) {
                PsiMethod psiMethod = PsiTreeUtil.getParentOfType(retrieveSizeMethodCall, PsiMethod.class);
                if(retrieveSizeMethodCall.getContext() instanceof PsiAssignmentExpression) {
                    PsiAssignmentExpression psiAssignmentExpression = (PsiAssignmentExpression) retrieveSizeMethodCall.getContext();
                    PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) psiAssignmentExpression.getLExpression();
                    PsiReference psiReference = psiReferenceExpression.getReference();

                    Collection<PsiReferenceExpression> references = PsiTreeUtil.findChildrenOfType(psiMethod.getBody(), PsiReferenceExpression.class);
                    references.removeIf(el -> !(el.getReference().getCanonicalText().equals(psiReference.getCanonicalText())) || el.getContext().equals(psiAssignmentExpression));

                    List<PsiReferenceExpression> listReferences = new LinkedList<PsiReferenceExpression>(references);
                    while(listReferences.size() > 0) {
                        PsiReferenceExpression currentPsiReference = listReferences.get(0);
                        PsiDeclarationStatement psiDeclarationStatement = (PsiDeclarationStatement) PsiTreeUtil.findFirstParent(currentPsiReference.getContext(), el -> el instanceof  PsiDeclarationStatement);
                        PsiElement psiMethodCallExpression = PsiTreeUtil.findFirstParent(currentPsiReference.getContext(), el -> el instanceof PsiMethodCallExpression);
                        if(psiDeclarationStatement != null) {
                            PsiLocalVariable declaredElement = (PsiLocalVariable) psiDeclarationStatement.getDeclaredElements()[0];
                            references = PsiTreeUtil.findChildrenOfType(psiMethod.getBody(), PsiReferenceExpression.class);
                            references.removeIf(el -> !( el.getReference().getCanonicalText().equals(declaredElement.getName())) || el.getContext().equals(psiAssignmentExpression));
                            listReferences.addAll(references);
                        }
                        else if (psiMethodCallExpression != null && psiMethodCallExpression.equals(updateSizeMethodCall)){ return true; }
                        listReferences.remove(0);
                    }
                }
                return false;
            }
        };


    }
}
