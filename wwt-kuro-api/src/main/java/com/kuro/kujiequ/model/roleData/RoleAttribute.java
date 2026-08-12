package com.kuro.kujiequ.model.roleData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description: 角色数值
 * @author: Leck
 * @create: 2025-06-13 17:59
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAttribute {
    private int attributeId;
    private String attributeName;
    private String attributeValue;
    private String iconUrl;
    private int sort;

    public int getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(int attributeId) {
        this.attributeId = attributeId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }
}