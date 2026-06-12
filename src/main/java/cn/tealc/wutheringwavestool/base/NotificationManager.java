package cn.tealc.wutheringwavestool.base;


import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.ui.component.BaseDialog;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.utils.notifications.NotificationObserver;
import javafx.scene.layout.Pane;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-12-17 17:10
 */
public class NotificationManager {
    public static void publish(String key, Object... objects) {
        MvvmFX.getNotificationCenter().publish(key, objects);
    }

    public static void subscribe(String key, NotificationObserver observer) {
        MvvmFX.getNotificationCenter().subscribe(key, observer);
    }

    public static void message(MessageInfo messageInfo) {
        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, messageInfo);
    }

    public static void dialog(JFXDialogLayout layout) {
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, layout);
    }
    public static void dialog(Pane pane, BaseDialog dialog) {
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, pane,dialog);
    }
}