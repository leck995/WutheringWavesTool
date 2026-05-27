package com.kuro.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BattlePassData {
    @JsonProperty("Level")
    private Integer level;
    
    @JsonProperty("WeekExp")
    private Integer weekExp;
    
    @JsonProperty("WeekMaxExp")
    private Integer weekMaxExp;
    
    @JsonProperty("IsUnlock")
    private Boolean isUnlock;
    
    @JsonProperty("IsOpen")
    private Boolean isOpen;
    
    @JsonProperty("Exp")
    private Integer exp;
    
    @JsonProperty("ExpLimit")
    private Integer expLimit;

    // 无参构造函数
    public BattlePassData() {
    }

    // Getter和Setter方法
    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getWeekExp() {
        return weekExp;
    }

    public void setWeekExp(Integer weekExp) {
        this.weekExp = weekExp;
    }

    public Integer getWeekMaxExp() {
        return weekMaxExp;
    }

    public void setWeekMaxExp(Integer weekMaxExp) {
        this.weekMaxExp = weekMaxExp;
    }

    public Boolean getUnlock() {
        return isUnlock;
    }

    public void setUnlock(Boolean unlock) {
        isUnlock = unlock;
    }

    public Boolean getOpen() {
        return isOpen;
    }

    public void setOpen(Boolean open) {
        isOpen = open;
    }

    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }

    public Integer getExpLimit() {
        return expLimit;
    }

    public void setExpLimit(Integer expLimit) {
        this.expLimit = expLimit;
    }

    @Override
    public String toString() {
        return "BattlePassData{" +
                "level=" + level +
                ", weekExp=" + weekExp +
                ", weekMaxExp=" + weekMaxExp +
                ", isUnlock=" + isUnlock +
                ", isOpen=" + isOpen +
                ", exp=" + exp +
                ", expLimit=" + expLimit +
                '}';
    }
}
