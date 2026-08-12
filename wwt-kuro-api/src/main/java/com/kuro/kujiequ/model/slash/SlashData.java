package com.kuro.kujiequ.model.slash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SlashData {
    private List<SlashDifficulty> difficultyList;
    private boolean isUnlock;
    private long seasonEndTime;

    public List<SlashDifficulty> getDifficultyList() {
        return difficultyList;
    }

    public void setDifficultyList(List<SlashDifficulty> difficultyList) {
        this.difficultyList = difficultyList;
    }

    public boolean isUnlock() {
        return isUnlock;
    }

    public void setUnlock(boolean unlock) {
        isUnlock = unlock;
    }

    public long getSeasonEndTime() {
        return seasonEndTime;
    }

    public void setSeasonEndTime(long seasonEndTime) {
        this.seasonEndTime = seasonEndTime;
    }
}