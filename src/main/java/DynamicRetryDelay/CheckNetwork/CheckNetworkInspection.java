package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

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

                // check if somewhere on the method (or a method that calls it) the method ConnectivityManager.getActiveNetworkInfo() is called
                Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiMethodCallExpression.class);
                Iterator<PsiMethodCallExpression> iterator = methodsCalls.iterator();
                System.out.println(methodsCalls.size());
                // if the siz is the same, it means nothing was removed from the collection and there is no direct call to the method
                while(iterator.hasNext()) {
                    // if something is left, then we find if any method being called, calls ConnectivityManager.getActiveNetworkInfo()
                    PsiMethodCallExpression currentMethodCall = iterator.next();
                    PsiExpression expr = currentMethodCall.getMethodExpression();
                    PsiClass psiClassConnectivyManager = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.net.ConnectivityManager", GlobalSearchScope.allScope(holder.getProject()));
                    PsiMethod met = psiClassConnectivyManager.findMethodsByName("getActiveNetworkInfo", true)[0];
                    if(expr.getReference().isReferenceTo(met)) {
                        checkNetworkQuickFix = new CheckNetworkQuickFix();
                        holder.registerProblem(method.getNameIdentifier(), DESCRIPTION_TEMPLATE_CHECK_NETWORK, checkNetworkQuickFix);
                    }
                    else if(checkIfBodyMethodChecksNetworkConnection(currentMethodCall, holder.getProject())) {
                        //TODO: só está a verificar a um nivel de metodos
                        System.out.println("CHECKING NETWORK CONNETION");
                        checkNetworkQuickFix = new CheckNetworkQuickFix();
                        holder.registerProblem(method.getNameIdentifier(), DESCRIPTION_TEMPLATE_CHECK_NETWORK, checkNetworkQuickFix);
                    }
                }

            }
        };
    }

    private boolean checkIfBodyMethodChecksNetworkConnection(PsiMethodCallExpression currentMethodCall, Project project) {
        PsiMethod currentMethod = currentMethodCall.resolveMethod();
        Collection<PsiMethodCallExpression> methodCallExpressions = PsiTreeUtil.collectElementsOfType(currentMethod.getBody(), PsiMethodCallExpression.class);
        Iterator<PsiMethodCallExpression> iterator = methodCallExpressions.iterator();
        while(iterator.hasNext()) {
            PsiMethodCallExpression innerMethodCall = iterator.next();
            PsiExpression expr = innerMethodCall.getMethodExpression();
            PsiClass psiClassConnectivyManager = JavaPsiFacade.getInstance(project).findClass("android.net.ConnectivityManager", GlobalSearchScope.allScope(project));
            PsiMethod met = psiClassConnectivyManager.findMethodsByName("getActiveNetworkInfo", true)[0];
            if(expr.getReference().isReferenceTo(met))
                return true;
        }
        return false;
    }

}
