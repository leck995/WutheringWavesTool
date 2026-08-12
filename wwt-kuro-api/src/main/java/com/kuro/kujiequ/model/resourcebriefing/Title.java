package com.kuro.kujiequ.model.resourcebriefing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-14 10:37
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Title {
    private String title;
    private int index;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return title;
    }
}