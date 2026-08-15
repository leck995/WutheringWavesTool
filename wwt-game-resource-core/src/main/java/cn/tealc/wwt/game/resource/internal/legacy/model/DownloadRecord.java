package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Download time record cached in KRDownloadRecordCache.json.
 * Corresponds to KRResources/DownloadRecord.cs.
 * Used by UpdateWatch to track cumulative download time across sessions.
 */
public class DownloadRecord {
    @JsonProperty("UsingVersion")
    public String usingVersion = "";

    @JsonProperty("TargetVersion")
    public String targetVersion = "";

    @JsonProperty("CostTime")
    public long costTime;

    @JsonProperty("Type")
    public String type = "";
}
