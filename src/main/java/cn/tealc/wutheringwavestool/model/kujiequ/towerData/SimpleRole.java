package cn.tealc.wutheringwavestool.model.kujiequ.towerData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-15 23:12
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleRole {
    private Integer roleId;
    private String iconUrl;

    public SimpleRole(Integer roleId, String iconUrl) {
        this.roleId = roleId;
        this.iconUrl = iconUrl;
    }

    public SimpleRole() {
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}