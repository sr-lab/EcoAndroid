package Cache.CheckMetadata;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class CheckMetadataInspection extends LocalInspectionTool {

    private final CheckMetadataQuickFix  checkMetadataQuickFix = new CheckMetadataQuickFix();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_CHECK_METADATA = "EcoAndroid: Cache [Check Metadata] can be applied";

            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                if(!(method.getName().equals("onReceive")))
                    return;

                PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                PsiClass broadcastReceiverClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.content.BroadcastReceiver", GlobalSearchScope.allScope(holder.getProject()));
                if(!(InheritanceUtil.isInheritorOrSelf(psiClass, broadcastReceiverClass, true))) { return;}

                Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiMethodCallExpression.class);
                Iterator<PsiMethodCallExpression> iterator = methodsCalls.iterator();
                ArrayList<PsiLocalVariable> intentVariables = new ArrayList<>();
                while(iterator.hasNext()) {
                    PsiMethodCallExpression currentMethodCall = iterator.next();

                    // check if there is a NotificationManager call in the method
                    if(currentMethodCall.resolveMethod() != null) {
                        PsiClass notificationManagerClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.app.NotificationManager", GlobalSearchScope.allScope(holder.getProject()));
                        if(InheritanceUtil.isInheritorOrSelf(currentMethodCall.resolveMethod().getContainingClass(), notificationManagerClass, true)) { return;}
                    }

                    if(currentMethodCall.getMethodExpression().getQualifier() == null)
                        continue;
                    if(currentMethodCall.getMethodExpression().getQualifier().getText().equals("intent")) {
                        if(currentMethodCall.getParent() instanceof PsiLocalVariable)
                            intentVariables.add((PsiLocalVariable) currentMethodCall.getParent());
                    }
                }
                // NOTE: IF THIS SIZE IS 0 MEANS NOTHING IS RETRIEVED FRMO THE intent PARAMETER
                if(intentVariables.size() == 0)
                    return;

                for (PsiLocalVariable intentVariable : intentVariables) {
                    boolean isChecked = false;
                    Collection<PsiReference> references = ReferencesSearch.search(intentVariable).findAll();

                    for (PsiReference ref : references) {
                        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) PsiTreeUtil.findFirstParent((PsiElement) ref, el -> el instanceof PsiReferenceExpression);
                        if(referenceExpression != null) {

                            // check if the ref is in a method call
                            PsiMethodCallExpression psiMethodCallExpression = PsiTreeUtil.getParentOfType(referenceExpression, PsiMethodCallExpression.class);
                            if(psiMethodCallExpression != null) {
                                PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
                                if(methodExpression.getQualifierExpression() != null
                                        && methodExpression.getQualifierExpression().getType() != null
                                        && methodExpression.getQualifierExpression().getType().getPresentableText().equals("String")
                                        && methodExpression.getQualifier().getText().startsWith("DownloadManager.")) {
                                    return;

                                }
                            }
                        }
                        PsiIfStatement firstParent = (PsiIfStatement) PsiTreeUtil.findFirstParent((PsiElement) ref, el -> el instanceof PsiIfStatement);
                        if (firstParent == null) {
                            continue;
                        }
                        // NOTE: COMPARING POSITIONS BETWEEN TEH REFERENCE AND THE CONDITION OF THE IF. IF ITS 0, THEN THE REFERENCE IS IN THE CONDITION
                        if (PsiUtilBase.compareElementsByPosition(firstParent.getCondition(), (PsiElement) ref) == 0) {
                            isChecked = true;
                        }

                    }
                    if (isChecked)
                        return;
                }

                checkMetadataQuickFix.setIntentVariables(intentVariables);
                holder.registerProblem(method.getNameIdentifier(), DESCRIPTION_TEMPLATE_CHECK_METADATA, checkMetadataQuickFix);
            }
        };
    }
}
