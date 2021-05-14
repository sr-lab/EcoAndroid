package leaks;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.refactoring.classMembers.MemberInfoTooltipManager;
import icons.ResourceLeakIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ResourceLeakLineMarker implements LineMarkerProvider
{
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        Project p = psiElement.getProject();
        ResultsProvider results = ServiceManager.getService(p, ResultsProvider.class);
        if (psiElement instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) psiElement;
            if (results.hasResourceLeaked(method)) {
                PsiIdentifier id = method.getNameIdentifier();
                return new LineMarkerInfo(id, id.getTextRange(), ResourceLeakIcons.ECO,
                        Pass.UPDATE_ALL, null, null,
                        GutterIconRenderer.Alignment.RIGHT);
            }
        }
        return null;
    }
}
