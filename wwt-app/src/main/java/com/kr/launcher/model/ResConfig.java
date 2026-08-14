package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * ResConfig - resource configuration for a game version.
 * Maps to default.config in server response.
 *
 * Server uses "patchConfig" (singular), not "patchConfigs".
 */
public class ResConfig {
    @SerializedName("version")
    public String version;

    @SerializedName("indexFile")
    public String indexFile;

    @SerializedName("indexFileMd5")
    public String indexFileMd5;

    @SerializedName("baseUrl")
    public String baseUrl;

    @SerializedName("folder")
    public String folder = "";

    @SerializedName("size")
    public long size;

    @SerializedName("unCompressSize")
    public long unCompressSize;

    @SerializedName("resourcesExcludePath")
    public List<String> resourcesExcludePath;

    @SerializedName("resourcesExcludePathNeedUpdate")
    public List<String> resourcesExcludePathNeedUpdate;

    @SerializedName("patchConfig")
    public List<PatchConfig> patchConfigs = new java.util.ArrayList<>();

    @SerializedName("zipConfig")
    public PatchConfig zipConfig;
}
