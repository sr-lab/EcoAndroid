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

                /*
                 *
                 * FIRST PHASE: LOOK FOR THE onHandleIntent METHOD FROM THE IntentService CLASS
                 *
                 */
                if(!method.getName().equals("onHandleIntent")) // This method is invoked on the worker thread with a request to process.
                    return;

                PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                PsiClass serviceClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.app.IntentService", GlobalSearchScope.allScope(holder.getProject()));
                if(!(InheritanceUtil.isInheritorOrSelf(psiClass, serviceClass, true))) { return;}

                /*
                 *
                 * SECOND PHASE: CHECK IF THE SOMEWHERE IN THE METHOD BODY THERE IS A CALL TO THE METHOD ConnectivityManager.getActiveNetworkInfo()
                 *
                 */
                Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiMethodCallExpression.class);
                Iterator<PsiMethodCallExpression> iterator = methodsCalls.iterator();
                // NOTE: IF THE SIZE IS THE SAME IT MEANS NOTHING WAS REMOVED FROM THE COLLECTION AND THERE IS NO DIRECT CALL TO THE METHOD
                while(iterator.hasNext()) {
                    // NOTE: CHECK IF INNER METHODS CALL ConnectivityManager.getActiveNetworkInfo()
                    PsiMethodCallExpression currentMethodCall = iterator.next();
                    PsiExpression expr = currentMethodCall.getMethodExpression();
                    PsiClass psiClassConnectivyManager = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.net.ConnectivityManager", GlobalSearchScope.allScope(holder.getProject()));
                    PsiMethod met = psiClassConnectivyManager.findMethodsByName("getActiveNetworkInfo", true)[0];
                    if(expr.getReference().isReferenceTo(met)) { return; }
                    else if(checkIfBodyMethodChecksNetworkConnection(currentMethodCall, holder.getProject())) {
                        //TODO: SÓ VERICA UM NIVEL DE METODOS
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
