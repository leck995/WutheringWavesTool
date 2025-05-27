package com.kuro.kujiequ.model.slash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SlashDifficulty {
    private int allScore;
    private List<Challenge> challengeList;
    private String detailPageBG;
    private int difficulty;
    private String difficultyName;
    private String homePageBG;
    private int maxScore;
    private String teamIcon;

    public int getAllScore() {
        return allScore;
    }

    public void setAllScore(int allScore) {
        this.allScore = allScore;
    }

    public List<Challenge> getChallengeList() {
        return challengeList;
    }

    public void setChallengeList(List<Challenge> challengeList) {
        this.challengeList = challengeList;
    }

    public String getDetailPageBG() {
        return detailPageBG;
    }

    public void setDetailPageBG(String detailPageBG) {
        this.detailPageBG = detailPageBG;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public String getDifficultyName() {
        return difficultyName;
    }

    public void setDifficultyName(String difficultyName) {
        this.difficultyName = difficultyName;
    }

    public String getHomePageBG() {
        return homePageBG;
    }

    public void setHomePageBG(String homePageBG) {
        this.homePageBG = homePageBG;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public String getTeamIcon() {
        return teamIcon;
    }

    public void setTeamIcon(String teamIcon) {
        this.teamIcon = teamIcon;
    }
}