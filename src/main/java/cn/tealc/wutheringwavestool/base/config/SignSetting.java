package cn.tealc.wutheringwavestool.base.config;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * 签到相关配置：库街区自动签到开关、上次签到时间。
 * <p>对应原 Setting 的"签到相关"分段。
 */
public class SignSetting {
    private SimpleBooleanProperty autoKujieQuSign = new SimpleBooleanProperty(false); //自动签到
    private SimpleIntegerProperty lastKujiequSignTime = new SimpleIntegerProperty(0);

    // ---------- autoKujieQuSign ----------
    public boolean isAutoKujieQuSign() { return autoKujieQuSign.get(); }
    public SimpleBooleanProperty autoKujieQuSignProperty() { return autoKujieQuSign; }
    public void setAutoKujieQuSign(boolean autoKujieQuSign) { this.autoKujieQuSign.set(autoKujieQuSign); }

    // ---------- lastKujiequSignTime ----------
    public int getLastKujiequSignTime() { return lastKujiequSignTime.get(); }
    public SimpleIntegerProperty lastKujiequSignTimeProperty() { return lastKujiequSignTime; }
    public void setLastKujiequSignTime(int lastKujiequSignTime) { this.lastKujiequSignTime.set(lastKujiequSignTime); }
}
