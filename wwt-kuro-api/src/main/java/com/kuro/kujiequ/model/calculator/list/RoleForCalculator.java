package com.kuro.kujiequ.model.calculator.list;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-29 21:45
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleForCalculator {
    private int roleId;
    private String roleName;
    private String roleIconUrl;
    private int starLevel;
    private int attributeId;
    private String attributeName;
    private int weaponTypeId;
    private String weaponTypeName;
    private String acronym; //缩写
    private boolean isPreview;
    private boolean isNew;
    private int priority;
    private List<CommonSkill> commonSkillList;
    private List<AdvanceSkillList> advanceSkillList;


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

    public String getRoleIconUrl() {
        return roleIconUrl;
    }

    public void setRoleIconUrl(String roleIconUrl) {
        this.roleIconUrl = roleIconUrl;
    }

    public int getStarLevel() {
        return starLevel;
    }

    public void setStarLevel(int starLevel) {
        this.starLevel = starLevel;
    }

    public int getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(int attributeId) {
        this.attributeId = attributeId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public int getWeaponTypeId() {
        return weaponTypeId;
    }

    public void setWeaponTypeId(int weaponTypeId) {
        this.weaponTypeId = weaponTypeId;
    }

    public String getWeaponTypeName() {
        return weaponTypeName;
    }

    public void setWeaponTypeName(String weaponTypeName) {
        this.weaponTypeName = weaponTypeName;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public boolean isPreview() {
        return isPreview;
    }

    public void setPreview(boolean preview) {
        isPreview = preview;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<CommonSkill> getCommonSkillList() {
        return commonSkillList;
    }

    public void setCommonSkillList(List<CommonSkill> commonSkillList) {
        this.commonSkillList = commonSkillList;
    }

    public List<AdvanceSkillList> getAdvanceSkillList() {
        return advanceSkillList;
    }

    public void setAdvanceSkillList(List<AdvanceSkillList> advanceSkillList) {
        this.advanceSkillList = advanceSkillList;
    }
}