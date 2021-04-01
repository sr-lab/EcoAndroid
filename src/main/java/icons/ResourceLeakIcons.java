package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface ResourceLeakIcons {
    Icon ECO = IconLoader.getIcon("/icons/test.png", ResourceLeakIcons.class);
    Icon ECO2 = IconLoader.getIcon("META-INF/pluginIcon.svg", ResourceLeakIcons.class);
}
