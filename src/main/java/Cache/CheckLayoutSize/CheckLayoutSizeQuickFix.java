package Cache.CheckLayoutSize;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class CheckLayoutSizeQuickFix implements LocalQuickFix {

    private final String QUICK_FIX_NAME = "EcoAndroid: cache energy pattern - checking layout size case";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return QUICK_FIX_NAME;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

        PsiElementFactory factory = PsiElementFactory.getInstance(project);
        PsiMethod psiMethod = (PsiMethod) ( problemDescriptor.getPsiElement()).getContext();
        PsiClass psiClass = psiMethod.getContainingClass();
        PsiFile psiFile = psiClass.getContainingFile();

        try {
            Collection<PsiMethodCallExpression> psiMethodCallExpressions = PsiTreeUtil.findChildrenOfType(psiMethod.getBody(), PsiMethodCallExpression.class);
            psiMethodCallExpressions.removeIf(el -> !(el.getMethodExpression().getCanonicalText().split("\\.")[el.getMethodExpression().getCanonicalText().split("\\.").length-1].equals("getMeasuredWidth"))
            || el.getMethodExpression().getCanonicalText().split("\\.")[el.getMethodExpression().getCanonicalText().split("\\.").length-1].equals("getMeasuredHeight"));
            Iterator<PsiMethodCallExpression> iterator = psiMethodCallExpressions.iterator();
            PsiMethodCallExpression next = iterator.next();
            String[] splittedName = next.getMethodExpression().getCanonicalText().split("\\.");

            PsiCodeBlock newBody = factory.createCodeBlock();
            psiMethod.getBody().getLBrace().delete();
            psiMethod.getBody().getRBrace().delete();
            PsiStatement statement = factory.createStatementFromText("if (!(" + splittedName[0] + ".getMeasuredWidth() == 0 || " + splittedName[0]
                            + ".getMeasuredHeight() == 0)) { "+ psiMethod.getBody().getText() + " }",
                    null);
            newBody.add(statement);
            psiMethod.getBody().replace(newBody);

            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: CACHE ENERGY PATTERN APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Before resetting a view, make sure the view's measures are existent \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Application changed java file \"" + psiClass.getContainingFile().getName() + "\"\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "*/", psiClass.getContainingFile());
            psiMethod.getParent().addBefore(comment, psiMethod);
        } catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: CACHE ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.getParent().addBefore(comment, psiMethod);
        }


    }
}
