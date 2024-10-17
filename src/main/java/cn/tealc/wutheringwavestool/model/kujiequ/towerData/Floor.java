package cn.tealc.wutheringwavestool.model.kujiequ.towerData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-15 23:14
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Floor {
    private Integer floor;
    private String picUrl;
    private List<SimpleRole> roleList;
    private int star;


    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public List<SimpleRole> getRoleList() {
        return roleList;
    }

    public void setRoleList(List<SimpleRole> roleList) {
        this.roleList = roleList;
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }
}
