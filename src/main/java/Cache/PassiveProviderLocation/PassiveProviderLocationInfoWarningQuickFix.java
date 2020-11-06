package Cache.PassiveProviderLocation;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class PassiveProviderLocationInfoWarningQuickFix implements LocalQuickFix {
    private static final String QUICK_FIX_NAME = "EcoAndroid: Cache - possible switch to PASSIVE_PROVIDER";

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
                + "* CACHE ENERGY PATTERN INFO WARNING \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* Another type of provider for LocationManager is PASSIVE_PROVIDER \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* This provider uses a cache mechanism to retrieve the location, which consumes less energy then the other options \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* This approach uses the android.location package \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* If you wish to know more about this topic, read the following information:\n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "* https://developer.android.com/reference/android/location/LocationManager#PASSIVE_PROVIDER \n "
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "*/", psiFile);
        psiMethod.addBefore(comment, psiMethod.getFirstChild());
    }
}
