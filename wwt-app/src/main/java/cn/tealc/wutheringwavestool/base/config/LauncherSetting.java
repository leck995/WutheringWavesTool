package cn.tealc.wutheringwavestool.base.config;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * 高级启动相关配置：是否启用高级启动、兼容旧版的启动参数字符串。
 * <p>对应原 Setting 的"高级启动相关"分段。
 * <p>新配置中的启动参数保存在 {@code GameInstallation}，此处的 {@code appParams} 仅用于旧版迁移。
 */
public class LauncherSetting {
    private SimpleBooleanProperty userAdvanceGameSettings = new SimpleBooleanProperty(false); //使用高级启动
    private SimpleStringProperty appParams = new SimpleStringProperty(); //启动参数

    // ---------- userAdvanceGameSettings ----------
    public boolean isUserAdvanceGameSettings() { return userAdvanceGameSettings.get(); }
    public SimpleBooleanProperty userAdvanceGameSettingsProperty() { return userAdvanceGameSettings; }
    public void setUserAdvanceGameSettings(boolean userAdvanceGameSettings) { this.userAdvanceGameSettings.set(userAdvanceGameSettings); }

    // ---------- appParams ----------
    public String getAppParams() { return appParams.get(); }
    public SimpleStringProperty appParamsProperty() { return appParams; }
    public void setAppParams(String appParams) { this.appParams.set(appParams); }
}
