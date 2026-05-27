package com.kuro.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerData {
    @JsonProperty("MotorData")
    private MotorData motorData;
    
    @JsonProperty("MusicData")
    private MusicData musicData;
    
    @JsonProperty("Base")
    private BaseData baseData;
    
    @JsonProperty("BattlePass")
    private BattlePassData battlePassData;

    // 无参构造函数
    public PlayerData() {
    }

    // Getter和Setter方法
    public MotorData getMotorData() {
        return motorData;
    }

    public void setMotorData(MotorData motorData) {
        this.motorData = motorData;
    }

    public MusicData getMusicData() {
        return musicData;
    }

    public void setMusicData(MusicData musicData) {
        this.musicData = musicData;
    }

    public BaseData getBaseData() {
        return baseData;
    }

    public void setBaseData(BaseData baseData) {
        this.baseData = baseData;
    }

    public BattlePassData getBattlePassData() {
        return battlePassData;
    }

    public void setBattlePassData(BattlePassData battlePassData) {
        this.battlePassData = battlePassData;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "motorData=" + motorData +
                ", musicData=" + musicData +
                ", baseData=" + baseData +
                ", battlePassData=" + battlePassData +
                '}';
    }
}