/* Adapted from CogniCrypt for AndroidStudio
 * https://github.com/secure-software-engineering/CogniCrypt-IntelliJ
 */
package leaks.platform;

import com.android.tools.idea.sdk.IdeSdks;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AndroidPlatformLocator {
    private static final Logger logger = Logger.getInstance(AndroidPlatformLocator.class);

    private static final String ANDROID_SDK = "ANDROID_SDK";

    public static Path getAndroidPlatformsLocation(Project project) {
        File androidSdkPath = IdeSdks.getInstance().getAndroidSdkPath();
        String android_sdk_root;

        if (androidSdkPath != null) {
            android_sdk_root = androidSdkPath.getAbsolutePath();
            logger.info("Choosing android sdk path automatically");
        }
        else {
            android_sdk_root = System.getenv(ANDROID_SDK);
            logger.info("Fallback for android sdk path to environment variable");
        }

        if (android_sdk_root == null) {
            throw new RuntimeException("Environment variable " + ANDROID_SDK + " not found!");
        }

        Path sdkPath = Paths.get(android_sdk_root);
        if (android_sdk_root.equals("") || !sdkPath.toFile().exists()) {
            throw new RuntimeException("Environment variable " + ANDROID_SDK + " not found!");
        }
        return sdkPath.resolve("platforms");
    }
}

