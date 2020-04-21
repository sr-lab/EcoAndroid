package Cache.CheckMetadata;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.Query;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class CheckMetadataInspection extends LocalInspectionTool {

    private CheckMetadataQuickFix checkMetadataQuickFix;


    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            /**
             *  This string defines the short message shown to a user signaling the inspection
             *  found a problem. It reuses a string from the inspections bundle.
             */
            @NonNls
            private final String DESCRIPTION_TEMPLATE_CHECK_METADATA = "Refactor4Green: Cache - Check Metadata";

            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                if(!(method.getName().equals("onReceive"))) { return; }

                PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                PsiClass broadcastReceiverClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.content.BroadcastReceiver", GlobalSearchScope.allScope(holder.getProject()));
                if(!(InheritanceUtil.isInheritorOrSelf(psiClass, broadcastReceiverClass, true))) { return;}

                // check if the content retrieve from the intent is being checked before restoring 
                Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiMethodCallExpression.class);
                Iterator<PsiMethodCallExpression> iterator = methodsCalls.iterator();
                ArrayList<PsiLocalVariable> intentVariables = new ArrayList<>();
                while(iterator.hasNext()) {
                    PsiMethodCallExpression currentMethodCall = iterator.next();
                if(currentMethodCall.getMethodExpression().getQualifier() == null) { continue; }
                    if(currentMethodCall.getMethodExpression().getQualifier().getText().equals("intent")) {
                        if(currentMethodCall.getParent() instanceof PsiLocalVariable) {
                            intentVariables.add((PsiLocalVariable) currentMethodCall.getParent());
                        }
                    }
                }
                // TODO: PRIMEIRA, VOU SO VER SE AS VARIAVEIS SAO UTILIZADAS EM IFS
                Iterator<PsiLocalVariable> iteratorLocalVariable = intentVariables.iterator();
                while(iteratorLocalVariable.hasNext()) {
                    boolean isChecked = false;
                    PsiLocalVariable localVariable = iteratorLocalVariable.next();
                    Collection<PsiReference> references = ReferencesSearch.search(localVariable).findAll();

                    Iterator<PsiReference> iterator1 = references.iterator();
                    while(iterator1.hasNext()){
                        PsiReference next = iterator1.next();
                        PsiIfStatement firstParent = (PsiIfStatement) PsiTreeUtil.findFirstParent((PsiElement) next, el -> el instanceof PsiIfStatement);
                        if(firstParent == null) { continue; }
                        // TODO: CHECK THIS NUMBER
                        if(PsiTreeUtil.getDepth((PsiElement) next, firstParent) <= 4) {
                            isChecked = true;
                        }
                    }
                    if(!isChecked) { return; }
                }
                System.out.println("resgistering problem! ");
                checkMetadataQuickFix = new CheckMetadataQuickFix();
                checkMetadataQuickFix.setIntentVariables(intentVariables);
                holder.registerProblem(method.getNameIdentifier(), DESCRIPTION_TEMPLATE_CHECK_METADATA, checkMetadataQuickFix);
            }
        };
    }
}
