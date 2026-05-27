package cn.tealc.wutheringwavestool.model;

/**
 * @description: OAuth 凭证模型
 * @author: Leck
 * @create: 2026-05-27
 */
public class OAuthCredential {
    private Integer id;
    private String roleId;
    private String oauthCode;
    private long updateTime;

    public OAuthCredential() {}

    public OAuthCredential(String roleId, String oauthCode, long updateTime) {
        this.roleId = roleId;
        this.oauthCode = oauthCode;
        this.updateTime = updateTime;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getOauthCode() { return oauthCode; }
    public void setOauthCode(String oauthCode) { this.oauthCode = oauthCode; }

    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
