package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CheckNetworkInspection extends LocalInspectionTool {

    private CheckNetworkQuickFix checkNetworkQuickFix;

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_CHECK_NETWORK = "Refactor4Green: Check Network Before Autorefresh";

            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                if(!method.getName().equals("onHandleIntent")) // This method is invoked on the worker thread with a request to process.
                    return;

                PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                PsiClass serviceClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.app.IntentService", GlobalSearchScope.allScope(holder.getProject()));
                if(!(InheritanceUtil.isInheritorOrSelf(psiClass, serviceClass, true))) { return;}

                Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiMethodCallExpression.class);
                Iterator<PsiMethodCallExpression> iterator = methodsCalls.iterator();
                // NOTE: IF THE SIZE IS THE SAME IT MEANS NOTHING WAS REMOVED FROM THE COLLECTION AND THERE IS NO DIRECT CALL TO THE METHOD
                while(iterator.hasNext()) {
                    // NOTE: CHECK IF INNER METHODS CALL ConnectivityManager.getActiveNetwork()
                    PsiMethodCallExpression currentMethodCall = iterator.next();
                    PsiExpression expr = currentMethodCall.getMethodExpression();
                    PsiClass psiClassConnectivyManager = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.net.ConnectivityManager", GlobalSearchScope.allScope(holder.getProject()));
                    PsiMethod met = psiClassConnectivyManager.findMethodsByName("getActiveNetwork", true)[0];
                    if(expr.getReference().isReferenceTo(met)) { return; }
                    else if(checkIfBodyMethodChecksNetworkConnection(currentMethodCall, holder.getProject())) {
                        //TODO: SÓ VERICA UM NIVEL DE METODOS
                        return;
                    }
                }
                if(!checkIfInternetIsSubscribed(holder.getProject(), method.getContainingClass().getContainingFile())) { return; }
                checkNetworkQuickFix = new CheckNetworkQuickFix();
                holder.registerProblem(method.getNameIdentifier(), DESCRIPTION_TEMPLATE_CHECK_NETWORK, checkNetworkQuickFix);
            }
        };
    }

    private boolean checkIfInternetIsSubscribed(Project project, PsiFile psiFile) {
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
        XmlTag rootTag = xmlFile.getRootTag();
        XmlTag[] subTags = rootTag.findSubTags("uses-permission");
        List<XmlTag> xmlTags = new LinkedList<>(Arrays.asList(subTags));
        int originalSize = xmlTags.size();
        xmlTags.removeIf(el -> (el.getAttributeValue("android:name").equals("android.permission.INTERNET")));
        return xmlTags.size() != originalSize;
    }

    private boolean checkIfBodyMethodChecksNetworkConnection(PsiMethodCallExpression currentMethodCall, Project project) {
        PsiMethod currentMethod = currentMethodCall.resolveMethod();
        Collection<PsiMethodCallExpression> methodCallExpressions = PsiTreeUtil.collectElementsOfType(currentMethod.getBody(), PsiMethodCallExpression.class);
        Iterator<PsiMethodCallExpression> iterator = methodCallExpressions.iterator();
        while(iterator.hasNext()) {
            PsiMethodCallExpression innerMethodCall = iterator.next();
            PsiExpression expr = innerMethodCall.getMethodExpression();
            PsiClass psiClassConnectivyManager = JavaPsiFacade.getInstance(project).findClass("android.net.ConnectivityManager", GlobalSearchScope.allScope(project));
            PsiMethod met = psiClassConnectivyManager.findMethodsByName("getActiveNetwork", true)[0];
            if(expr.getReference().isReferenceTo(met)) { return true; }
        }
        return false;
    }

}
