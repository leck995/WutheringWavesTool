package com.kuro.kujiequ.model.newTowerData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewTowerBuff {
    private String buffIcon;
    private int buffId;
    private String buffName;
    private String desc;

    public String getBuffIcon() {
        return buffIcon;
    }

    public void setBuffIcon(String buffIcon) {
        this.buffIcon = buffIcon;
    }

    public int getBuffId() {
        return buffId;
    }

    public void setBuffId(int buffId) {
        this.buffId = buffId;
    }

    public String getBuffName() {
        return buffName;
    }

    public void setBuffName(String buffName) {
        this.buffName = buffName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
