package com.kuro.kujiequ.model.calculator.list;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 15:15
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonSkill {
    private String type;
    private String iconUrl;

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}