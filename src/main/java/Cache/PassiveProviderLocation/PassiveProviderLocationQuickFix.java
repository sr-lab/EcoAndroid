package Cache.PassiveProviderLocation;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class PassiveProviderLocationQuickFix implements LocalQuickFix {
    private static final String QUICK_FIX_NAME = "Refactor4Green - PASSIVE_PROVIDER";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiMethodCallExpression psiMethodCallExpression = (PsiMethodCallExpression) problemDescriptor.getPsiElement();
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiMethod.class);
        PsiFile psiFile = PsiTreeUtil.getParentOfType(psiMethod.getContainingClass(), PsiFile.class);
        PsiDirectory psiDirectory = psiFile.getContainingDirectory();

        PsiComment comment = factory.createCommentFromText("/* Refactor4Green: CACHE ENERGY PATTERN \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "This energy pattern changes the type of LocationManager to \"PASSIVE_PROVIDER\". \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "This change consumes less energy because it doesn't actually initiating a location fix.  \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                + "Application changed file \"" + psiFile.getName() + " and xml file \"AndroidManifest.xml\"." +
                "*/", psiMethod.getContainingClass().getContainingFile());
        psiMethodCallExpression.addBefore(comment, psiMethod.getFirstChild());

        // mudar o argumento na chamada a funcao
        // TODO: make sure que so tenho que mudar esse
        PsiExpression psiExpression = psiMethodCallExpression.getArgumentList().getExpressions()[0];
        PsiExpression psiNewExpression = factory.createExpressionFromText("LocationManager.PASSIVE_PROVIDER",null);
        psiExpression.replace(psiNewExpression);

        // adicionar a linha ao ficheiro AndroidManifest.xml
        XmlElementFactory xmlElementFactory = XmlElementFactory.getInstance(project);
        XmlFile xmlFile = null;
        PsiDirectory currentPsiDirectory = psiDirectory;
        while (currentPsiDirectory != null) {
            xmlFile = (XmlFile) currentPsiDirectory.findFile("AndroidManifest.xml");
            if(xmlFile != null) { break; }
            PsiDirectory[] subDirectories = currentPsiDirectory.getSubdirectories();
            boolean checked = false;
            for (int i = 0; i < subDirectories.length; i++) {
                xmlFile = (XmlFile) currentPsiDirectory.findFile("AndroidManifest.xml");
                if(xmlFile != null) {
                    checked = true;
                    break;
                }
            }
            if(checked)
                break;
            currentPsiDirectory = currentPsiDirectory.getParentDirectory();
        }
        if(xmlFile != null) {
            // criar a tag para a permissao do acess ao estado
            XmlTag rootTag = xmlFile.getRootTag();
            XmlTag[] subTags = rootTag.findSubTags("uses-permission");
            List<XmlTag> xmlTags = Arrays.asList(subTags);
            Predicate<XmlTag> xmlTagAccessPredicate = el -> el.getAttributeValue("android:name") == "android.permission.ACCESS_FINE_LOCATION";
            int originalSize = xmlTags.size();
            xmlTags.removeIf(xmlTagAccessPredicate);
            if(xmlTags.size() == originalSize) {
                // nao ha permissao para acesso ainda
                XmlTag usesPermissionTag = xmlElementFactory.createTagFromText("<uses-permission/>");
                usesPermissionTag.setAttribute("android:name", "android.permission.ACCESS_FINE_LOCATION");
                if(originalSize > 0) { subTags[0].addAfter(usesPermissionTag,rootTag); }
                else { rootTag.add(usesPermissionTag); }
            }

        }
        else {
            // NOTE: I'M ASSUMING THAT THIS FILES ALWAYS EXISTS BECAUSE IM ASSUMING THERE IS ALREADY A LISTENER.
        }
    }
}
