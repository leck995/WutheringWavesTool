package com.kuro.kujiequ.model.roleData.user;

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
    private RoleDailyDetail storeEnergyData; //结晶单质
    private RoleDailyDetail towerData; //逆境深塔·实验区
    private RoleDailyDetail newTowerData; //终焉矩阵
    private RoleDailyDetail slashTowerData; //冥歌海墟·再生-湍渊
    private RoleDailyDetail weeklyData; //战歌重奏
    private RoleDailyDetail weeklyFrameData; //周度游历
    private RoleDailyDetail weeklyRougeData; //千道门扉的异想
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

    public RoleDailyDetail getStoreEnergyData() {
        return storeEnergyData;
    }

    public void setStoreEnergyData(RoleDailyDetail storeEnergyData) {
        this.storeEnergyData = storeEnergyData;
    }

    public RoleDailyDetail getTowerData() {
        return towerData;
    }

    public void setTowerData(RoleDailyDetail towerData) {
        this.towerData = towerData;
    }

    public RoleDailyDetail getSlashTowerData() {
        return slashTowerData;
    }

    public void setSlashTowerData(RoleDailyDetail slashTowerData) {
        this.slashTowerData = slashTowerData;
    }

    public RoleDailyDetail getWeeklyData() {
        return weeklyData;
    }

    public void setWeeklyData(RoleDailyDetail weeklyData) {
        this.weeklyData = weeklyData;
    }

    public RoleDailyDetail getNewTowerData() {
        return newTowerData;
    }

    public void setNewTowerData(RoleDailyDetail newTowerData) {
        this.newTowerData = newTowerData;
    }

    public RoleDailyDetail getWeeklyFrameData() {
        return weeklyFrameData;
    }

    public void setWeeklyFrameData(RoleDailyDetail weeklyFrameData) {
        this.weeklyFrameData = weeklyFrameData;
    }

    public RoleDailyDetail getWeeklyRougeData() {
        return weeklyRougeData;
    }

    public void setWeeklyRougeData(RoleDailyDetail weeklyRougeData) {
        this.weeklyRougeData = weeklyRougeData;
    }
}