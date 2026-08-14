package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Server-side game config model.
 * Corresponds to KRGameServerConfig
 *
 * Actual server response structure:
 * {
 * "default": {
 * "cdnList": [...],
 * "config": { "version": "3.4.1", "indexFile": "...", "patchConfig": [...] },
 * "resources": "launcher/.../resource.json"
 * },
 * "keyFileCheckSwitch": 1,
 * "keyFileCheckList": [...],
 * "fingerprints": [...],
 * ...
 * }
 */
public class GameServerConfig {
    @SerializedName("default")
    public ResUpdateConfig defaultConfig;

    @SerializedName("resourcesGray")
    public GrayConfig grayConfig;

    @SerializedName("predownload")
    public ResUpdateConfig preDownloadConfig;

    @SerializedName("RHIOptionSwitch")
    public int rhiOptionSwitch;

    @SerializedName("RHIOptionList")
    public List<Object> rhiOptionList = new java.util.ArrayList<>();

    @SerializedName("commandSwitch")
    public int extendCommandSwitch;

    @SerializedName("commandList")
    public List<Object> extendCommandList = new java.util.ArrayList<>();

    @SerializedName("keyFileCheckSwitch")
    public int keyFileCheckSwitch = 1;

    @SerializedName("keyFileCheckList")
    public List<String> keyFileCheckList = new java.util.ArrayList<>();

    @SerializedName("fileChunkCheckSwitch")
    public int fileChunkCheckSwitch = 1;

    @SerializedName("resValidCheckTimeOut")
    public int resValidCheckTimeOut = 10;

    @SerializedName("fingerprints")
    public List<String> fingerprints = new java.util.ArrayList<>();

    @SerializedName("experiment")
    public java.util.Map<String, java.util.Map<String, String>> experiment = new java.util.HashMap<>();

    @SerializedName("functionCode")
    public java.util.Map<String, Object> functionCode = new java.util.HashMap<>();

    public boolean checkConfigValid() {
        List<String> errors = new java.util.ArrayList<>();
        // C# KRGameServerConfig.cs:79-86: Default is `required` so never null
        // after successful deserialization. In Java/Gson we must null-check.
        if (defaultConfig == null) {
            errors.add("defaultConfig is null");
        } else {
            if (defaultConfig.cdnList == null || defaultConfig.cdnList.isEmpty()) {
                errors.add("default.CdnList is empty");
            }
            if (defaultConfig.config == null) {
                errors.add("default.config is null");
            } else if (defaultConfig.config.version == null
                    || defaultConfig.config.version.trim().isEmpty()) {
                // C# uses string.IsNullOrWhiteSpace — also rejects whitespace-only.
                errors.add("default.config.version is empty");
            }
        }
        if (errors.isEmpty()) {
            return true;
        }
        org.slf4j.LoggerFactory.getLogger(GameServerConfig.class)
                .error("Check GameServerConfig invalid: {}", String.join(", ", errors));
        return false;
    }
}
