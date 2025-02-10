package com.kuro.game.model.launcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceChunk {
    private String lastMd5;
    private String lastResourceChunkPath;
    private String lastResources;
    private String lastVersion;
    private String md5;
    private String resourceChunkPath;

    public String getLastMd5() {
        return lastMd5;
    }

    public void setLastMd5(String lastMd5) {
        this.lastMd5 = lastMd5;
    }

    public String getLastResourceChunkPath() {
        return lastResourceChunkPath;
    }

    public void setLastResourceChunkPath(String lastResourceChunkPath) {
        this.lastResourceChunkPath = lastResourceChunkPath;
    }

    public String getLastResources() {
        return lastResources;
    }

    public void setLastResources(String lastResources) {
        this.lastResources = lastResources;
    }

    public String getLastVersion() {
        return lastVersion;
    }

    public void setLastVersion(String lastVersion) {
        this.lastVersion = lastVersion;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getResourceChunkPath() {
        return resourceChunkPath;
    }

    public void setResourceChunkPath(String resourceChunkPath) {
        this.resourceChunkPath = resourceChunkPath;
    }
}
