package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
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

        //TODO: ir buscar a variavel de classe que implemente SharedPreferences.onSharedPreferenceChangeListener

        /*
        *
        * CREATE A METHOD TO CHECK THE NETWORK CONNECTION
        *
        * */
        String psiCheckNetworkMethodString =
                "private boolean checkNetwork() {\n" +
                "\t\tfinal ConnectivityManager connManager =\n" +
                "\t\t\t(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);\n" +
                "\t\tfinal NetworkInfo net = connManager.getActiveNetworkInfo();\n" +
                "\t\tfinal boolean isConnected = net != null && net.isConnected();\n" +
                "\n" +
                "\t\treturn isConnected;\n" +
                "\t}";
        PsiMethod psiCheckNetworkMethod = factory.createMethodFromText(psiCheckNetworkMethodString, null);
        //TODO: adicionar à class referida em cima

        /*
        *
        * CREATE A CLASS THAT EXTENDS "BROADCASTRECEIVER" THAT IMPLEMENTS:
        *           1. onReceive
        *           2. enable
        *           3. disable
        * */
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



    }

}
