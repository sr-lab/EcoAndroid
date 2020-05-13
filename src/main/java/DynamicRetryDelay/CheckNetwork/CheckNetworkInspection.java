package DynamicRetryDelay.CheckNetwork;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

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
                if(!method.getName().equals("onHandleIntent")) // This method is invoked on the worker thread with a request to process.
                    return;

                // check if the class the method is inserted in extends "IntentService"
                PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                PsiClass serviceClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.app.IntentService", GlobalSearchScope.allScope(holder.getProject()));
                if(!(InheritanceUtil.isInheritorOrSelf(psiClass, serviceClass, true))) { return;}

                // check if somewhere on the method (or a method that calls it) the method ConnectivityManager.getActiveNetworkInfo() is called
                Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiMethodCallExpression.class);
                Iterator<PsiMethodCallExpression> iterator = methodsCalls.iterator();
                // if the siz is the same, it means nothing was removed from the collection and there is no direct call to the method
                while(iterator.hasNext()) {
                    // if something is left, then we find if any method being called, calls ConnectivityManager.getActiveNetworkInfo()
                    PsiMethodCallExpression currentMethodCall = iterator.next();
                    PsiExpression expr = currentMethodCall.getMethodExpression();
                    PsiClass psiClassConnectivyManager = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.net.ConnectivityManager", GlobalSearchScope.allScope(holder.getProject()));
                    PsiMethod met = psiClassConnectivyManager.findMethodsByName("getActiveNetworkInfo", true)[0];
                    if(expr.getReference().isReferenceTo(met)) {
                        System.out.println("CHECKING NETWORK FOUND!");
                        return;
                    }
                    else if(checkIfBodyMethodChecksNetworkConnection(currentMethodCall, holder.getProject())) {
                        //TODO: SÓ VERICA UM NIVEL DE METODOS
                        System.out.println("CHECKING NETWORK FOUND!");
                        return;
                    }
                }
                checkNetworkQuickFix = new CheckNetworkQuickFix();
                holder.registerProblem(method.getNameIdentifier(), DESCRIPTION_TEMPLATE_CHECK_NETWORK, checkNetworkQuickFix);
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
            if(expr.getReference().isReferenceTo(met)) { return true; }
        }
        return false;
    }

}
