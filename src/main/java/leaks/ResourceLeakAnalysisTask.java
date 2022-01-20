package leaks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import leaks.analysis.AnalysisWrapper;
import org.jetbrains.annotations.NotNull;

public class ResourceLeakAnalysisTask extends Task.Backgroundable {
    private final Project project;
    private final String apkPath;
    private final String androidSdkPath;

    public ResourceLeakAnalysisTask(Project project, String apkPath, String androidSdkPath) {
        super(project, "Resource Leak Analysis");
        this.project = project;
        this.apkPath = apkPath;
        this.androidSdkPath = androidSdkPath;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        AnalysisWrapper.getInstance().RunIntellijAnalysis(project, indicator, apkPath, androidSdkPath);
    }
}