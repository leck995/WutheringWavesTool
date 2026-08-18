package cn.tealc.wwt.game.resource.internal.legacy.config;

import cn.tealc.wwt.game.resource.internal.legacy.model.LauncherConfig;
import cn.tealc.wwt.game.resource.internal.legacy.model.LauncherDownloadConfig;
import cn.tealc.wwt.game.resource.internal.legacy.model.GameResourceRecord;
import cn.tealc.wwt.game.resource.internal.legacy.model.GameServerConfig;
import cn.tealc.wwt.game.resource.internal.legacy.model.IndexFile;
import cn.tealc.wwt.game.resource.internal.legacy.model.ResConfig;
import cn.tealc.wwt.game.resource.internal.legacy.util.*;
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
        this.gameConfigFilePath = PathUtils.combine(gameDir, LAUNCHER_DOWNLOAD_CONFIG);
        this.gameExePath = PathUtils.combine(gameDir, launcherConfig.gameExeName);
        setCacheDir(cacheDirPathOf());
    }

    /**
     * 覆盖下载缓存目录（默认为 {@code gameDir/launcherDownload}）。
     * 下载临时数据（含断点续传配置）将写入该目录，而非游戏目录所在盘。
     * 传空或 null 时回落默认值。
     */
    public void setCustomCacheDir(String cacheDir) {
        setCacheDir(hasText(cacheDir) ? cacheDir : cacheDirPathOf());
    }

    private String cacheDirPathOf() {
        return PathUtils.combine(gameDirPath, LAUNCHER_DOWNLOAD);
    }

    private void setCacheDir(String cacheDir) {
        this.gameCacheDirPath = cacheDir;
        this.gameDownloadingConfigFilePath = PathUtils.combine(cacheDir, LAUNCHER_DOWNLOAD_CONFIG);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
