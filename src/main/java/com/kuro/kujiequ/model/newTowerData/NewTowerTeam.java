package com.kuro.kujiequ.model.newTowerData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewTowerTeam {
    private int bossCount;
    private List<NewTowerBuff> buffs;
    private int passBoss;
    private List<String> roleIcons;
    private List<NewTowerRole> roleList;
    private int round;
    private int score;

    public int getBossCount() {
        return bossCount;
    }

    public void setBossCount(int bossCount) {
        this.bossCount = bossCount;
    }

    public List<NewTowerBuff> getBuffs() {
        return buffs;
    }

    public void setBuffs(List<NewTowerBuff> buffs) {
        this.buffs = buffs;
    }

    public int getPassBoss() {
        return passBoss;
    }

    public void setPassBoss(int passBoss) {
        this.passBoss = passBoss;
    }

    public List<String> getRoleIcons() {
        return roleIcons;
    }

    public void setRoleIcons(List<String> roleIcons) {
        this.roleIcons = roleIcons;
    }

    public List<NewTowerRole> getRoleList() {
        return roleList;
    }

    public void setRoleList(List<NewTowerRole> roleList) {
        this.roleList = roleList;
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
}
