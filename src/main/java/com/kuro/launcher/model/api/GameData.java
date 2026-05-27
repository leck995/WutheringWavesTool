package com.kuro.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameData {
    @JsonProperty("China")
    private PlayerData china;

    // 无参构造函数
    public GameData() {
    }

    // Getter和Setter方法
    public PlayerData getChina() {
        return china;
    }

    public void setChina(PlayerData china) {
        this.china = china;
    }

    @Override
    public String toString() {
        return "GameData{" +
                "china=" + china +
                '}';
    }
}