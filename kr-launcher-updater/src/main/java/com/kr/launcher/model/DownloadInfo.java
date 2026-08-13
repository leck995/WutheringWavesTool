package com.kr.launcher.model;

import java.util.List;

public class DownloadInfo {
    public String url;
    public String destPath;
    public long fileSize;
    public String md5;
    public String basePath;
    public List<ChunkInfo> chunkInfoList;

    public DownloadInfo() {}

    public DownloadInfo(String url, String destPath, long fileSize, String md5) {
        this.url = url;
        this.destPath = destPath;
        this.fileSize = fileSize;
        this.md5 = md5;
    }
}
