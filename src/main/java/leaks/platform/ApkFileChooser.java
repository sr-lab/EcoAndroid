package leaks.platform;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class ApkFileChooser {

    private static VirtualFile chooseApk(Project project) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("apk");
        descriptor.setDescription("Choose the APK to be analyzed. It must match the current version of the source code.");
        descriptor.setRoots(project.getBaseDir());
        descriptor.setForcedToUseIdeaFileChooser(true);

        VirtualFile apk = FileChooser.chooseFile(descriptor, project, project.getBaseDir());

        if (apk == null) {
            throw new RuntimeException("Could not get APK to analyze!");
        }

        return apk;
    }

    public static String getApkPath(Project project) {
        return chooseApk(project).getPath();
    }
}


