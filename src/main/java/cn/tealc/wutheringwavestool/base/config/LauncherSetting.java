package cn.tealc.wutheringwavestool.base.config;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * 高级启动相关配置：是否启用高级启动、启动参数字符串。
 * <p>对应原 Setting 的"高级启动相关"分段。
 * <p>注意：{@code startUpParams} 因使用自定义 Jackson 序列化器，且 {@code @JsonUnwrapped}
 * 场景下字段级 {@code @JsonDeserialize} 不生效，该字段保留在 {@link cn.tealc.wutheringwavestool.base.Setting}
 * 顶层，不放入本分组类。
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
