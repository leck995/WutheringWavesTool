package cn.tealc.wwt.game.resource.internal.legacy.flow;

import cn.tealc.wwt.game.resource.internal.legacy.config.LauncherDownloadConfigHelper;
import cn.tealc.wwt.game.resource.internal.legacy.config.ResourceConfigManager;
import cn.tealc.wwt.game.resource.internal.legacy.download.*;
import cn.tealc.wwt.game.resource.internal.legacy.model.*;
import cn.tealc.wwt.game.resource.internal.legacy.task.*;
import cn.tealc.wwt.game.resource.internal.legacy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Main update flow orchestrator.
 * Corresponds to KRUpdateFlow (825 lines).
 */
public class UpdateFlow {
    private static final Logger log = LoggerFactory.getLogger(UpdateFlow.class);

    private final ResourceConfigManager configManager;
    private UpdateInfo updateInfo;
    private ResStateInfo stateInfo;
    private IndexFile indexFile;
    private List<FileInfo> updateFileInfoList;
    private List<MoveFileRecord> moveRecords;
    private ProgressCallback progressCallback;
    private CompleteCallback completeCallback;
    private boolean skipMoveFile = false;

    // ResourcesDownloadTask instances for download and re-download paths.
    // Corresponds to C# KRUpdateFlow._resourcesDownloadTask and
    // _resourcesReDownloadTask.
    private ResourcesDownloadTask resourcesDownloadTask;
    private ResourcesDownloadTask resourcesReDownloadTask;
    // Tracks whether the main download has completed (for MD5 check progress
    // gating).
    // Corresponds to C# KRUpdateFlow._isResourcesDownloadComplete.
    private boolean isResourcesDownloadComplete;
    private boolean isResourcesReDownloadComplete;
    // Cached values used by callbacks (set before invoking download task).
    private long completedSizeBeforeDownload;
    private long totalDownloadSize;
    private String downloadBaseDestPath;
    private List<MoveFileRecord> redownloadMoveFileRecords;

    public UpdateFlow(ResourceConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setProgressCallback(ProgressCallback cb) {
        this.progressCallback = cb;
    }

    public void setCompleteCallback(CompleteCallback cb) {
        this.completeCallback = cb;
    }

    public void setSkipMoveFile(boolean skip) {
        this.skipMoveFile = skip;
    }

    /**
     * Execute the full update pipeline.
     * Corresponds to KRUpdateFlow.Exec() (line 98-161) + OnPrepareCompleted (line
     * 167-216).
     */
    public void exec(ResStateInfo stateInfo, UpdateInfo updateInfo) {
        this.stateInfo = stateInfo;
        this.updateInfo = updateInfo;

        try {
            if (stateInfo == null || updateInfo == null) {
                notifyComplete(false, ResourceError.UPDATE_FLOW_RES_STATE_EMPTY,
                        "KRUpdateFlow.Exec, resStateInfo is empty", 0);
                return;
            }

            if (!updateInfo.hasNewUpdate) {
                log.info("No update available, skipping");
                notifyComplete(true, 0, "", 0);
                return;
            }

            // Save downloading config (C# lines 125-144).
            saveDownloadingConfig(updateInfo.version);

            // Step 1: Prepare - download index file and build download list.
            log.info("=== Step 1: Prepare ===");
            PrepareTask prepareTask = new PrepareTask(configManager, updateInfo);
            PrepareTask.PrepareResult prepareResult = prepareTask.run();

            if (!prepareResult.success) {
                // C# KRUpdateFlow.cs:171-178: prepare failure sets
                // ErrorType = ERROR_TYPE_GET_INDEX_FILE_ERROR.
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.errorCode = prepareResult.errorCode;
                result.errorMessage = prepareResult.errorMessage;
                result.state = 0;
                result.setErrorType(UpdateResult.ERROR_TYPE_GET_INDEX_FILE_ERROR);
                notifyComplete(result);
                return;
            }

            this.indexFile = prepareResult.indexFile;
            this.updateFileInfoList = prepareResult.updateFileInfoList;

            // C# lines 184-194: validate updateInfo.Size is non-zero before proceeding.
            // Prepare may have succeeded with an empty download list (all files already
            // cached),
            // but the updateInfo itself must describe a non-zero total size.
            if (updateInfo.size == 0L) {
                log.error("KRUpdateFlow.Exec, updateInfo is empty (size == 0)");
                notifyComplete(false, 7002005, "KRUpdateFlow.Exec, updateInfo is empty", 0);
                return;
            }

            String downloadBaseDestPath = PathUtils.combine(configManager.gameCacheDirPath, updateInfo.version);
            // C# KRUpdateFlow.cs:200 sets _downloadBaseDestPath BEFORE creating the
            // ResourcesDownloadTask, so it is always populated even when the download
            // list is empty (apply/move path). saveCompletedConfig relies on this field
            // to delete the moving config.
            this.downloadBaseDestPath = downloadBaseDestPath;

            if (prepareResult.downloadInfoList == null || prepareResult.downloadInfoList.isEmpty()) {
                log.info("All files already up to date, skipping download");
                // Still need to apply if there are patch/group/zip infos.
                applyOrMove(downloadBaseDestPath, prepareResult.completedSize);
                return;
            }

            // Step 2: Download.
            log.info("=== Step 2: Download ({} files, {} bytes to download) ===",
                    prepareResult.downloadInfoList.size(),
                    prepareResult.downloadInfoList.stream().mapToLong(d -> d.fileSize).sum());

            // C# uses PathUtils.Combine(BaseUrl, Folder) — matches C# path joining behavior
            String downloadBaseUrl = PathUtils.combine(updateInfo.baseUrl, updateInfo.folder);

            // Use ResourcesDownloadTask (wraps CDNDownloadTask with retry-on-timeout,
            // proxy-exception handling, and MD5 check progress).
            // Corresponds to C# KRUpdateFlow.OnPrepareCompleted line 202.
            completedSizeBeforeDownload = prepareResult.completedSize;
            totalDownloadSize = updateInfo.size;
            isResourcesDownloadComplete = false;

            resourcesDownloadTask = new ResourcesDownloadTask(
                    configManager,
                    updateInfo.cdnList,
                    prepareResult.downloadInfoList,
                    downloadBaseUrl,
                    downloadBaseDestPath,
                    this::onDownloadProgressChanged,
                    this::onDownloadStateChanged);
            resourcesDownloadTask.setMd5CheckProgressCallback(this::onDownloadMd5CheckProgressChanged);

            log.info("Run ResourceDownloadTask");
            resourcesDownloadTask.run();

        } catch (Exception e) {
            log.error("UpdateFlow failed", e);
            notifyComplete(false, ExceptionUtils.getHResult(e), e.getMessage(), 0);
        }
    }

    // ==================== Download Callbacks ====================
    // Correspond to C# KRUpdateFlow OnDownloadProgressChanged,
    // OnDownloadStateChanged, OnDownloadMd5CheckProgressChanged.

    /**
     * Download progress callback.
     * Corresponds to C# KRUpdateFlow.OnDownloadProgressChanged (lines 244-264).
     * Reports state=1 with cumulative completed size.
     */
    private void onDownloadProgressChanged(ResourcesDownloadTask sender,
            DownloadProgressChangedEventArgs progressInfo) {
        long currentTotal = completedSizeBeforeDownload + progressInfo.receivedBytesSize;
        if (currentTotal == totalDownloadSize) {
            isResourcesDownloadComplete = true;
        }
        if (progressCallback != null) {
            progressCallback.onProgress(1, currentTotal, totalDownloadSize, 0, 0);
        }
    }

    /**
     * Download state change callback.
     * Corresponds to C# KRUpdateFlow.OnDownloadStateChanged (lines 266-366).
     * Handles FAILED (error mapping + disk size info), COMPLETE (proceed to apply),
     * and CANCELED states.
     */
    private void onDownloadStateChanged(ResourcesDownloadTask sender, DownloadStateChangedEventArgs stateInfo) {
        if (stateInfo.state == DownloadState.FAILED) {
            // C# KRUpdateFlow.cs:270: prefers CSharpErrorCode over ErrorCode.
            int downloadErrCode = stateInfo.cSharpErrorCode != 0
                    ? stateInfo.cSharpErrorCode
                    : stateInfo.errorCode;
            log.error("Download Failed, ErrorCode: {}, CSharpErrorCode: {}, ErrorMessage: {}",
                    stateInfo.errorCode, stateInfo.cSharpErrorCode, stateInfo.errorMessage);
            // C# KRUpdateFlow.cs:273-280: error type mapping uses the ORIGINAL
            // stateInfo.ErrorCode (not the resolved downloadErrCode) for comparison.
            int errorType = mapDownloadErrorToType(stateInfo.errorCode);
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = downloadErrCode;
            result.setErrorType(errorType);
            result.state = 1;
            result.errorMessage = "Download File Fail, Msg: " + stateInfo.errorMessage;
            // C# always calls UpdateResultInfoDownloadDiskSizeInfo; it no-ops if
            // errorType != DISK_NOT_ENOUGH_SPACE.
            long unCompressSize = updateInfo.needDeCompress ? updateInfo.unCompressSize : 0;
            ResourceHelper.updateResultInfoDownloadDiskSizeInfo(result, configManager, resourcesDownloadTask,
                    unCompressSize);
            notifyComplete(result);
        } else if (stateInfo.state == DownloadState.COMPLETE) {
            log.info("Download completed, proceeding to apply");
            applyOrMove(downloadBaseDestPath, completedSizeBeforeDownload);
        } else if (stateInfo.state == DownloadState.CANCELED) {
            // C# KRUpdateFlow.cs:266-366: CANCELED is silently ignored (return).
            // Do not report as failure — cancellation is user-initiated.
            log.info("Download canceled by user");
        }
    }

    /**
     * MD5 check progress callback (state=9).
     * Corresponds to C# KRUpdateFlow.OnDownloadMd5CheckProgressChanged (lines
     * 218-229).
     * Only reported after the main download has completed.
     */
    private void onDownloadMd5CheckProgressChanged(ResourcesDownloadTask sender,
            DownloadMd5CheckProgressChangedEventArgs progressInfo) {
        if (isResourcesDownloadComplete && progressCallback != null) {
            progressCallback.onProgress(9, progressInfo.completedBytesSize, progressInfo.totalBytes, 0, 0);
        }
    }

    /**
     * Re-download progress callback.
     * Corresponds to C# KRUpdateFlow.OnReDownloadProgressChanged (lines 649-669).
     */
    private void onReDownloadProgressChanged(ResourcesDownloadTask sender,
            DownloadProgressChangedEventArgs progressInfo) {
        if (progressInfo.receivedBytesSize == progressInfo.totalBytesToReceive) {
            isResourcesReDownloadComplete = true;
        }
        if (progressCallback != null) {
            progressCallback.onProgress(4, progressInfo.receivedBytesSize, progressInfo.totalBytesToReceive, 0, 0);
        }
    }

    /**
     * Re-download state change callback.
     * Corresponds to C# KRUpdateFlow.OnReDownloadStateChanged (lines 671-804).
     */
    private void onReDownloadStateChanged(ResourcesDownloadTask sender, DownloadStateChangedEventArgs stateInfo) {
        if (stateInfo.state == DownloadState.FAILED) {
            // C# KRUpdateFlow.cs:675: prefers CSharpErrorCode over ErrorCode (same as main
            // path).
            int downloadErrCode = stateInfo.cSharpErrorCode != 0
                    ? stateInfo.cSharpErrorCode
                    : stateInfo.errorCode;
            // C# KRUpdateFlow.cs:681 maps error type using stateInfo.ErrorCode (the
            // ORIGINAL code), not the resolved downloadErrCode. When cSharpErrorCode
            // is set and stateInfo.errorCode == NETWORK, C# returns ERROR_TYPE_NETWORK.
            int errorType = (stateInfo.errorCode == DownloadError.NETWORK)
                    ? UpdateResult.ERROR_TYPE_NETWORK
                    : UpdateResult.ERROR_TYPE_UNKNOWN;
            UpdateResult result = new UpdateResult();
            result.success = false;
            result.errorCode = downloadErrCode;
            result.setErrorType(errorType);
            result.state = 4;
            result.errorMessage = "Download File Fail, Msg: " + stateInfo.errorMessage;
            ResourceHelper.updateResultInfoDownloadDiskSizeInfo(result, configManager, resourcesReDownloadTask, 0);
            notifyComplete(result);
        } else if (stateInfo.state == DownloadState.COMPLETE) {
            log.info("Re-download completed, proceeding to move files");
            // Defensive: C# OnReDownloadStateChanged does NOT kill the game process
            // before move (the game was killed once before apply at line 338). However,
            // re-download can take significant time, and the user may have restarted
            // the game in the interim. Kill again here to prevent file-occupancy
            // failures during move.
            try {
                GameProcessUtils.killProcess(configManager);
            } catch (Exception e) {
                log.error("Kill Process Exception before re-download move: {}", e.getMessage());
            }
            moveFilesAndComplete(downloadBaseDestPath, redownloadMoveFileRecords);
        } else if (stateInfo.state == DownloadState.CANCELED) {
            // C# KRUpdateFlow.cs: CANCELED is silently ignored.
            log.info("Re-download canceled by user");
        }
    }

    /**
     * Re-download MD5 check progress callback (state=9).
     */
    private void onReDownloadMd5CheckProgressChanged(ResourcesDownloadTask sender,
            DownloadMd5CheckProgressChangedEventArgs progressInfo) {
        if (isResourcesReDownloadComplete && progressCallback != null) {
            progressCallback.onProgress(9, progressInfo.completedBytesSize, progressInfo.totalBytes, 0, 0);
        }
    }

    // ==================== Pause/Resume/Stop ====================
    // Correspond to C# KRUpdateFlow.Pause/Resume/Stop (lines 806-824).

    public void pause() {
        log.info("Try Call ResourceDownloadTask Pause");
        if (resourcesDownloadTask != null)
            resourcesDownloadTask.pause();
        if (resourcesReDownloadTask != null)
            resourcesReDownloadTask.pause();
    }

    public void resume() {
        if (resourcesDownloadTask != null)
            resourcesDownloadTask.resume();
        if (resourcesReDownloadTask != null)
            resourcesReDownloadTask.resume();
    }

    public void stop() {
        log.info("Try Call ResourceDownloadTask Stop");
        if (resourcesDownloadTask != null)
            resourcesDownloadTask.stop();
        if (resourcesReDownloadTask != null)
            resourcesReDownloadTask.stop();
    }

    /**
     * Apply downloaded resources or move files directly.
     * Corresponds to KRUpdateFlow.OnApplyCompleteChanged() (lines 445-647).
     */
    private void applyOrMove(String downloadBaseDestPath, long completedSizeBefore) {
        try {
            // Step 3: Apply.
            log.info("=== Step 3: Apply ===");

            // C# lines 336-343: kill game process before running the apply task so files
            // being patched are not locked.
            try {
                GameProcessUtils.killProcess(configManager);
            } catch (Exception e) {
                log.error("Kill Process Exception: {}", e.getMessage());
            }

            ApplyResult applyResult = executeApply(downloadBaseDestPath);

            if (applyResult.errorCode == UpdateResult.APPLY_CHECK_MD5_NOT_MATCH && applyResult.failFileInfos != null) {
                // MD5 mismatch → re-download failed files (C# lines 581-618).
                log.info("Apply MD5 check failed, re-downloading {} failed files...", applyResult.failFileInfos.size());
                reDownloadAndMove(downloadBaseDestPath, applyResult.moveRecords, applyResult.failFileInfos);
            } else if (applyResult.errorCode == UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE) {
                // Disk space error → report with requiredDiskSpaceSize (C# lines 620-635).
                long needDiskSize = applyResult.needDiskSize > 0 ? applyResult.needDiskSize : 0;
                UpdateResult result = new UpdateResult();
                result.success = false;
                result.state = 2;
                result.errorCode = applyResult.errorCode;
                result.errorMessage = applyResult.errorMessage;
                if (needDiskSize > 0) {
                    result.extInfos.put("remainDownloadSize", 0L);
                    result.extInfos.put("requiredDiskSpaceSize", needDiskSize);
                }
                notifyComplete(result);
            } else if (!applyResult.success) {
                // Other apply error - C# default branch sets State = 2 (line 643).
                notifyComplete(false, applyResult.errorCode, applyResult.errorMessage, 2);
            } else {
                // Apply succeeded → move files.
                moveFilesAndComplete(downloadBaseDestPath, applyResult.moveRecords);
            }

        } catch (Exception e) {
            log.error("Apply/Move failed", e);
            notifyComplete(false, ExceptionUtils.getHResult(e), e.getMessage(), 0);
        }
    }

    /**
     * Execute the apply task based on index file contents.
     * Corresponds to C# KRUpdateFlow.OnDownloadStateChanged lines 299-334.
     *
     * C# logic:
     * - if updateFileInfoList == null → NopApplyTask (null list, no files to move)
     * - else if groupInfos+feature=group → GroupApplyTask
     * - else if patchInfos → PatchApplyTask
     * - else if groupInfos → GroupApplyTask (fallback)
     * - else if zipInfos → ZipApplyTask
     * - else → NopApplyTask (with file list, builds direct move records)
     *
     * IMPORTANT: empty (but non-null) updateFileInfoList still checks
     * patch/group/zip
     * infos, because patches create new files from existing ones without needing
     * downloaded resource files.
     */
    private ApplyResult executeApply(String downloadBaseDestPath) {
        if (updateFileInfoList == null) {
            // C# line 301: _updateFileInfoList == null → NopApplyTask with null list
            log.info("No files to apply (updateFileInfoList is null), NopApply");
            return new ApplyResult(true, new ArrayList<>(), null, 0, "", 0);
        } else if (indexFile != null && indexFile.groupInfos != null && !indexFile.groupInfos.isEmpty()
                && "group".equals(getApplyMethodFeature())) {
            log.info("Applying group update (applyMethodFeature=group)...");
            return applyGroupWithResult(downloadBaseDestPath);
        } else if (indexFile != null && indexFile.patchInfos != null && !indexFile.patchInfos.isEmpty()) {
            log.info("Applying patch update (HPatchZ)...");
            return applyPatchWithResult(downloadBaseDestPath);
        } else if (indexFile != null && indexFile.groupInfos != null && !indexFile.groupInfos.isEmpty()) {
            log.info("Applying group update (fallback)...");
            return applyGroupWithResult(downloadBaseDestPath);
        } else if (indexFile != null && indexFile.zipInfos != null && !indexFile.zipInfos.isEmpty()) {
            log.info("Applying zip update...");
            return applyZipWithResult(downloadBaseDestPath);
        } else {
            log.info("No apply needed, direct file move (NopApply)");
            return new ApplyResult(true, buildDirectMoveRecords(downloadBaseDestPath), null, 0, "", 0);
        }
    }

    /**
     * Apply group-based patch update with sequential multi-group patching.
     * Corresponds to C# KRUpdateFlow creating KRGroupApplyTask.
     */
    private ApplyResult applyGroupWithResult(String downloadResPath) {
        String hpatchzPath = findHPatchZ();
        if (hpatchzPath == null) {
            log.warn("HPatchZ.exe not found, falling back to direct file copy");
            return new ApplyResult(true, buildDirectMoveRecords(downloadResPath), null, 0, "", 0);
        }

        long deCompressSize = updateInfo.needDeCompress ? updateInfo.unCompressSize : 0;
        GroupApplyTask groupTask = new GroupApplyTask(updateInfo, configManager,
                configManager.gameDirPath, downloadResPath, indexFile,
                deCompressSize, hpatchzPath);

        GroupApplyTask.GroupApplyResult result = groupTask
                .run((state, completed, total, completedCount, totalCount) -> {
                    if (progressCallback != null) {
                        progressCallback.onProgress(state, completed, total, completedCount, totalCount);
                    }
                });

        long needDiskSize = groupTask.getNeedDiskSize();
        return new ApplyResult(result.success, result.moveRecords, result.failFileInfos,
                result.errorCode, result.errorMessage, needDiskSize);
    }

    private ApplyResult applyPatchWithResult(String downloadResPath) {
        String hpatchzPath = findHPatchZ();
        if (hpatchzPath == null) {
            log.warn("HPatchZ.exe not found, falling back to direct file copy");
            return new ApplyResult(true, buildDirectMoveRecords(downloadResPath), null, 0, "", 0);
        }

        long deCompressSize = updateInfo.needDeCompress ? updateInfo.unCompressSize : 0;
        PatchApplyTask patchTask = new PatchApplyTask(
                updateInfo, configManager.gameDirPath, downloadResPath,
                indexFile, deCompressSize, hpatchzPath);

        PatchApplyTask.PatchApplyResult result = patchTask
                .run((state, completed, total, completedCount, totalCount) -> {
                    if (progressCallback != null) {
                        progressCallback.onProgress(state, completed, total, completedCount, totalCount);
                    }
                });

        long needDiskSize = patchTask.getNeedDiskSize();
        return new ApplyResult(result.success, result.moveRecords, result.failFileInfos,
                result.errorCode, result.errorMessage, needDiskSize);
    }

    private ApplyResult applyZipWithResult(String downloadResPath) {
        ZipApplyTask zipTask = new ZipApplyTask(updateInfo, configManager.gameDirPath, downloadResPath,
                indexFile, updateInfo.unCompressSize);
        ZipApplyTask.ApplyResult result = zipTask.run((state, completed, total, completedCount, totalCount) -> {
            if (progressCallback != null) {
                progressCallback.onProgress(state, completed, total, completedCount, totalCount);
            }
        });

        if (result.success) {
            return new ApplyResult(true, result.moveRecords, null, 0, "", 0);
        }
        return new ApplyResult(false, result.moveRecords, null, result.errorCode != 0 ? result.errorCode : -1,
                result.errorMessage != null ? result.errorMessage : "Zip apply failed", 0);
    }

    /**
     * Re-download failed files and complete the update.
     * Corresponds to C# OnApplyCompleteChanged lines 581-618 +
     * OnReDownloadStateChanged lines 671-729.
     *
     * Uses ResourcesDownloadTask (not CDNDownloadTask directly) to get
     * retry-on-timeout and MD5 check progress, matching C#.
     */
    private void reDownloadAndMove(String downloadBaseDestPath, List<MoveFileRecord> existingMoveRecords,
            List<FileInfo> failFileInfos) {
        try {
            String reDownloadBaseUrl = PathUtils.combine(updateInfo.originBaseUrl, updateInfo.originFolder);
            // C# uses KRResourceHelper.TransformFileInfoListToDownloadInfoList for
            // re-download.
            List<DownloadInfo> reDownloadList = ResourceHelper.transformFileInfoListToDownloadInfoList(failFileInfos);

            // Build combined move records (C# lines 587-596).
            redownloadMoveFileRecords = new ArrayList<>();
            redownloadMoveFileRecords.add(new MoveFileRecord(downloadBaseDestPath, failFileInfos));
            if (existingMoveRecords != null) {
                redownloadMoveFileRecords.addAll(existingMoveRecords);
            }

            log.info("Re-downloading {} failed files from {}", reDownloadList.size(), reDownloadBaseUrl);

            isResourcesReDownloadComplete = false;
            resourcesReDownloadTask = new ResourcesDownloadTask(
                    configManager,
                    updateInfo.cdnList,
                    reDownloadList,
                    reDownloadBaseUrl,
                    downloadBaseDestPath,
                    this::onReDownloadProgressChanged,
                    this::onReDownloadStateChanged);
            resourcesReDownloadTask.setMd5CheckProgressCallback(this::onReDownloadMd5CheckProgressChanged);

            resourcesReDownloadTask.run();

        } catch (Exception e) {
            log.error("Re-download failed", e);
            notifyComplete(false, ExceptionUtils.getHResult(e), e.getMessage(), 0);
        }
    }

    /**
     * Move files, delete redundant files, update config, and complete.
     * Corresponds to C# OnApplyCompleteChanged success path (lines 461-521).
     */
    private void moveFilesAndComplete(String downloadBaseDestPath, List<MoveFileRecord> moveRecords) {
        if (skipMoveFile) {
            log.info("=== Step 4: Move files (SKIPPED for testing) ===");
            log.info("Download completed. Files are in: {}", downloadBaseDestPath);
            log.info("Would have moved {} file records to: {}",
                    moveRecords != null ? moveRecords.size() : 0, configManager.gameDirPath);
            notifyComplete(true, 0, "Download completed (move skipped for testing)", 0);
            return;
        }

        log.info("=== Step 4: Move files ===");
        if (moveRecords == null) {
            moveRecords = new ArrayList<>();
        }

        // C# KRUpdateFlow.cs:461-579: always writes STATE_MOVING config and runs
        // MoveFileTask, even when moveRecords is empty. The moving state file is
        // needed for crash recovery (PrepareTask checks for STATE_MOVEING).
        // Save moving state to cache config (C# lines 468-472).
        String movingConfigPath = PathUtils.combine(downloadBaseDestPath,
                ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG);
        LauncherDownloadConfig movingConfig = new LauncherDownloadConfig();
        movingConfig.version = updateInfo.version;
        movingConfig.state = LauncherDownloadConfig.STATE_MOVING;
        movingConfig.appId = configManager.launcherConfig.appId;
        LauncherDownloadConfigHelper.save(movingConfig, movingConfigPath);

        MoveFileTask moveTask = new MoveFileTask(configManager.gameDirPath, moveRecords);
        moveTask.setProgressNotifyIntervalMillis(0L);
        MoveFileTask.MoveResult moveResult = moveTask.run((completed, total) -> {
            if (progressCallback != null) {
                progressCallback.onProgress(5, completed, total, 0, 0);
            }
        });

        if (!moveResult.success) {
            // C# lines 533-550: on FILE_MISSING, persist STATE_REPAIRING.
            if (moveResult.errorCode == UpdateResult.ERROR_CODE_FILE_MISSING) {
                try {
                    LauncherDownloadConfig dlCfg = configManager.getDownloadConfig();
                    if (dlCfg == null)
                        dlCfg = new LauncherDownloadConfig();
                    dlCfg.version = updateInfo.version;
                    dlCfg.state = LauncherDownloadConfig.STATE_REPAIRING;
                    configManager.saveDownloadConfig(dlCfg);
                } catch (Exception ex) {
                    log.warn("Failed to set repairing state: {}", ex.getMessage());
                }
            }
            // C# line 557: move failure sets State = 5 (ROLLBACK).
            notifyComplete(false, moveResult.errorCode, moveResult.errorMessage, 5);
            return;
        }

        // Step 5: Delete redundant files - errors propagate (C# lines 410-443).
        log.info("=== Step 5: Delete redundant files ===");
        DeleteRedundantResult deleteResult = deleteRedundantFiles();
        if (!deleteResult.success) {
            // C# lines 523-529: deletion failure fails the update with State = 5
            // (ROLLBACK).
            notifyComplete(false, deleteResult.errorCode, deleteResult.errorMessage, 5);
            return;
        }

        // Step 6: Update config.
        log.info("=== Step 6: Update config ===");
        saveCompletedConfig(updateInfo.version);

        // Cleanup (non-critical: update has already succeeded).
        if (downloadBaseDestPath != null) {
            try {
                FileUtils.deleteDirectory(downloadBaseDestPath);
            } catch (Exception e) {
                log.warn("Failed to clean up download directory: {}", downloadBaseDestPath, e);
            }
        }

        log.info("Update completed successfully! Version: {}", updateInfo.version);
        notifyComplete(true, 0, "", 0);
    }

    private void notifyComplete(UpdateResult result) {
        if (completeCallback != null) {
            completeCallback.onComplete(result);
        }
    }

    /**
     * Structured result from apply task.
     */
    private static class ApplyResult {
        boolean success;
        List<MoveFileRecord> moveRecords;
        List<FileInfo> failFileInfos;
        int errorCode;
        String errorMessage;
        long needDiskSize;

        ApplyResult(boolean success, List<MoveFileRecord> moveRecords, List<FileInfo> failFileInfos,
                int errorCode, String errorMessage, long needDiskSize) {
            this.success = success;
            this.moveRecords = moveRecords;
            this.failFileInfos = failFileInfos;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.needDiskSize = needDiskSize;
        }
    }

    private String findHPatchZ() {
        String[] searchPaths = {
                "hpatchz.exe",
                "../hpatchz.exe",
                "../../hpatchz.exe",
                configManager.gameDirPath != null
                        ? PathUtils.combine(PathUtils.getParentDir(configManager.gameDirPath), "hpatchz.exe")
                        : null
        };

        for (String path : searchPaths) {
            if (path != null && java.nio.file.Files.exists(java.nio.file.Path.of(path))) {
                return path;
            }
        }
        return null;
    }

    private String getApplyMethodFeature() {
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                var applyConfig = configManager.gameServerConfig.experiment.get("apply");
                if (applyConfig != null && applyConfig.containsKey("applyMethodFeature")) {
                    return applyConfig.get("applyMethodFeature");
                }
            }
        } catch (Exception e) {
            log.warn("Get applyMethodFeature failed", e);
        }
        return "patch";
    }

    private List<MoveFileRecord> buildDirectMoveRecords(String downloadResPath) {
        List<MoveFileRecord> records = new ArrayList<>();
        if (updateFileInfoList != null && !updateFileInfoList.isEmpty()) {
            records.add(new MoveFileRecord(downloadResPath, updateFileInfoList));
        }
        return records;
    }

    /**
     * Delete files listed in indexFile.deleteFiles.
     * Corresponds to KRUpdateFlow.DeleteRedundantFiles() (lines 410-443).
     * Errors propagate: if any deletion fails, the whole operation is reported as
     * failed.
     */
    private DeleteRedundantResult deleteRedundantFiles() {
        if (indexFile == null || indexFile.deleteFiles == null) {
            return new DeleteRedundantResult(true, 0, "");
        }

        List<String> deletedFiles = new ArrayList<>();
        boolean failed = false;
        int errorCode = 0;
        String errorMessage = "";
        int totalToDelete = indexFile.deleteFiles.size();
        int deleteIndex = 0;
        for (String fileToDelete : indexFile.deleteFiles) {
            String fullPath = PathUtils.combine(configManager.gameDirPath, fileToDelete);
            try {
                FileUtils.deleteFile(fullPath);
                deletedFiles.add(fileToDelete);
                deleteIndex++;
                if (progressCallback != null) {
                    progressCallback.onProgress(6, deleteIndex, totalToDelete, 0, 0);
                }
            } catch (Exception e) {
                // C# KRUpdateFlow.cs:426-432: on exception, break immediately
                // but STILL update GameResourceRecord with successfully deleted
                // files below (lines 434-440) before returning false.
                log.warn("Failed to delete redundant file: {}", fileToDelete, e);
                failed = true;
                errorCode = ExceptionUtils.getHResult(e);
                errorMessage = e.getMessage();
                break;
            }
        }

        // C# KRUpdateFlow.cs:434-440: always update GameResourceRecord with
        // successfully deleted files, even on partial failure.
        if (!deletedFiles.isEmpty()) {
            String configPath = PathUtils.combine(configManager.gameDirPath,
                    ResourceConfigManager.LOCAL_INDEX_FILE_NAME);
            GameResourceRecord record = GameResourceRecord.get(configPath);
            if (record != null) {
                record.deleteResource(deletedFiles);
                GameResourceRecord.save(record, configPath);
            }
        }

        if (failed) {
            return new DeleteRedundantResult(false, errorCode, errorMessage);
        }
        return new DeleteRedundantResult(true, 0, "");
    }

    private static class DeleteRedundantResult {
        boolean success;
        int errorCode;
        String errorMessage;

        DeleteRedundantResult(boolean success, int errorCode, String errorMessage) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Save downloading config when starting.
     * Matches C# KRUpdateFlow.cs lines 125-144.
     */
    private void saveDownloadingConfig(String targetVersion) {
        LauncherDownloadConfig config = configManager.getDownloadingConfig();

        if (config != null && targetVersion.equals(config.version)) {
            log.info("Last download version equals new version, skip updating downloading config");
        } else {
            if (config == null) {
                config = new LauncherDownloadConfig();
            }
            // ReUseVersion = previous downloading version, NOT local installed version.
            config.reUseVersion = config.version;
            config.version = targetVersion;
        }

        if (config != null && config.isPreDownload) {
            config.isPreDownload = false;
            log.info("Reset last Download Config PreDownload Flag: False");
        }

        configManager.saveDownloadingConfig(config);
    }

    private void saveCompletedConfig(String version) {
        LauncherDownloadConfig config = configManager.getDownloadConfig();
        if (config == null) {
            config = new LauncherDownloadConfig();
        }
        config.version = version;
        config.state = "";
        configManager.saveDownloadConfig(config);

        // C# KRUpdateFlow.cs:494: deletes the MOVING config at
        // downloadBaseDestPath/LAUNCHER_DOWNLOAD_CONFIG (saved in
        // moveFilesAndComplete).
        // The downloading config at gameDir/LAUNCHER_DOWNLOAD/ is NOT deleted — it
        // persists for crash recovery on next launch.
        try {
            String movingConfigPath = PathUtils.combine(downloadBaseDestPath,
                    ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG);
            FileUtils.deleteFile(movingConfigPath);
        } catch (Exception e) {
            log.warn("Failed to delete moving config file", e);
        }
    }

    private void notifyComplete(boolean success, int errorCode, String errorMessage, int state) {
        if (completeCallback != null) {
            UpdateResult result = new UpdateResult();
            result.success = success;
            result.errorCode = errorCode;
            result.errorMessage = errorMessage;
            result.state = state;
            completeCallback.onComplete(result);
        }
    }

    /**
     * Map a C# KRDownloadError code to a KRUpdateResult error type.
     * Corresponds to C# OnDownloadStateChanged (lines 272-279).
     * NETWORK → ERROR_TYPE_NETWORK
     * STREAM_READ_BLOCK_TIMEOUT → ERROR_TYPE_RETRY_COUNT_EXCEEDED
     * otherwise → ERROR_TYPE_UNKNOWN
     */
    private int mapDownloadErrorToType(int downloadErrCode) {
        // C# KRUpdateFlow.cs:272-280: only maps NETWORK and STREAM_READ_BLOCK_TIMEOUT.
        // DISK_SPACE_CHECK_FAIL is NOT mapped to a specific error type.
        if (downloadErrCode == DownloadError.NETWORK) {
            return UpdateResult.ERROR_TYPE_NETWORK;
        }
        if (downloadErrCode == DownloadError.STREAM_READ_BLOCK_TIMEOUT) {
            return UpdateResult.ERROR_TYPE_RETRY_COUNT_EXCEEDED;
        }
        return UpdateResult.ERROR_TYPE_UNKNOWN;
    }

    public interface ProgressCallback {
        void onProgress(int state, long completedSize, long totalSize, long completedCount, long totalCount);
    }

    public interface CompleteCallback {
        void onComplete(UpdateResult result);
    }
}
