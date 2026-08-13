package com.kr.launcher.config;

import com.kr.launcher.model.LauncherDownloadConfig;
import com.kr.launcher.util.FileUtils;
import com.kr.launcher.util.JsonUtils;
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
