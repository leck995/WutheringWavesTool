package com.kr.launcher.flow;

import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.model.*;
import com.kr.launcher.task.FileChunkCheckTask;
import com.kr.launcher.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Resource check flow: verifies file integrity using chunk MD5 sampling.
 * Corresponds to KRResCheckFlow.cs.
 *
 * Flow: GetFileIndexInfo (origin index) → FileChunkCheckTask.
 * Skips check if game version != server version, or if index file download
 * times out (4s). On timeout/failure, returns success (skip check) to avoid
 * blocking game launch.
 */
public class ResCheckFlow {
    private static final Logger log = LoggerFactory.getLogger(ResCheckFlow.class);

    public interface ProgressCallback {
        void onProgress(int progressPercent);
    }

    public interface ResultCallback {
        void onResult(boolean success, int errCode, String errMessage, String path);
    }

    private final ResourceConfigManager configManager;
    private final ResUpdateModule resUpdateModule;

    private ProgressCallback progressCallback;
    private ResultCallback resultCallback;

    private long lastNotifyProgressTime;
    private int lastNotifyProgress;

    public ResCheckFlow(ResourceConfigManager configManager, ResUpdateModule module) {
        this.configManager = configManager;
        this.resUpdateModule = module;
    }

    public void exec(UpdateInfo updateInfo,
            ProgressCallback progressCallback,
            ResultCallback resultCallback) {
        this.progressCallback = progressCallback;
        this.resultCallback = resultCallback;
        try {
            if (updateInfo == null) {
                onResultCallback(true, ResourceError.NO_ERROR,
                        "Skip Res Check, Because Get Update Info Null", null);
                return;
            }

            String gameDirPath = configManager.gameDirPath;
            GameServerConfig gameServerConfig = configManager.gameServerConfig;
            LauncherDownloadConfig launcherDownloadConfig = configManager.getDownloadConfig();

            if (launcherDownloadConfig == null) {
                log.error("Key File Not Exist, Need Repair");
                onResultCallback(false, ResourceError.CHUNK_CHECK_ERROR_FILE_NOT_EXIST,
                        "Key File Not Exist", ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG);
                return;
            }
            if (gameServerConfig == null) {
                log.warn("Skip Res Check, Because Get Server Config Fail");
                onResultCallback(true, ResourceError.NO_ERROR,
                        "Skip Res Check, Because Get Server Config Fail", null);
                return;
            }

            String serverVersion = gameServerConfig.defaultConfig != null
                    && gameServerConfig.defaultConfig.config != null
                            ? gameServerConfig.defaultConfig.config.version
                            : null;

            if (serverVersion == null || !serverVersion.equals(launcherDownloadConfig.version)) {
                log.warn("Skip Res Check, Because Game Version Not Equals Server Version");
                onResultCallback(true, ResourceError.NO_ERROR,
                        "Skip Res Check, Because Game Version Not Equals Server Version", null);
                return;
            }

            // Build key file check list
            List<String> keyFileCheckList = new ArrayList<>();
            if (gameServerConfig.keyFileCheckSwitch != 0 && gameServerConfig.keyFileCheckList != null) {
                keyFileCheckList.addAll(gameServerConfig.keyFileCheckList);
            }

            // Download origin index file
            String cacheIndexFilePath = PathUtils.combine(configManager.gameCacheDirPath,
                    serverVersion, ResourceConfigManager.ORIGIN_INDEX_FILE_NAME);
            String url = updateInfo.cdnList.get(0).url;
            String originIndexFile = updateInfo.originIndexFile;
            String originIndexFileMd5 = updateInfo.originIndexFileMd5;
            String primaryUrl = UrlUtils.appendPath(url, originIndexFile);
            List<String> backupUrls = new ArrayList<>();
            for (int j = 1; j < updateInfo.cdnList.size(); j++) {
                backupUrls.add(UrlUtils.appendPath(updateInfo.cdnList.get(j).url, originIndexFile));
            }

            String modifyTimeCacheFilePath = PathUtils.combine(configManager.gameCacheDirPath,
                    serverVersion, ResourceConfigManager.MODIFY_TIME_CACHE_FILE_NAME);

            // 4-second timeout: skip check if index download takes too long
            final boolean[] isTimeout = { false };
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isTimeout[0] = true;
                    onResultCallback(true, ResourceError.NO_ERROR,
                            "Skip Res Check, Because Get Origin File Index File Timeout", null);
                }
            }, 4000);

            ResourceHelper.getFileIndexInfo(cacheIndexFilePath, primaryUrl, backupUrls, originIndexFileMd5,
                    (succ, indexFile, errCode, errMessage) -> {
                        timer.cancel();
                        if (isTimeout[0])
                            return;

                        if (succ && indexFile != null && indexFile.getResource() != null) {
                            runFileChunkCheck(gameDirPath, modifyTimeCacheFilePath,
                                    keyFileCheckList, indexFile.getResource(),
                                    gameServerConfig);
                        } else {
                            log.warn("Skip Res Check, Because Get Origin File Index File Fail");
                            onResultCallback(true, ResourceError.NO_ERROR,
                                    "Skip Res Check, Because Get Origin File Index File Fail", null);
                        }
                    }, ApplyType.DEFAULT);
        } catch (Exception e) {
            onResultCallback(false, ExceptionUtils.getHResult(e), e.getMessage(), null);
        }
    }

    private void runFileChunkCheck(String gameDirPath, String modifyTimeCacheFilePath,
            List<String> keyFileCheckList, List<FileInfo> resourceList,
            GameServerConfig gameServerConfig) {
        boolean fileChunkCheckSwitch = true;
        int timeOut = 10;
        boolean fileSizeCheckSwitch = true;
        boolean fileModifyTimeCheckSwitch = true;
        String fileCheckWhiteListConfig = "";

        try {
            Map<String, String> resCheckExp = gameServerConfig.experiment.get("res_check");
            if (resCheckExp != null) {
                if (resCheckExp.containsKey("fileChunkCheckSwitch")) {
                    fileChunkCheckSwitch = Integer.parseInt(resCheckExp.get("fileChunkCheckSwitch")) == 1;
                }
                if (resCheckExp.containsKey("resValidCheckTimeOut")) {
                    timeOut = Integer.parseInt(resCheckExp.get("resValidCheckTimeOut"));
                }
                if (resCheckExp.containsKey("fileSizeCheckSwitch")) {
                    fileSizeCheckSwitch = Integer.parseInt(resCheckExp.get("fileSizeCheckSwitch")) == 1;
                }
                if (resCheckExp.containsKey("fileCheckWhiteListConfig")) {
                    fileCheckWhiteListConfig = resCheckExp.get("fileCheckWhiteListConfig");
                }
                if (resCheckExp.containsKey("fileModifyTimeCheckSwitch")) {
                    fileModifyTimeCheckSwitch = Integer.parseInt(resCheckExp.get("fileModifyTimeCheckSwitch")) == 1;
                }
            }
        } catch (Exception e) {
            log.warn("Check Res Valid Server Config Parse Fail: {}", e.getMessage());
        }

        log.info("Res Valid Check Config: FileChunkCheckSwitch: {}, TimeOut: {}", fileChunkCheckSwitch, timeOut);

        FileChunkCheckTask task = new FileChunkCheckTask(
                gameDirPath,
                modifyTimeCacheFilePath,
                keyFileCheckList,
                resourceList,
                timeOut,
                fileChunkCheckSwitch,
                fileSizeCheckSwitch,
                fileCheckWhiteListConfig,
                fileModifyTimeCheckSwitch,
                this::onProgressCallback,
                (success, code, msg, path) -> onResultCallback(success, code, msg, path));
        task.run();
    }

    private void onProgressCallback(int percent) {
        long now = System.currentTimeMillis();
        if (now - lastNotifyProgressTime > resUpdateModule.getProgressNotifyIntervalMillis()
                || percent == 100) {
            if (lastNotifyProgress != percent) {
                log.info("ResCheck Progress: {}", percent);
            }
            progressCallback.onProgress(percent);
            lastNotifyProgressTime = now;
            lastNotifyProgress = percent;
        }
    }

    private void onResultCallback(boolean success, int errCode, String errMessage, String path) {
        resultCallback.onResult(success, errCode, errMessage, path);
    }
}
