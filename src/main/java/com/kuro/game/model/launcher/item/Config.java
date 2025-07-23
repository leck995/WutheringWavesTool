package com.kuro.game.model.launcher.item;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-20 10:12
 */
public class Config {
    private String indexFileMd5;
    private long unCompressSize;
    private String baseUrl;
    private long size;
    private String patchType;
    private String indexFile;
    private String version;
    private List<PatchConfig> patchConfig;

    public String getIndexFileMd5() {
        return indexFileMd5;
    }

    public void setIndexFileMd5(String indexFileMd5) {
        this.indexFileMd5 = indexFileMd5;
    }

    public long getUnCompressSize() {
        return unCompressSize;
    }

    public void setUnCompressSize(long unCompressSize) {
        this.unCompressSize = unCompressSize;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPatchType() {
        return patchType;
    }

    public void setPatchType(String patchType) {
        this.patchType = patchType;
    }

    public String getIndexFile() {
        return indexFile;
    }

    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<PatchConfig> getPatchConfig() {
        return patchConfig;
    }

    public void setPatchConfig(List<PatchConfig> patchConfig) {
        this.patchConfig = patchConfig;
    }
}