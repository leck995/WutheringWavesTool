package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import cn.tealc.wwt.game.resource.internal.legacy.util.FileUtils;
import cn.tealc.wwt.game.resource.internal.legacy.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persisted state for multi-group patch apply (resume support).
 * Corresponds to KRLauncherApplyConfig.cs.
 *
 * Stored at {downloadResPath}/krpdiff_temp/launcherGroupApplyConfig.json.
 * Tracks which group index was last patched so interrupted group applies can
 * resume.
 */
public class LauncherApplyConfig {
    private static final Logger log = LoggerFactory.getLogger(LauncherApplyConfig.class);

    public static final String STATE_PATCHING = "patching";
    public static final String STATE_COMPLETED = "completed";
    public static final String STATE_INITIALIZED = "initialized";

    @JsonProperty("version")
    public String version = "";

    @JsonProperty("state")
    public String state = STATE_INITIALIZED;

    @JsonProperty("patchedIndex")
    public int patchedIndex = -1;

    @JsonProperty("appId")
    public String appId = "";

    public static void save(LauncherApplyConfig config, String configPath) {
        try {
            String json = JsonUtils.serialize(config);
            FileUtils.deleteFile(configPath);
            FileUtils.write(json, configPath);
        } catch (Exception e) {
            log.warn("Failed to save LauncherApplyConfig: {}", e.getMessage());
        }
    }

    public static LauncherApplyConfig get(String configPath) {
        if (configPath == null || configPath.isEmpty())
            return null;
        if (!FileUtils.exists(configPath))
            return null;
        try {
            String json = FileUtils.read(configPath);
            return JsonUtils.safeDeserialize(json, LauncherApplyConfig.class);
        } catch (Exception e) {
            return null;
        }
    }
}
