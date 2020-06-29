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

    private CheckMetadataQuickFix checkMetadataQuickFix;

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @NonNls
            private final String DESCRIPTION_TEMPLATE_CHECK_METADATA = "Refactor4Green: Cache - Check Metadata";

            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                /*
                 *
                 * FIRST PHASE: LOOK FOR THE METHOD onReceive FROM THE CLASS BroadcastReceiver
                 *
                 */
                if(!(method.getName().equals("onReceive")))
                    return;

                PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                PsiClass broadcastReceiverClass = JavaPsiFacade.getInstance(holder.getProject()).findClass("android.content.BroadcastReceiver", GlobalSearchScope.allScope(holder.getProject()));
                if(!(InheritanceUtil.isInheritorOrSelf(psiClass, broadcastReceiverClass, true))) { return;}

                /*
                 *
                 * SECOND PHASE: LOOK FOR THE VARIABLES THAT STORE THE VALUE OF THE intent PARAMETER
                 *
                 */
                Collection<PsiMethodCallExpression> methodsCalls = PsiTreeUtil.collectElementsOfType(method.getBody(), PsiMethodCallExpression.class);
                Iterator<PsiMethodCallExpression> iterator = methodsCalls.iterator();
                ArrayList<PsiLocalVariable> intentVariables = new ArrayList<>();
                while(iterator.hasNext()) {
                    PsiMethodCallExpression currentMethodCall = iterator.next();
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

                /*
                 *
                 * THIRD PHASE: CHECK IF THE CONTENT RETRIEVED FROM THE intent PARAMETER IS BEING USED IN IF CONDITIONALS
                 *
                 */
                // TODO: AT THIS LEVEL, ONLY CHECKING IF THE VARIABLES ARE USED IN THE IF CONDITION
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
                        // NOTE: COMPARING POSITIONS BETWEEN TEH REFERENCE AND THE CONDITION OF THE IF. IF ITS 0, THEN THE REFERENCE IS IN THE CONDITION
                        if(PsiUtilBase.compareElementsByPosition(firstParent.getCondition(), (PsiElement) next) == 0) { isChecked = true; }
                    }
                    if(isChecked)
                        return;
                }
                checkMetadataQuickFix = new CheckMetadataQuickFix();
                checkMetadataQuickFix.setIntentVariables(intentVariables);
                holder.registerProblem(method.getNameIdentifier(), DESCRIPTION_TEMPLATE_CHECK_METADATA, checkMetadataQuickFix);
            }
        };
    }
}
