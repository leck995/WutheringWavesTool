package cn.tealc.wutheringwavestool.model.kujiequ.roleData.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-08 05:06
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleDailyData {
    private int gameId;
    private int userId;
    private long serverTime;
    private String serverId;
    private String serverName;
    private String signInUrl;
    private String signInTxt;
    private boolean hasSignIn;
    private String roleId;
    private String roleName;
    private RoleDailyDetail energyData;
    private RoleDailyDetail livenessData;
    private List<RoleDailyDetail> battlePassData;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getSignInUrl() {
        return signInUrl;
    }

    public void setSignInUrl(String signInUrl) {
        this.signInUrl = signInUrl;
    }

    public String getSignInTxt() {
        return signInTxt;
    }

    public void setSignInTxt(String signInTxt) {
        this.signInTxt = signInTxt;
    }

    public boolean isHasSignIn() {
        return hasSignIn;
    }

    public void setHasSignIn(boolean hasSignIn) {
        this.hasSignIn = hasSignIn;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public RoleDailyDetail getEnergyData() {
        return energyData;
    }

    public void setEnergyData(RoleDailyDetail energyData) {
        this.energyData = energyData;
    }

    public RoleDailyDetail getLivenessData() {
        return livenessData;
    }

    public void setLivenessData(RoleDailyDetail livenessData) {
        this.livenessData = livenessData;
    }

    public List<RoleDailyDetail> getBattlePassData() {
        return battlePassData;
    }

    public void setBattlePassData(List<RoleDailyDetail> battlePassData) {
        this.battlePassData = battlePassData;
    }
}