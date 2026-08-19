package com.kr.launcher.config;

import com.kr.launcher.model.LauncherConfig;
import com.kr.launcher.model.LauncherDownloadConfig;
import com.kr.launcher.model.GameResourceRecord;
import com.kr.launcher.model.GameServerConfig;
import com.kr.launcher.model.IndexFile;
import com.kr.launcher.model.ResConfig;
import com.kr.launcher.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ResourceConfigManager {
    private static final Logger log = LoggerFactory.getLogger(ResourceConfigManager.class);

    public static final String CDN_CONFIG = "cdnConfig.json";
    public static final String LAUNCHER_DOWNLOAD_CONFIG = "launcherDownloadConfig.json";
    public static final String LAUNCHER_APPLY_CONFIG = "launcherGroupApplyConfig.json";
    public static final String LAUNCHER_DOWNLOAD = "launcherDownload";
    public static final String INDEX_FILE_CACHE = "indexFileCache";
    public static final String LOCAL_INDEX_FILE_NAME = "LocalGameResources.json";
    public static final String INDEX_FILE_NAME = "gameResources.json";
    public static final String ORIGIN_INDEX_FILE_NAME = "OriginResource.json";
    public static final String MODIFY_TIME_CACHE_FILE_NAME = "modifyTimeCache.json";

    public LauncherConfig launcherConfig;
    public GameServerConfig gameServerConfig;
    public String gameDirPath;
    public String gameCacheDirPath;
    public String gameConfigFilePath;
    public String gameDownloadingConfigFilePath;
    public String gameExePath;

    public ResourceConfigManager(LauncherConfig config, String gameDirPath) {
        this.launcherConfig = config;
        setGameDir(gameDirPath);
    }

    public void setGameDir(String gameDir) {
        this.gameDirPath = gameDir;
        this.gameCacheDirPath = PathUtils.combine(gameDir, LAUNCHER_DOWNLOAD);
        this.gameConfigFilePath = PathUtils.combine(gameDir, LAUNCHER_DOWNLOAD_CONFIG);
        this.gameDownloadingConfigFilePath = PathUtils.combine(gameDir, LAUNCHER_DOWNLOAD, LAUNCHER_DOWNLOAD_CONFIG);
        this.gameExePath = PathUtils.combine(gameDir, launcherConfig.gameExeName);
    }

    public LauncherDownloadConfig getDownloadConfig() {
        return LauncherDownloadConfigHelper.get(gameConfigFilePath);
    }

    public LauncherDownloadConfig getDownloadingConfig() {
        return LauncherDownloadConfigHelper.get(gameDownloadingConfigFilePath);
    }

    public void saveDownloadConfig(LauncherDownloadConfig config) {
        // C# KRLauncherDownloadConfig.AppId defaults to KRLauncherConfig.Get().AppId
        // at object creation. Inject here to match C# behavior.
        config.appId = launcherConfig.appId;
        LauncherDownloadConfigHelper.save(config, gameConfigFilePath);
    }

    public void saveDownloadingConfig(LauncherDownloadConfig config) {
        config.appId = launcherConfig.appId;
        LauncherDownloadConfigHelper.save(config, gameDownloadingConfigFilePath);
    }

    public GameResourceRecord getLocalResourceRecord() {
        String path = PathUtils.combine(gameDirPath, LOCAL_INDEX_FILE_NAME);
        try {
            String json = FileUtils.read(path);
            if (json == null)
                return null;
            return JsonUtils.safeDeserialize(json, GameResourceRecord.class);
        } catch (Exception e) {
            return null;
        }
    }
}
