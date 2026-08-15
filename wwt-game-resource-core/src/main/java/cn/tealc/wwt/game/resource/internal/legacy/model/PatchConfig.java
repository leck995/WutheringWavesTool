package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * PatchConfig - configuration for a single patch from one version to another.
 * Maps to entries in default.config.patchConfig array.
 *
 * Server fields: indexFileMd5, unCompressSize, ext, baseUrl, size, indexFile, version
 * Note: NO "folder" field in patchConfig entries.
 */
public class PatchConfig {
    @JsonProperty("version")
    public String version;

    @JsonProperty("indexFile")
    public String indexFile;

    @JsonProperty("indexFileMd5")
    public String indexFileMd5;

    @JsonProperty("folder")
    public String folder = "";

    @JsonProperty("size")
    public long size;

    @JsonProperty("unCompressSize")
    public long unCompressSize;

    @JsonProperty("baseUrl")
    public String baseUrl = "";

    @JsonProperty("ext")
    public java.util.Map<String, Object> ext;
}
