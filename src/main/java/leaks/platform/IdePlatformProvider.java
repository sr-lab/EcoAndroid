/* Adapted from CogniCrypt for AndroidStudio
 * https://github.com/secure-software-engineering/CogniCrypt-IntelliJ
 */
package leaks.platform;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;

public final class IdePlatformProvider
{
    private static final String AndroidIdeName= "Android Studio";
    private static final String IntelliJIdeName = "IntelliJ IDEA";

    private final IdeType instanceType;

    public IdePlatformProvider(Project project) {
        this.instanceType = GetTypeInternal();
    }

    public IdeType GetRunningPlatform()
    {
        return this.instanceType;
    }

    private IdeType GetTypeInternal(){
        ApplicationNamesInfo nameInfo = ApplicationNamesInfo.getInstance();
        String ideName = nameInfo.getFullProductName();

        switch (ideName)
        {
            case AndroidIdeName:
                return IdeType.AndroidStudio;
            case IntelliJIdeName:
                return IdeType.IntelliJ;
            default:
                return IdeType.Unknown;
        }
    }
}