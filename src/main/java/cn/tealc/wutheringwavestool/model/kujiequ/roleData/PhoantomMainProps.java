package cn.tealc.wutheringwavestool.model.kujiequ.roleData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: WutheringWavesTool
 * @description: 声骸词条
 * @author: Leck
 * @create: 2024-08-08 20:35
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhoantomMainProps {
    private String attributeName;
    private String iconUrl;
    private String attributeValue;
    private int level;//该词条对角色的重要程度，0，1，2，3

    private double attributeMaxValue;
    private double percent; //词条数值与最大值的百分比

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public double getAttributeMaxValue() {
        return attributeMaxValue;
    }

    public void setAttributeMaxValue(double attributeMaxValue) {
        this.attributeMaxValue = attributeMaxValue;
    }
}