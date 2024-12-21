package cn.tealc.wutheringwavestool.base;

import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import de.saxsys.mvvmfx.MvvmFX;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-12-17 17:10
 */
public class NotificationManager {
    public static void message(MessageInfo messageInfo) {
        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, messageInfo);
    }
}