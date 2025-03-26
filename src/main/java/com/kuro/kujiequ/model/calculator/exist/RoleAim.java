package com.kuro.kujiequ.model.calculator.exist;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/*
* 提交计算的类
* */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAim {
    private int roleId;
    private int roleStartLevel;
    private int roleEndLevel;
    private List<SkillLevelUp> skillLevelUpList;
    private List<Object> advanceSkillList; // 根据需要定义具体类型
    private int weaponId;
    private int weaponStartLevel;
    private int weaponEndLevel;
    @JsonAlias(value = "_category")
    private String category; // 注意：JSON 中的字段名是 "_category"，但在 Java 中通常使用 camelCase


    // 内部类表示技能升级信息
    public static class SkillLevelUp {
        private int startLevel;
        private int endLevel;

        // Getters and Setters

        public int getStartLevel() {
            return startLevel;
        }

        public void setStartLevel(int startLevel) {
            this.startLevel = startLevel;
        }

        public int getEndLevel() {
            return endLevel;
        }

        public void setEndLevel(int endLevel) {
            this.endLevel = endLevel;
        }
    }


    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public int getRoleStartLevel() {
        return roleStartLevel;
    }

    public void setRoleStartLevel(int roleStartLevel) {
        this.roleStartLevel = roleStartLevel;
    }

    public int getRoleEndLevel() {
        return roleEndLevel;
    }

    public void setRoleEndLevel(int roleEndLevel) {
        this.roleEndLevel = roleEndLevel;
    }

    public List<SkillLevelUp> getSkillLevelUpList() {
        return skillLevelUpList;
    }

    public void setSkillLevelUpList(List<SkillLevelUp> skillLevelUpList) {
        this.skillLevelUpList = skillLevelUpList;
    }

    public List<Object> getAdvanceSkillList() {
        return advanceSkillList;
    }

    public void setAdvanceSkillList(List<Object> advanceSkillList) {
        this.advanceSkillList = advanceSkillList;
    }

    public int getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }

    public int getWeaponStartLevel() {
        return weaponStartLevel;
    }

    public void setWeaponStartLevel(int weaponStartLevel) {
        this.weaponStartLevel = weaponStartLevel;
    }

    public int getWeaponEndLevel() {
        return weaponEndLevel;
    }

    public void setWeaponEndLevel(int weaponEndLevel) {
        this.weaponEndLevel = weaponEndLevel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
