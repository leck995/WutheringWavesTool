package com.kr.launcher.util;

import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Static utility methods for resource management.
 * Corresponds to KRResources/KRResourceHelper.cs.
 *
 * Key functions:
 * - GetFileIndexInfo: download/cache index file with MD5 verification
 * - TransformFileInfoToDownloadInfo: convert FileInfo to DownloadInfo
 * (preserves chunks)
 * - TransformFileInfoArrayToMap: build path→FileInfo map (optionally
 * case-insensitive)
 * - CheckFilesMd5WithProgress: verify file MD5s with progress reporting
 * - CheckFilesMultiMd5WithProgress: verify with local file list support
 * - UpdateResultInfoDownloadDiskSizeInfo: fill disk size info on disk-full
 * errors
 * - GetDiskSpaceCalculationRatio: read experiment config
 * - GetCdnSelectTestDuration: read experiment config
 * - GetApplyMethodFeature: read experiment config
 * - GetDirectoryCheckEntries: read directory check config
 * - GetEnableGameRestarting: read experiment config
 */
public final class ResourceHelper {
    private static final Logger log = LoggerFactory.getLogger(ResourceHelper.class);

    // ==================== Download Info Transform ====================

    /**
     * Transform a FileInfo into a DownloadInfo, preserving chunk info.
     * Corresponds to C# KRResourceHelper.TransformFileInfoToDownloadInfo.
     */
    public static DownloadInfo transformFileInfoToDownloadInfo(FileInfo fileInfo) {
        DownloadInfo dlInfo = new DownloadInfo();
        dlInfo.url = fileInfo.path;
        dlInfo.destPath = fileInfo.path;
        dlInfo.fileSize = fileInfo.size;
        dlInfo.md5 = fileInfo.md5;
        dlInfo.basePath = fileInfo.fromFolder;
        if (fileInfo.chunkInfos != null && !fileInfo.chunkInfos.isEmpty()) {
            dlInfo.chunkInfoList = new ArrayList<>();
            for (FileChunkInfo chunk : fileInfo.chunkInfos) {
                dlInfo.chunkInfoList.add(new ChunkInfo(chunk.start, chunk.end, chunk.md5));
            }
        }
        return dlInfo;
    }

    /**
     * Transform a list of FileInfos into a list of DownloadInfos.
     */
    public static List<DownloadInfo> transformFileInfoListToDownloadInfoList(List<FileInfo> fileInfoList) {
        List<DownloadInfo> list = new ArrayList<>();
        if (fileInfoList != null) {
            for (FileInfo fi : fileInfoList) {
                list.add(transformFileInfoToDownloadInfo(fi));
            }
        }
        return list;
    }

    /**
     * Build a path→FileInfo map from a list.
     * Corresponds to C# KRResourceHelper.TransformFileInfoArrayToMap.
     * 
     * @param fileInfos       list of FileInfo (may be null)
     * @param caseInsensitive if true, uses case-insensitive keys
     */
    public static Map<String, FileInfo> transformFileInfoArrayToMap(List<FileInfo> fileInfos, boolean caseInsensitive) {
        Map<String, FileInfo> map = caseInsensitive
                ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                : new HashMap<>();
        if (fileInfos != null) {
            for (FileInfo fi : fileInfos) {
                map.put(fi.path, fi);
            }
        }
        return map;
    }

    public static Map<String, FileInfo> transformFileInfoArrayToMap(List<FileInfo> fileInfos) {
        return transformFileInfoArrayToMap(fileInfos, false);
    }

    // ==================== File Size Calculation ====================

    /**
     * Get total size of a FileInfo list.
     */
    public static long getFileInfoListSize(List<FileInfo> fileInfoList) {
        if (fileInfoList == null)
            return 0;
        long total = 0;
        for (FileInfo fi : fileInfoList) {
            total += fi.size;
        }
        return total;
    }

    // ==================== MD5 Verification ====================

    /**
     * Verify file MD5s with progress reporting.
     * Corresponds to C# KRResourceHelper.CheckFilesMd5WithProgress.
     *
     * @param destDirPath      base directory
     * @param fileInfos        files to verify
     * @param progressCallback (completedSize, totalSize)
     * @param completeCallback (success, failFileInfos)
     */
    public static void checkFilesMd5WithProgress(
            String destDirPath,
            List<FileInfo> fileInfos,
            CheckFileMd5ProgressCallback progressCallback,
            CheckFileMd5ResultCallback completeCallback) {

        long totalSize = getFileInfoListSize(fileInfos);
        long completedSize = 0;
        List<FileInfo> failList = new ArrayList<>();

        for (FileInfo fi : fileInfos) {
            String filePath = PathUtils.combine(destDirPath, fi.path);
            File file = new File(filePath);

            if (!file.exists() || file.length() != fi.size) {
                completedSize += fi.size;
                failList.add(fi);
                progressCallback.onProgress(completedSize, totalSize);
                continue;
            }

            long tempCompleted = completedSize;
            String actualMd5 = MD5Utils.getFileMD5WithProgress(filePath,
                    (fileCompleted, fileTotal) -> progressCallback.onProgress(tempCompleted + fileCompleted,
                            totalSize));

            completedSize += fi.size;
            progressCallback.onProgress(completedSize, totalSize);

            if (actualMd5 == null || !actualMd5.equalsIgnoreCase(fi.md5)) {
                failList.add(fi);
            }
        }

        completeCallback.onResult(failList.isEmpty(), failList);
    }

    /**
     * Verify file MD5s with local file list support.
     * Corresponds to C# KRResourceHelper.CheckFilesMultiMd5WithProgress.
     */
    public static void checkFilesMultiMd5WithProgress(
            String destDirPath,
            List<FileInfo> remoteFileInfos,
            List<FileInfo> localFileInfos,
            List<String> resourcesExcludePathList,
            List<String> resourcesExcludeWhitePathList,
            CheckFileMd5ProgressCallback progressCallback,
            CheckFileMd5ResultCallback completeCallback) {

        if (localFileInfos == null || localFileInfos.isEmpty()) {
            checkFilesMd5WithProgress(destDirPath, remoteFileInfos, progressCallback, completeCallback);
            return;
        }

        // C# KRResourceHelper.cs:409-422: totalSize only sums files with
        // non-blank paths (inside the IsNullOrWhiteSpace check).
        long totalSize = 0;
        for (FileInfo fi : remoteFileInfos) {
            if (fi.path != null && !fi.path.trim().isEmpty()) {
                totalSize += fi.size;
            }
        }
        final long finalTotalSize = totalSize;
        Map<String, List<FileInfo>> dict = new HashMap<>();

        for (FileInfo remote : remoteFileInfos) {
            if (remote.path != null && !remote.path.trim().isEmpty()) {
                dict.computeIfAbsent(remote.path, k -> new ArrayList<>()).add(remote);
            }
        }

        for (FileInfo local : localFileInfos) {
            if (local.path != null && !local.path.trim().isEmpty()
                    && isExcludePathConfigContainsLocalPath(local.path, resourcesExcludePathList,
                            resourcesExcludeWhitePathList)) {
                dict.computeIfAbsent(local.path, k -> new ArrayList<>()).add(local);
            }
        }

        long completedSize = 0;
        List<FileInfo> failList = new ArrayList<>();

        for (FileInfo remote : remoteFileInfos) {
            String filePath = PathUtils.combine(destDirPath, remote.path);
            File file = new File(filePath);

            if (!file.exists()) {
                completedSize += remote.size;
                failList.add(remote);
                progressCallback.onProgress(completedSize, totalSize);
                continue;
            }

            List<FileInfo> matching = dict.get(remote.path);
            boolean sizeMatch = false;
            if (matching != null) {
                for (FileInfo m : matching) {
                    if (m.size == file.length()) {
                        sizeMatch = true;
                        break;
                    }
                }
            }

            if (!sizeMatch) {
                completedSize += remote.size;
                failList.add(remote);
                progressCallback.onProgress(completedSize, totalSize);
                continue;
            }

            long tempCompleted = completedSize;
            String actualMd5 = MD5Utils.getFileMD5WithProgress(filePath,
                    (fileCompleted, fileTotal) -> progressCallback.onProgress(tempCompleted + fileCompleted,
                            finalTotalSize));

            completedSize += remote.size;
            progressCallback.onProgress(completedSize, totalSize);

            boolean md5Match = false;
            if (matching != null && actualMd5 != null) {
                for (FileInfo m : matching) {
                    if (m.md5 != null && m.md5.trim().equalsIgnoreCase(actualMd5)) {
                        md5Match = true;
                        break;
                    }
                }
            }
            if (!md5Match) {
                failList.add(remote);
            }
        }

        completeCallback.onResult(failList.isEmpty(), failList);
    }

    private static boolean isExcludePathConfigContainsLocalPath(
            String path, List<String> resourcesExcludePathList, List<String> resourcesExcludeWhitePathList) {
        if (resourcesExcludePathList != null) {
            for (String exclude : resourcesExcludePathList) {
                if (!path.startsWith(exclude))
                    continue;
                if (resourcesExcludeWhitePathList != null) {
                    return !resourcesExcludeWhitePathList.contains(path);
                }
                return true;
            }
        }
        return false;
    }

    // ==================== Experiment Config Getters ====================

    /**
     * Get disk space calculation ratio from experiment config (default 1.2).
     */
    public static double getDiskSpaceCalculationRatio(ResourceConfigManager configManager) {
        double ratio = 1.2;
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> downloadCfg = configManager.gameServerConfig.experiment.get("download");
                if (downloadCfg != null && downloadCfg.containsKey("diskSpaceCalculationRatio")) {
                    ratio = Double.parseDouble(downloadCfg.get("diskSpaceCalculationRatio"));
                }
            }
        } catch (Exception e) {
            log.warn("Get diskSpaceCalculationRatio Fail: {}", e.getMessage());
        }
        return ratio;
    }

    /**
     * Get CDN select test duration in ms (default 3000).
     */
    public static long getCdnSelectTestDuration(ResourceConfigManager configManager) {
        long duration = 3000L;
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> downloadCfg = configManager.gameServerConfig.experiment.get("download");
                if (downloadCfg != null && downloadCfg.containsKey("downloadCdnSelectTestDuration")) {
                    duration = Long.parseLong(downloadCfg.get("downloadCdnSelectTestDuration"));
                }
            }
        } catch (Exception e) {
            log.warn("Get cdnSelectTestDuration Fail: {}", e.getMessage());
        }
        if (duration <= 0)
            duration = 3000L;
        return duration;
    }

    /**
     * Get read block timeout (default 0).
     */
    public static long getReadBlockTimeout(ResourceConfigManager configManager) {
        long timeout = 0L;
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> downloadCfg = configManager.gameServerConfig.experiment.get("download");
                if (downloadCfg != null && downloadCfg.containsKey("downloadReadBlockTimeout")) {
                    timeout = Long.parseLong(downloadCfg.get("downloadReadBlockTimeout"));
                }
            }
        } catch (Exception e) {
            log.warn("Get readBlockTime Fail: {}", e.getMessage());
        }
        return timeout;
    }

    /**
     * Get apply method feature (default "patch").
     */
    public static String getApplyMethodFeature(ResourceConfigManager configManager) {
        String feature = ApplyType.PATCH;
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> applyCfg = configManager.gameServerConfig.experiment.get("apply");
                if (applyCfg != null && applyCfg.containsKey("applyMethodFeature")) {
                    feature = applyCfg.get("applyMethodFeature");
                }
            }
        } catch (Exception e) {
            log.warn("Get applyMethodFeature Fail: {}", e.getMessage());
        }
        return feature;
    }

    /**
     * Get enable game restarting flag (null = use launcher config default).
     */
    public static Boolean getEnableGameRestarting(ResourceConfigManager configManager) {
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> gameCfg = configManager.gameServerConfig.experiment.get("game");
                if (gameCfg != null && gameCfg.containsKey("enableGameRestarting")) {
                    return Boolean.parseBoolean(gameCfg.get("enableGameRestarting"));
                }
            }
        } catch (Exception e) {
            log.warn("Get enableGameRestarting Fail: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get directory check entries from experiment config.
     */
    public static List<DirectoryCheckEntry> getDirectoryCheckEntries(ResourceConfigManager configManager) {
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> repairCfg = configManager.gameServerConfig.experiment.get("repair");
                if (repairCfg != null && repairCfg.containsKey("directoryIntegrityCheckList")) {
                    String text = repairCfg.get("directoryIntegrityCheckList");
                    return JsonUtils.safeDeserializeList(text, DirectoryCheckEntry.class);
                }
            }
        } catch (Exception e) {
            log.warn("Get directoryCheckEntries Fail: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Whether to reset HTTP client when read timeout occurs (default true).
     */
    public static boolean resetHttpClientWhenReadTimeout(ResourceConfigManager configManager) {
        boolean flag = true;
        try {
            if (configManager.gameServerConfig != null && configManager.gameServerConfig.experiment != null) {
                Map<String, String> downloadCfg = configManager.gameServerConfig.experiment.get("download");
                if (downloadCfg != null && downloadCfg.containsKey("downloadReadTimeoutResetHttpClient")) {
                    flag = Integer.parseInt(downloadCfg.get("downloadReadTimeoutResetHttpClient")) == 1;
                }
            }
        } catch (Exception e) {
            log.warn("Get resetHttpClientWhenReadTimeout Fail: {}", e.getMessage());
        }
        return flag;
    }

    // ==================== Disk Size Info ====================

    /**
     * Fill disk size info into UpdateResult on disk-full errors.
     * Corresponds to C# KRResourceHelper.UpdateResultInfoDownloadDiskSizeInfo.
     *
     * @param updateResult   the result to fill
     * @param configManager  for ratio config
     * @param needDiskSize   remaining download size (from download task)
     * @param unCompressSize decompression size (0 if no decompression)
     */
    public static void updateResultInfoDownloadDiskSizeInfo(
            UpdateResult updateResult,
            ResourceConfigManager configManager,
            long needDiskSize,
            long unCompressSize) {

        if (updateResult.getErrorType() != UpdateResult.ERROR_TYPE_DISK_NOT_ENOUGH_SPACE) {
            return;
        }

        double ratio = getDiskSpaceCalculationRatio(configManager);
        long remainDownloadSize;
        long requiredDiskSpaceSize;

        if (unCompressSize > 0) {
            ratio = 1.0;
            remainDownloadSize = needDiskSize;
            requiredDiskSpaceSize = unCompressSize + (long) (needDiskSize * ratio);
        } else {
            remainDownloadSize = needDiskSize;
            requiredDiskSpaceSize = (long) (needDiskSize * ratio);
        }

        updateResult.extInfos.put("remainDownloadSize", remainDownloadSize);
        updateResult.extInfos.put("requiredDiskSpaceSize", requiredDiskSpaceSize);
    }

    /**
     * Overload taking a ResourcesDownloadTask to query need-disk-size.
     * Corresponds to C# KRResourceHelper.UpdateResultInfoDownloadDiskSizeInfo
     * with KRResourcesDownloadTask parameter.
     *
     * Matches C# IsDownloadFinish branching:
     * - If NOT finished: remainDownloadSize = GetNeedDiskSize(1.0),
     * requiredDiskSpaceSize += GetNeedDiskSize(ratio).
     * - If finished: remainDownloadSize = 0,
     * requiredDiskSpaceSize = (unCompressSize <= 0)
     * ? (num2 + GetNeedDiskSize(ratio)) : unCompressSize.
     */
    public static void updateResultInfoDownloadDiskSizeInfo(
            UpdateResult updateResult,
            ResourceConfigManager configManager,
            com.kr.launcher.download.ResourcesDownloadTask resourcesDownloadTask,
            long unCompressSize) {

        if (updateResult.getErrorType() != UpdateResult.ERROR_TYPE_DISK_NOT_ENOUGH_SPACE) {
            return;
        }
        if (resourcesDownloadTask == null) {
            return;
        }

        double ratio = getDiskSpaceCalculationRatio(configManager);
        long remainDownloadSize = 0L;
        long requiredDiskSpaceSize = 0L;

        if (unCompressSize > 0) {
            ratio = 1.0;
            requiredDiskSpaceSize += unCompressSize;
        }

        if (!resourcesDownloadTask.isDownloadFinish()) {
            remainDownloadSize = resourcesDownloadTask.getNeedDiskSize(1.0);
            requiredDiskSpaceSize += resourcesDownloadTask.getNeedDiskSize(ratio);
        } else {
            remainDownloadSize = 0L;
            requiredDiskSpaceSize = (unCompressSize <= 0)
                    ? (requiredDiskSpaceSize + resourcesDownloadTask.getNeedDiskSize(ratio))
                    : unCompressSize;
        }

        updateResult.extInfos.put("remainDownloadSize", remainDownloadSize);
        updateResult.extInfos.put("requiredDiskSpaceSize", requiredDiskSpaceSize);
    }

    // ==================== Callbacks ====================

    public interface CheckFileMd5ProgressCallback {
        void onProgress(long completedSize, long totalSize);
    }

    public interface CheckFileMd5ResultCallback {
        void onResult(boolean success, List<FileInfo> failFileInfos);
    }

    public interface IndexFileCallback {
        void onResult(boolean succ, IndexFile indexFile, int errCode, String errMessage);
    }

    // ==================== Index File Download ====================

    /**
     * Parse an index file from disk.
     * Corresponds to C# KRResourceHelper.ParseFileIndexInfo.
     */
    public static void parseFileIndexInfo(String indexFilePath, IndexFileCallback callback, String applyType) {
        try {
            String content = FileUtils.read(indexFilePath);
            IndexFileFactory factory = new IndexFileFactory(content, applyType);
            IndexFile indexFile = factory.create();
            // C# KRResourceHelper.cs:83-84: always calls callback with succ=true,
            // even if indexFile is null (no null check). Downstream PrepareTask
            // handles null indexFile by returning error 7002003.
            callback.onResult(true, indexFile, 0, "");
        } catch (Exception e) {
            callback.onResult(false, null, ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    /**
     * Download (if needed) and parse the index file.
     * Corresponds to C# KRResourceHelper.GetFileIndexInfo.
     *
     * If a cached file exists with matching MD5, parses it directly.
     * Otherwise downloads from url (with backup URL failover), then parses.
     */
    public static void getFileIndexInfo(String cacheIndexFilePath, String url,
            List<String> backUpUrls, String md5,
            IndexFileCallback callback, String applyType) {
        try {
            String indexFilePath = cacheIndexFilePath;
            File cacheFile = new File(indexFilePath);
            if (cacheFile.exists()) {
                String cachedMd5 = MD5Utils.safeGetFileMd5(indexFilePath);
                // C# KRResourceHelper.cs#L100: if (md5 != text) File.Delete.
                // When md5 is null, this deletes the cache (null != computedMd5),
                // forcing a fresh download. Match C# exactly with Objects.equals.
                if (!java.util.Objects.equals(md5, cachedMd5)) {
                    cacheFile.delete();
                }
            }
            if (cacheFile.exists()) {
                parseFileIndexInfo(indexFilePath, callback, applyType);
                return;
            }

            int maxRetry = (backUpUrls != null ? backUpUrls.size() : 0) + 1;
            maxRetry = maxRetry * 5 - 1;

            com.kr.launcher.download.DownloadTaskBuilder builder = new com.kr.launcher.download.DownloadTaskBuilder(url,
                    indexFilePath);
            builder.withMd5(md5);
            builder.withBackUpUrls(backUpUrls);
            builder.withMaxRetryCount(maxRetry);
            // C# KRResourceHelper.cs#L115-117: index file downloads disable
            // Range and file-size checks, and use independent dispatcher.
            builder.withDisableDownloadRange(true);
            builder.withDisableCheckFileSize(true);
            com.kr.launcher.download.DownloadTask task = builder.build();

            task.setStateCallback((state, error) -> {
                if (state == com.kr.launcher.download.DownloadState.COMPLETE) {
                    parseFileIndexInfo(indexFilePath, callback, applyType);
                } else if (state == com.kr.launcher.download.DownloadState.FAILED
                        || state == com.kr.launcher.download.DownloadState.CANCELED) {
                    // C# KRResourceHelper.cs#L126: prefer CSharpErrorCode (HRESULT),
                    // C# KRResourceHelper.cs:126: prefer CSharpErrorCode (HRESULT),
                    // fall back to ErrorCode. No NETWORK fallback — if both are 0,
                    // errCode stays 0 (matching C#).
                    int errCode = task.getCSharpErrorCode();
                    if (errCode == 0) {
                        errCode = task.getErrorCode();
                    }
                    callback.onResult(false, null, errCode, error != null ? error : "");
                }
            });
            task.run();
        } catch (Exception e) {
            callback.onResult(false, null, ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    private ResourceHelper() {
    }
}
