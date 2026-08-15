package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * ResConfig - resource configuration for a game version.
 * Maps to default.config in server response.
 *
 * Server uses "patchConfig" (singular), not "patchConfigs".
 */
public class ResConfig {
    @JsonProperty("version")
    public String version;

    @JsonProperty("indexFile")
    public String indexFile;

    @JsonProperty("indexFileMd5")
    public String indexFileMd5;

    @JsonProperty("baseUrl")
    public String baseUrl;

    @JsonProperty("folder")
    public String folder = "";

    @JsonProperty("size")
    public long size;

    @JsonProperty("unCompressSize")
    public long unCompressSize;

    @JsonProperty("resourcesExcludePath")
    public List<String> resourcesExcludePath;

    @JsonProperty("resourcesExcludePathNeedUpdate")
    public List<String> resourcesExcludePathNeedUpdate;

    @JsonProperty("patchConfig")
    public List<PatchConfig> patchConfigs = new java.util.ArrayList<>();

    @JsonProperty("zipConfig")
    public PatchConfig zipConfig;
}
