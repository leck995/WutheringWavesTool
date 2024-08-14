package cn.tealc.wutheringwavestool.model.roleData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: WutheringWavesTool
 * @description: 声骸基本信息，不涉及词条
 * @author: Leck
 * @create: 2024-07-30 16:20
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhantomProp {
    private int phantomPropId;
    private String name;
    private int quality;
    private int phantomId;
    private int cost;
    private String iconUrl;
    private String skillDescription;

    public int getPhantomPropId() {
        return phantomPropId;
    }

    public void setPhantomPropId(int phantomPropId) {
        this.phantomPropId = phantomPropId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getPhantomId() {
        return phantomId;
    }

    public void setPhantomId(int phantomId) {
        this.phantomId = phantomId;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getSkillDescription() {
        return skillDescription;
    }

    public void setSkillDescription(String skillDescription) {
        this.skillDescription = skillDescription;
    }
}