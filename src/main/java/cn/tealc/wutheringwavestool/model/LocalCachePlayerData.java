package cn.tealc.wutheringwavestool.model;

import com.kuro.launcher.model.api.PlayerInfo;

/**
 * @description: 本地缓存的玩家数据，继承 PlayerInfo 并补充 OAuth 凭证信息
 * @author: Leck
 * @create: 2026-05-28
 */
public class LocalCachePlayerData extends PlayerInfo {
    private Integer id;
    private String oauthCode;
    private String cuid;
    private long updateTime;

    public LocalCachePlayerData() {}

    public LocalCachePlayerData(PlayerInfo playerInfo) {
        setRoleId(playerInfo.getRoleId());
        setRegion(playerInfo.getRegion());
        setRoleName(playerInfo.getRoleName());
        setLevel(playerInfo.getLevel());
        setSex(playerInfo.getSex());
        setHeadPhoto(playerInfo.getHeadPhoto());
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getOauthCode() { return oauthCode; }
    public void setOauthCode(String oauthCode) { this.oauthCode = oauthCode; }

    public String getCuid() { return cuid; }
    public void setCuid(String cuid) { this.cuid = cuid; }

    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
