package com.kuro.game.model.game;

/**
 * @description:
 * @author: Leck
 * @create: 2025-08-28 19:08
 */
public class DownloadTaskInfo{
    private String downloadUrl;
    private String gameDir;
    private FileInfo fileInfo;
    private String aimPath;
    private long size;
    private String md5;
    private Status status = Status.READY;
    private double progressPercentage = 0;
    private long downloadedBytes = 0;
    private long speedDownload = 0;
    public DownloadTaskInfo(String downloadUrl, String gameDir, FileInfo fileInfo) {
        this.downloadUrl = downloadUrl;
        this.gameDir = gameDir;
        this.fileInfo = fileInfo;
        this.size = fileInfo.getSize();
        this.aimPath = gameDir +"/"+ fileInfo.getDest();
        this.md5 = fileInfo.getMd5();


    }

    public enum Status{
        READY,FINISHED,FAILED,IN_PROGRESS
    }

    public void addSpeedDownload(long speedDownload) {
        this.speedDownload += downloadedBytes;
    }
    public long getAndClearSpeedDownload() {
        long size = this.speedDownload;
        this.speedDownload = 0;
        return size;
    }


    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getGameDir() {
        return gameDir;
    }

    public void setGameDir(String gameDir) {
        this.gameDir = gameDir;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public String getAimPath() {
        return aimPath;
    }

    public void setAimPath(String aimPath) {
        this.aimPath = aimPath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }
}