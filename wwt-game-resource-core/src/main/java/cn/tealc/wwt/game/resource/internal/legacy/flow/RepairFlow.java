package cn.tealc.wwt.game.resource.internal.legacy.flow;

import cn.tealc.wwt.game.resource.DownloadOptions;
import cn.tealc.wwt.game.resource.internal.legacy.config.LauncherDownloadConfigHelper;
import cn.tealc.wwt.game.resource.internal.legacy.config.ResourceConfigManager;
import cn.tealc.wwt.game.resource.internal.legacy.download.*;
import cn.tealc.wwt.game.resource.internal.legacy.model.*;
import cn.tealc.wwt.game.resource.internal.legacy.task.*;
import cn.tealc.wwt.game.resource.internal.legacy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Repair flow: verifies file integrity and re-downloads corrupted files.
 * Corresponds to KRRepairFlow.cs.
 *
 * Flow:
 * 1. GetFileIndexInfo (origin index)
 * 2. CheckFileTask — full MD5 verification of all files
 * 3. If all valid: complete (state=8)
 * 4. If some wrong: delete wrong files → ResourcesDownloadTask → MoveFileTask →
 * DirectoryCheckTask
 */
public class RepairFlow {
    private static final Logger log = LoggerFactory.getLogger(RepairFlow.class);

    public interface ProgressCallback {
        void onProgress(int state, UpdateProgressInfo progressInfo);
    }

    public interface CompleteCallback {
        void onComplete(UpdateResult updateResult);
    }

    private final ResourceConfigManager configManager;
    private final ResUpdateModule resUpdateModule;

    private ProgressCallback repairProgressCallback;
    private CompleteCallback repairCompleteCallback;

    private UpdateInfo updateInfo;
    private List<FileInfo> wrongFileInfos;
    private CheckFileTask checkFileTask;
    private ResourcesDownloadTask resourcesDownloadTask;
    private MoveFileTask moveFileTask;
    private DirectoryCheckTask checkDirectoryTask;

    private boolean isResourcesDownloadComplete;
    private volatile boolean stopped;
    private IndexFile indexFile;

    private long lastNotifyProgressTime;
    private int lastNotifyState = -1;
    private int lastNotifyProgress;

    private long totalSize;
    private long completedSizeBeforeDownload;
    private String downloadBaseDestPath = "";
    private DownloadOptions downloadOptions = DownloadOptions.DEFAULT;

    public RepairFlow(ResourceConfigManager configManager, ResUpdateModule module) {
        this.configManager = configManager;
        this.resUpdateModule = module;
    }

    public void setDownloadOptions(DownloadOptions downloadOptions) {
        this.downloadOptions = downloadOptions != null ? downloadOptions : DownloadOptions.DEFAULT;
    }

    private void onRepairProgressChanged(int state, UpdateProgressInfo progressInfo) {
        if (stopped) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastNotifyProgressTime > resUpdateModule.getProgressNotifyIntervalMillis()
                || state != lastNotifyState
                || progressInfo.progressPercentage == 100) {
            if (progressInfo.progressPercentage != lastNotifyProgress) {
                log.info("Repair Progress: {}/{}, Percent: {}",
                        progressInfo.completedSize, progressInfo.totalSize, progressInfo.progressPercentage);
            }
            repairProgressCallback.onProgress(state, progressInfo);
            lastNotifyProgressTime = now;
            lastNotifyState = state;
            lastNotifyProgress = progressInfo.progressPercentage;
        }
    }

    private void onPostDirectoryChecked(int repairState) {
        try {
            LauncherDownloadConfig config = configManager.getDownloadConfig();
            if (config == null) {
                config = new LauncherDownloadConfig();
            }
            config.version = updateInfo.version;
            config.state = "";
            configManager.saveDownloadConfig(config);
        } catch (Exception e) {
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = ExceptionUtils.getHResult(e);
            result.errorMessage = e.getMessage();
            result.state = 5;
            repairCompleteCallback.onComplete(result);
            return;
        }
        UpdateResult result = new UpdateResult();
        result.success = true;
        result.state = repairState;
        repairCompleteCallback.onComplete(result);
    }

    private void onRepairCompleteChanged(UpdateResult updateResult) {
        if (stopped) {
            return;
        }
        if (updateResult.success && (updateResult.state == 8 || updateResult.state == 6)) {
            String gameDirPath = configManager.gameDirPath;
            List<DirectoryCheckEntry> directoryCheckEntries = ResourceHelper.getDirectoryCheckEntries(configManager);
            if (directoryCheckEntries != null && !directoryCheckEntries.isEmpty()) {
                checkDirectoryTask = new DirectoryCheckTask(gameDirPath, indexFile, directoryCheckEntries,
                        (checkState, cSharpErrorCode) -> {
                            switch (checkState) {
                                case CHECK_FAILED:
                                    UpdateResult failResult = new UpdateResult();
                                    failResult.success = false;
                                    failResult.errorCode = cSharpErrorCode;
                                    failResult.state = 5;
                                    repairCompleteCallback.onComplete(failResult);
                                    return;
                                case NOT_NEED_CHECK:
                                    if (updateResult.state == 8) {
                                        onPostDirectoryChecked(8);
                                        return;
                                    }
                                    break;
                                default:
                                    break;
                            }
                            onPostDirectoryChecked(6);
                        });
                checkDirectoryTask.run();
            } else {
                onPostDirectoryChecked(updateResult.state);
            }
        } else {
            repairCompleteCallback.onComplete(updateResult);
        }
    }

    public void exec(UpdateInfo updateInfo,
            ProgressCallback onRepairProgressChanged,
            CompleteCallback onRepairCompleted) {
        stopped = false;
        this.repairProgressCallback = onRepairProgressChanged;
        this.repairCompleteCallback = onRepairCompleted;
        try {
            if (updateInfo == null) {
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.errorCode = ResourceError.UPDATE_FLOW_RES_STATE_EMPTY;
                result.errorMessage = "KRUpdateFlow.Exec, resStateInfo is empty";
                result.state = 0;
                onRepairCompleteChanged(result);
                return;
            }
            this.updateInfo = updateInfo;

            GameServerConfig gameServerConfig = configManager.gameServerConfig;
            if (gameServerConfig == null) {
                log.warn("Repair failed, Because Get Server Config Fail");
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.errorCode = ResourceError.REPAIR_FLOW_WITHOUT_GAME_SERVER_CONFIG;
                result.errorMessage = "KRRepairFlow.Exec, null KRGameServerConfig";
                result.state = 0;
                onRepairCompleteChanged(result);
                return;
            }

            String serverVersion = gameServerConfig.defaultConfig != null
                    && gameServerConfig.defaultConfig.config != null
                            ? gameServerConfig.defaultConfig.config.version
                            : null;
            if (serverVersion == null) {
                log.warn("Repair failed, Because Get Server Config version Fail");
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.errorCode = ResourceError.REPAIR_FLOW_WITH_WRONG_GAME_SERVER_CONFIG;
                result.errorMessage = "KRRepairFlow.Exec, KRGameServerConfig wrong";
                result.state = 0;
                onRepairCompleteChanged(result);
                return;
            }

            // Download origin index file
            String cacheIndexFilePath = PathUtils.combine(configManager.gameCacheDirPath,
                    serverVersion, ResourceConfigManager.ORIGIN_INDEX_FILE_NAME);
            String url = updateInfo.cdnList.get(0).url;
            String originIndexFile = updateInfo.originIndexFile;
            String originIndexFileMd5 = updateInfo.originIndexFileMd5;
            String primaryUrl = UrlUtils.appendPath(url, originIndexFile);
            List<String> backupUrls = new ArrayList<>();
            for (int i = 1; i < updateInfo.cdnList.size(); i++) {
                backupUrls.add(UrlUtils.appendPath(updateInfo.cdnList.get(i).url, originIndexFile));
            }

            ResourceHelper.getFileIndexInfo(cacheIndexFilePath, primaryUrl, backupUrls, originIndexFileMd5,
                    this::onGetFileIndexInfoCB, ApplyType.DEFAULT);
        } catch (Exception e) {
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = ExceptionUtils.getHResult(e);
            result.errorMessage = e.getMessage();
            result.state = 0;
            onRepairCompleteChanged(result);
        }
    }

    private void onGetFileIndexInfoCB(boolean succ, IndexFile indexFile, int errCode, String errMessage) {
        if (stopped) {
            return;
        }
        if (!succ || indexFile == null || indexFile.getResource() == null) {
            log.warn("Repair failed, Because Get Origin File Index File Fail");
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = errCode;
            result.errorMessage = "KRRepairFlow.Exec, Get Origin File Index File Fail";
            result.setErrorType(UpdateResult.ERROR_TYPE_GET_INDEX_FILE_ERROR);
            result.state = 0;
            onRepairCompleteChanged(result);
            return;
        }
        this.indexFile = indexFile;

        if (checkFileTask == null) {
            String gameDirPath = configManager.gameDirPath;
            String configPath = PathUtils.combine(gameDirPath, ResourceConfigManager.LOCAL_INDEX_FILE_NAME);

            GameServerConfig gameServerConfig = configManager.gameServerConfig;
            List<FileInfo> localFileList = null;
            if (gameServerConfig != null
                    && gameServerConfig.defaultConfig != null
                    && gameServerConfig.defaultConfig.config != null
                    && gameServerConfig.defaultConfig.config.resourcesExcludePath != null
                    && !gameServerConfig.defaultConfig.config.resourcesExcludePath.isEmpty()) {
                GameResourceRecord record = GameResourceRecord.get(configPath);
                localFileList = record != null ? record.resource : null;
            }

            List<String> resourcesExcludePathList = gameServerConfig != null
                    && gameServerConfig.defaultConfig != null
                    && gameServerConfig.defaultConfig.config != null
                            ? gameServerConfig.defaultConfig.config.resourcesExcludePath
                            : null;
            List<String> resourcesExcludeWhitePathList = gameServerConfig != null
                    && gameServerConfig.defaultConfig != null
                    && gameServerConfig.defaultConfig.config != null
                            ? gameServerConfig.defaultConfig.config.resourcesExcludePathNeedUpdate
                            : null;

            checkFileTask = new CheckFileTask(
                    gameDirPath,
                    indexFile.getResource(),
                    localFileList,
                    resourcesExcludePathList,
                    resourcesExcludeWhitePathList,
                    (completedSize, totalSize) -> {
                        UpdateProgressInfo info = new UpdateProgressInfo();
                        info.totalSize = totalSize;
                        info.completedSize = completedSize;
                        info.progressPercentage = totalSize != 0L
                                ? (int) ((double) completedSize * 100.0 / (double) totalSize)
                                : 0;
                        onRepairProgressChanged(0, info);
                    },
                    this::onCheckFileResultCB);
        }
        checkFileTask.run();
    }

    private void onCheckFileResultCB(boolean allFileValid, List<FileInfo> wrongFileInfos, int cSharpCode) {
        if (stopped) {
            return;
        }
        if (cSharpCode != 0) {
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = cSharpCode;
            result.errorMessage = "KRRepairFlow.Exec, repair check throw except: " + cSharpCode;
            result.state = 0;
            onRepairCompleteChanged(result);
            return;
        }

        try {
            LauncherDownloadConfig config = configManager.getDownloadConfig();
            if (config == null) {
                config = new LauncherDownloadConfig();
            }
            config.version = updateInfo.version;
            config.state = LauncherDownloadConfig.STATE_REPAIRING;
            configManager.saveDownloadConfig(config);
        } catch (Exception e) {
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = ExceptionUtils.getHResult(e);
            result.errorMessage = e.getMessage();
            result.state = 0;
            onRepairCompleteChanged(result);
            return;
        }

        if (allFileValid) {
            UpdateResult result = new UpdateResult();
            result.success = true;
            result.state = 8;
            onRepairCompleteChanged(result);
            return;
        }

        if (resourcesDownloadTask == null) {
            if (updateInfo == null || updateInfo.size == 0L) {
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.errorCode = ResourceError.REPAIR_FLOW_UPDATE_INFO_INVALID;
                result.errorMessage = "KRRepairFlow.Exec, updateInfo is empty";
                result.state = 0;
                onRepairCompleteChanged(result);
                return;
            }

            this.wrongFileInfos = wrongFileInfos;
            totalSize = 0L;
            List<DownloadInfo> downloadInfoList = new ArrayList<>();

            for (FileInfo wrongFile : wrongFileInfos) {
                try {
                    String path = PathUtils.combine(configManager.gameDirPath, wrongFile.path);
                    File file = new File(path);
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception e) {
                    log.warn("delete wrong game file throw exception", e);
                }
                totalSize += wrongFile.size;
                downloadInfoList.add(ResourceHelper.transformFileInfoToDownloadInfo(wrongFile));
            }

            downloadBaseDestPath = PathUtils.combine(configManager.gameCacheDirPath, updateInfo.version);
            String baseUrl = PathUtils.combine(updateInfo.originBaseUrl, updateInfo.originFolder);

            resourcesDownloadTask = new ResourcesDownloadTask(
                    configManager,
                    updateInfo.cdnList,
                    downloadInfoList,
                    baseUrl,
                    downloadBaseDestPath,
                    this::onDownloadProgressChanged,
                    this::onDownloadStateChanged,
                    downloadOptions);
            // C# KRRepairFlow.cs#L409: SetMd5CheckProgressCallback
            resourcesDownloadTask.setMd5CheckProgressCallback(this::onDownloadMd5CheckProgressChanged);
        }
        isResourcesDownloadComplete = false;
        resourcesDownloadTask.run();
    }

    private void onDownloadProgressChanged(ResourcesDownloadTask sender,
            DownloadProgressChangedEventArgs progressInfo) {
        if (stopped) {
            return;
        }
        long num = completedSizeBeforeDownload + progressInfo.receivedBytesSize;
        long remainingTime = UpdateProgressInfo.MAX_REMAINING_TIME;
        if (progressInfo.bytesPerSecondSpeed > 0.0) {
            remainingTime = (long) ((progressInfo.totalBytesToReceive - progressInfo.receivedBytesSize)
                    / progressInfo.bytesPerSecondSpeed);
        }
        if (num == totalSize) {
            isResourcesDownloadComplete = true;
        }
        UpdateProgressInfo info = new UpdateProgressInfo();
        info.totalSize = totalSize;
        info.completedSize = num;
        info.speed = progressInfo.bytesPerSecondSpeed;
        info.progressPercentage = (int) ((double) num * 1.0 / (double) totalSize * 100.0);
        info.setRemainingTime(remainingTime);
        onRepairProgressChanged(1, info);
    }

    /**
     * MD5 check progress callback (state=9).
     * Corresponds to C# KRRepairFlow.OnDownloadMd5CheckProgressChanged.
     * Only reported after the main download has completed.
     */
    private void onDownloadMd5CheckProgressChanged(ResourcesDownloadTask sender,
            DownloadMd5CheckProgressChangedEventArgs progressInfo) {
        if (!stopped && isResourcesDownloadComplete) {
            UpdateProgressInfo info = new UpdateProgressInfo();
            info.totalSize = progressInfo.totalBytes;
            info.completedSize = progressInfo.completedBytesSize;
            info.progressPercentage = progressInfo.totalBytes > 0
                    ? (int) ((double) progressInfo.completedBytesSize * 100.0 / (double) progressInfo.totalBytes)
                    : 0;
            onRepairProgressChanged(9, info);
        }
    }

    private void onDownloadStateChanged(ResourcesDownloadTask sender,
            DownloadStateChangedEventArgs stateInfo) {
        if (stopped) {
            return;
        }
        if (stateInfo.state == DownloadState.FAILED) {
            int errorCode = stateInfo.cSharpErrorCode != 0 ? stateInfo.cSharpErrorCode : stateInfo.errorCode;
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = errorCode;
            result.setErrorType(stateInfo.errorCode == DownloadError.NETWORK
                    ? UpdateResult.ERROR_TYPE_NETWORK
                    : UpdateResult.ERROR_TYPE_UNKNOWN);
            result.errorMessage = "Download File Fail, Msg: " + stateInfo.errorMessage;
            result.state = 1;
            ResourceHelper.updateResultInfoDownloadDiskSizeInfo(result, configManager, resourcesDownloadTask, 0L);
            onRepairCompleteChanged(result);
            return;
        }

        if (stateInfo.state != DownloadState.COMPLETE) {
            return;
        }

        if (moveFileTask == null) {
            String gameDirPath = configManager.gameDirPath;
            List<FileInfo> moveFileInfos = wrongFileInfos != null ? wrongFileInfos : new ArrayList<>();
            MoveFileRecord record = new MoveFileRecord(downloadBaseDestPath, moveFileInfos);
            List<MoveFileRecord> records = new ArrayList<>();
            records.add(record);

            moveFileTask = new MoveFileTask(gameDirPath, records);
        }

        try {
            GameProcessUtils.killProcess(configManager);
        } catch (Exception e) {
            log.error("Kill Process Exception: {}", e.getMessage());
        }

        MoveFileTask.MoveResult moveResult = moveFileTask.run((completedSize, totalSize) -> {
            UpdateProgressInfo info = new UpdateProgressInfo();
            info.completedSize = completedSize;
            info.totalSize = totalSize;
            info.progressPercentage = totalSize == 0L ? 100
                    : (int) ((double) completedSize * 100.0 / (double) totalSize);
            onRepairProgressChanged(5, info);
        });

        if (moveResult.success) {
            UpdateResult result = new UpdateResult();
            result.success = true;
            result.errorCode = 0;
            result.errorMessage = "";
            result.state = 6;
            try {
                FileUtils.deleteDirectory(downloadBaseDestPath);
            } catch (Exception e) {
                log.error("After Move File, Delete Temp Download Dir Fail: {}", e.getMessage());
            }
            onRepairCompleteChanged(result);
        } else {
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = moveResult.errorCode;
            result.errorMessage = moveResult.errorMessage;
            result.state = 5;
            onRepairCompleteChanged(result);
        }
    }

    public void pause() {
        log.info("Pause repair");
        if (resourcesDownloadTask != null)
            resourcesDownloadTask.pause();
    }

    public void stop() {
        log.info("Stop repair");
        stopped = true;
        if (checkFileTask != null)
            checkFileTask.stop();
        if (resourcesDownloadTask != null)
            resourcesDownloadTask.stop();
    }

    public void resume() {
        log.info("Resume repair");
        if (resourcesDownloadTask != null)
            resourcesDownloadTask.resume();
    }
}
