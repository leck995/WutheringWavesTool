package com.kuro.game.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @description: 文件下载分块信息
 * @author: Leck
 * @create: 2025-06-20 10:36
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkInfo {
    private long start;//开始位置
    private long end;//结束位置
    private String md5;//md5

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}