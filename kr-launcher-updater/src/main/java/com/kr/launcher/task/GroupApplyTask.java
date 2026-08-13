package com.kr.launcher.task;

import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.model.*;
import com.kr.launcher.patch.PatchExecutor;
import com.kr.launcher.patch.PatchStartResult;
import com.kr.launcher.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Multi-group sequential patch apply task.
 * Corresponds to KRGroupApplyTask.cs (362 lines).
 *
 * Flow:
 * 1. Separate .krdiff/.krpdiff files from regular resource files; add regular
 * files to move records
 * 2. Ensure krpdiff_temp directory exists
 * 3. Read LauncherApplyConfig for resume support (patchedIndex)
 * 4. Remove source files for already-patched groups
 * 5. Check disk space against peak disk usage
 * 6. Sequentially apply each group's patch via HPatchZ
 * 7. After each group: save patched index, delete source files, proceed to next
 * 8. After all groups: verify MD5 of all destination files
 * 9. Filter BugFix files (entries also in resource list)
 */
public class GroupApplyTask {
    private static final Logger log = LoggerFactory.getLogger(GroupApplyTask.class);

    private final UpdateInfo updateInfo;
    private final ResourceConfigManager configManager;
    private final String gameDirPath;
    private final String downloadResPath;
    private final IndexFile indexFile;
    private final long deCompressSize;
    private long currentDeCompressSize = 0;
    private long totalSize = 0;
    private long totalCount = 0;
    private final List<MoveFileRecord> moveRecords = new ArrayList<>();
    private List<FileInfo> applyFailFileInfos = null;
    private final String hpatchzExePath;

    /** Per-group progress tracking for aggregation. */
    private static class PartialProgress {
        long fileSize = 0;
        long fileCount = 0;
    }

    /** Exception carrying a specific error code from partial apply. */
    private static class ApplyErrorException extends Exception {
        final int errorCode;

        ApplyErrorException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
    }

    private List<PartialProgress> partialProgressList;

    public GroupApplyTask(UpdateInfo updateInfo, ResourceConfigManager configManager,
            String gameDirPath, String downloadResPath, IndexFile indexFile,
            long deCompressSize, String hpatchzExePath) {
        this.updateInfo = updateInfo;
        this.configManager = configManager;
        this.gameDirPath = gameDirPath;
        this.downloadResPath = downloadResPath;
        this.indexFile = indexFile;
        this.deCompressSize = deCompressSize;
        this.hpatchzExePath = hpatchzExePath;
    }

    public GroupApplyResult run(GroupProgressCallback progressCallback) {
        try {
            log.info("=== GroupApplyTask: Starting ===");

            totalCount = 0;
            totalSize = 0;

            // Step 1: Separate .krdiff/.krpdiff files from regular resource (C# lines
            // 38-60).
            List<FileInfo> regularFiles = new ArrayList<>();
            if (indexFile.getResource() != null) {
                for (FileInfo fi : indexFile.getResource()) {
                    if (fi.path != null && (fi.path.endsWith(".krdiff") || fi.path.endsWith(".krpdiff"))) {
                        log.info("Skip KRDiff/KRPDiff File Move, Path: {}", fi.path);
                    } else {
                        regularFiles.add(fi);
                    }
                }
            }
            if (!regularFiles.isEmpty()) {
                moveRecords.add(new MoveFileRecord(downloadResPath, regularFiles));
            }

            // Step 2: Ensure krpdiff_temp directory exists (C# lines 61-63).
            String patchTempDir = PathUtils.combine(downloadResPath, "krpdiff_temp");
            FileUtils.ensureDir(patchTempDir);

            // C# KRGroupApplyTask.cs#L66,71: if GroupInfos is null, accessing .Count
            // throws NRE caught by base class → failure. If empty (Count==0),
            // patchedGroupIndex=-1, nextIndex=0 >= 0 → CheckApplyFiles() → success.
            // No explicit null/empty check here; let it flow through naturally.

            // Step 3: Get patched group index for resume (C# lines 64-65).
            int patchedGroupIndex = getPatchedGroupIndex(patchTempDir);
            log.info("Patched group index: {}", patchedGroupIndex);

            // Step 4: Remove source files for already-patched groups (C# lines 66-69).
            if (patchedGroupIndex >= 0 && patchedGroupIndex < indexFile.groupInfos.size()) {
                removeSourceFiles(patchedGroupIndex);
            }

            // Step 5: Check if all groups already patched (C# lines 70-75).
            int nextIndex = patchedGroupIndex + 1;
            if (nextIndex >= indexFile.groupInfos.size()) {
                log.info("All groups already patched, checking apply files");
                checkApplyFiles(patchTempDir, progressCallback);
                return buildResult();
            }

            // Step 6: Check disk space against peak disk usage (C# lines 76-82).
            long availableDisk = getAvailableDiskSize();
            long peekDiskUsage = calcPeekDiskUsage(nextIndex);
            if (availableDisk > 0 && availableDisk < peekDiskUsage) {
                return new GroupApplyResult(false, null, null,
                        UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE,
                        "Disk Not Enough Space, AvailableSize: " + availableDisk
                                + ", PeekDiskUsage: " + peekDiskUsage);
            }

            // Step 7: Initialize partial progress and calculate totals (C# lines 83-96).
            partialProgressList = new ArrayList<>();
            for (int i = 0; i < indexFile.groupInfos.size(); i++) {
                partialProgressList.add(new PartialProgress());
            }
            for (int k = nextIndex; k < indexFile.groupInfos.size(); k++) {
                GroupFileInfo groupInfo = indexFile.groupInfos.get(k);
                if (groupInfo.dstFiles != null) {
                    for (FileInfo dstFile : groupInfo.dstFiles) {
                        totalSize += dstFile.size;
                    }
                    totalCount += groupInfo.dstFiles.size();
                }
            }

            // Step 8: Sequentially apply each group (C# lines 97-98).
            partialApplyFiles(nextIndex, patchTempDir, progressCallback);

            return buildResult();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("GroupApplyTask interrupted", e);
            return new GroupApplyResult(false, null, null, -1, "Interrupted");
        } catch (ApplyErrorException e) {
            log.error("GroupApplyTask failed: {}", e.getMessage());
            return new GroupApplyResult(false, null, null, e.errorCode, e.getMessage());
        } catch (Exception e) {
            log.error("GroupApplyTask failed", e);
            return new GroupApplyResult(false, null, null, ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    /**
     * Apply a single group's patch, then recursively apply the next group.
     * Corresponds to KRGroupApplyTask.PartialApplyFiles() (lines 280-355).
     */
    private void partialApplyFiles(int partialIndex, String patchTempDir,
            GroupProgressCallback progressCallback) throws InterruptedException, ApplyErrorException {
        GroupFileInfo groupInfo = indexFile.groupInfos.get(partialIndex);
        String dest = groupInfo.dest;
        String diffPath = PathUtils.combine(downloadResPath, dest);
        String newPath = patchTempDir;

        log.info("Applying group {}/{}: {}", partialIndex + 1, indexFile.groupInfos.size(), dest);

        PatchExecutor executor = new PatchExecutor();
        CountDownLatch completionLatch = new CountDownLatch(1);
        final int[] exitCodeResult = { 0 };
        final boolean[] hadError = { false };

        executor.progressCallback = (patchedFileCount, fileTotalCount, patchingFileCurrSize,
                patchingFileTotalSize, patchedCurrBytes, patchTotalBytes) -> {
            partialProgressList.get(partialIndex).fileSize = patchedCurrBytes;
            partialProgressList.get(partialIndex).fileCount = patchedFileCount;
            long totalFileSize = 0;
            long totalFileCount = 0;
            for (PartialProgress p : partialProgressList) {
                totalFileSize += p.fileSize;
                totalFileCount += p.fileCount;
            }
            if (progressCallback != null) {
                progressCallback.onProgress(2, totalFileSize, totalSize, totalFileCount, totalCount);
            }
        };

        executor.finishedCallback = (exitCode) -> {
            exitCodeResult[0] = exitCode;
            completionLatch.countDown();
        };

        PatchStartResult startResult = executor.startAsync(hpatchzExePath, gameDirPath, diffPath, newPath, 1000);
        if (startResult.errorCode != 0) {
            log.error("Failed to start HPatchZ for group {}: {}", partialIndex, startResult.errorMessage);
            executor.close();
            throw new ApplyErrorException(PatchExecutor.ERROR_CODE_START_PATCH_FAILED,
                    "Failed to start HPatchZ: " + startResult.errorMessage);
        }

        // Wait for HPatchZ to complete (no overall timeout — matches C# which only
        // polls progress every 1s with no maximum duration).
        completionLatch.await();

        int exitCode = exitCodeResult[0];

        // Handle exit codes (C# lines 303-352).
        if (exitCode == PatchExecutor.EXIT_FILE_OCCUPANCY_1 || exitCode == PatchExecutor.EXIT_FILE_OCCUPANCY_2) {
            throw new ApplyErrorException(UpdateResult.ERROR_CODE_FILE_OCCUPANCY,
                    "DIR_PATCH FAIL, Because File OCCUPANCY (exit code " + exitCode + ")");
        }
        if (exitCode == PatchExecutor.EXIT_DISK_NOT_ENOUGH_SPACE) {
            throw new ApplyErrorException(UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE,
                    "DIR_PATCH FAIL, Because Disk Not Enough Space");
        }
        if (exitCode == PatchExecutor.EXIT_PERMISSION_DENIED_1 || exitCode == PatchExecutor.EXIT_PERMISSION_DENIED_2) {
            throw new ApplyErrorException(UpdateResult.ERROR_CODE_FILE_PERMISSION_DENY,
                    "DIR_PATCH FAIL, Because File Permission Deny");
        }

        // Success: update progress (C# lines 317-325).
        long totalFileSize = 0;
        long totalFileCount = 0;
        for (PartialProgress p : partialProgressList) {
            totalFileSize += p.fileSize;
            totalFileCount += p.fileCount;
        }
        if (progressCallback != null) {
            progressCallback.onProgress(2, totalFileSize, totalSize, totalFileCount, totalCount);
        }

        // Save patched group index for resume (C# lines 326-335).
        try {
            setPatchedGroupIndex(partialIndex, patchTempDir);
        } catch (Exception e) {
            log.error("SetPatchedGroupIndex Failed, partialIndex: {}", partialIndex, e);
            throw new ApplyErrorException(UpdateResult.ERROR_CODE_FILE_OCCUPANCY,
                    "DIR_PATCH FAIL, Because File OCCUPANCY (set index failed)");
        }

        // Remove source files for this group (C# line 336).
        removeSourceFiles(partialIndex);

        // Proceed to next group or verify (C# lines 337-348).
        if (partialIndex + 1 < indexFile.groupInfos.size()) {
            partialApplyFiles(partialIndex + 1, patchTempDir, progressCallback);
        } else {
            if (progressCallback != null) {
                progressCallback.onProgress(2, totalSize, totalSize, totalCount, totalCount);
            }
            checkApplyFiles(patchTempDir, progressCallback);
        }
    }

    /**
     * Verify MD5 of all destination files across all groups.
     * Corresponds to KRGroupApplyTask.CheckApplyFiles() (lines 200-278).
     */
    private void checkApplyFiles(String patchTempDir, GroupProgressCallback progressCallback) {
        // Collect all destination files from all groups.
        List<FileInfo> allDestinationFiles = new ArrayList<>();
        for (GroupFileInfo groupInfo : indexFile.groupInfos) {
            if (groupInfo.dstFiles != null) {
                allDestinationFiles.addAll(groupInfo.dstFiles);
            }
        }

        log.info("Verifying MD5 of {} destination files across all groups", allDestinationFiles.size());
        // C# KRGroupApplyTask does NOT pre-emit a state=3 (0,0) event; the first
        // state=3 progress comes from inside CheckFilesMd5WithProgress's callback.

        // C# KRGroupApplyTask uses KRResourceHelper.CheckFilesMd5WithProgress
        // for post-patch verification with state=3 progress events.
        @SuppressWarnings("unchecked")
        List<FileInfo>[] failHolder = (List<FileInfo>[]) new List<?>[1];
        failHolder[0] = new ArrayList<>();
        final GroupProgressCallback groupCb = progressCallback;
        ResourceHelper.checkFilesMd5WithProgress(
                patchTempDir,
                allDestinationFiles,
                (completedSize, totalMd5Size) -> {
                    if (groupCb != null) {
                        groupCb.onProgress(3, completedSize, totalMd5Size, 0, 0);
                    }
                },
                (success, failFileInfos) -> failHolder[0] = failFileInfos != null ? failFileInfos
                        : new ArrayList<>());
        List<FileInfo> failedFiles = failHolder[0];

        if (failedFiles.isEmpty()) {
            // Success: add to move records, filtering BugFix files (C# lines 216-234).
            Map<String, FileInfo> resourceMap = buildResourceMapCaseInsensitive();
            List<FileInfo> filesToMove = new ArrayList<>();
            for (FileInfo entry : allDestinationFiles) {
                if (resourceMap.containsKey(entry.path.toLowerCase())) {
                    log.warn("BugFix File: {}, Use Resource File", entry.path);
                } else {
                    filesToMove.add(entry);
                }
            }
            if (!filesToMove.isEmpty()) {
                moveRecords.add(new MoveFileRecord(patchTempDir, filesToMove));
            }
            log.info("Group apply and MD5 verification completed successfully");
        } else {
            // Failure: delete failed files, move successful files (C# lines 237-274).
            log.warn("Group MD5 verification failed for {} files", failedFiles.size());

            for (FileInfo failFile : failedFiles) {
                try {
                    FileUtils.deleteFile(PathUtils.combine(patchTempDir, failFile.path));
                } catch (Exception e) {
                    log.error("Delete Apply Fail Temp File Fail, Path: {}", failFile.path, e);
                }
            }

            Map<String, FileInfo> resourceMap = buildResourceMapCaseInsensitive();
            Map<String, FileInfo> failMap = new HashMap<>();
            for (FileInfo fi : failedFiles) {
                failMap.put(fi.path, fi);
            }

            List<FileInfo> successFiles = new ArrayList<>();
            applyFailFileInfos = new ArrayList<>();
            long failSize = 0;

            for (FileInfo entry : allDestinationFiles) {
                if (resourceMap.containsKey(entry.path.toLowerCase())) {
                    log.warn("BugFix File: {}, Use Resource File", entry.path);
                } else if (failMap.containsKey(entry.path)) {
                    applyFailFileInfos.add(entry);
                    failSize += entry.size;
                } else {
                    successFiles.add(entry);
                }
            }

            // C# KRGroupApplyTask.cs:270-271 always adds the move record
            // (even if list2 is empty). Match that behavior.
            moveRecords.add(new MoveFileRecord(patchTempDir, successFiles));

            log.warn("Group apply completed: {} success, {} fail, fail total size: {}",
                    successFiles.size(), applyFailFileInfos.size(), failSize);
        }
    }

    private List<FileInfo> verifyFilesMd5(String destDirPath, List<FileInfo> fileInfos,
            GroupProgressCallback progressCallback) {
        List<FileInfo> failedFiles = new ArrayList<>();
        long totalSize = 0;
        for (FileInfo fi : fileInfos) {
            totalSize += fi.size;
        }
        long completedSize = 0;

        for (FileInfo fi : fileInfos) {
            String fullPath = PathUtils.combine(destDirPath, fi.path);
            File file = new File(fullPath);

            if (!file.exists() || file.length() != fi.size) {
                completedSize += fi.size;
                failedFiles.add(fi);
                if (progressCallback != null) {
                    progressCallback.onProgress(3, completedSize, totalSize, 0, 0);
                }
                continue;
            }

            String actualMd5 = MD5Utils.getFileMD5(fullPath);
            completedSize += fi.size;
            if (progressCallback != null) {
                progressCallback.onProgress(3, completedSize, totalSize, 0, 0);
            }

            if (fi.md5 != null && !fi.md5.isEmpty()
                    && actualMd5 != null && !actualMd5.equalsIgnoreCase(fi.md5)) {
                log.error("MD5 mismatch: {} expected={}, actual={}", fi.path, fi.md5, actualMd5);
                failedFiles.add(fi);
            }
        }
        return failedFiles;
    }

    /**
     * Delete source files for a patched group from the game directory.
     * Corresponds to KRGroupApplyTask.RemoveSourceFiles() (lines 124-159).
     */
    private void removeSourceFiles(int partialIndex) {
        GroupFileInfo groupInfo = indexFile.groupInfos.get(partialIndex);
        if (groupInfo.srcFiles == null)
            return;

        log.info("Removing {} source files for group {}", groupInfo.srcFiles.size(), partialIndex + 1);
        for (FileInfo srcFile : groupInfo.srcFiles) {
            String path = PathUtils.combine(gameDirPath, srcFile.path);
            try {
                FileUtils.deleteFile(path);
            } catch (Exception e) {
                log.error("Delete source file failed, Path: {}", path, e);
            }
        }
    }

    /**
     * Calculate peak disk usage for remaining groups.
     * Corresponds to KRGroupApplyTask.CalcPeekDiskUsage() (lines 161-198).
     */
    private long calcPeekDiskUsage(int startIndex) {
        List<Long> srcSizes = new ArrayList<>();
        List<Long> dstSizes = new ArrayList<>();
        srcSizes.add(0L);

        for (int i = startIndex; i < indexFile.groupInfos.size(); i++) {
            GroupFileInfo groupInfo = indexFile.groupInfos.get(i);
            long srcSize = 0;
            long dstSize = 0;

            if (groupInfo.srcFiles != null) {
                for (FileInfo srcFile : groupInfo.srcFiles) {
                    String path = PathUtils.combine(gameDirPath, srcFile.path);
                    if (new File(path).exists()) {
                        srcSize += srcFile.size;
                    }
                }
            }
            if (groupInfo.dstFiles != null) {
                for (FileInfo dstFile : groupInfo.dstFiles) {
                    dstSize += dstFile.size;
                }
            }
            srcSizes.add(srcSize);
            dstSizes.add(dstSize);
        }

        long peekDiskUsage = 0;
        long cumulative = 0;
        for (int j = 0; j < dstSizes.size(); j++) {
            cumulative += dstSizes.get(j);
            cumulative -= srcSizes.get(j);
            if (cumulative > peekDiskUsage) {
                peekDiskUsage = cumulative;
            }
        }
        return peekDiskUsage;
    }

    private int getPatchedGroupIndex(String patchTempDir) {
        String configPath = PathUtils.combine(patchTempDir, ResourceConfigManager.LAUNCHER_APPLY_CONFIG);
        LauncherApplyConfig config = LauncherApplyConfig.get(configPath);
        return config != null ? config.patchedIndex : -1;
    }

    private void setPatchedGroupIndex(int groupIndex, String patchTempDir) {
        String configPath = PathUtils.combine(patchTempDir, ResourceConfigManager.LAUNCHER_APPLY_CONFIG);
        LauncherApplyConfig config = LauncherApplyConfig.get(configPath);
        if (config == null) {
            config = new LauncherApplyConfig();
        }
        config.state = LauncherApplyConfig.STATE_PATCHING;
        config.patchedIndex = groupIndex;
        LauncherApplyConfig.save(config, configPath);
    }

    private Map<String, FileInfo> buildResourceMapCaseInsensitive() {
        Map<String, FileInfo> map = new HashMap<>();
        if (indexFile.getResource() != null) {
            for (FileInfo fi : indexFile.getResource()) {
                if (fi.path != null) {
                    map.put(fi.path.toLowerCase(), fi);
                }
            }
        }
        return map;
    }

    private long getAvailableDiskSize() {
        try {
            File file = new File(downloadResPath);
            return file.getUsableSpace();
        } catch (Exception e) {
            log.warn("Failed to get disk size", e);
            return 0;
        }
    }

    public long getNeedDiskSize() {
        String patchTempDir = PathUtils.combine(downloadResPath, "krpdiff_temp");
        int partialIndex = getPatchedGroupIndex(patchTempDir) + 1;
        return calcPeekDiskUsage(partialIndex);
    }

    private GroupApplyResult buildResult() {
        if (applyFailFileInfos != null && !applyFailFileInfos.isEmpty()) {
            return new GroupApplyResult(false, moveRecords, applyFailFileInfos,
                    UpdateResult.APPLY_CHECK_MD5_NOT_MATCH, "UnZip File, Check Md5 Fail");
        }
        return new GroupApplyResult(true, moveRecords, null);
    }

    public interface GroupProgressCallback {
        void onProgress(int state, long completedSize, long totalSize, long completedCount, long totalCount);
    }

    public static class GroupApplyResult {
        public boolean success;
        public List<MoveFileRecord> moveRecords;
        public List<FileInfo> failFileInfos;
        public int errorCode;
        public String errorMessage;

        public GroupApplyResult(boolean success, List<MoveFileRecord> moveRecords, List<FileInfo> failFileInfos) {
            this(success, moveRecords, failFileInfos, 0, "");
        }

        public GroupApplyResult(boolean success, List<MoveFileRecord> moveRecords, List<FileInfo> failFileInfos,
                int errorCode, String errorMessage) {
            this.success = success;
            this.moveRecords = moveRecords;
            this.failFileInfos = failFileInfos;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }
}
