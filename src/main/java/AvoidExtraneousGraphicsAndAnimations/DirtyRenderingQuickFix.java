package AvoidExtraneousGraphicsAndAnimations;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class DirtyRenderingQuickFix implements LocalQuickFix {
    private static final String QUICK_FIX_NAME = "Refactor4Green: Avoid Extraneous Graphics and Animations Energy Pattern - Only rendering when surface is created or when requested";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) descriptor.getPsiElement();
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiMethod.class);
        PsiClass psiClass = psiMethod.getContainingClass();
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiFile psiFile = psiClass.getContainingFile();

        try {
            PsiExpression argExpression = psiMethodCallExpression.getArgumentList().getExpressions()[0];

            PsiExpression psiNewExpression = factory.createExpressionFromText("GLSurfaceView.RENDERMODE_WHEN_DIRTY",null);
            argExpression.replace(psiNewExpression);

            PsiComment comment = factory.createCommentFromText("/* \n "
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Refactor4Green: AVOID EXTRANEOUS GRAPHICS AND ANIMATIONS ENERGY PATTERN APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Changing rendering mode to one that only renders when it is created or when is it requested by the method \"requestRender()\"\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Application changed java file \"" + psiClass.getContainingFile().getName() + "\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "*/", psiClass.getContainingFile());
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
        }catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Refactor4Green: CACHE ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
        }



    }
}
