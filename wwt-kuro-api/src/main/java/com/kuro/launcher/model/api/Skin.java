package com.kuro.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Skin {
    @JsonProperty("SkinId")
    private Long skinId;
    
    @JsonProperty("Quality")
    private Integer quality;

    // 无参构造函数
    public Skin() {
    }

    // Getter和Setter方法
    public Long getSkinId() {
        return skinId;
    }

    public void setSkinId(Long skinId) {
        this.skinId = skinId;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    @Override
    public String toString() {
        return "Skin{" +
                "skinId=" + skinId +
                ", quality=" + quality +
                '}';
    }
}