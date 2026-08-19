package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * PatchConfig - configuration for a single patch from one version to another.
 * Maps to entries in default.config.patchConfig array.
 *
 * Server fields: indexFileMd5, unCompressSize, ext, baseUrl, size, indexFile, version
 * Note: NO "folder" field in patchConfig entries.
 */
public class PatchConfig {
    @SerializedName("version")
    public String version;

    @SerializedName("indexFile")
    public String indexFile;

    @SerializedName("indexFileMd5")
    public String indexFileMd5;

    @SerializedName("folder")
    public String folder = "";

    @SerializedName("size")
    public long size;

    @SerializedName("unCompressSize")
    public long unCompressSize;

    @SerializedName("baseUrl")
    public String baseUrl = "";

    @SerializedName("ext")
    public java.util.Map<String, Object> ext;
}
