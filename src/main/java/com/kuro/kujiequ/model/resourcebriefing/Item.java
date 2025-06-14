package com.kuro.kujiequ.model.resourcebriefing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-14 10:37
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    private String type;
    private int num;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return type+"---"+num;
    }
}