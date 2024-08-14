package cn.tealc.wutheringwavestool.model.roleData;

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
}