package com.kuro.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MotorData {
    @JsonProperty("Level")
    private Integer level;
    
    @JsonProperty("Exp")
    private Integer exp;
    
    @JsonProperty("NextExp")
    private Integer nextExp;
    
    @JsonProperty("Skins")
    private List<Skin> skins;
    
    @JsonProperty("EquipSkin")
    private Skin equipSkin;

    // 无参构造函数
    public MotorData() {
    }

    // Getter和Setter方法
    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }

    public Integer getNextExp() {
        return nextExp;
    }

    public void setNextExp(Integer nextExp) {
        this.nextExp = nextExp;
    }

    public List<Skin> getSkins() {
        return skins;
    }

    public void setSkins(List<Skin> skins) {
        this.skins = skins;
    }

    public Skin getEquipSkin() {
        return equipSkin;
    }

    public void setEquipSkin(Skin equipSkin) {
        this.equipSkin = equipSkin;
    }

    @Override
    public String toString() {
        return "MotorData{" +
                "level=" + level +
                ", exp=" + exp +
                ", nextExp=" + nextExp +
                ", skins=" + skins +
                ", equipSkin=" + equipSkin +
                '}';
    }
}