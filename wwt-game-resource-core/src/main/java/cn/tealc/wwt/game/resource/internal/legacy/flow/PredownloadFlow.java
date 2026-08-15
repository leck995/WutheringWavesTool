package cn.tealc.wwt.game.resource.internal.legacy.flow;

import cn.tealc.wwt.game.resource.internal.legacy.config.LauncherDownloadConfigHelper;
import cn.tealc.wwt.game.resource.internal.legacy.config.ResourceConfigManager;
import cn.tealc.wwt.game.resource.internal.legacy.download.*;
import cn.tealc.wwt.game.resource.internal.legacy.model.*;
import cn.tealc.wwt.game.resource.internal.legacy.task.PrepareTask;
import cn.tealc.wwt.game.resource.internal.legacy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Pre-download flow: prepares and downloads resources without applying them.
 * Corresponds to KRPredownloadFlow.cs.
 *
 * Flow: PrepareTask → ResourcesDownloadTask → save config as download_complete.
 * The downloaded files are left in the cache directory for later apply by
 * UpdateFlow.
 */
public class PredownloadFlow {
    private static final Logger log = LoggerFactory.getLogger(PredownloadFlow.class);

    public interface ProgressCallback {
        void onProgress(int state, UpdateProgressInfo progressInfo);
    }

    public interface CompleteCallback {
        void onComplete(UpdateResult updateResult);
    }

    public interface Callback {
        void invoke();
    }

    private final ResourceConfigManager configManager;
    private final ResUpdateModule resUpdateModule;

    private ProgressCallback predownloadProgressCallback;
    private CompleteCallback predownloadCompleteCallback;

    private UpdateInfo updateInfo;
    private PrepareTask prepareTask;
    private ResourcesDownloadTask resourcesDownloadTask;
    private boolean isResourcesDownloadComplete;

    private long lastNotifyProgressTime;
    private int lastNotifyState = -1;
    private int lastNotifyProgress;

    private long completedSizeBeforeDownload;
    private long totalSize;
    private List<FileInfo> updateFileInfoList;
    private String downloadBaseDestPath = "";

    public boolean predownloadRunning = false;

    public PredownloadFlow(ResourceConfigManager configManager, ResUpdateModule module) {
        this.configManager = configManager;
        this.resUpdateModule = module;
    }

    private void onPredownloadProgressChanged(int state, UpdateProgressInfo progressInfo) {
        long now = System.currentTimeMillis();
        if (now - lastNotifyProgressTime > resUpdateModule.getProgressNotifyIntervalMillis()
                || state != lastNotifyState
                || progressInfo.progressPercentage == 100) {
            if (progressInfo.progressPercentage != lastNotifyProgress) {
                log.info("PreDownload Progress: {}/{}, Percent: {}",
                        progressInfo.completedSize, progressInfo.totalSize, progressInfo.progressPercentage);
            }
            predownloadProgressCallback.onProgress(state, progressInfo);
            lastNotifyProgressTime = now;
            lastNotifyState = state;
        }
    }

    private void onPredownloadCompleteChanged(UpdateResult updateResult) {
        predownloadRunning = false;
        predownloadCompleteCallback.onComplete(updateResult);
    }

    public void exec(UpdateInfo updateInfo,
            ProgressCallback onPredownloadProgressChanged,
            CompleteCallback onPredownloadCompleted) {
        this.predownloadProgressCallback = onPredownloadProgressChanged;
        this.predownloadCompleteCallback = onPredownloadCompleted;
        try {
            predownloadRunning = true;
            if (updateInfo == null) {
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.errorCode = ResourceError.UPDATE_FLOW_RES_STATE_EMPTY;
                result.errorMessage = "KRUpdateFlow.Exec, resStateInfo is empty";
                result.state = 0;
                onPredownloadCompleteChanged(result);
                return;
            }
            if (!updateInfo.hasNewUpdate) {
                log.warn("has not update, but call update api, skip update callback complete event");
                UpdateResult result = new UpdateResult();
                result.success = true;
                onPredownloadCompleteChanged(result);
                return;
            }
            this.updateInfo = updateInfo;

            LauncherDownloadConfig downloadingConfig = configManager.getDownloadingConfig();
            if (downloadingConfig != null && updateInfo.version.equals(downloadingConfig.version)) {
                log.info("last download version equals new version, skip update downloading launcherDownloadConfig");
            } else {
                if (downloadingConfig == null) {
                    downloadingConfig = new LauncherDownloadConfig();
                }
                downloadingConfig.reUseVersion = downloadingConfig.version;
                downloadingConfig.version = updateInfo.version;
                downloadingConfig.isPreDownload = true;
                configManager.saveDownloadingConfig(downloadingConfig);
            }

            if (prepareTask == null) {
                prepareTask = new PrepareTask(configManager, updateInfo, true);
            }
            PrepareTask.PrepareResult prepareResult = prepareTask.run();
            onPrepareCompleted(prepareResult);
        } catch (Exception e) {
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = ExceptionUtils.getHResult(e);
            result.errorMessage = e.getMessage();
            result.state = 0;
            onPredownloadCompleteChanged(result);
        }
    }

    private void onPrepareCompleted(PrepareTask.PrepareResult prepareResult) {
        if (!prepareResult.success) {
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = prepareResult.errorCode;
            result.errorMessage = prepareResult.errorMessage;
            result.setErrorType(UpdateResult.ERROR_TYPE_GET_INDEX_FILE_ERROR);
            result.state = 0;
            onPredownloadCompleteChanged(result);
            return;
        }

        if (resourcesDownloadTask == null) {
            if (updateInfo == null || updateInfo.size == 0L) {
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.errorCode = ResourceError.UPDATE_FLOW_UPDATE_INFO_INVALID;
                result.errorMessage = "KRPredwonloadFlow.Exec, updateInfo is empty";
                result.state = 0;
                onPredownloadCompleteChanged(result);
                return;
            }

            List<DownloadInfo> downloadInfoList = prepareResult.downloadInfoList != null
                    ? prepareResult.downloadInfoList
                    : new ArrayList<>();
            completedSizeBeforeDownload = prepareResult.completedSize;
            totalSize = updateInfo.size;
            updateFileInfoList = prepareResult.updateFileInfoList;
            downloadBaseDestPath = PathUtils.combine(configManager.gameCacheDirPath, updateInfo.version);
            String baseUrl = PathUtils.combine(updateInfo.baseUrl, updateInfo.folder);

            resourcesDownloadTask = new ResourcesDownloadTask(
                    configManager,
                    updateInfo.cdnList,
                    downloadInfoList,
                    baseUrl,
                    downloadBaseDestPath,
                    this::onDownloadProgressChanged,
                    this::onDownloadStateChanged);
            // C# KRPredownloadFlow.cs#L180: SetMd5CheckProgressCallback
            resourcesDownloadTask.setMd5CheckProgressCallback(this::onDownloadMd5CheckProgressChanged);
        }
        isResourcesDownloadComplete = false;
        resourcesDownloadTask.run();
    }

    private void onDownloadProgressChanged(ResourcesDownloadTask sender,
            DownloadProgressChangedEventArgs progressInfo) {
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
        onPredownloadProgressChanged(1, info);
    }

    /**
     * MD5 check progress callback (state=9).
     * Corresponds to C# KRPredownloadFlow.OnDownloadMd5CheckProgressChanged.
     * Only reported after the main download has completed.
     */
    private void onDownloadMd5CheckProgressChanged(ResourcesDownloadTask sender,
            DownloadMd5CheckProgressChangedEventArgs progressInfo) {
        if (isResourcesDownloadComplete) {
            UpdateProgressInfo info = new UpdateProgressInfo();
            info.totalSize = progressInfo.totalBytes;
            info.completedSize = progressInfo.completedBytesSize;
            info.progressPercentage = progressInfo.totalBytes > 0
                    ? (int) ((double) progressInfo.completedBytesSize * 100.0 / (double) progressInfo.totalBytes)
                    : 0;
            onPredownloadProgressChanged(9, info);
        }
    }

    private void onDownloadStateChanged(ResourcesDownloadTask sender,
            DownloadStateChangedEventArgs stateInfo) {
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

            long unCompressSize = 0L;
            if (updateInfo != null && updateInfo.size != updateInfo.unCompressSize) {
                unCompressSize = updateInfo.maxFileSize == -1 ? updateInfo.size : updateInfo.maxFileSize;
            }
            ResourceHelper.updateResultInfoDownloadDiskSizeInfo(result, configManager, resourcesDownloadTask,
                    unCompressSize);
            onPredownloadCompleteChanged(result);
        } else if (stateInfo.state == DownloadState.COMPLETE) {
            try {
                String configPath = PathUtils.combine(downloadBaseDestPath,
                        ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG);
                LauncherDownloadConfig newConfig = new LauncherDownloadConfig();
                newConfig.version = updateInfo.version;
                newConfig.state = LauncherDownloadConfig.STATE_DOWNLOAD_COMPLETE;
                newConfig.isPreDownload = true;
                newConfig.appId = configManager.launcherConfig.appId;
                LauncherDownloadConfigHelper.save(newConfig, configPath);

                UpdateResult result = new UpdateResult();
                result.success = true;
                onPredownloadCompleteChanged(result);
            } catch (Exception e) {
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.errorCode = ExceptionUtils.getHResult(e);
                result.errorMessage = e.getMessage();
                result.state = 1;
                onPredownloadCompleteChanged(result);
            }
        }
    }

    public void pause() {
        log.info("Try Call ResourceDownloadTask Pause");
        if (resourcesDownloadTask != null)
            resourcesDownloadTask.pause();
    }

    public void stop() {
        stop(null);
    }

    public void stop(Callback callback) {
        log.info("Try Call ResourceDownloadTask Stop");
        if (resourcesDownloadTask != null) {
            resourcesDownloadTask.stop(() -> {
                if (callback != null)
                    callback.invoke();
            });
        } else if (callback != null) {
            callback.invoke();
        }
    }

    public void resume() {
        if (resourcesDownloadTask != null)
            resourcesDownloadTask.resume();
    }
}
