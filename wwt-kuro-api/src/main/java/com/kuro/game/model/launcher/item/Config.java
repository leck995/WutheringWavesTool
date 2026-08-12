package com.kuro.game.model.launcher.item;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-20 10:12
 */
public class Config {
    private String indexFile; //资源清单
    private String indexFileMd5; //资源清单MD5
    private long unCompressSize; //未压缩资源总大小
    private String baseUrl;//下载前缀
    private long size; //目前同unCompressSize
    private String patchType; //补丁类型，未发现实际作用
    private String version; //资源版本
    private List<PatchConfig> patchConfig;//补丁类型，从1.0.0到上一版本，好像也没用，更新直接校验最新的就好了

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