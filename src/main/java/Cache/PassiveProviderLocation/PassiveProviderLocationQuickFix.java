package Cache.PassiveProviderLocation;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PassiveProviderLocationQuickFix implements LocalQuickFix {
    private static final String QUICK_FIX_NAME = "EcoAndroid: Apply pattern Cache [Switching to PASSIVE_PROVIDER]";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiExpression psiExpression = (PsiExpression) problemDescriptor.getPsiElement();
        PsiMethodCallExpression psiMethodCallExpression = PsiTreeUtil.getParentOfType(psiExpression, PsiMethodCallExpression.class);
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiExpression, PsiMethod.class);
        PsiFile psiFile = PsiTreeUtil.getParentOfType(psiMethod.getContainingClass(), PsiFile.class);
        PsiClass psiClass = psiMethod.getContainingClass();

        try {

            PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) psiExpression;
            if(psiReferenceExpression.getText().startsWith("LocationManager.")){
                String locationManagerName = psiMethodCallExpression.getText().split("\\.")[0];
                PsiStatement psiStatement = PsiTreeUtil.getParentOfType(psiMethodCallExpression, PsiStatement.class);

                PsiExpression psiNewExpression = factory.createExpressionFromText("lm",null);
                psiExpression.replace(psiNewExpression);
                String criteriaString = psiStatement.getText();
                
                psiNewExpression = factory.createExpressionFromText("LocationManager.PASSIVE_PROVIDER", null);
                psiExpression = psiMethodCallExpression.getArgumentList().getExpressions()[0];
                psiExpression.replace(psiNewExpression);
                String passiveProviderString = psiStatement.getText();

                String newBlockString = "{ " +
                        "/* \n " +
                        " * This next piece of code presents two ways to implement a location manager that spends less energy. \n" +
                        " * 1 - Switching to PassiveProvider \n" +
                        " * 2 - Using the criteria class to get the best provider for the needs requested (with the need for POWER_LOW) \n" +
                        " * The second option has been giving \"priority\". However, the goal is for the programmer to chose the one which fits bets. \n" +
                        " * If you wish to know more, please read: https://developer.android.com/reference/android/location/LocationManager \n" +
                        " */ \n" +
                        "boolean flagEcoAndroid = false; \n}";
                String ifStatement =
                        "if(flagEcoAndroid) { \n" +
                            " Criteria criteria = new Criteria(); \n" +
                            " criteria.setPowerRequirement(Criteria.POWER_LOW); \n" +
                            " String lm = " + locationManagerName + ".getBestProvider(criteria, true);\n" +
                            criteriaString + "\n" +
                        "} else { \n" +
                            passiveProviderString + "\n" +
                        "}";

                PsiCodeBlock newBlock = factory.createCodeBlockFromText(newBlockString, psiFile);
                newBlock.getLBrace().delete();
                newBlock.getRBrace().delete();
                PsiIfStatement psiIfStatement = (PsiIfStatement) factory.createStatementFromText(ifStatement, psiFile);
                psiStatement.getParent().addAfter(psiIfStatement, psiStatement);
                psiStatement.replace(newBlock);

                JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
                javaCodeStyleManager.shortenClassReferences(psiClass);
            }
            else {

            }

            // mudar o argumento na chamada a funcao
            //PsiExpression psiNewExpression = factory.createExpressionFromText("LocationManager.PASSIVE_PROVIDER",null);
            //psiExpression.replace(psiNewExpression);

            // adicionar a linha ao ficheiro AndroidManifest.xml
            XmlElementFactory xmlElementFactory = XmlElementFactory.getInstance(project);
            PsiFile[] xmlFiles =  FilenameIndex.getFilesByName(project, "AndroidManifest.xml", GlobalSearchScope.projectScope(project));
            XmlFile xmlFile = (XmlFile) xmlFiles[0];
            if(xmlFiles.length > 1) {
                String filePath = psiFile.getVirtualFile().getPath();
                double distance = StringUtils.getLevenshteinDistance(xmlFile.getVirtualFile().getPath(), filePath);;
                for(PsiFile currXmlFile: xmlFiles){
                    double auxDistance = StringUtils.getLevenshteinDistance(currXmlFile.getVirtualFile().getPath(), filePath);
                    if(auxDistance < distance ) {
                        distance = auxDistance;
                        xmlFile = (XmlFile) currXmlFile;
                    }
                }
            }
            // criar a tag para a permissao do acess ao estado
            XmlTag rootTag = xmlFile.getRootTag();
            XmlTag[] subTags = rootTag.findSubTags("uses-permission");
            List<XmlTag> xmlTags = new LinkedList<>(Arrays.asList(subTags));
            int originalSize = xmlTags.size();
            xmlTags.removeIf(el -> (el.getAttributeValue("android:name").equals("android.permission.ACCESS_FINE_LOCATION")));
            if(xmlTags.size() == originalSize) {
                // nao ha permissao para acesso ainda
                XmlTag usesPermissionTag = xmlElementFactory.createTagFromText("<uses-permission/>");
                usesPermissionTag.setAttribute("android:name", "android.permission.ACCESS_FINE_LOCATION");
                if(originalSize > 0) { subTags[0].getParent().addAfter(usesPermissionTag,subTags[0]); }
                else { rootTag.add(usesPermissionTag); }
            }

            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: CACHE ENERGY PATTERN \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* This energy pattern changes the type of LocationManager to \"PASSIVE_PROVIDER\". \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* This change consumes less energy because it doesn't actually initiating a location fix.\n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Application changed file \"" + psiFile.getName() + "\" and xml file \"AndroidManifest.xml\". \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "*/", psiMethod.getContainingClass().getContainingFile());
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
        } catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* EcoAndroid: CACHE ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
        }



    }
}
