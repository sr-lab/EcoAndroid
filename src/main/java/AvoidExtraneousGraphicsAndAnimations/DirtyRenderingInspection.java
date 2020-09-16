package AvoidExtraneousGraphicsAndAnimations;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class DirtyRenderingInspection extends LocalInspectionTool {

    private final DirtyRenderingQuickFix dirtyRenderingQuickFix = new DirtyRenderingQuickFix();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_DIRTY_RENDERING = "Refactor4Green: Avoid Extraneous Graphics and Animations Energy Pattern - Only rendering when surface is created or when requested";


            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                if(!(expression.getMethodExpression().getReferenceName().equals("setRenderMode"))) { return; }

                PsiMethod psiMethodResolved = expression.resolveMethod();
                PsiClass psiSSLContextClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.opengl.GLSurfaceView", GlobalSearchScope.allScope(holder.getProject()));
                if(!psiMethodResolved.getContainingClass().equals(psiSSLContextClass)) { return ; }

                PsiExpression argExpression = expression.getArgumentList().getExpressions()[0];
                if(!argExpression.getText().equals("GLSurfaceView.RENDERMODE_CONTINUOUSLY")) { return; }

                holder.registerProblem(expression, DESCRIPTION_TEMPLATE_DIRTY_RENDERING, dirtyRenderingQuickFix);




            }
        };
    }
}