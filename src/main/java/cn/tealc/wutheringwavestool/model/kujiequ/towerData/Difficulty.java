package cn.tealc.wutheringwavestool.model.kujiequ.towerData;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-15 23:19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Difficulty {
    private int difficulty;
    private String difficultyName;
    private List<TowerArea> towerAreaList;

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

    public List<TowerArea> getTowerAreaList() {
        return towerAreaList;
    }

    public void setTowerAreaList(List<TowerArea> towerAreaList) {
        this.towerAreaList = towerAreaList;
    }
}