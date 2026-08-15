package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LauncherDownloadConfig {
    public static final String STATE_MOVING = "moving";
    public static final String STATE_DOWNLOAD_COMPLETE = "download_completed";
    public static final String STATE_REPAIRING = "repairing";

    @JsonProperty("version")
    public String version = "";

    @JsonProperty("reUseVersion")
    public String reUseVersion = "";

    @JsonProperty("state")
    public String state = "";

    @JsonProperty("isPreDownload")
    public boolean isPreDownload;

    @JsonProperty("appId")
    public String appId = "";
}
