package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;

public class LauncherDownloadConfig {
    public static final String STATE_MOVING = "moving";
    public static final String STATE_DOWNLOAD_COMPLETE = "download_completed";
    public static final String STATE_REPAIRING = "repairing";

    @SerializedName("version")
    public String version = "";

    @SerializedName("reUseVersion")
    public String reUseVersion = "";

    @SerializedName("state")
    public String state = "";

    @SerializedName("isPreDownload")
    public boolean isPreDownload;

    @SerializedName("appId")
    public String appId = "";
}
