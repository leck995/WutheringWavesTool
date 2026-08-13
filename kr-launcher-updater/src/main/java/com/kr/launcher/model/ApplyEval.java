package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;

/**
 * applyEvals entry inside patchConfig.ext.
 * Corresponds to KRApplyEval.cs
 *
 * Used to override size / disk space / maxFileSize based on the
 * server-configured applyMethodFeature ("patch" or "group").
 */
public class ApplyEval {
    @SerializedName("name")
    public String name = "";

    @SerializedName("size")
    public long size;

    @SerializedName("unCompressSize")
    public long unCompressSize;

    @SerializedName("requiredDiskSpace")
    public long requiredDiskSpace;

    @SerializedName("maxFileSize")
    public long maxFileSize;
}
