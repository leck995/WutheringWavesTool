package cn.tealc.wutheringwavestool.model.sign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 20:19
 */
public class UserInfo extends SignUserInfo{
    private Integer id;
    private Long lastSignTime;
    private Boolean isWeb;//标志该Token时web的还是app的

    public UserInfo() {
    }

    public UserInfo(String userId, String roleId, String token, Boolean isMain, Boolean isWeb) {
        super(userId, roleId, token, isMain);
        this.isWeb = isWeb;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getLastSignTime() {
        return lastSignTime;
    }

    public void setLastSignTime(Long lastSignTime) {
        this.lastSignTime = lastSignTime;
    }

    public Boolean getWeb() {
        return isWeb;
    }

    public void setWeb(Boolean web) {
        isWeb = web;
    }
    public Boolean getIsWeb() {
        return isWeb;
    }

    public void setIsWeb(Boolean web) {
        isWeb = web;
    }




   }