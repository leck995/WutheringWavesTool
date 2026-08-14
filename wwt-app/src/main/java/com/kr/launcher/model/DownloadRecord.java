package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;

/**
 * Download time record cached in KRDownloadRecordCache.json.
 * Corresponds to KRResources/DownloadRecord.cs.
 * Used by UpdateWatch to track cumulative download time across sessions.
 */
public class DownloadRecord {
    @SerializedName("UsingVersion")
    public String usingVersion = "";

    @SerializedName("TargetVersion")
    public String targetVersion = "";

    @SerializedName("CostTime")
    public long costTime;

    @SerializedName("Type")
    public String type = "";
}
