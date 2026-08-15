package cn.tealc.wwt.game.resource.internal.legacy.flow;

import cn.tealc.wwt.game.resource.internal.legacy.config.LauncherDownloadConfigHelper;
import cn.tealc.wwt.game.resource.internal.legacy.config.ResourceConfigManager;
import cn.tealc.wwt.game.resource.internal.legacy.model.*;
import cn.tealc.wwt.game.resource.internal.legacy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CheckUpdateFlow {
    private static final Logger log = LoggerFactory.getLogger(CheckUpdateFlow.class);

    private final ResourceConfigManager configManager;

    public CheckUpdateFlow(ResourceConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Entry point: fetch config, check gray, determine update state.
     * Corresponds to KRCheckUpdateFlow.Exec() (line 589-662)
     */
    public CheckUpdateResult exec() {
        try {
            // C# KRCheckUpdateFlow.cs:609-618: validate GameCacheDirPath is set
            // before fetching config. Returns LAUNCHER_CACHE_DIR_EMPTY (7002002)
            // so callers can distinguish "cache dir not set" from "config fetch
            // failed".
            if (configManager.gameCacheDirPath == null || configManager.gameCacheDirPath.isEmpty()) {
                return fail(ResourceError.LAUNCHER_CACHE_DIR_EMPTY,
                        "KRCheckUpdateFlow.Exec, cache dir is empty");
            }

            String configUrl = configManager.launcherConfig.configUrl;
            String backUpConfigUrl = configManager.launcherConfig.backUpConfigUrl;

            if (configUrl == null || configUrl.isEmpty()) {
                return fail(7002001, "Config URL is empty");
            }

            log.info("Fetching game server config from: {}", configUrl);
            GameServerConfig gameConfig = initGameServerConfig(configUrl, backUpConfigUrl);
            if (gameConfig == null) {
                return fail(7002001, "Failed to fetch/parse game server config");
            }

            // Try gray config
            gameConfig = tryGetGrayConfig(gameConfig);
            configManager.gameServerConfig = gameConfig;

            // Determine update state
            return checkUpdateState(gameConfig);
        } catch (Exception e) {
            log.error("CheckUpdate failed", e);
            return fail(ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    /**
     * Check local state using the cached gameServerConfig (no network fetch).
     * Corresponds to KRCheckUpdateFlow.CheckLocalState() (line 560-587).
     *
     * If the cached config is null, returns CHECK_LOCAL_STATE_FAIL.
     * Otherwise, runs the same CheckUpdateState logic as exec() but with the
     * already-cached gameServerConfig.
     */
    public CheckUpdateResult checkLocalState() {
        try {
            if (configManager == null || configManager.gameServerConfig == null) {
                return fail(ResourceError.CHECK_LOCAL_STATE_FAIL,
                        "Check Local State Fail, Parse Game Config Error, Because Game Config is Empty");
            }
            return checkUpdateState(configManager.gameServerConfig);
        } catch (Exception e) {
            log.error("CheckLocalState failed", e);
            return fail(ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    /**
     * Fetch and parse KRGameServerConfig from server.
     * Corresponds to KRCheckUpdateFlow.InitGameServerConfig() (line 91-146)
     *
     * Uses a response validator (matching C# lines 129-145) so that if the
     * primary URL returns HTTP 200 with invalid content (e.g. an HTML error
     * page), the backup URL is tried automatically.
     */
    private GameServerConfig initGameServerConfig(String configUrl, String backUpConfigUrl) {
        // C# KRCheckUpdateFlow.cs:93-97: defense-in-depth null check on
        // GameCacheDirPath. exec() already checks, but this matches the C#
        // structure where InitGameServerConfig also validates.
        if (configManager.gameCacheDirPath == null) {
            log.error("GameCacheDirPath is null in initGameServerConfig");
            return null;
        }

        // C# KRCheckUpdateFlow.cs:129-145: passes a response validator that
        // deserializes and calls CheckConfigValid(). If the primary URL
        // returns invalid content, the backup URL is tried.
        HttpUtils.KRHttpResponse response = HttpUtils.getStringWithBackUpUrl(
                configUrl, backUpConfigUrl, 10,
                10_000L, null,
                resp -> {
                    if (resp.data == null || resp.data.isEmpty()) {
                        return false;
                    }
                    try {
                        GameServerConfig cfg = JsonUtils.deserialize(resp.data, GameServerConfig.class);
                        return cfg != null && cfg.checkConfigValid();
                    } catch (Exception e) {
                        log.warn("Response validation failed: {}", e.getMessage());
                        return false;
                    }
                });

        String json = (response != null && response.errorCode == 0) ? response.data : null;
        if (json == null || json.isEmpty()) {
            log.error("Config response is empty");
            return null;
        }

        log.info("Config response length: {}", json.length());
        log.debug("Config response preview: {}", json.substring(0, Math.min(500, json.length())));

        GameServerConfig config = JsonUtils.safeDeserialize(json, GameServerConfig.class);
        if (config == null) {
            log.error("Failed to parse KRGameServerConfig");
            return null;
        }

        log.info("Parsed config: defaultConfig={}, cdnList={}, config={}",
                config.defaultConfig != null ? "present" : "null",
                config.defaultConfig != null && config.defaultConfig.cdnList != null
                        ? config.defaultConfig.cdnList.size()
                        : 0,
                config.defaultConfig != null && config.defaultConfig.config != null ? "present" : "null");

        if (!config.checkConfigValid()) {
            log.error("KRGameServerConfig validation failed");
            return null;
        }

        log.info("Fetched config successfully, version: {}", config.defaultConfig.config.version);
        return config;
    }

    /**
     * Check gray (canary) config for newer version.
     * Corresponds to KRCheckUpdateFlow.TryGetGrayConfig() (line 35-89)
     */
    private GameServerConfig tryGetGrayConfig(GameServerConfig gameConfig) {
        GrayConfig gray = gameConfig.grayConfig;
        String officialVersion = gameConfig.defaultConfig.config.version;

        // C# KRCheckUpdateFlow.cs:40: also checks kRLauncherConfig == null and
        // version == null before proceeding. Without these checks, a null
        // launcherConfig would NPE when building the gray request params.
        if (gray == null || gray.graySwitch != 1 || gray.url == null || gray.url.isEmpty()
                || configManager.launcherConfig == null || officialVersion == null) {
            return gameConfig;
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("deviceId", DeviceUtils.getKRDeviceId(configManager.gameCacheDirPath));
            params.put("gameId", configManager.launcherConfig.gameId);
            params.put("appId", configManager.launcherConfig.appId + "_" + configManager.launcherConfig.appKey);
            params.put("identify", "game");

            String grayUrl = UrlUtils.buildUrl(gray.url, params);
            log.info("Fetching gray config from: {}", grayUrl);

            String json = HttpUtils.getString(grayUrl);
            if (json == null)
                return gameConfig;

            GrayServerConfig grayConfig = JsonUtils.safeDeserialize(json, GrayServerConfig.class);
            // C# KRCheckUpdateFlow.cs:61: requires Code==0, Data!=null, Default!=null,
            // Config!=null, and non-empty Version before use.
            if (grayConfig == null || grayConfig.code != 0
                    || grayConfig.data == null || grayConfig.data.defaultConfig == null
                    || grayConfig.data.defaultConfig.config == null
                    || grayConfig.data.defaultConfig.config.version == null
                    || grayConfig.data.defaultConfig.config.version.isEmpty()) {
                log.warn("Gray config response invalid (code={}, data={}), using official config",
                        grayConfig != null ? grayConfig.code : "null",
                        grayConfig != null && grayConfig.data != null ? "present" : "null");
                return gameConfig;
            }

            String grayVersion = grayConfig.data.defaultConfig.config.version;
            if (grayVersion != null && VersionUtils.compare(grayVersion, officialVersion) > 0) {
                log.info("Gray version {} > official version {}, using gray config", grayVersion, officialVersion);
                return grayConfig.data;
            } else {
                log.info("Gray version {} <= official version {}, using official config", grayVersion, officialVersion);
                return gameConfig;
            }
        } catch (Exception e) {
            log.warn("Gray config fetch failed, using official config: {}", e.getMessage());
            return gameConfig;
        }
    }

    /**
     * Core state determination logic.
     * Corresponds to KRCheckUpdateFlow.CheckUpdateState() (line 282-558)
     */
    private CheckUpdateResult checkUpdateState(GameServerConfig gameConfig) {
        CheckUpdateResult result = new CheckUpdateResult();
        // C# line 284/292-294: succ defaults to true; set false if server version
        // is null/empty/whitespace.
        boolean succ = true;

        // Read local version
        LauncherDownloadConfig localConfig = configManager.getDownloadConfig();
        String usingVersion = (localConfig != null) ? localConfig.version : "";
        if (usingVersion == null)
            usingVersion = "";
        String serverVersion = gameConfig.defaultConfig.config.version;
        if (serverVersion == null || serverVersion.trim().isEmpty()) {
            log.error("Server version is empty, marking check-update as failed");
            succ = false;
        }

        // Read downloading version
        LauncherDownloadConfig downloadingConfig = configManager.getDownloadingConfig();
        String downloadingVersion = (downloadingConfig != null) ? downloadingConfig.version : null;

        // Read predownload version
        String preDownloadVersion = (gameConfig.preDownloadConfig != null
                && gameConfig.preDownloadConfig.config != null)
                        ? gameConfig.preDownloadConfig.config.version
                        : null;

        log.info("UsingVersion: '{}', NewVersion: '{}', DownloadingVersion: '{}', PreDownloadVersion: '{}'",
                usingVersion, serverVersion, downloadingVersion, preDownloadVersion);

        // Build ResStateInfo (corresponds to C# lines 304-315)
        long size = gameConfig.defaultConfig.config.size;
        long unCompressSize = gameConfig.defaultConfig.config.unCompressSize;
        Long preDownloadSize = (gameConfig.preDownloadConfig != null && gameConfig.preDownloadConfig.config != null)
                ? gameConfig.preDownloadConfig.config.size
                : null;
        Long preDownloadUnCompressSize = (gameConfig.preDownloadConfig != null
                && gameConfig.preDownloadConfig.config != null)
                        ? gameConfig.preDownloadConfig.config.unCompressSize
                        : null;
        double diskSpaceRatio = getDiskSpaceCalculationRatio();
        long neededSize = (size == unCompressSize)
                ? (long) (size * diskSpaceRatio)
                : (size + unCompressSize);

        ResStateInfo stateInfo = new ResStateInfo();
        stateInfo.newVersion = serverVersion;
        stateInfo.usingVersion = usingVersion;
        stateInfo.preDownloadVersion = preDownloadVersion != null ? preDownloadVersion : "";
        stateInfo.size = size;
        stateInfo.originSize = size;
        stateInfo.neededSize = neededSize;
        // C# KRCheckUpdateFlow.cs:305-315 does NOT set UnCompressSize in the
        // initial KRResStateInfo creation — it defaults to 0 and is only set
        // later in findMatchingPatch (lines 393, 423) when a matching patch is
        // found. Setting it here would report a non-zero unCompressSize even
        // when no patch applies, diverging from C# behavior.
        stateInfo.preDownloadSize = preDownloadSize != null ? preDownloadSize : 0L;
        stateInfo.preDownloadNeededSize = preDownloadUnCompressSize != null ? preDownloadUnCompressSize : 0L;

        // Build base UpdateInfo (corresponds to C# lines 316-363)
        UpdateInfo updateInfo = buildBaseUpdateInfo(gameConfig, usingVersion, serverVersion, stateInfo);

        // Determine state (corresponds to C# lines 429-490)
        if (usingVersion == null || usingVersion.isEmpty()) {
            // Fix #5: missing STATE_REPAIRING check - C# also checks localConfig.state ==
            // "repairing"
            boolean shouldRepair = FileUtils.exists(configManager.gameExePath)
                    || (localConfig != null && LauncherDownloadConfig.STATE_REPAIRING.equals(localConfig.state));
            if (shouldRepair) {
                stateInfo.state = ResStateInfo.STATE_REPAIRING;
            } else {
                updateInfo.hasNewUpdate = true;
                if (downloadingVersion == null || downloadingVersion.isEmpty()) {
                    stateInfo.state = ResStateInfo.STATE_NEED_DOWNLOAD;
                } else if (VersionUtils.compare(downloadingVersion, serverVersion) == 0) {
                    stateInfo.state = ResStateInfo.STATE_DOWNLOADING;
                } else {
                    stateInfo.state = ResStateInfo.STATE_NEED_DOWNLOAD;
                }
            }
        } else {
            int compare = VersionUtils.compare(usingVersion, serverVersion);
            if (compare == 0) {
                if (localConfig != null && LauncherDownloadConfig.STATE_REPAIRING.equals(localConfig.state)) {
                    log.warn("Not Update, But Repairing, Continue Repair");
                    stateInfo.state = ResStateInfo.STATE_REPAIRING;
                } else {
                    stateInfo.state = ResStateInfo.STATE_UP_TO_DATE;
                }
            } else if (compare < 0) {
                updateInfo.hasNewUpdate = true;
                if (VersionUtils.compare(downloadingVersion, serverVersion) == 0) {
                    if (downloadingConfig != null && downloadingConfig.isPreDownload) {
                        stateInfo.state = ResStateInfo.STATE_PRE_DOWNLOAD;
                    } else {
                        stateInfo.state = ResStateInfo.STATE_DOWNLOADING;
                    }
                } else {
                    stateInfo.state = ResStateInfo.STATE_PRE_DOWNLOAD;
                }
            } else {
                stateInfo.state = ResStateInfo.STATE_ROLLBACK;
            }
        }

        // Extra check: exe missing -> repair (C# lines 491-494)
        if ((stateInfo.state == ResStateInfo.STATE_UP_TO_DATE || stateInfo.state == ResStateInfo.STATE_ROLLBACK)
                && !FileUtils.exists(configManager.gameExePath)) {
            stateInfo.state = ResStateInfo.STATE_REPAIRING;
        }

        // Try to find matching patch config (overrides size / disk space)
        findMatchingPatch(gameConfig, usingVersion, updateInfo, stateInfo);

        // Check for predownload info (C# lines 495-525)
        // Fix #6: missing rollback flag check
        boolean flag = VersionUtils.compare(stateInfo.usingVersion, serverVersion) > 0;
        if ((stateInfo.state == ResStateInfo.STATE_UP_TO_DATE
                || (stateInfo.state == ResStateInfo.STATE_ROLLBACK && flag))
                && preDownloadVersion != null
                && VersionUtils.compare(preDownloadVersion, serverVersion) > 0) {
            UpdateInfo preDownloadInfo = buildPreDownloadInfo(gameConfig, stateInfo.usingVersion, flag);
            if (preDownloadInfo != null) {
                result.predownloadUpdateInfo = preDownloadInfo;
                stateInfo.enablePreDownload = true;
                stateInfo.preDownloadOriginSize = preDownloadInfo.originSize;
                stateInfo.preDownloadSize = preDownloadInfo.size;
                // C# line 505-506: PreDownloadNeededSize calculation
                long num4 = (preDownloadInfo.maxFileSize == -1)
                        ? (preDownloadInfo.size * 2)
                        : (preDownloadInfo.maxFileSize + preDownloadInfo.size);
                stateInfo.preDownloadNeededSize = (preDownloadInfo.size == preDownloadInfo.unCompressSize)
                        ? (long) (preDownloadInfo.size * diskSpaceRatio)
                        : num4;
                // C# lines 507-514: IsFirstPreDownload check
                String preDownloadVersionDir = PathUtils.combine(configManager.gameCacheDirPath,
                        preDownloadInfo.version);
                stateInfo.isFirstPreDownload = !FileUtils.exists(preDownloadVersionDir);
                // C# lines 515-523: PreDownloadComplete check
                String preDownloadConfigPath = PathUtils.combine(preDownloadVersionDir,
                        ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG);
                if (FileUtils.exists(preDownloadConfigPath)) {
                    LauncherDownloadConfig pdConfig = LauncherDownloadConfigHelper.get(preDownloadConfigPath);
                    if (pdConfig != null && LauncherDownloadConfig.STATE_DOWNLOAD_COMPLETE.equals(pdConfig.state)) {
                        stateInfo.preDownloadComplete = true;
                    }
                }
            }
        }

        result.succ = succ;
        result.stateInfo = stateInfo;
        result.updateInfo = updateInfo;

        log.info("CheckUpdate result: succ={}, state={}, updateType={}, hasNewUpdate={}, size={}",
                succ, stateInfo.state, updateInfo.updateType, updateInfo.hasNewUpdate, updateInfo.size);

        return result;
    }

    /**
     * Build base UpdateInfo from server config.
     * Corresponds to C# lines 316-363 (CheckUpdateState base updateInfo
     * construction)
     */
    private UpdateInfo buildBaseUpdateInfo(GameServerConfig gameConfig, String usingVersion, String serverVersion,
            ResStateInfo stateInfo) {
        UpdateInfo info = new UpdateInfo();
        info.updateType = UpdateInfo.KR_UPDATE_TYPE_ORIGIN;
        info.version = serverVersion;
        info.usingVersion = usingVersion;

        ResConfig resConfig = gameConfig.defaultConfig.config;
        info.size = resConfig.size;
        info.unCompressSize = resConfig.unCompressSize;
        info.needDeCompress = (info.size != info.unCompressSize);
        info.indexFile = resConfig.indexFile != null ? resConfig.indexFile : "";
        info.indexFileMd5 = resConfig.indexFileMd5 != null ? resConfig.indexFileMd5 : "";
        info.folder = resConfig.folder != null ? resConfig.folder : "";
        info.baseUrl = resConfig.baseUrl != null ? resConfig.baseUrl : "";
        info.originFolder = info.folder;
        info.originBaseUrl = info.baseUrl;
        info.originIndexFile = info.indexFile;
        info.originIndexFileMd5 = info.indexFileMd5;
        info.originSize = stateInfo.originSize;

        List<CdnConfig> cdns = gameConfig.defaultConfig.cdnList;
        info.cdnList = (cdns != null) ? cdns : new ArrayList<>();

        // Check for zipConfig (C# lines 338-362)
        PatchConfig zipConfig = resConfig.zipConfig;
        if (zipConfig != null) {
            info.updateType = UpdateInfo.KR_UPDATE_TYPE_ZIP;
            info.size = zipConfig.size;
            info.unCompressSize = zipConfig.unCompressSize;
            info.indexFile = zipConfig.indexFile;
            info.indexFileMd5 = zipConfig.indexFileMd5;
            info.folder = zipConfig.folder;
            info.baseUrl = zipConfig.baseUrl;
            info.needDeCompress = (info.size != info.unCompressSize);
            // Fix #4: missing maxFileSize ext handling
            extractMaxFileSize(zipConfig, info);
        }

        return info;
    }

    /**
     * Find matching patch for current version.
     * Corresponds to KRCheckUpdateFlow lines 364-428
     *
     * Fix #1: stateInfo.neededSize/unCompressSize updates
     * Fix #2: requiredDiskSpace ext handling
     * Fix #3: applyEvals ext handling (applyMethodFeature override)
     */
    private void findMatchingPatch(GameServerConfig gameConfig, String usingVersion,
            UpdateInfo updateInfo, ResStateInfo stateInfo) {
        List<PatchConfig> patchConfigs = gameConfig.defaultConfig.config.patchConfigs;
        if (usingVersion == null || usingVersion.isEmpty() || patchConfigs == null || patchConfigs.isEmpty()) {
            return;
        }

        double diskSpaceRatio = getDiskSpaceCalculationRatio();

        for (PatchConfig patch : patchConfigs) {
            if (patch.version == null || patch.version.isEmpty())
                continue;
            if (VersionUtils.compare(usingVersion, patch.version) != 0)
                continue;

            log.info("Found patch config, usingVersion={}, patch source={}", usingVersion, patch.version);
            updateInfo.updateType = UpdateInfo.KR_UPDATE_TYPE_PATCH;
            updateInfo.size = patch.size;
            updateInfo.unCompressSize = patch.unCompressSize;
            updateInfo.indexFile = patch.indexFile;
            updateInfo.indexFileMd5 = patch.indexFileMd5;
            updateInfo.folder = patch.folder;
            updateInfo.baseUrl = patch.baseUrl;
            updateInfo.needDeCompress = (patch.size != patch.unCompressSize);

            // Fix #1: stateInfo size/neededSize updates (C# lines 382-383)
            stateInfo.size = patch.size;
            stateInfo.neededSize = (patch.size == patch.unCompressSize)
                    ? (long) (patch.size * diskSpaceRatio)
                    : (patch.size + patch.unCompressSize);

            // Fix #2: requiredDiskSpace ext handling (C# lines 385-400)
            if (patch.ext != null && patch.ext.containsKey("requiredDiskSpace")) {
                try {
                    long requiredDiskSpace = toLong(patch.ext.get("requiredDiskSpace"), -1L);
                    if (requiredDiskSpace >= 0) {
                        updateInfo.unCompressSize = requiredDiskSpace;
                        stateInfo.unCompressSize = requiredDiskSpace;
                        stateInfo.neededSize = patch.size + requiredDiskSpace;
                    }
                } catch (Exception e) {
                    log.info("Not using group-only scenes (requiredDiskSpace parse failed)");
                }
            }

            // Fix #3: applyEvals ext handling (C# lines 401-426)
            // C# does NOT set MaxFileSize in CheckUpdateState (only in
            // BuildPreDownloadUpdateInfo). C# also does NOT break — iterates
            // all matching patches (last-match-wins).
            List<ApplyEval> applyEvals = extractApplyEvals(patch);
            if (!applyEvals.isEmpty()) {
                String applyMethodFeature = getApplyMethodFeature();
                for (ApplyEval eval : applyEvals) {
                    if (applyMethodFeature != null && applyMethodFeature.equals(eval.name)) {
                        updateInfo.size = eval.size;
                        updateInfo.unCompressSize = eval.requiredDiskSpace;
                        updateInfo.needDeCompress = (eval.size != eval.unCompressSize);
                        stateInfo.size = eval.size;
                        stateInfo.unCompressSize = eval.requiredDiskSpace;
                        stateInfo.neededSize = eval.size + eval.requiredDiskSpace;
                        log.info("applyEvals override: feature={}, size={}, requiredDiskSpace={}",
                                applyMethodFeature, eval.size, eval.requiredDiskSpace);
                    }
                }
            }
        }
    }

    /**
     * Build predownload update info.
     * Corresponds to KRCheckUpdateFlow.BuildPreDownloadUpdateInfo() (line 148-280)
     *
     * Fix #7: forcedPatch parameter; if forcedPatch && no matching patch found,
     * return null
     * Fix #2/#3/#4: requiredDiskSpace / applyEvals / maxFileSize ext handling
     */
    private UpdateInfo buildPreDownloadInfo(GameServerConfig gameConfig, String usingVersion, boolean forcedPatch) {
        ResUpdateConfig preConfig = gameConfig.preDownloadConfig;
        if (preConfig == null || preConfig.config == null)
            return null;

        UpdateInfo info = new UpdateInfo();
        info.updateType = UpdateInfo.KR_UPDATE_TYPE_ORIGIN;
        info.version = preConfig.config.version;
        info.usingVersion = usingVersion;
        info.baseUrl = preConfig.config.baseUrl;
        info.cdnList = (preConfig.cdnList != null && !preConfig.cdnList.isEmpty())
                ? preConfig.cdnList
                : gameConfig.defaultConfig.cdnList;
        info.size = preConfig.config.size;
        info.unCompressSize = preConfig.config.unCompressSize;
        info.indexFile = preConfig.config.indexFile;
        info.indexFileMd5 = preConfig.config.indexFileMd5;
        info.folder = preConfig.config.folder;
        info.originFolder = info.folder;
        info.originBaseUrl = info.baseUrl;
        info.originIndexFile = info.indexFile;
        info.originIndexFileMd5 = info.indexFileMd5;
        info.originSize = info.size;
        info.hasNewUpdate = true;

        // Check zipConfig (C# lines 177-201)
        PatchConfig zipConfig = preConfig.config.zipConfig;
        if (zipConfig != null) {
            info.updateType = UpdateInfo.KR_UPDATE_TYPE_ZIP;
            info.size = zipConfig.size;
            info.unCompressSize = zipConfig.unCompressSize;
            info.indexFile = zipConfig.indexFile;
            info.indexFileMd5 = zipConfig.indexFileMd5;
            info.folder = zipConfig.folder;
            info.baseUrl = zipConfig.baseUrl;
            // Fix #4: maxFileSize ext handling
            extractMaxFileSize(zipConfig, info);
        }

        // Check patchConfigs (C# lines 202-278)
        boolean foundPatch = false;
        List<PatchConfig> patches = preConfig.config.patchConfigs;
        if (patches != null && !patches.isEmpty()) {
            for (PatchConfig patch : patches) {
                if (patch.version == null || patch.version.isEmpty())
                    continue;
                if (VersionUtils.compare(usingVersion, patch.version) != 0)
                    continue;

                log.info("Find PreDownload Patch Config, UsingVersion={}, NewVersion={}", usingVersion, info.version);
                info.updateType = UpdateInfo.KR_UPDATE_TYPE_PATCH;
                info.size = patch.size;
                info.unCompressSize = patch.unCompressSize;
                info.indexFile = patch.indexFile;
                info.indexFileMd5 = patch.indexFileMd5;
                info.folder = patch.folder;
                info.baseUrl = patch.baseUrl;

                // maxFileSize ext handling (C# lines 221-234)
                if (patch.ext != null && patch.ext.containsKey("maxFileSize")) {
                    try {
                        long maxFileSize = toLong(patch.ext.get("maxFileSize"), -1L);
                        info.maxFileSize = maxFileSize;
                    } catch (Exception e) {
                        log.info("Not using group-only scenes (maxFileSize parse failed)");
                    }
                }
                // requiredDiskSpace ext handling (C# lines 235-248)
                if (patch.ext != null && patch.ext.containsKey("requiredDiskSpace")) {
                    try {
                        long requiredDiskSpace = toLong(patch.ext.get("requiredDiskSpace"), -1L);
                        if (requiredDiskSpace >= 0) {
                            info.unCompressSize = requiredDiskSpace;
                        }
                    } catch (Exception e) {
                        log.info("Not using group-only scenes (requiredDiskSpace parse failed)");
                    }
                }
                // applyEvals ext handling (C# lines 249-272)
                List<ApplyEval> applyEvals = extractApplyEvals(patch);
                String applyMethodFeature = getApplyMethodFeature();
                for (ApplyEval eval : applyEvals) {
                    if (applyMethodFeature != null && applyMethodFeature.equals(eval.name)) {
                        info.size = eval.size;
                        info.unCompressSize = eval.requiredDiskSpace;
                        info.maxFileSize = eval.maxFileSize;
                    }
                }
                foundPatch = true;
                // C# KRCheckUpdateFlow.cs:206-273: no break — iterates all matching
                // patches (last-match-wins).
            }
            // Fix #7: forcedPatch — if user has rolled back (flag) but no patch found,
            // cannot predownload
            if (forcedPatch && !foundPatch) {
                return null;
            }
        }

        return info;
    }

    /**
     * Extract maxFileSize from patchConfig.ext into UpdateInfo.
     * Corresponds to C# lines 349-362 (zipConfig maxFileSize handling)
     */
    private void extractMaxFileSize(PatchConfig zipConfig, UpdateInfo info) {
        if (zipConfig.ext != null && zipConfig.ext.containsKey("maxFileSize")) {
            try {
                long maxFileSize = toLong(zipConfig.ext.get("maxFileSize"), -1L);
                info.maxFileSize = maxFileSize;
            } catch (Exception e) {
                log.info("Try Get MaxFileSize Fail: {}", e.getMessage());
            }
        }
    }

    /**
     * Parse applyEvals from patchConfig.ext["applyEvals"].
     * Corresponds to C# lines 249-261 / 401-413
     */
    private List<ApplyEval> extractApplyEvals(PatchConfig patch) {
        if (patch.ext == null)
            return Collections.emptyList();
        Object raw = patch.ext.get("applyEvals");
        if (raw == null)
            return Collections.emptyList();
        try {
            String json = (raw instanceof String) ? (String) raw : JsonUtils.serialize(raw);
            if (json == null || json.isEmpty())
                return Collections.emptyList();
            ApplyEval[] arr = JsonUtils.safeDeserialize(json, ApplyEval[].class);
            if (arr == null)
                return Collections.emptyList();
            return Arrays.asList(arr);
        } catch (Exception e) {
            log.info("Parse applyEvals field failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private long toLong(Object value, long defaultValue) {
        if (value == null)
            return defaultValue;
        if (value instanceof Number)
            return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double getDiskSpaceCalculationRatio() {
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> downloadCfg = configManager.gameServerConfig.experiment.get("download");
                if (downloadCfg != null && downloadCfg.containsKey("diskSpaceCalculationRatio")) {
                    String text = downloadCfg.get("diskSpaceCalculationRatio");
                    log.info("diskSpaceCalculationRatio Config is {}", text);
                    double ratio = Double.parseDouble(text);
                    log.info("diskSpaceCalculationRatio is {}", ratio);
                    return ratio;
                }
            }
        } catch (Exception e) {
            log.warn("Get diskSpaceCalculationRatio failed: {}", e.getMessage());
        }
        return 1.2;
    }

    private String getApplyMethodFeature() {
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> applyConfig = configManager.gameServerConfig.experiment.get("apply");
                if (applyConfig != null && applyConfig.containsKey("applyMethodFeature")) {
                    return applyConfig.get("applyMethodFeature");
                }
            }
        } catch (Exception e) {
            log.warn("Get applyMethodFeature failed", e);
        }
        return "patch"; // default
    }

    private CheckUpdateResult fail(int code, String msg) {
        CheckUpdateResult r = new CheckUpdateResult();
        r.succ = false;
        r.errorCode = code;
        r.errorMessage = msg;
        return r;
    }
}
