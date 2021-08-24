package leaks;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ResourceLeakAnalysisTask extends Task.Backgroundable {
    private static final Logger logger = Logger.getInstance(ResourceLeakAnalysisTask.class);
    private AnActionEvent event;
    private Project project;
    private String apkPath;

    public ResourceLeakAnalysisTask(Project project, AnActionEvent event, String apkPath){
        super(project, "Resource Leak Analysis");
        this.project = project;
        this.event = event;
        this.apkPath = apkPath;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        AnalysisWrapper.getInstance().RunIntellijAnalysis(project, indicator, apkPath);
    }
}