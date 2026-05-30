package cn.tealc.wutheringwavestool.service;

import com.google.inject.Singleton;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AutoStartService {
    private static final Logger LOG = LoggerFactory.getLogger(AutoStartService.class);
    private static final String RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "WutheringWavesTool";

    public void enable() {
        String exePath = ProcessHandle.current().info().command().orElse("");
        if (exePath.isEmpty() || !exePath.endsWith(".exe")) {
            LOG.warn("无法获取 exe 路径，跳过注册表写入: {}", exePath);
            return;
        }
        String command = exePath + " --auto-start";
        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, APP_NAME, command);
        LOG.info("已设置开机启动: {}", command);
    }

    public void disable() {
        if (!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, APP_NAME)) {
            return;
        }
        Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, APP_NAME);
        LOG.info("已取消开机启动");
    }
}
