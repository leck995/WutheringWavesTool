package com.kuro.kujiequ.model.calculator.list;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 15:15
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdvanceSkillList {
    private String location;
    private String iconUrl;

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}