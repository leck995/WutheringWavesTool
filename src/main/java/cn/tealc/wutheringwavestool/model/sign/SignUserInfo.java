package cn.tealc.wutheringwavestool.model.sign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 20:19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignUserInfo{

    private String userId;
    private String roleId;
    private String token;
    private Boolean isMain;
    public SignUserInfo() {
    }

    public SignUserInfo(String userId, String roleId, String token, Boolean isMain) {
        this.userId = userId;
        this.roleId = roleId;
        this.token = token;
        this.isMain = isMain;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getMain() {
        return isMain;
    }
    public void setMain(Boolean main) {
        isMain = main;
    }
    public void setIsMain(Boolean main) {
        isMain = main;
    }
    public Boolean getIsMain() {
        return isMain;
    }



    @Override
    public String toString() {
        return String.format("用户ID: %s \n游戏ID: %s", userId, roleId);
    }
}