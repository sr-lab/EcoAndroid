package leaks.ui;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import icons.ResourceLeakIcons;
import leaks.results.ResultsIntellij;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResourceLeakLineMarker implements LineMarkerProvider
{
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        Project p = psiElement.getProject();
        ResultsIntellij results = ServiceManager.getService(p, ResultsIntellij.class);
        if (psiElement instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) psiElement;
            if (results.hasLeak(method)) {
                PsiIdentifier id = method.getNameIdentifier();
                return new LineMarkerInfo(id, id.getTextRange(), ResourceLeakIcons.ECO,
                        Pass.UPDATE_ALL, null, null,
                        GutterIconRenderer.Alignment.RIGHT);
            }
        }
        return null;
    }
}
