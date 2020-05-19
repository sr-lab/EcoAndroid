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

    private final String QUICK_FIX_NAME = "Refactor4Green: Cache - Check Layout Size";

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

        PsiComment comment = factory.createCommentFromText("/* Refactor4Green: CACHE ENERGY PATTERN APPLIED \n"
               // + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "Before resetting a view, make sure the view's measures are existent \n"
               // + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "Application changed java file \"" + psiClass.getContainingFile().getName() +
                "*/", psiClass.getContainingFile());
        psiMethod.addBefore(comment, psiMethod.getFirstChild());

        Collection<PsiMethodCallExpression> psiMethodCallExpressions = PsiTreeUtil.findChildrenOfType(psiMethod.getBody(), PsiMethodCallExpression.class);
        psiMethodCallExpressions.removeIf(el -> !(el.getMethodExpression().getCanonicalText().split("\\.")[el.getMethodExpression().getCanonicalText().split("\\.").length-1].equals("getMeasuredWidth")));
        Iterator<PsiMethodCallExpression> iterator = psiMethodCallExpressions.iterator();

        System.out.println(psiMethodCallExpressions.size());
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

    }
}
