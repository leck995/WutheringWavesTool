package com.kuro.kujiequ.model.calculator.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class CostList {
    private List<Cost> allCost;
    private List<Cost> missingCost;
    private List<Cost> synthetic;
    private List<Cost> missingRoleCost;
    private List<Cost> missingSkillCost;
    private List<Cost> missingWeaponCost;
    private Integer roleId;
    private Integer weaponId;
    private List<Strategy> strategyList;
    private boolean showStrategy;

    public List<Cost> getAllCost() {
        return allCost;
    }

    public void setAllCost(List<Cost> allCost) {
        this.allCost = allCost;
    }

    public List<Cost> getMissingCost() {
        return missingCost;
    }

    public void setMissingCost(List<Cost> missingCost) {
        this.missingCost = missingCost;
    }

    public List<Cost> getSynthetic() {
        return synthetic;
    }

    public void setSynthetic(List<Cost> synthetic) {
        this.synthetic = synthetic;
    }

    public List<Cost> getMissingRoleCost() {
        return missingRoleCost;
    }

    public void setMissingRoleCost(List<Cost> missingRoleCost) {
        this.missingRoleCost = missingRoleCost;
    }

    public List<Cost> getMissingSkillCost() {
        return missingSkillCost;
    }

    public void setMissingSkillCost(List<Cost> missingSkillCost) {
        this.missingSkillCost = missingSkillCost;
    }

    public List<Cost> getMissingWeaponCost() {
        return missingWeaponCost;
    }

    public void setMissingWeaponCost(List<Cost> missingWeaponCost) {
        this.missingWeaponCost = missingWeaponCost;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(Integer weaponId) {
        this.weaponId = weaponId;
    }

    public List<Strategy> getStrategyList() {
        return strategyList;
    }

    public void setStrategyList(List<Strategy> strategyList) {
        this.strategyList = strategyList;
    }

    public boolean isShowStrategy() {
        return showStrategy;
    }

    public void setShowStrategy(boolean showStrategy) {
        this.showStrategy = showStrategy;
    }
}
