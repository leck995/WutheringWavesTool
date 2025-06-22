package com.kuro.game.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @description: 游戏资源文件信息
 * @author: Leck
 * @create: 2025-02-10 16:12
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileInfo {
    private String dest;
    private String md5;
    private Long size;
    private List<ChunkInfo> chunkInfos;

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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public List<ChunkInfo> getChunkInfos() {
        return chunkInfos;
    }

    public void setChunkInfos(List<ChunkInfo> chunkInfos) {
        this.chunkInfos = chunkInfos;
    }
}