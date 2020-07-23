package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
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
import java.util.function.Predicate;

public class CheckNetworkQuickFix implements LocalQuickFix {

    private final String QUICK_FIX_NAME = "Refactor4Green: Dynamic Retry Delay Energy Pattern - Checking network connection before processing request case";

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() { return QUICK_FIX_NAME; }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiIdentifier psiIdentifier = (PsiIdentifier) problemDescriptor.getPsiElement();
        PsiMethod psiMethod = (PsiMethod) psiIdentifier.getContext();
        PsiClass intentServiceClass = psiMethod.getContainingClass();
        PsiFile psiFile = PsiTreeUtil.getParentOfType(intentServiceClass, PsiFile.class);

        try {
            String psiCheckNetworkMethodString =
                    "boolean checkNetwork() {\n"
                    + "     final android.net.ConnectivityManager connManager = (android.net.ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);\n"
                    + "     android.net.Network activeNetwork = connManager.getActiveNetwork();\n"
                    + "     if(activeNetwork != null) {\n"
                    + "         return true;\n"
                    + "     }\n"
                    + "     return false;\n"
                    + "}";
            PsiMethod psiCheckNetworkMethod = factory.createMethodFromText(psiCheckNetworkMethodString, null);
            intentServiceClass.add(psiCheckNetworkMethod);

            PsiIfStatement ifStatement = (PsiIfStatement) factory.createStatementFromText("if (checkNetwork()) { b; } else { NetworkStateReceiver.enable(getApplicationContext()); }", intentServiceClass);
            ifStatement.getThenBranch().replace(psiMethod.getBody());
            PsiCodeBlock newBody = factory.createCodeBlock();
            newBody.add(ifStatement);
            psiMethod.getBody().replace(newBody);

            String broadcastReceiverString =
                    "public static class NetworkStateReceiver extends android.content.BroadcastReceiver {\n" +
                    "       private static final String TAG = NetworkStateReceiver.class.getName();\n" +
                    "\n" +
                    "       private static " + intentServiceClass.getName() + " service;\n" +
                    "\n" +
                    "       public static void setService(" + intentServiceClass.getName() + " newService) { service = newService; }\n" +
                    "\n" +
                    "       @Override\n" +
                    "       public void onReceive(android.content.Context context, android.content.Intent intent) {\n" +
                    "           if (service.checkNetwork()) {\n" +
                    "               NetworkStateReceiver.disable(context);\n" +
                    "\n" +
                    "               final android.app.AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);\n" +
                    "\n" +
                    "               final android.content.Intent innerIntent = new Intent(context, " + intentServiceClass.getName() + ".class);\n" +
                    "               final android.app.PendingIntent pendingIntent = PendingIntent.getService(context, 0, innerIntent, 0);\n" +
                    "\n" +
                    "               android.content.SharedPreferences preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);\n" +
                    "               preferences.edit();\n" +
                    "               boolean autoRefreshEnabled = preferences.getBoolean(\"pref_auto_refresh_enabled\", false);\n" +
                    "\n" +
                    "               final String hours = preferences.getString(\"pref_auto_refresh_enabled\", \"0\");\n" +
                    "               long hoursLong = Long.parseLong(hours) * 60 * 60 * 1000;\n" +
                    "\n" +
                    "                if (autoRefreshEnabled && hoursLong != 0) {\n" +
                    "                   final long alarmTime =  preferences.getLong(\"last_auto_refresh_time\", 0) + hoursLong;\n" +
                    "                   alarmManager.set(AlarmManager.RTC, alarmTime, pendingIntent);\n" +
                    "                } else {\n" +
                    "                    alarmManager.cancel(pendingIntent);\n" +
                    "            }" +
                    "       }" +
                    "    }" +
                    "\n" +
                    "       public static void enable(Context context) {\n" +
                    "           final android.content.pm.PackageManager packageManager = context.getPackageManager();\n" +
                    "           final android.content.ComponentName receiver = new ComponentName(context, NetworkStateReceiver.class);\n" +
                    "           packageManager.setComponentEnabledSetting(receiver, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP);\n" +
                    "       }\n" +
                    "\n" +
                    "       public static void disable(Context context) {\n" +
                    "           final android.content.pm.PackageManager packageManager = context.getPackageManager();\n" +
                    "           final android.content.ComponentName receiver = new ComponentName(context, NetworkStateReceiver.class);\n" +
                    "           packageManager.setComponentEnabledSetting( receiver, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP);\n" +
                    "       }" +
                    "\n} ";
            PsiClass broadcastReceiverClass = factory.createClassFromText(broadcastReceiverString, null);
            intentServiceClass.add(broadcastReceiverClass.getInnerClasses()[0]);
            // para fazer imports é assim! - tem que ser sempre com o fully qualified name na string e depois chamar o metodo shortenClassReferences !!
            JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
            javaCodeStyleManager.shortenClassReferences(intentServiceClass);

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
            xmlTags.removeIf(el -> (el.getAttributeValue("android:name").equals("android.permission.ACCESS_NETWORK_STATE")));
            if(xmlTags.size() == originalSize) {
                // nao ha permissao para acesso ainda
                XmlTag usesPermissionTag = xmlElementFactory.createTagFromText("<uses-permission/>");
                usesPermissionTag.setAttribute("android:name", "android.permission.ACCESS_NETWORK_STATE");
                if(originalSize > 0) { subTags[0].getParent().addAfter(usesPermissionTag, subTags[0]); }
                else { rootTag.add(usesPermissionTag); }
            }

            // criar a tag para o receiver
            XmlTag receiverTag = xmlElementFactory.createTagFromText("<receiver/>");
            receiverTag.setAttribute("android:name",  intentServiceClass.getQualifiedName() + "$NetworkStateReceiver");
            receiverTag.setAttribute("android:exported", "true");
            receiverTag.setAttribute("android:enabled", "false");
            XmlTag intentFilterTag = xmlElementFactory.createTagFromText("<intent-filter/>");
            XmlTag actionTag = xmlElementFactory.createTagFromText("<action/>");
            actionTag.setAttribute("android:name", "android.net.conn.CONNECTIVITY_CHANGE");
            intentFilterTag.add(actionTag);
            receiverTag.add(intentFilterTag);
            XmlTag applicationTag = rootTag.findFirstSubTag("application");
            boolean applicationTagNull = false;
            if(applicationTag == null) {
                applicationTagNull = true;
                applicationTag = xmlElementFactory.createTagFromText("<application/>");
            }
            XmlTag firstProvider = applicationTag.findFirstSubTag("provider");
            if(firstProvider != null)
                applicationTag.addBefore(receiverTag, firstProvider);
            else
                applicationTag.add(receiverTag);
            if(applicationTagNull)
                xmlFile.add(applicationTag);

            PsiComment comment = factory.createCommentFromText("/*\n "
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getFirstChild().getNode()))
                    + "* Refactor4Green: DYNAMIC RETRY DELAY ENERGY PATTERN APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getFirstChild().getNode()))
                    + "* Checking the network connection before attempting to answer a request \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getFirstChild().getNode()))
                    + "* Application changed java file \"" + intentServiceClass.getContainingFile().getName() + "\"  and xml file \"AndroidManifest.xml\". \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getFirstChild().getNode()))
                    + "*/", intentServiceClass.getContainingFile());
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
        } catch(Throwable e) {
            PsiComment comment = factory.createCommentFromText("/* \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Refactor4Green: DYNAMIC RETRY DELAY ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
       }
    }

}
