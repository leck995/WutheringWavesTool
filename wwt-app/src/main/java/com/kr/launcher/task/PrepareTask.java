package com.kr.launcher.task;

import com.kr.launcher.config.LauncherDownloadConfigHelper;
import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.model.*;
import com.kr.launcher.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Prepares for update by downloading index file, checking reuse, building
 * download list.
 * Corresponds to KRPrepareTask.cs.
 */
public class PrepareTask {
    private static final Logger log = LoggerFactory.getLogger(PrepareTask.class);

    private final ResourceConfigManager configManager;
    private final UpdateInfo updateInfo;
    private final boolean isPreDownload;

    public PrepareTask(ResourceConfigManager configManager, UpdateInfo updateInfo, boolean isPreDownload) {
        this.configManager = configManager;
        this.updateInfo = updateInfo;
        this.isPreDownload = isPreDownload;
    }

    public PrepareTask(ResourceConfigManager configManager, UpdateInfo updateInfo) {
        this(configManager, updateInfo, false);
    }

    /**
     * Execute prepare task.
     * 1. Download index file (always the "patch" index, i.e. IndexFile field -
     * never OriginIndexFile)
     * 2. If a previous move was interrupted (state="moving"), recover by re-running
     * the move
     * using the ORIGIN index file to enumerate all expected files.
     * 3. Check ReUseVersion for file reuse
     * 4. Check existing files in cache (including chunk-dir awareness) and build
     * download list
     */
    public PrepareResult run() {
        try {
            log.info("Start PrepareTask");

            // Step 1: Download index file via ResourceHelper.getFileIndexInfo.
            // C# KRPrepareTask.cs line 243 calls GetFileIndexInfo which delegates
            // to KRResourceHelper.GetFileIndexInfo (line 92-136). This uses
            // DownloadTask with backup URLs, DisableDownloadRange,
            // DisableCheckFileSize, and proper error code propagation
            // (CSharpErrorCode → ErrorCode fallback).
            String indexFileField = updateInfo.indexFile;
            String indexFileMd5 = updateInfo.indexFileMd5;
            String indexFileName = "gameResources.json";

            String newVersionResourceDirPath = PathUtils.combine(configManager.gameCacheDirPath, updateInfo.version);
            String cacheIndexFilePath = PathUtils.combine(newVersionResourceDirPath, indexFileName);

            // Build primary URL and backup URLs from CDN list.
            List<String> allUrls = new ArrayList<>();
            if (updateInfo.cdnList != null) {
                for (CdnConfig cdn : updateInfo.cdnList) {
                    if (cdn.url != null && !cdn.url.isEmpty()) {
                        allUrls.add(UrlUtils.appendPath(cdn.url, indexFileField));
                    }
                }
            }

            if (allUrls.isEmpty()) {
                return fail(7002003, "No CDN URLs available for index file");
            }

            String primaryUrl = allUrls.get(0);
            List<String> backupUrls = allUrls.size() > 1
                    ? new ArrayList<>(allUrls.subList(1, allUrls.size()))
                    : null;

            // Compute applyMethodFeature BEFORE downloading index file.
            // C# KRPrepareTask.cs:78-79: passes applyMethodFeature to
            // GetFileIndexInfo, which sets _targetApplyType on the IndexFile so
            // getResource() returns groupResource when applyType=="group".
            String applyMethodFeature = getApplyMethodFeature();

            // Use ResourceHelper.getFileIndexInfo for proper download with error
            // codes. The callback fires synchronously (task.run() is blocking).
            IndexFile[] indexHolder = new IndexFile[1];
            int[] errCodeHolder = { 0 };
            String[] errMsgHolder = { "" };
            ResourceHelper.getFileIndexInfo(cacheIndexFilePath, primaryUrl, backupUrls, indexFileMd5,
                    (succ, indexFile, errCode, errMessage) -> {
                        if (succ) {
                            indexHolder[0] = indexFile;
                        } else {
                            errCodeHolder[0] = errCode;
                            errMsgHolder[0] = errMessage;
                        }
                    }, applyMethodFeature);

            IndexFile indexFileObj = indexHolder[0];
            if (indexFileObj == null) {
                // C# KRPrepareTask.cs#L476-480: propagate errCode/errMessage directly.
                return fail(errCodeHolder[0] != 0 ? errCodeHolder[0] : 7002003,
                        errMsgHolder[0] != null && !errMsgHolder[0].isEmpty()
                                ? errMsgHolder[0]
                                : "Failed to download index file");
            }

            List<FileInfo> resourceList = indexFileObj.getResource(applyMethodFeature);
            log.info("Index file contains {} resource files (applyType={})",
                    resourceList != null ? resourceList.size() : 0, applyMethodFeature);

            Map<String, FileInfo> newFileInfoMap = new HashMap<>();
            if (resourceList != null) {
                for (FileInfo fi : resourceList) {
                    newFileInfoMap.put(fi.path, fi);
                }
            }

            // Check moving state recovery (C# lines 232-354).
            String movingConfigPath = PathUtils.combine(newVersionResourceDirPath,
                    ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG);
            boolean isMovingFile = false;
            if (FileUtils.exists(movingConfigPath)) {
                LauncherDownloadConfig movingConfig = LauncherDownloadConfigHelper.get(movingConfigPath);
                if (movingConfig != null && "moving".equals(movingConfig.state)) {
                    isMovingFile = true;
                }
            }

            if (isMovingFile) {
                log.warn("Moving fail occurred, should continue move file");
                PrepareResult recoveryResult = handleMovingRecovery(newVersionResourceDirPath, indexFileObj);
                return recoveryResult;
            }

            // Step 2: Check ReUseVersion for file reuse (C# lines 429-447).
            LauncherDownloadConfig downloadingConfig = configManager.getDownloadingConfig();
            String reUseVersion = downloadingConfig != null ? downloadingConfig.reUseVersion : null;
            if (reUseVersion != null && !reUseVersion.isEmpty()) {
                String reUseVersionDir = PathUtils.combine(configManager.gameCacheDirPath, reUseVersion);
                String reUseIndexFile = PathUtils.combine(reUseVersionDir, "gameResources.json");
                if (FileUtils.exists(reUseIndexFile)) {
                    log.info("ReUse Index File Exist: {}, Try Reuse", reUseIndexFile);
                    String reUseJson = FileUtils.read(reUseIndexFile);
                    if (reUseJson != null) {
                        IndexFile oldIndexFile = JsonUtils.safeDeserialize(reUseJson, IndexFile.class);
                        if (oldIndexFile != null && oldIndexFile.getResource() != null) {
                            Map<String, FileInfo> oldFileInfoMap = new HashMap<>();
                            for (FileInfo fi : oldIndexFile.getResource()) {
                                oldFileInfoMap.put(fi.path, fi);
                            }
                            checkAndMoveReuseFiles(newFileInfoMap, oldFileInfoMap, reUseVersionDir,
                                    newVersionResourceDirPath);
                        }
                    }
                }
            }

            // Step 3: Check existing files in cache and build download list (C# lines
            // 150-188).
            long completedSize = 0;
            Map<String, FileInfo> downloadFileMap = new LinkedHashMap<>();
            for (Map.Entry<String, FileInfo> entry : newFileInfoMap.entrySet()) {
                String path = entry.getKey();
                FileInfo newFile = entry.getValue();
                String fullPath = PathUtils.combine(newVersionResourceDirPath, path);

                try {
                    File file = new File(fullPath);
                    String chunkDirPath = getChunkDownloadDirPath(fullPath);
                    boolean chunkDirExists = chunkDirPath != null && Files.exists(Path.of(chunkDirPath));

                    if (file.exists()) {
                        // C# line 164: only skip if size matches AND no chunk dir (chunk dir means
                        // an interrupted chunk download that should be resumed).
                        if (file.length() == newFile.size && !chunkDirExists) {
                            completedSize += file.length();
                            continue;
                        }
                        newFile.isDownloading = true;
                        downloadFileMap.put(path, newFile);
                    } else {
                        // C# lines 174-178: if file doesn't exist but chunk dir does, the file
                        // was being downloaded via chunks - mark IsDownloading so the downloader
                        // knows to resume rather than start fresh.
                        if (chunkDirExists) {
                            newFile.isDownloading = true;
                        }
                        downloadFileMap.put(path, newFile);
                    }
                } catch (Exception e) {
                    log.warn("Check file failed: {}, adding to download list", path);
                    downloadFileMap.put(path, newFile);
                }
            }

            // Sort by file size descending (large files first).
            // C# KRPrepareTask.cs:190-199 uses OrderByDescending(pair => pair.Value)
            // which sorts by KRFileInfo (IComparable compares by Size). C# does NOT
            // sort by isDownloading — removing the Java-only isDownloading sort key.
            List<DownloadInfo> downloadList = new ArrayList<>();
            List<FileInfo> sortedFiles = new ArrayList<>(downloadFileMap.values());
            sortedFiles.sort((a, b) -> Long.compare(b.size, a.size));

            for (FileInfo fi : sortedFiles) {
                downloadList.add(ResourceHelper.transformFileInfoToDownloadInfo(fi));
            }

            log.info("Build download list: {} files to download, {} bytes already complete",
                    downloadList.size(), completedSize);

            PrepareResult result = new PrepareResult();
            result.success = true;
            result.downloadInfoList = downloadList;
            result.updateFileInfoList = resourceList;
            result.completedSize = completedSize;
            result.indexFile = indexFileObj;
            return result;
        } catch (Exception e) {
            log.error("PrepareTask failed", e);
            return fail(ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    /**
     * Handle moving-state recovery by fetching the ORIGIN index file, building move
     * records
     * from it, killing the game process, and running the move task directly.
     * Corresponds to KRPrepareTask.cs lines 256-412.
     *
     * Returns a PrepareResult. On FILE_MISSING move failure, persists
     * STATE_REPAIRING to the
     * game's launcherDownloadConfig so the next run triggers a repair.
     */
    private PrepareResult handleMovingRecovery(String newVersionResourceDirPath, IndexFile indexFileObj) {
        // C# KRPrepareTask.cs:256 calls GetFileIndexInfo(KR_UPDATE_TYPE_ORIGIN)
        // which delegates to KRResourceHelper.GetFileIndexInfo using DownloadTask
        // with DisableDownloadRange, DisableCheckFileSize, backup URLs, MD5 check,
        // and proper error code propagation.
        List<String> allUrls = new ArrayList<>();
        if (updateInfo.cdnList != null) {
            for (CdnConfig cdn : updateInfo.cdnList) {
                if (cdn.url != null && !cdn.url.isEmpty()) {
                    allUrls.add(UrlUtils.appendPath(cdn.url, updateInfo.originIndexFile));
                }
            }
        }
        if (allUrls.isEmpty()) {
            return fail(7002003, "Continute Moving File Fail, Because No CDN URLs");
        }

        String primaryOriginUrl = allUrls.get(0);
        List<String> backupOriginUrls = allUrls.size() > 1
                ? new ArrayList<>(allUrls.subList(1, allUrls.size()))
                : null;

        // Use ResourceHelper.getFileIndexInfo for proper download with error codes.
        // The origin index file is saved to OriginResource.json (C#
        // KRResourceConfigManager.ORIGIN_INDEX_FILE_NAME).
        String cacheOriginPath = PathUtils.combine(newVersionResourceDirPath,
                ResourceConfigManager.ORIGIN_INDEX_FILE_NAME);

        IndexFile[] originHolder = new IndexFile[1];
        int[] originErrCode = { 0 };
        String[] originErrMsg = { "" };
        ResourceHelper.getFileIndexInfo(cacheOriginPath, primaryOriginUrl, backupOriginUrls,
                updateInfo.originIndexFileMd5,
                (succ, indexFile, errCode, errMessage) -> {
                    if (succ) {
                        originHolder[0] = indexFile;
                    } else {
                        originErrCode[0] = errCode;
                        originErrMsg[0] = errMessage;
                    }
                }, getApplyMethodFeature());

        IndexFile originIndexFileObj = originHolder[0];
        if (originIndexFileObj == null || originIndexFileObj.getResource() == null) {
            // C# KRPrepareTask.cs:262-267: PARSE_ORIGIN_INDEX_FILE_ERROR on null resource.
            int code = originErrCode[0] != 0 ? originErrCode[0] : 7002003;
            String msg = originErrMsg[0] != null && !originErrMsg[0].isEmpty()
                    ? originErrMsg[0]
                    : "Continute Moving File Fail, Because Download/Parse Origin Index File Fail";
            return fail(code, msg);
        }

        List<FileInfo> originResources = originIndexFileObj.getResource();
        String launcherDownloadPath = newVersionResourceDirPath;

        // Build move records matching C# lines 283-354.
        List<MoveFileRecord> moveRecords = new ArrayList<>();

        // Record 1: files from launcherDownloadPath itself (files that exist there).
        List<FileInfo> filesInDownloadPath = new ArrayList<>();
        for (FileInfo fi : originResources) {
            String path = PathUtils.combine(launcherDownloadPath, fi.path);
            if (new File(path).exists()) {
                filesInDownloadPath.add(fi);
            }
        }
        moveRecords.add(new MoveFileRecord(launcherDownloadPath, filesInDownloadPath));

        // Records 2-4: files from each temp dir that DON'T exist in
        // launcherDownloadPath.
        String[] tempDirNames = { "krdiff_temp", "krpdiff_temp", "krzip_temp" };
        for (String tempDirName : tempDirNames) {
            String tempDir = PathUtils.combine(newVersionResourceDirPath, tempDirName);
            if (!FileUtils.exists(tempDir))
                continue;

            List<FileInfo> filesInTemp = new ArrayList<>();
            for (FileInfo fi : originResources) {
                String mainPath = PathUtils.combine(launcherDownloadPath, fi.path);
                String tempPath = PathUtils.combine(tempDir, fi.path);
                if (!new File(mainPath).exists() && new File(tempPath).exists()) {
                    filesInTemp.add(fi);
                }
            }
            if (!filesInTemp.isEmpty()) {
                moveRecords.add(new MoveFileRecord(tempDir, filesInTemp));
            }
        }

        // Kill game process before move (C# lines 397-403).
        try {
            GameProcessUtils.killProcess(configManager);
        } catch (Exception e) {
            log.error("Kill Process Exception: {}", e.getMessage());
        }

        // Run the move task.
        MoveFileTask moveTask = new MoveFileTask(configManager.gameDirPath, moveRecords);
        MoveFileTask.MoveResult moveResult = moveTask.run((completed, total) -> {
        });

        if (moveResult.success) {
            // Recovery succeeded - no further download or apply needed.
            // C# KRPrepareTask.cs lines 359-363: sets KRUpdateIndexFile = _indexFile
            // (the patch index), leaves KRDownloadInfoList and KRUpdateFileInfoList as
            // null (NOT empty list).
            PrepareResult r = new PrepareResult();
            r.success = true;
            r.downloadInfoList = null;
            r.updateFileInfoList = null;
            r.completedSize = 0;
            r.indexFile = indexFileObj;
            return r;
        }

        // On FILE_MISSING, persist STATE_REPAIRING to the game config (C# lines
        // 371-388).
        if (moveResult.errorCode == UpdateResult.ERROR_CODE_FILE_MISSING) {
            try {
                LauncherDownloadConfig gameConfig = configManager.getDownloadConfig();
                if (gameConfig == null) {
                    gameConfig = new LauncherDownloadConfig();
                }
                gameConfig.version = updateInfo.version;
                gameConfig.state = LauncherDownloadConfig.STATE_REPAIRING;
                configManager.saveDownloadConfig(gameConfig);
            } catch (Exception ex) {
                log.warn("Move File Fail, File Not Exist, But Write Game LauncherDownloadConfig Fail, ErrorMessage: {}",
                        ex.getMessage());
            }
        }

        return fail(moveResult.errorCode, moveResult.errorMessage);
    }

    /**
     * Move matching files from old version directory to new version directory.
     * Corresponds to KRPrepareTask.CheckAndMoveReuseFiles() (lines 85-148).
     *
     * Three cases per file (C# lines 110-128):
     * 1. Main file exists at dst → skip
     * 2. Main file exists at src → move it
     * 3. Main file missing at src but chunk dir exists and IsAllChunkSame → move
     * chunk dir
     */
    private void checkAndMoveReuseFiles(Map<String, FileInfo> newFileInfoMap,
            Map<String, FileInfo> oldFileInfoMap,
            String reUseVersionDir, String newVersionDir) {
        int movedCount = 0;
        int skippedCount = 0;

        for (Map.Entry<String, FileInfo> entry : oldFileInfoMap.entrySet()) {
            String path = entry.getKey();
            FileInfo oldFile = entry.getValue();
            FileInfo newFile = newFileInfoMap.get(path);

            // C# KRPrepareTask.cs:97 uses KRFileInfo.IsSameFile which null-coalesces
            // Md5 to "" — so two null md5 fields are considered the same file.
            if (FileInfo.isSameFile(oldFile, newFile)) {
                String srcPath = PathUtils.combine(reUseVersionDir, path);
                String dstPath = PathUtils.combine(newVersionDir, path);

                if (FileUtils.exists(dstPath)) {
                    skippedCount++;
                    continue;
                }

                try {
                    if (FileUtils.exists(srcPath)) {
                        // Case 2: main file at src → move it.
                        FileUtils.ensureDir(PathUtils.getParentDir(dstPath));
                        Files.move(Path.of(srcPath), Path.of(dstPath),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        movedCount++;
                    } else {
                        // Case 3: main file missing but chunk dir may exist — move chunk dir
                        // if chunks match (C# lines 115-128).
                        String srcChunkDir = getChunkDownloadDirPath(srcPath);
                        String dstChunkDir = getChunkDownloadDirPath(dstPath);
                        if (srcChunkDir != null && dstChunkDir != null
                                && FileUtils.exists(srcChunkDir)
                                && !FileUtils.exists(dstChunkDir)
                                && FileInfo.isAllChunkSame(oldFile, newFile)) {
                            FileUtils.ensureDir(PathUtils.getParentDir(dstChunkDir));
                            try {
                                Files.move(Path.of(srcChunkDir), Path.of(dstChunkDir),
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                movedCount++;
                                log.info("Reused chunk dir for: {}", path);
                            } catch (Exception e) {
                                log.warn("Failed to move reuse chunk dir: {}", path, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to move reuse file: {}", path, e);
                }
            }
        }

        try {
            FileUtils.deleteDirectory(reUseVersionDir);
        } catch (Exception e) {
            log.warn("Failed to delete reuse version directory: {}", reUseVersionDir);
        }

        if (movedCount > 0 || skippedCount > 0) {
            log.info("File reuse: {} moved, {} skipped (already exist)", movedCount, skippedCount);
        }
    }

    /**
     * Chunk download directory path for a given file path. MUST match the
     * convention used
     * by DownloadTask.downloadWithChunks: sibling directory named
     * "<filename>_Chunks".
     * PrepareTask checks this directory's existence to detect interrupted chunk
     * downloads.
     */
    private String getChunkDownloadDirPath(String filePath) {
        if (filePath == null || filePath.isEmpty())
            return null;
        return filePath + "_Chunks";
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

    private PrepareResult fail(int code, String msg) {
        PrepareResult r = new PrepareResult();
        r.success = false;
        r.errorCode = code;
        r.errorMessage = msg;
        return r;
    }

    public static class PrepareResult {
        public boolean success;
        public int errorCode;
        public String errorMessage;
        public List<DownloadInfo> downloadInfoList;
        public List<FileInfo> updateFileInfoList;
        public long completedSize;
        public IndexFile indexFile;
    }
}
