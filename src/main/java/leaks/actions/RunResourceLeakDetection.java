package leaks.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import leaks.ResourceLeakAnalysisTask;
import leaks.platform.IdePlatformProvider;
import leaks.platform.IdeType;

public class RunResourceLeakDetection extends AnAction {
    private static final Logger logger = Logger.getInstance(RunResourceLeakDetection.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();

        IdePlatformProvider idePlatformProvider = ServiceManager.getService(project, IdePlatformProvider.class);
        IdeType ideType = idePlatformProvider.GetRunningPlatform();

        switch (ideType) {
            case IntelliJ:
            case AndroidStudio:
                ProgressManager.getInstance().run(new ResourceLeakAnalysisTask(project, event));
                Notifications.Bus.notify(new Notification(
                        "Tasks", "EcoAndroid", "Running analysis", NotificationType.INFORMATION));
                break;
            default:
                Notifications.Bus.notify(new Notification(
                        "Tasks", "EcoAndroid", "Could not run analysis on the current IDE", NotificationType.ERROR));
                break;
        }

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

