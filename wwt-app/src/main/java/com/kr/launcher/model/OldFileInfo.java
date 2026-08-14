package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;

/**
 * Old file info entry from version history.
 * Corresponds to KRResources/OldFileInfo.cs.
 * Used by KRVersion.HistoryGameList.
 */
public class OldFileInfo {
    @SerializedName("md5")
    public String md5 = "";

    @SerializedName("dest")
    public String dest = "";

    @SerializedName("size")
    public long size;
}
