package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CheckNetworkInspection extends LocalInspectionTool {

    /*
    *
    *  This inspection follows the steps:
    *        1 - look if there is a "onHandleInternet" in the class that extends "IntentService"
    *        2 -
    * */
    private CheckNetworkQuickFix checkNetworkQuickFix;

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            /**
             *  This string defines the short message shown to a user signaling the inspection
             *  found a problem. It reuses a string from the inspections bundle.
             */
            @NonNls
            private final String DESCRIPTION_TEMPLATE_CHECK_NETWORK = "Refactor4Green: Check Network Before Autorefresh";

            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                // check if the name of the method is "onHandleIntent"
                if(!method.getName().equals("onHandleIntent"))
                    return;

                // check if the class the method is inserted in extends "IntentService"
                PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                PsiClassType[] list = psiClass.getExtendsListTypes();
                boolean extendsIntent = false;
                for (int i = 0; i < list.length ; i++ ) {
                    if (list[i].getName().equals("IntentService")) {
                        extendsIntent = true;
                        break;
                    }
                }
                if(!extendsIntent)
                    return;

                // 
                checkNetworkQuickFix = new CheckNetworkQuickFix();
                holder.registerProblem(method, DESCRIPTION_TEMPLATE_CHECK_NETWORK, checkNetworkQuickFix);
                System.out.println("registered problem!");


            }
        };
    }
}
