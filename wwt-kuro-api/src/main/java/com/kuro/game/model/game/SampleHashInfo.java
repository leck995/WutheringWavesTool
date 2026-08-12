package com.kuro.game.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description: 简单记录下载资源的数量与大小
 * @author: Leck
 * @create: 2025-02-10 16:14
 */
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleHashInfo {
    private Integer sampleNum;
    private Integer sampleBlockMaxSize;

    public Integer getSampleNum() {
        return sampleNum;
    }

    public void setSampleNum(Integer sampleNum) {
        this.sampleNum = sampleNum;
    }

    public Integer getSampleBlockMaxSize() {
        return sampleBlockMaxSize;
    }

    public void setSampleBlockMaxSize(Integer sampleBlockMaxSize) {
        this.sampleBlockMaxSize = sampleBlockMaxSize;
    }
}