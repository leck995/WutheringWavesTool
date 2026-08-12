package com.kuro.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Album {
    @JsonProperty("Id")
    private Integer id;
    
    @JsonProperty("Count")
    private Integer count;
    
    @JsonProperty("TotalCount")
    private Integer totalCount;

    // 无参构造函数
    public Album() {
    }

    // Getter和Setter方法
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", count=" + count +
                ", totalCount=" + totalCount +
                '}';
    }
}