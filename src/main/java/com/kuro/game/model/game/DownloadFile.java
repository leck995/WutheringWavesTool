package com.kuro.game.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description: 游戏资源文件信息
 * @author: Leck
 * @create: 2025-02-10 16:12
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DownloadFile {
    private String dest;
    private String md5;
    private String sampleHash;
    private Long size;

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSampleHash() {
        return sampleHash;
    }

    public void setSampleHash(String sampleHash) {
        this.sampleHash = sampleHash;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}