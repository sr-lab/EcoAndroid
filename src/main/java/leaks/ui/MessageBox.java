/* Adapted from CogniCrypt for AndroidStudio
 * https://github.com/secure-software-engineering/CogniCrypt-IntelliJ
 */
package leaks.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.util.ui.AppleBoldDottedPainter;
import org.jdesktop.swingx.plaf.UIManagerExt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class MessageBox
{
    private static final String DefaultCaption = "EcoAndroid";

    public static MessageBoxResult Show(String text, MessageBoxButton button, MessageBoxType type, MessageBoxResult defaultResult)
    {
        return ShowInternal(null, text, button, type, defaultResult);
    }

    public static MessageBoxResult Show(String text, MessageBoxButton button)
    {
        return ShowInternal(null, text, button, MessageBoxType.None, MessageBoxResult.YesOK);
    }

    public static MessageBoxResult Show(String text, MessageBoxButton button, MessageBoxType type)
    {
        return ShowInternal(null, text, button, type,  MessageBoxResult.YesOK);
    }

    public static MessageBoxResult Show(String text)
    {
        return ShowInternal(null, text, MessageBoxButton.OK, MessageBoxType.None, MessageBoxResult.YesOK);
    }

    public static MessageBoxResult Show(Component owner, String text, MessageBoxButton button, MessageBoxType type,
                                        MessageBoxResult defaultResult)
    {
        return ShowInternal(owner, text, button, type, defaultResult);
    }

    public static MessageBoxResult Show(Component owner, String text, MessageBoxButton button, MessageBoxType type)
    {
        return ShowInternal(owner, text, button, type, MessageBoxResult.YesOK);
    }

    public static MessageBoxResult Show(Component owner, String text, MessageBoxButton button)
    {
        return ShowInternal(owner, text, button, MessageBoxType.None, MessageBoxResult.YesOK);
    }

    public static MessageBoxResult Show(Component owner, String text)
    {
        return ShowInternal(owner, text, MessageBoxButton.OK, MessageBoxType.None, MessageBoxResult.YesOK);
    }

    static MessageBoxResult ShowInternal(Component owner, String text, MessageBoxButton button, MessageBoxType type, MessageBoxResult defaultResult)
    {
        JOptionPane pane = new JOptionPane(text, type.GetValue(), button.GetValue(), null, null, defaultResult);

        if (owner == null)
            owner = WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow();

        JDialog dialog = pane.createDialog(owner, DefaultCaption);

        Set forwardTraversalKeys = new HashSet(dialog.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forwardTraversalKeys.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.VK_UNDEFINED));
        dialog.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardTraversalKeys);

        Set backwardTraversalKeys = new HashSet(dialog.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        backwardTraversalKeys.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_LEFT, KeyEvent.VK_UNDEFINED));
        dialog.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardTraversalKeys);

        dialog.setVisible(true);
        dialog.dispose();

        Integer result = (Integer) pane.getValue();
        if (result == null)
            return MessageBoxResult.Cancel;
        return MessageBoxResult.FromNumber(result.intValue());
    }

    /**
     * Matches the JOptionDialog return values
     */
    public enum MessageBoxResult
    {
        YesOK(0),
        No(1),
        Cancel(2),
        Closed(-1);

        private final int _value;

        MessageBoxResult(int value)
        {
            _value = value;
        }

        public int GetValue()
        {
            return _value;
        }

        static MessageBoxResult FromNumber(int number)
        {
            switch (number){
                case 0:
                    return YesOK;
                case 1:
                    return No;
                case 2:
                    return Cancel;
                case -1:
                    return Closed;
                default:
                    throw new IllegalStateException("Unexpected value: " + number);
            }
        }
    }

    /**
     * Matches the optionType values from JOptionPane
     */
    public enum MessageBoxButton
    {
        OK(-1),
        OKCancel(2),
        YesNoCancel(1),
        YesNo(0);

        private final int _value;

        MessageBoxButton(int value)
        {
            _value = value;
        }

        public int GetValue()
        {
            return _value;
        }
    }

    /**
     * Matches the messageType values from JOptionPane
     */
    public enum MessageBoxType
    {
        Error(0),
        Information(1),
        Warning(2),
        Question(3),
        None(-1);

        private final int _value;

        MessageBoxType(int value)
        {
            _value = value;
        }

        public int GetValue()
        {
            return _value;
        }
    }
}



