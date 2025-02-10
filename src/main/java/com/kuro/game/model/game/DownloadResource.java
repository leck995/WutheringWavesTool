package com.kuro.game.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 16:16
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DownloadResource {
    private List<DownloadFile> resource;
    private SampleHashInfo sampleHashInfo;

    public List<DownloadFile> getResource() {
        return resource;
    }

    public void setResource(List<DownloadFile> resource) {
        this.resource = resource;
    }

    public SampleHashInfo getSampleHashInfo() {
        return sampleHashInfo;
    }

    public void setSampleHashInfo(SampleHashInfo sampleHashInfo) {
        this.sampleHashInfo = sampleHashInfo;
    }
}