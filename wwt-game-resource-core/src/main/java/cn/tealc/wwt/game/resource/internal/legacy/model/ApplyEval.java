package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * applyEvals entry inside patchConfig.ext.
 * Corresponds to KRApplyEval.cs
 *
 * Used to override size / disk space / maxFileSize based on the
 * server-configured applyMethodFeature ("patch" or "group").
 */
public class ApplyEval {
    @JsonProperty("name")
    public String name = "";

    @JsonProperty("size")
    public long size;

    @JsonProperty("unCompressSize")
    public long unCompressSize;

    @JsonProperty("requiredDiskSpace")
    public long requiredDiskSpace;

    @JsonProperty("maxFileSize")
    public long maxFileSize;
}
