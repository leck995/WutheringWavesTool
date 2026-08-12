package com.kuro.kujiequ.model.calculator.list;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 15:18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaponForCalculator {
    private int weaponId;
    private String weaponName;
    private int weaponType;
    private int weaponStarLevel;
    private String weaponIcon;
    private String acronym; //缩写
    private boolean isPreview;
    private boolean isNew;
    private int priority;

    public int getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }

    public String getWeaponName() {
        return weaponName;
    }

    public void setWeaponName(String weaponName) {
        this.weaponName = weaponName;
    }

    public int getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(int weaponType) {
        this.weaponType = weaponType;
    }

    public int getWeaponStarLevel() {
        return weaponStarLevel;
    }

    public void setWeaponStarLevel(int weaponStarLevel) {
        this.weaponStarLevel = weaponStarLevel;
    }

    public String getWeaponIcon() {
        return weaponIcon;
    }

    public void setWeaponIcon(String weaponIcon) {
        this.weaponIcon = weaponIcon;
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
}