package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
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

    private final String QUICK_FIX_NAME = "EcoAndroid: Apply pattern Dynamic Retry Delay [Check Network]";

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
            String psiHasActiveNetworkString =
                    "protected boolean hasActiveNetwork() {\n"
                            + "     final android.net.ConnectivityManager connManager = (android.net.ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);\n"
                            + "     android.net.Network activeNetwork = connManager.getActiveNetwork();\n"
                            + "     return (activeNetwork != null);\n"
                            + "}";
            PsiMethod psiHasActiveNetworkMethod = factory.createMethodFromText(psiHasActiveNetworkString, null);
            PsiComment hasActiveNetworkCommentString = factory.createCommentFromText("//The method hasActiveNetwork() checks whether the network connection is active", psiFile);
            psiHasActiveNetworkMethod.addBefore(hasActiveNetworkCommentString, psiHasActiveNetworkMethod.getFirstChild());
            intentServiceClass.add(psiHasActiveNetworkMethod);


            PsiIfStatement ifStatement = (PsiIfStatement) factory.createStatementFromText("if (hasActiveNetwork()) { b; } else { " +
                    "NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();\n" +
                    "ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);\n" +
                    "networkStateReceiver.enable(getApplicationContext());\n" +
                    "networkStateReceiver.setService(this);\n" +
                    "connectivityManager.registerDefaultNetworkCallback( networkStateReceiver);" +
                    "}", intentServiceClass);
            ifStatement.getThenBranch().replace(psiMethod.getBody());
            PsiCodeBlock newBody = factory.createCodeBlock();
            newBody.add(ifStatement);
            psiMethod.getBody().replace(newBody);

            String broadcastReceiverString =
                    "public class NetworkStateReceiver extends android.net.ConnectivityManager.NetworkCallback {\n" +
                            "       private " + intentServiceClass.getName() + " service;\n" +
                            "       public void setService(" + intentServiceClass.getName() + " newService) { service = newService; }" +
                            "\n" +
                            "       @Override\n" +
                            "       public void onAvailable(Network network) {\n" +
                            "\n         // EcoAndroid: If there is an active network connection, this method will \"turn off\" this class and arrange to process the request\n" +
                            "           if (android.os.Build.VERSION.SDK_INT < 24 || service.hasActiveNetwork()) {\n" +
                            "               Context context = getApplicationContext();\n" +
                            "               disable(context);" +
                            "               final android.app.AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);\n" +
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
                            "\n     // EcoAndroid: Method to  \"turn on\" this class \n" +
                            "       public void enable(Context context) {\n" +
                            "           ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);\n" +
                            "           connectivityManager.registerDefaultNetworkCallback(this);" +
                            "       }\n" +
                            "\n"
                            + "     // EcoAndroid: Method to  \"turn off\" this class\n" +
                            "        public void disable(Context context) {\n" +
                            "            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);\n" +
                            "            connectivityManager.unregisterNetworkCallback(this);\n" +
                            "       }\n" +
                            "\n} ";
            PsiClass broadcastReceiverClass = factory.createClassFromText(broadcastReceiverString, null);
            intentServiceClass.add(broadcastReceiverClass.getInnerClasses()[0]);
            PsiComment networkStateReceiverComment = factory.createCommentFromText("// This class is used to, when the connection failes, to check when there is an active connection", psiFile);
            broadcastReceiverClass.addBefore(networkStateReceiverComment, broadcastReceiverClass.getFirstChild());
            // para fazer imports é assim! - tem que ser sempre com o fully qualified name na string e depois chamar o metodo shortenClassReferences !!
            JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
            javaCodeStyleManager.shortenClassReferences(intentServiceClass);

            XmlElementFactory xmlElementFactory = XmlElementFactory.getInstance(project);
            PsiFile[] xmlFiles = FilenameIndex.getFilesByName(project, "AndroidManifest.xml", GlobalSearchScope.projectScope(project));
            XmlFile xmlFile = (XmlFile) xmlFiles[0];
            if (xmlFiles.length > 1) {
                String filePath = psiFile.getVirtualFile().getPath();
                double distance = StringUtils.getLevenshteinDistance(xmlFile.getVirtualFile().getPath(), filePath);
                ;
                for (PsiFile currXmlFile : xmlFiles) {
                    double auxDistance = StringUtils.getLevenshteinDistance(currXmlFile.getVirtualFile().getPath(), filePath);
                    if (auxDistance < distance) {
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
            if (xmlTags.size() == originalSize) {
                // nao ha permissao para acesso ainda
                XmlTag usesPermissionTag = xmlElementFactory.createTagFromText("<uses-permission/>");
                usesPermissionTag.setAttribute("android:name", "android.permission.ACCESS_NETWORK_STATE");
                if (originalSize > 0) {
                    subTags[0].getParent().addAfter(usesPermissionTag, subTags[0]);
                } else {
                    rootTag.add(usesPermissionTag);
                }
            }

            PsiComment comment = factory.createCommentFromText("/*\n "
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getFirstChild().getNode()))
                    + "* EcoAndroid: DYNAMIC RETRY DELAY ENERGY PATTERN APPLIED \n"
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
                    + "* EcoAndroid: DYNAMIC RETRY DELAY ENERGY PATTERN NOT APPLIED \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    + "* Something went wrong and the pattern could not be applied! \n"
                    + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getNode()))
                    +"*/", psiFile);
            psiMethod.addBefore(comment, psiMethod.getFirstChild());
        }
    }
}
