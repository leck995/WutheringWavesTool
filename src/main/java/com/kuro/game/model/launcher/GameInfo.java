package com.kuro.game.model.launcher;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 16:43
 */
public class GameInfo {
    private String fileName;
    private String md5;
    private String version;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}