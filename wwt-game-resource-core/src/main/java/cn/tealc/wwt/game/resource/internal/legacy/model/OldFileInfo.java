package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Old file info entry from version history.
 * Corresponds to KRResources/OldFileInfo.cs.
 * Used by KRVersion.HistoryGameList.
 */
public class OldFileInfo {
    @JsonProperty("md5")
    public String md5 = "";

    @JsonProperty("dest")
    public String dest = "";

    @JsonProperty("size")
    public long size;
}
