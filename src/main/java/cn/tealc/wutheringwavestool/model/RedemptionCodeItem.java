package cn.tealc.wutheringwavestool.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: WutheringWavesTool
 * @description: 兑换码条目
 * @author: Leck
 * @create: 2026-06-02
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedemptionCodeItem {
    private String key;
    private String startTime;
    private String endTime;
    private String description;
    private String reward;
    private String contributors;
    private boolean valid;
    private String gameName;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }

    public String getContributors() {
        return contributors;
    }

    public void setContributors(String contributors) {
        this.contributors = contributors;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
}
