package cn.tealc.wutheringwavestool.model.kujiequ.roleData.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-08 17:30
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleInfo {
    private String name;
    private long id;
    private long creatTime; // 注意：这里假设是毫秒级时间戳
    private int activeDays;
    private int level;
    private int worldLevel;
    private int roleNum;
    private int soundBox;
    private int energy;
    private int maxEnergy;
    private int liveness;
    private int livenessMaxCount;
    private boolean livenessUnlock;
    private int chapterId;
    private int bigCount;
    private int smallCount;
    private int achievementCount;
    private int achievementStar;
    private List<BoxInfo> boxList;
    private boolean showToGuest;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreatTime() {
        return creatTime;
    }

    public void setCreatTime(long creatTime) {
        this.creatTime = creatTime;
    }

    public int getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(int activeDays) {
        this.activeDays = activeDays;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getWorldLevel() {
        return worldLevel;
    }

    public void setWorldLevel(int worldLevel) {
        this.worldLevel = worldLevel;
    }

    public int getRoleNum() {
        return roleNum;
    }

    public void setRoleNum(int roleNum) {
        this.roleNum = roleNum;
    }

    public int getSoundBox() {
        return soundBox;
    }

    public void setSoundBox(int soundBox) {
        this.soundBox = soundBox;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
    }

    public int getLiveness() {
        return liveness;
    }

    public void setLiveness(int liveness) {
        this.liveness = liveness;
    }

    public int getLivenessMaxCount() {
        return livenessMaxCount;
    }

    public void setLivenessMaxCount(int livenessMaxCount) {
        this.livenessMaxCount = livenessMaxCount;
    }

    public boolean isLivenessUnlock() {
        return livenessUnlock;
    }

    public void setLivenessUnlock(boolean livenessUnlock) {
        this.livenessUnlock = livenessUnlock;
    }

    public int getChapterId() {
        return chapterId;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public int getBigCount() {
        return bigCount;
    }

    public void setBigCount(int bigCount) {
        this.bigCount = bigCount;
    }

    public int getSmallCount() {
        return smallCount;
    }

    public void setSmallCount(int smallCount) {
        this.smallCount = smallCount;
    }

    public int getAchievementCount() {
        return achievementCount;
    }

    public void setAchievementCount(int achievementCount) {
        this.achievementCount = achievementCount;
    }

    public List<BoxInfo> getBoxList() {
        return boxList;
    }

    public void setBoxList(List<BoxInfo> boxList) {
        this.boxList = boxList;
    }

    public boolean isShowToGuest() {
        return showToGuest;
    }

    public void setShowToGuest(boolean showToGuest) {
        this.showToGuest = showToGuest;
    }

    public int getAchievementStar() {
        return achievementStar;
    }

    public void setAchievementStar(int achievementStar) {
        this.achievementStar = achievementStar;
    }
}