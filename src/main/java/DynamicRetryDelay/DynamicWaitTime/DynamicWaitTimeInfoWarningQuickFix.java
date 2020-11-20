package DynamicRetryDelay.DynamicWaitTime;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class DynamicWaitTimeInfoWarningQuickFix implements LocalQuickFix {

    private final String QUICK_FIX_NAME = "EcoAndroid: Information about applying pattern Dynamic Retry Delay [Dynamic Wait Time]";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiElement psiElement = problemDescriptor.getPsiElement();
        PsiFile psiFile = PsiTreeUtil.getParentOfType(psiElement, PsiFile.class);
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);

        PsiComment comment = factory.createCommentFromText("/* \n "
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* TODO EcoAndroid \n "
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* DYNAMIC RETRY DELAY ENERGY PATTERN INFO WARNING \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* Another way to implement a mechanism that manages the execution of tasks and their retrying, if said task fails \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* This approach uses the android.work package \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* If you wish to know more about this topic, read the following information:\n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work \n "
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "*/", psiFile);
        psiMethod.addBefore(comment, psiMethod.getFirstChild());
    }
}
