package cn.tealc.wwt.game.resource.internal.legacy.model;

import java.util.List;

public class UpdateInfo {
    public static final String KR_UPDATE_TYPE_ORIGIN = "Origin";
    public static final String KR_UPDATE_TYPE_PATCH = "Patch";
    public static final String KR_UPDATE_TYPE_ZIP = "Zip";

    public boolean needDeCompress;
    public String indexFile = "";
    public String indexFileMd5 = "";
    public String baseUrl = "";
    public String folder = "";
    public String originIndexFile = "";
    public String originIndexFileMd5 = "";
    public String originFolder = "";
    public long originSize;
    public String originBaseUrl = "";
    public String version = "";
    public String usingVersion = "";
    public String updateType = KR_UPDATE_TYPE_ORIGIN;
    public long size;
    public long unCompressSize;
    public long maxFileSize = -1;
    public boolean hasNewUpdate;
    public List<CdnConfig> cdnList = new java.util.ArrayList<>();
}
