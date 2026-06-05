package com.kuro.kujiequ.model.newTowerData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewTowerRole {
    private String iconUrl;
    private int roleId;
    private Integer skillBranchIndex;

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public Integer getSkillBranchIndex() {
        return skillBranchIndex;
    }

    public void setSkillBranchIndex(Integer skillBranchIndex) {
        this.skillBranchIndex = skillBranchIndex;
    }
}
