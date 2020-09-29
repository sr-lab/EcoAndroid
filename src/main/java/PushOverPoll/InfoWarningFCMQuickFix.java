package PushOverPoll;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class InfoWarningFCMQuickFix implements LocalQuickFix {

    private final String QUICK_FIX_NAME = "EcoAndroid: Push Over Poll Energy Pattern - info warning about Firebase Cloud Messaging";


    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiElement psiElement = descriptor.getPsiElement();
        PsiFile psiFile = PsiTreeUtil.getParentOfType(psiElement, PsiFile.class);
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);

        PsiComment comment = factory.createCommentFromText("/* \n "
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* TODO EcoAndroid \n "
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* PUSH OVER POLL ENERGY PATTERN INFO WARNING \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* An alternative to a polling service is a to use push notifications\n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* One way to implement them is to use Firebase Cloud Messaging \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* FCM uses an API and works with Android Studio 1.4 or higher with Gradle projects \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* If you wish to know more about this topic, read the following information:\n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* https://firebase.google.com/docs/cloud-messaging/android/client \n "
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "*/", psiFile);
        psiMethod.getParent().addBefore(comment, psiMethod);

    }
}
