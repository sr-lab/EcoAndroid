package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
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
    *
    * */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

        /*
        *
        * CREATE A METHOD TO CHECK THE NETWORK CONNECTION
        *
        * */
        PsiMethod psiCheckNetworkMethod = factory.createMethod("checkNetwork", PsiType.BOOLEAN);

        /*
        *
        * CREATE A CLASS THAT EXTENDS "BROADCASTRECEIVER" THAT IMPLEMENTS:
        *           1. onReceive
        *           2. enable
        *           3. disable
        * */
        PsiClass broadcastReceiverClass = factory.createClass("NetworkStateReceiver");
        // extends class
        //broadcastReceiverClass.getExtendsList().add(android.content.BroadcastReceiver.class);

        // onReceive Method
        PsiMethod onReceiveMethod = factory.createMethod("onReceive", PsiType.VOID);
        // TODO: como é que eu crio um parametro que nao é um tipo primitivo ?
        // !!! a class PsiType tem um metodo estatico chamado getTypeByName que encontra a classe pelo qualified class name,
        // no entanto é dentro de um certo scope !!!
        //PsiParameter contextParameter = factory.createParameter("context",);
        PsiCodeBlock onReceiveMethodBlock = onReceiveMethod.getBody();


        // enable Method
        PsiMethod enableMethod = factory.createMethod("enable", PsiType.VOID);
        PsiCodeBlock enableMethodBody = onReceiveMethod.getBody();


        // disable Method
        PsiMethod disabelMethod = factory.createMethod("disable", PsiType.VOID);
        PsiCodeBlock disableMethodBody = onReceiveMethod.getBody();


    }

}
