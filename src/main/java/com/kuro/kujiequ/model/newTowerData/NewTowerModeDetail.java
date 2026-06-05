package com.kuro.kujiequ.model.newTowerData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewTowerModeDetail {
    private int bossCount;
    private boolean hasRecord;
    private boolean isUnlock;
    private int modeId;
    private int passBoss;
    private int rank;
    private int round;
    private int score;
    private List<NewTowerTeam> teams;

    public int getBossCount() {
        return bossCount;
    }

    public void setBossCount(int bossCount) {
        this.bossCount = bossCount;
    }

    public boolean isHasRecord() {
        return hasRecord;
    }

    public void setHasRecord(boolean hasRecord) {
        this.hasRecord = hasRecord;
    }

    public boolean isUnlock() {
        return isUnlock;
    }

    public void setUnlock(boolean unlock) {
        isUnlock = unlock;
    }

    public int getModeId() {
        return modeId;
    }

    public void setModeId(int modeId) {
        this.modeId = modeId;
    }

    public int getPassBoss() {
        return passBoss;
    }

    public void setPassBoss(int passBoss) {
        this.passBoss = passBoss;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<NewTowerTeam> getTeams() {
        return teams;
    }

    public void setTeams(List<NewTowerTeam> teams) {
        this.teams = teams;
    }
}
