package Cache.CheckLayoutSize;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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
                PsiMethod psiMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
                PsiStatement[] statements = psiMethod.getBody().getStatements();
                boolean isGoingToBeSet = false;
                for (PsiStatement statement : statements) {
                    Collection<PsiMethodCallExpression> childOfType = PsiTreeUtil.findChildrenOfType(statement, PsiMethodCallExpression.class);
                    childOfType.removeIf( el -> !(
                            el.getMethodExpression().getCanonicalText().split("\\.")[el.getMethodExpression().getCanonicalText().split("\\.").length-1].equals("setFixedSize")));
                    if(childOfType.size() > 0) {
                        PsiMethodCallExpression next = childOfType.iterator().next();
                        PsiMethod resolvedMethod = (PsiMethod) next.getMethodExpression().resolve();
                        PsiClass psiClass = resolvedMethod.getContainingClass();
                        PsiClass surfaceHolderClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.view.SurfaceHolder", GlobalSearchScope.allScope(holder.getProject()));
                        if(psiClass.equals(surfaceHolderClass) || hasClassInList(psiClass.getImplementsList(), surfaceHolderClass)) {
                            isGoingToBeSet = true;
                            break;
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
        };


    }
}
