package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class CheckNetworkQuickFix implements LocalQuickFix {

    private final String QUICK_FIX_NAME = "Refactor4Green: Dynamic Retry Delay Dynamic Check Network";


    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return QUICK_FIX_NAME;
    }


    /*
    *
    * The changes applied to the code are:
    *      1 - create a method to check the network connection
    *      2 - rewrite the code of the "onHandleIntent" method to check network before autorefreshing
    *      3 - adding class that extends "BroadcastReceiver" that implements methods:
    *               3.1 - onReceive
    *               3.2 - enable
    *               3.3 - disable
    *      4 - change the xml settings
    *
    * */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiIdentifier psiIdentifier = (PsiIdentifier) problemDescriptor.getPsiElement();
        PsiMethod psiMethod = (PsiMethod) psiIdentifier.getContext();
        PsiClass intentServiceClass = psiMethod.getContainingClass();
        PsiFile psiFile = PsiTreeUtil.getParentOfType(intentServiceClass, PsiFile.class);
        PsiDirectory psiDirectory = psiFile.getContainingDirectory();

        /*
         * ADDING COMMENT THAT SUMMARIZES CHANGES MADE TO THE CODE
         */
        PsiComment comment = factory.createCommentFromText("/* Refactor4Green: DYNAMIC RETRY DELAY ENERGY PATTERN APPLIED \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getFirstChild().getNode()))
                + "Checking the network connection before attempting to answer a request \n"
                + StringUtils.repeat(" ", IndentHelper.getInstance().getIndent(psiFile, psiMethod.getFirstChild().getNode()))
                + "Application changed java file \"" + intentServiceClass.getContainingFile().getName() + "\"  and xml file \"AndroidManifest.xml\"." +
                "*/", intentServiceClass.getContainingFile());
        psiMethod.addBefore(comment, psiMethod.getFirstChild());

        Collection<PsiReferenceExpression> references = PsiTreeUtil.findChildrenOfType(psiMethod.getBody(), PsiReferenceExpression.class);
        Predicate<PsiReferenceExpression> predicateRefExpr = a -> !(a.resolve() instanceof PsiField || a.resolve() instanceof PsiLocalVariable);
        references.removeIf(predicateRefExpr);
        Iterator<PsiReferenceExpression> iterator = references.iterator();
        PsiReferenceExpression ref = null;
        boolean implementsClass = false;
        PsiClass aClass = null;
        while (iterator.hasNext()) {
            // TODO: ESTA VERIFICAO DEVE PODER SER OPTIMIZADA
            ref = iterator.next();
            if(ref.getType() == null) continue;
            aClass = JavaPsiFacade.getInstance(project).findClass(ref.getType().getCanonicalText(), GlobalSearchScope.allScope(project));
            if(aClass == null) continue;
            // vejo se a class implementa a class do helper ? --- isto parece muito martelado
            PsiClass sharedPreferencesClass = JavaPsiFacade.getInstance(project).findClass("SharedPreferences.OnSharedPreferenceChangeListener", GlobalSearchScope.allScope(project));
            PsiClassType[] list = aClass.getImplementsListTypes();
            implementsClass = false;
            for (int i = 0; i < list.length ; i++ ) {
                if (list[i].getName().equals("OnSharedPreferenceChangeListener")) {
                    implementsClass = true;
                    break;
                }
            }
            if(implementsClass) { break; }
        }

        /*
         * CREATE A METHOD TO CHECK THE NETWORK CONNECTION
         */
        String psiCheckNetworkMethodString =
                "private boolean checkNetwork() {\n" +
                "\t\tfinal android.net.ConnectivityManager connManager =\n" +
                "\t\t\t(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);\n" +
                "\t\tfinal android.net.NetworkInfo  net = connManager.getActiveNetworkInfo();\n" +
                "\t\tfinal boolean isConnected = net != null && net.isConnected();\n" +
                "\n" +
                "\t\treturn isConnected;\n" +
                "\t}";
        PsiMethod psiCheckNetworkMethod = factory.createMethodFromText(psiCheckNetworkMethodString, null);
        aClass.add(psiCheckNetworkMethod);

        PsiIfStatement ifStatement = (PsiIfStatement) factory.createStatementFromText("if ( " + ref.getElement().getText() + ".checkNetwork())"  +
                " { b; } else { NetworkStateReceiver.enable(this); }", intentServiceClass);
        PsiVariable var = (PsiVariable) ref.resolve();
        PsiDeclarationStatement decl = (PsiDeclarationStatement) factory.createStatementFromText("final " + aClass.getName() + " " + ref.getElement().getText() + " = " + var.getInitializer().getText() +  ";", intentServiceClass);
        // retirar a declaracao
        String name = ref.getElement().getText();
        Collection<PsiDeclarationStatement> decls = PsiTreeUtil.findChildrenOfAnyType(psiMethod.getBody(), PsiDeclarationStatement.class);
        Predicate<PsiDeclarationStatement> predicateDecls = a ->
                ! ( ((PsiLocalVariable) a.getDeclaredElements()[0]).getName().equals(name) );
        decls.removeIf(predicateDecls);
        decls.iterator().next().delete();
        ifStatement.getThenBranch().replace(psiMethod.getBody());
        PsiCodeBlock newBody =  factory.createCodeBlock();
        newBody.add(decl);
        newBody.add(ifStatement);
        psiMethod.getBody().replace(newBody);

        /*
         * CREATE A CLASS THAT EXTENDS "BROADCASTRECEIVER" THAT IMPLEMENTS:
         *           1. onReceive
         *           2. enable
         *           3. disable
         */
        String broadcastReceiverString =
                "public static class NetworkStateReceiver extends BroadcastReceiver {\n" +
                "\t\tprivate static final String TAG = NetworkStateReceiver.class.getName();\n" +
                "\n" +
                "\t\t@Override\n" +
                "\t\tpublic void onReceive(Context context, Intent intent) {\n" +
                "\t\t\tandroid.util.Log.i(TAG, \"Network state change received.\");\n" +
                "\n" +
                "\t\t\tfinal AutoRefreshHelper helper =\n" +
                "\t\t\t\tAutoRefreshHelper.getInstance(context.getApplicationContext());\n" +
                "\n" +
                "\t\t\tif (helper.checkNetwork()) {\n" +
                        //TODO: falta fazer o rescheduleAlarm
                "\t\t\t\thelper.rescheduleAlarm();\n" +
                "\t\t\t}\n" +
                "\t\t}\n" +
                "\n" +
                "\t\tpublic static void enable(Context context) {\n" +
                "\t\t\tfinal android.content.pm.PackageManager packageManager = context.getPackageManager();\n" +
                "\n" +
                "\t\t\tfinal android.content.ComponentName receiver =\n" +
                "\t\t\t\tnew ComponentName(context, NetworkStateReceiver.class);\n" +
                "\n" +
                "\t\t\tif (packageManager.getComponentEnabledSetting(receiver) !=\n" +
                "\t\t\t    PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {\n" +
                "\t\t\t\tandroid.util.Log.i(TAG, \"Enabling network state receiver.\");\n" +
                "\t\t\t}\n" +
                "\n" +
                "\t\t\tpackageManager.setComponentEnabledSetting(\n" +
                "\t\t\t\treceiver,\n" +
                "\t\t\t\tandroid.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,\n" +
                "\t\t\t\tandroid.content.pm.PackageManager.DONT_KILL_APP);\n" +
                "\t\t}\n" +
                "\n" +
                "\t\tpublic static void disable(Context context) {\n" +
                "\t\t\tfinal android.content.pm.PackageManager packageManager = context.getPackageManager();\n" +
                "\n" +
                "\t\t\tfinal android.content.ComponentName receiver =\n" +
                "\t\t\t\tnew ComponentName(context, NetworkStateReceiver.class);\n" +
                "\n" +
                "\t\t\tif (packageManager.getComponentEnabledSetting(receiver) !=\n" +
                "\t\t\t    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {\n" +
                "\t\t\t\tandroid.util.Log.i(TAG, \"Disabling network state receiver.\");\n" +
                "\t\t\t}\n" +
                "\n" +
                "\t\t\tpackageManager.setComponentEnabledSetting(\n" +
                "\t\t\t\treceiver,\n" +
                "\t\t\t\tandroid.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,\n" +
                "\t\t\t\tandroid.content.pm.PackageManager.DONT_KILL_APP);\n" +
                "\t\t}" +
                "\n} ";
        PsiClass broadcastReceiverClass = factory.createClassFromText(broadcastReceiverString, null);
        intentServiceClass.addAfter(broadcastReceiverClass.getInnerClasses()[0], intentServiceClass.getRBrace());
        // para fazer imports é assim! - tem que ser sempre com o fully qualified name na string e depois chamar o metodo shortenClassReferences !!
        JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        javaCodeStyleManager.shortenClassReferences(intentServiceClass);

        /*
         * ADD THE NEW RECEIVER TO THE "AndroidManifest.xml" FILE
         */
        XmlElementFactory xmlElementFactory = XmlElementFactory.getInstance(project);
        // assumo que nunca irá estar na directoria dos ficheiro java
        // TODO: ESTA A FAZER BOTTOM-UP MAS NAO OLHA PARA OS LADOS
        XmlFile xmlFile = null;
        while (psiDirectory.getParentDirectory() != null) {
            xmlFile = (XmlFile) psiDirectory.getParentDirectory().findFile("AndroidManifest.xml");
            if (xmlFile != null) {
                System.out.println("Found xml file!");
                break;
            }
            psiDirectory = psiDirectory.getParentDirectory();
        }
        if(xmlFile != null) {
            // TODO: findFileSystemItem in PsiUtilCore
            // já existe ficheiro
            // criar a tag para a permissao do acess ao estado
            XmlTag rootTag = xmlFile.getRootTag();
            XmlTag[] subTags = rootTag.findSubTags("uses-permission");
            List<XmlTag> xmlTags = Arrays.asList(subTags);
            Predicate<XmlTag> xmlTagAccessPredicate = a -> a.getAttributeValue("android:name") == "android.permission.ACCESS_NETWORK_STATE";
            int originalSize = xmlTags.size();
            xmlTags.removeIf(xmlTagAccessPredicate);
            if(xmlTags.size() == originalSize) {
                // nao ha permissao para acesso ainda
                XmlTag usesPermissionTag = xmlElementFactory.createTagFromText("<uses-permission/>");
                usesPermissionTag.setAttribute("android:name", "android.permission.ACCESS_NETWORK_STATE");
                if(originalSize > 0) { subTags[0].addAfter(usesPermissionTag,rootTag); }
                else { rootTag.add(usesPermissionTag); }
            }

            // criar a tag para o receiver
            XmlTag receiverTag = xmlElementFactory.createTagFromText("<receiver/>");
            receiverTag.setAttribute("android:name", "." + aClass.getName() + "$NetworkStateReceiver");
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
            if(firstProvider != null) { applicationTag.addBefore(receiverTag, firstProvider); }
            else { applicationTag.add(receiverTag); }
            if(applicationTagNull) { xmlFile.add(applicationTag); }

        }
        else {
            // TODO: NAO EXISTINDO O FICHEIRO XML, DEVE CRIAR CERTO? PORQUE TENHO QUE DECLARAR O RECEIVER
        }
    }

}
