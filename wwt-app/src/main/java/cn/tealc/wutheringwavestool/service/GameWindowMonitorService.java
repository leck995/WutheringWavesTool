package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.jna.GameAppListener;
import com.google.inject.Singleton;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;

@Singleton
public class GameWindowMonitorService {
    private WinNT.HANDLE hookHandle;

    public void start() {
        GameAppListener appListener = GameAppListener.getInstance();
        hookHandle = User32.INSTANCE.SetWinEventHook(0x0003, 0x0003, null, appListener, 0, 0, 0);
    }

    public void stop() {
        if (hookHandle != null) {
            User32.INSTANCE.UnhookWinEvent(hookHandle);
        }
    }
}
