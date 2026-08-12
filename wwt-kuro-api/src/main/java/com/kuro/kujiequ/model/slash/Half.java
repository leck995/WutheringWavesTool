package com.kuro.kujiequ.model.slash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kuro.kujiequ.model.towerData.SimpleRole;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Half {
    private String buffDescription;
    private String buffIcon;
    private String buffName;
    private int buffQuality;
    private List<SimpleRole> roleList;
    private int score;

    public String getBuffDescription() {
        return buffDescription;
    }

    public void setBuffDescription(String buffDescription) {
        this.buffDescription = buffDescription;
    }

    public String getBuffIcon() {
        return buffIcon;
    }

    public void setBuffIcon(String buffIcon) {
        this.buffIcon = buffIcon;
    }

    public String getBuffName() {
        return buffName;
    }

    public void setBuffName(String buffName) {
        this.buffName = buffName;
    }

    public int getBuffQuality() {
        return buffQuality;
    }

    public void setBuffQuality(int buffQuality) {
        this.buffQuality = buffQuality;
    }

    public List<SimpleRole> getRoleList() {
        return roleList;
    }

    public void setRoleList(List<SimpleRole> roleList) {
        this.roleList = roleList;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}