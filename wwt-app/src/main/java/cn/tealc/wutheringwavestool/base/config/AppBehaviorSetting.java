package cn.tealc.wutheringwavestool.base.config;

import cn.tealc.wutheringwavestool.base.AppConstants;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
/**
 * 应用行为相关配置：开发模式、赞助、版本跳过、自启、静默启动、关闭行为、资源源、
 * 库街区开关、本地缓存、主页头像/角色、日志等级。
 * <p>对应原 Setting 的顶部 UI 字段与"其他设置"分段。
 */
public class AppBehaviorSetting {
    private SimpleBooleanProperty devModel = new SimpleBooleanProperty(false); //开发者模式。用于测试
    private SimpleBooleanProperty support = new SimpleBooleanProperty(false);  //标志是否赞助
    private SimpleStringProperty skipVersion = new SimpleStringProperty(AppConstants.VERSION);
    private SimpleBooleanProperty checkNewVersion = new SimpleBooleanProperty(true); //检查更新
    private SimpleBooleanProperty autoStart = new SimpleBooleanProperty(false); //开机自启
    private SimpleBooleanProperty silentStart = new SimpleBooleanProperty(false); //静默启动，不显示窗口
    private SimpleIntegerProperty closeEvent = new SimpleIntegerProperty(0); //关闭主界面行为，0选择，1退出，2最小化
    private SimpleIntegerProperty resourceSource = new SimpleIntegerProperty(1); //0代表Github，1代表码云或其他
    private SimpleBooleanProperty noKuJieQu = new SimpleBooleanProperty(false); //不使用库街区
    private SimpleBooleanProperty useLocalCacheUser = new SimpleBooleanProperty(true);
    private SimpleStringProperty homeViewIcon = new SimpleStringProperty();  //主页头像
    private SimpleStringProperty homeViewRole = new SimpleStringProperty(); //主页人物
    private SimpleStringProperty logLevel = new SimpleStringProperty("INFO"); //日志等级

    // noKuJieQu / resourceSource 的默认值依赖 language，由 Setting 聚合层在构造后修正
    public AppBehaviorSetting() {
    }

    // ---------- devModel ----------
    public boolean isDevModel() { return devModel.get(); }
    public SimpleBooleanProperty devModelProperty() { return devModel; }
    public void setDevModel(boolean devModel) { this.devModel.set(devModel); }

    // ---------- support ----------
    public boolean isSupport() { return support.get(); }
    public SimpleBooleanProperty supportProperty() { return support; }
    public void setSupport(boolean support) { this.support.set(support); }

    // ---------- skipVersion ----------
    public String getSkipVersion() { return skipVersion.get(); }
    public SimpleStringProperty skipVersionProperty() { return skipVersion; }
    public void setSkipVersion(String skipVersion) { this.skipVersion.set(skipVersion); }

    // ---------- checkNewVersion ----------
    public boolean isCheckNewVersion() { return checkNewVersion.get(); }
    public SimpleBooleanProperty checkNewVersionProperty() { return checkNewVersion; }
    public void setCheckNewVersion(boolean checkNewVersion) { this.checkNewVersion.set(checkNewVersion); }

    // ---------- autoStart ----------
    public boolean isAutoStart() { return autoStart.get(); }
    public SimpleBooleanProperty autoStartProperty() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart.set(autoStart); }

    // ---------- silentStart ----------
    public boolean isSilentStart() { return silentStart.get(); }
    public SimpleBooleanProperty silentStartProperty() { return silentStart; }
    public void setSilentStart(boolean silentStart) { this.silentStart.set(silentStart); }

    // ---------- closeEvent ----------
    public int getCloseEvent() { return closeEvent.get(); }
    public SimpleIntegerProperty closeEventProperty() { return closeEvent; }
    public void setCloseEvent(int closeEvent) { this.closeEvent.set(closeEvent); }

    // ---------- resourceSource ----------
    public int getResourceSource() { return resourceSource.get(); }
    public SimpleIntegerProperty resourceSourceProperty() { return resourceSource; }
    public void setResourceSource(int resourceSource) { this.resourceSource.set(resourceSource); }

    // ---------- noKuJieQu ----------
    public boolean isNoKuJieQu() { return noKuJieQu.get(); }
    public SimpleBooleanProperty noKuJieQuProperty() { return noKuJieQu; }
    public void setNoKuJieQu(boolean noKuJieQu) { this.noKuJieQu.set(noKuJieQu); }

    // ---------- useLocalCacheUser ----------
    public boolean isUseLocalCacheUser() { return useLocalCacheUser.get(); }
    public SimpleBooleanProperty useLocalCacheUserProperty() { return useLocalCacheUser; }
    public void setUseLocalCacheUser(boolean useLocalCacheUser) { this.useLocalCacheUser.set(useLocalCacheUser); }

    // ---------- homeViewIcon ----------
    public String getHomeViewIcon() { return homeViewIcon.get(); }
    public SimpleStringProperty homeViewIconProperty() { return homeViewIcon; }
    public void setHomeViewIcon(String homeViewIcon) { this.homeViewIcon.set(homeViewIcon); }

    // ---------- homeViewRole ----------
    public String getHomeViewRole() { return homeViewRole.get(); }
    public SimpleStringProperty homeViewRoleProperty() { return homeViewRole; }
    public void setHomeViewRole(String homeViewRole) { this.homeViewRole.set(homeViewRole); }

    // ---------- logLevel ----------
    public String getLogLevel() { return logLevel.get(); }
    public SimpleStringProperty logLevelProperty() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel.set(logLevel); }
}
