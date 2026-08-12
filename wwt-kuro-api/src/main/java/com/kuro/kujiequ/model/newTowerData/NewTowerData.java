package com.kuro.kujiequ.model.newTowerData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewTowerData {
    private long endTime;
    private boolean isUnlock;
    private List<NewTowerModeDetail> modeDetails;
    private int reward;
    private int totalReward;

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isUnlock() {
        return isUnlock;
    }

    public void setUnlock(boolean unlock) {
        isUnlock = unlock;
    }

    public List<NewTowerModeDetail> getModeDetails() {
        return modeDetails;
    }

    public void setModeDetails(List<NewTowerModeDetail> modeDetails) {
        this.modeDetails = modeDetails;
    }

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public int getTotalReward() {
        return totalReward;
    }

    public void setTotalReward(int totalReward) {
        this.totalReward = totalReward;
    }
}
