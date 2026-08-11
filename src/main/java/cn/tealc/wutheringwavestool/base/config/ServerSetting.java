package cn.tealc.wutheringwavestool.base.config;

import javafx.beans.property.SimpleStringProperty;

/**
 * 服务器相关配置：云同步/资源服务器的用户名与密码。
 * <p>对应原 Setting 的"服务器相关"分段。
 */
public class ServerSetting {
    private SimpleStringProperty serverUsername = new SimpleStringProperty();
    private SimpleStringProperty serverPassword = new SimpleStringProperty();

    // ---------- serverUsername ----------
    public String getServerUsername() { return serverUsername.get(); }
    public SimpleStringProperty serverUsernameProperty() { return serverUsername; }
    public void setServerUsername(String serverUsername) { this.serverUsername.set(serverUsername); }

    // ---------- serverPassword ----------
    public String getServerPassword() { return serverPassword.get(); }
    public SimpleStringProperty serverPasswordProperty() { return serverPassword; }
    public void setServerPassword(String serverPassword) { this.serverPassword.set(serverPassword); }
}
