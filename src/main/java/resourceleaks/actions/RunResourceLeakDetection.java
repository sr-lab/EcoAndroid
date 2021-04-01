package resourceleaks.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import resourceleaks.ResourceLeakAnalysisTask;

import javax.swing.*;
import java.awt.*;


public class RunResourceLeakDetection extends AnAction {
    private static final Logger logger = Logger.getInstance(RunResourceLeakDetection.class);

    public RunResourceLeakDetection() {
        super();
    }

    public RunResourceLeakDetection(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        ProgressManager.getInstance().run(new ResourceLeakAnalysisTask(project, event));
        Notification notification = new Notification(
                "Tasks", "EcoAndroid", "Running analysis", NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);

        /*
        System.out.println("Start highlight");
        PsiClass psiClass = JavaPsiFacade.getInstance(event.getProject()).findClass("com.ichi2.anki.MyAccount", GlobalSearchScope.allScope(event.getProject()));
        //HIGHLIGHTING TEST ON PSIELEMENT (IN THIS CASE, MYACCOUNT CLASS)
        //MUST BE DONE ON UI THREAD, CANT BE DONE ON TASK :(
        //int lineNum = document.getLineNumber(needHighlightPsiElement.getTextRange().getStartOffset());
        Color color= new Color(10, 10, 200);
        final TextAttributes textattributes = new TextAttributes(null, color, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
        //final Project project = needHighlightPsiElement.getProject();
        //final FileEditorManager editorManager = FileEditorManager.getInstance(project);
        //final Editor editor = editorManager.getSelectedTextEditor();
        //editor.getMarkupModel().addLineHighlighter(lineNum, HighlighterLayer.CARET_ROW, textattributes);

        //int lineNum = event.getData(PlatformDataKeys.EDITOR).getDocument().getLineNumber(psiClass.getTextRange().getStartOffset());
        FileEditorManager.getInstance(event.getProject()).getSelectedTextEditor().getMarkupModel().addLineHighlighter(40, HighlighterLayer.CARET_ROW, textattributes);
        System.out.println("End highlight");
        */
    }

    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}

