package com.kuro.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseData {
    @JsonProperty("Name")
    private String name;
    
    @JsonProperty("Id")
    private Long id;
    
    @JsonProperty("CreatTime")
    private Long creatTime;
    
    @JsonProperty("ActiveDays")
    private Integer activeDays;
    
    @JsonProperty("Level")
    private Integer level;
    
    @JsonProperty("WorldLevel")
    private Integer worldLevel;
    
    @JsonProperty("RoleNum")
    private Integer roleNum;
    
    @JsonProperty("SoundBox")
    private Integer soundBox;
    
    @JsonProperty("Energy")
    private Integer energy;
    
    @JsonProperty("MaxEnergy")
    private Integer maxEnergy;
    
    @JsonProperty("StoreEnergy")
    private Integer storeEnergy;
    
    @JsonProperty("StoreEnergyRecoverTime")
    private Long storeEnergyRecoverTime;
    
    @JsonProperty("MaxStoreEnergy")
    private Integer maxStoreEnergy;
    
    @JsonProperty("EnergyRecoverTime")
    private Long energyRecoverTime;
    
    @JsonProperty("Liveness")
    private Integer liveness;
    
    @JsonProperty("LivenessMaxCount")
    private Integer livenessMaxCount;
    
    @JsonProperty("LivenessUnlock")
    private Boolean livenessUnlock;
    
    @JsonProperty("ChapterId")
    private Integer chapterId;
    
    @JsonProperty("WeeklyInstCount")
    private Integer weeklyInstCount;
    
    @JsonProperty("Boxes")
    private Map<String, Integer> boxes;
    
    @JsonProperty("BasicBoxes")
    private Map<String, Integer> basicBoxes;
    
    @JsonProperty("PhantomBoxes")
    private Map<String, Integer> phantomBoxes;
    
    @JsonProperty("BirthMon")
    private Integer birthMon;
    
    @JsonProperty("BirthDay")
    private Integer birthDay;

    // 无参构造函数
    public BaseData() {
    }

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreatTime() {
        return creatTime;
    }

    public void setCreatTime(Long creatTime) {
        this.creatTime = creatTime;
    }

    public Integer getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(Integer activeDays) {
        this.activeDays = activeDays;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getWorldLevel() {
        return worldLevel;
    }

    public void setWorldLevel(Integer worldLevel) {
        this.worldLevel = worldLevel;
    }

    public Integer getRoleNum() {
        return roleNum;
    }

    public void setRoleNum(Integer roleNum) {
        this.roleNum = roleNum;
    }

    public Integer getSoundBox() {
        return soundBox;
    }

    public void setSoundBox(Integer soundBox) {
        this.soundBox = soundBox;
    }

    public Integer getEnergy() {
        return energy;
    }

    public void setEnergy(Integer energy) {
        this.energy = energy;
    }

    public Integer getMaxEnergy() {
        return maxEnergy;
    }

    public void setMaxEnergy(Integer maxEnergy) {
        this.maxEnergy = maxEnergy;
    }

    public Integer getStoreEnergy() {
        return storeEnergy;
    }

    public void setStoreEnergy(Integer storeEnergy) {
        this.storeEnergy = storeEnergy;
    }

    public Long getStoreEnergyRecoverTime() {
        return storeEnergyRecoverTime;
    }

    public void setStoreEnergyRecoverTime(Long storeEnergyRecoverTime) {
        this.storeEnergyRecoverTime = storeEnergyRecoverTime;
    }

    public Integer getMaxStoreEnergy() {
        return maxStoreEnergy;
    }

    public void setMaxStoreEnergy(Integer maxStoreEnergy) {
        this.maxStoreEnergy = maxStoreEnergy;
    }

    public Long getEnergyRecoverTime() {
        return energyRecoverTime;
    }

    public void setEnergyRecoverTime(Long energyRecoverTime) {
        this.energyRecoverTime = energyRecoverTime;
    }

    public Integer getLiveness() {
        return liveness;
    }

    public void setLiveness(Integer liveness) {
        this.liveness = liveness;
    }

    public Integer getLivenessMaxCount() {
        return livenessMaxCount;
    }

    public void setLivenessMaxCount(Integer livenessMaxCount) {
        this.livenessMaxCount = livenessMaxCount;
    }

    public Boolean getLivenessUnlock() {
        return livenessUnlock;
    }

    public void setLivenessUnlock(Boolean livenessUnlock) {
        this.livenessUnlock = livenessUnlock;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getWeeklyInstCount() {
        return weeklyInstCount;
    }

    public void setWeeklyInstCount(Integer weeklyInstCount) {
        this.weeklyInstCount = weeklyInstCount;
    }

    public Map<String, Integer> getBoxes() {
        return boxes;
    }

    public void setBoxes(Map<String, Integer> boxes) {
        this.boxes = boxes;
    }

    public Map<String, Integer> getBasicBoxes() {
        return basicBoxes;
    }

    public void setBasicBoxes(Map<String, Integer> basicBoxes) {
        this.basicBoxes = basicBoxes;
    }

    public Map<String, Integer> getPhantomBoxes() {
        return phantomBoxes;
    }

    public void setPhantomBoxes(Map<String, Integer> phantomBoxes) {
        this.phantomBoxes = phantomBoxes;
    }

    public Integer getBirthMon() {
        return birthMon;
    }

    public void setBirthMon(Integer birthMon) {
        this.birthMon = birthMon;
    }

    public Integer getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Integer birthDay) {
        this.birthDay = birthDay;
    }

    @Override
    public String toString() {
        return "BaseData{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", creatTime=" + creatTime +
                ", activeDays=" + activeDays +
                ", level=" + level +
                ", worldLevel=" + worldLevel +
                ", roleNum=" + roleNum +
                ", soundBox=" + soundBox +
                ", energy=" + energy +
                ", maxEnergy=" + maxEnergy +
                ", storeEnergy=" + storeEnergy +
                ", storeEnergyRecoverTime=" + storeEnergyRecoverTime +
                ", maxStoreEnergy=" + maxStoreEnergy +
                ", energyRecoverTime=" + energyRecoverTime +
                ", liveness=" + liveness +
                ", livenessMaxCount=" + livenessMaxCount +
                ", livenessUnlock=" + livenessUnlock +
                ", chapterId=" + chapterId +
                ", weeklyInstCount=" + weeklyInstCount +
                ", boxes=" + boxes +
                ", basicBoxes=" + basicBoxes +
                ", phantomBoxes=" + phantomBoxes +
                ", birthMon=" + birthMon +
                ", birthDay=" + birthDay +
                '}';
    }
}