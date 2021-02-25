package resourceleaks.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import resourceleaks.ResourceLeakAnalysisTask;
import resourceleaks.ui.NotificationProvider;

import javax.swing.*;


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
        // Using the event, create and show a dialog
        Project project = event.getProject();
        StringBuilder dlgMsg = new StringBuilder(event.getPresentation().getText() + " Selected!");
        String dlgTitle = event.getPresentation().getDescription();
        // If an element is selected in the editor, add info about it.
        Navigatable nav = event.getData(CommonDataKeys.NAVIGATABLE);
        ProgressManager.getInstance().run(new ResourceLeakAnalysisTask(project));
        //NotificationProvider.info("Started running resource leak detection");
        Notification notification = new Notification(
                "Tasks", "EcoAndroid", "Running analysis", NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);

    }

    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}