package cn.tealc.wutheringwavestool.model.kujiequ.towerData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-15 23:52
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DifficultyTotal {
    private List<Difficulty> difficultyList;
    private boolean isUnlock;
    private long seasonEndTime;

    public List<Difficulty> getDifficultyList() {
        return difficultyList;
    }

    public void setDifficultyList(List<Difficulty> difficultyList) {
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