package com.kuro.kujiequ.model.calculator.exist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExistedRoleDataForCalculator {
    private int roleId;
    private String roleName;
    private int roleLevel;
    private int roleBreakLevel;
    private List<SkillLevel> skillLevelList;
    private List<String> skillBreakList;


    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public int getRoleLevel() {
        return roleLevel;
    }

    public void setRoleLevel(int roleLevel) {
        this.roleLevel = roleLevel;
    }

    public int getRoleBreakLevel() {
        return roleBreakLevel;
    }

    public void setRoleBreakLevel(int roleBreakLevel) {
        this.roleBreakLevel = roleBreakLevel;
    }

    public List<SkillLevel> getSkillLevelList() {
        return skillLevelList;
    }

    public void setSkillLevelList(List<SkillLevel> skillLevelList) {
        this.skillLevelList = skillLevelList;
    }

    public List<String> getSkillBreakList() {
        return skillBreakList;
    }

    public void setSkillBreakList(List<String> skillBreakList) {
        this.skillBreakList = skillBreakList;
    }
}