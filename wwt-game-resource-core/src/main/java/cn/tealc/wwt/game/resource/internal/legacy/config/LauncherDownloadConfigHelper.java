package cn.tealc.wwt.game.resource.internal.legacy.config;

import cn.tealc.wwt.game.resource.internal.legacy.model.LauncherDownloadConfig;
import cn.tealc.wwt.game.resource.internal.legacy.util.FileUtils;
import cn.tealc.wwt.game.resource.internal.legacy.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherDownloadConfigHelper {
    private static final Logger log = LoggerFactory.getLogger(LauncherDownloadConfigHelper.class);

    public static void save(LauncherDownloadConfig config, String configPath) {
        try {
            String content = JsonUtils.serialize(config);
            FileUtils.deleteFile(configPath);
            FileUtils.write(content, configPath);
        } catch (Exception e) {
            log.error("Failed to save download config: {}", configPath, e);
        }
    }

    public static LauncherDownloadConfig get(String configPath) {
        try {
            if (configPath == null || configPath.isEmpty()) return null;
            if (!FileUtils.exists(configPath)) return null;
            return JsonUtils.deserialize(FileUtils.read(configPath), LauncherDownloadConfig.class);
        } catch (Exception e) {
            return null;
        }
    }
}
