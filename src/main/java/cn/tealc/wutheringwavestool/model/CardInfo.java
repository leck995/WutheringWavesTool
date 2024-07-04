package cn.tealc.wutheringwavestool.model;

import java.time.LocalDateTime;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 00:02
 */
public class CardInfo {
    private String cardPoolType;
    private int resourceId;
    private int qualityLevel;
    private String resourceType;
    private String name;
    private int count;
    private String time;

    public String getCardPoolType() {
        return cardPoolType;
    }

    public void setCardPoolType(String cardPoolType) {
        this.cardPoolType = cardPoolType;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getQualityLevel() {
        return qualityLevel;
    }

    public void setQualityLevel(int qualityLevel) {
        this.qualityLevel = qualityLevel;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }


}