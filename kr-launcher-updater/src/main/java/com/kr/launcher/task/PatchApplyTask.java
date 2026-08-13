package com.kr.launcher.task;

import com.kr.launcher.model.*;
import com.kr.launcher.patch.PatchExecutor;
import com.kr.launcher.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * HPatchZ binary patch apply task.
 * Corresponds to KRPatchApplyTask.cs (181 lines)
 *
 * Flow:
 * 1. Separate .krdiff files from regular resource files
 * 2. Create temp directory krdiff_temp/
 * 3. Check disk space
 * 4. Launch HPatchZ.exe via PatchExecutor
 * 5. Wait for completion, handle exit codes
 * 6. Verify MD5 of patched files
 * 7. Return move records for successful files
 */
public class PatchApplyTask {
    private static final Logger log = LoggerFactory.getLogger(PatchApplyTask.class);

    private final UpdateInfo updateInfo;
    private final String gameDirPath;
    private final String downloadResPath;
    private final IndexFile indexFile;
    private final long deCompressSize;
    private long currentDeCompressSize = 0;
    private long totalSize = 0;
    private long totalCount = 0;
    private final List<MoveFileRecord> moveFileRecords = new ArrayList<>();
    private List<FileInfo> applyFailFileInfos = null;
    private String hpatchzExePath;

    public PatchApplyTask(UpdateInfo updateInfo, String gameDirPath, String downloadResPath,
            IndexFile indexFile, long deCompressSize, String hpatchzExePath) {
        this.updateInfo = updateInfo;
        this.gameDirPath = gameDirPath;
        this.downloadResPath = downloadResPath;
        this.indexFile = indexFile;
        this.deCompressSize = deCompressSize;
        this.hpatchzExePath = hpatchzExePath;
    }

    /**
     * Execute the patch apply.
     * Corresponds to KRPatchApplyTask.ApplyFiles() (line 24-164)
     */
    public PatchApplyResult run(PatchProgressCallback progressCallback) {
        try {
            log.info("=== PatchApplyTask: Starting ===");

            // Step 1: Separate .krdiff files from regular resource files
            // Lines 28-49 in C#
            List<FileInfo> regularFiles = new ArrayList<>();
            if (indexFile.getResource() != null) {
                for (FileInfo fi : indexFile.getResource()) {
                    if (fi.path != null && fi.path.endsWith(".krdiff")) {
                        log.info("Skip KRDiff File Move, Path: {}", fi.path);
                    } else {
                        regularFiles.add(fi);
                    }
                }
            }

            // Add regular files to move records (they go directly to game dir)
            if (!regularFiles.isEmpty()) {
                moveFileRecords.add(new MoveFileRecord(downloadResPath, regularFiles));
            }

            // Step 2: Get patch info from index file.
            // C# KRPatchApplyTask.cs#L50: _updateIndexFile.PatchInfos[0] throws if empty,
            // caught by base class → failure. Match C# by returning failure.
            if (indexFile.patchInfos == null || indexFile.patchInfos.isEmpty()) {
                log.error("No patchInfos in index file, PatchApplyTask requires non-empty patchInfos");
                return new PatchApplyResult(false, moveFileRecords, null,
                        PatchExecutor.ERROR_CODE_START_PATCH_FAILED,
                        "PatchApplyTask requires non-empty patchInfos");
            }

            MixedFileInfo diffFileInfo = indexFile.patchInfos.get(0);
            String dest = diffFileInfo.dest;
            String diffPath = PathUtils.combine(downloadResPath, dest);
            String patchDirPath = PathUtils.combine(downloadResPath, "krdiff_temp");

            log.info("Patch dest: {}, diffPath: {}, patchDir: {}", dest, diffPath, patchDirPath);

            // Step 3: Create temp directory
            // Lines 53-57 in C#
            FileUtils.deleteDirectory(patchDirPath);
            FileUtils.ensureDir(patchDirPath);

            // Step 4: Check disk space
            // Lines 58-63 in C#
            long availableDisk = getAvailableDiskSize();
            if (availableDisk > 0 && availableDisk < deCompressSize) {
                return new PatchApplyResult(false, null, null,
                        PatchExecutor.ERROR_CODE_DISK_NOT_ENOUGH_SPACE,
                        "Disk Not Enough Space, AvailableSize: " + availableDisk + ", DeCompressSize: "
                                + deCompressSize);
            }

            // Step 5: Launch HPatchZ
            // Lines 64-163 in C#
            log.info("Starting HPatchZ patch process...");
            PatchExecutor executor = new PatchExecutor();
            CountDownLatch completionLatch = new CountDownLatch(1);
            final int[] exitCodeResult = { 0 };

            // Set progress callback
            executor.progressCallback = (patchedFileCount, fileTotalCount, patchingFileCurrBytes,
                    patchingFileTotalBytes, patchedCurrBytes, patchTotalBytes) -> {
                totalSize = patchTotalBytes;
                totalCount = fileTotalCount;
                currentDeCompressSize = patchedCurrBytes;
                log.debug("Patch progress: files={}/{}, bytes={}/{}",
                        patchedFileCount, fileTotalCount, patchedCurrBytes, patchTotalBytes);
                if (progressCallback != null) {
                    progressCallback.onProgress(2, patchedCurrBytes, patchTotalBytes, patchedFileCount, fileTotalCount);
                }
            };

            // Set finished callback
            executor.finishedCallback = (exitCode) -> {
                log.info("HPatchZ finished, exit code: {}", exitCode);
                exitCodeResult[0] = exitCode;
                completionLatch.countDown();
            };

            // C# KRPatchApplyTask.cs:163 calls StartAsyncEx (not StartAsync).
            // StartAsyncEx wraps StartAsync: on start failure
            // (non-PATCH_PROCESS_IS_RUNNING)
            // it invokes PatchFinished(START_PATCH_PROCESS_FAILED) via the callback
            // executor.
            // That exit code falls through to the default case below, which runs MD5 check
            // on the empty temp dir → all files fail → APPLY_CHECK_MD5_NOT_MATCH →
            // re-download.
            // This is graceful degradation: a hpatchz start failure does NOT fail the whole
            // update; instead it triggers re-download of all patched files.
            executor.startAsyncEx(
                    hpatchzExePath, gameDirPath, diffPath, patchDirPath, 1000);

            // Wait for HPatchZ to complete (no overall timeout — matches C# which only
            // polls progress every 1s with no maximum duration). On start failure,
            // finishedCallback is invoked by startAsyncEx with START_PATCH_PROCESS_FAILED,
            // which counts down the latch.
            log.info("Waiting for HPatchZ to complete...");
            completionLatch.await();

            int exitCode = exitCodeResult[0];

            // Step 6: Handle exit codes
            // Lines 75-159 in C#
            if (exitCode == PatchExecutor.EXIT_FILE_OCCUPANCY_1 || exitCode == PatchExecutor.EXIT_FILE_OCCUPANCY_2) {
                log.error("HPatchZ failed: File Occupancy (exit code {})", exitCode);
                return new PatchApplyResult(false, null, null,
                        PatchExecutor.ERROR_CODE_FILE_OCCUPANCY,
                        PatchExecutor.getExitCodeMessage(exitCode));
            }
            if (exitCode == PatchExecutor.EXIT_DISK_NOT_ENOUGH_SPACE) {
                log.error("HPatchZ failed: Disk Not Enough Space (exit code {})", exitCode);
                return new PatchApplyResult(false, null, null,
                        PatchExecutor.ERROR_CODE_DISK_NOT_ENOUGH_SPACE,
                        PatchExecutor.getExitCodeMessage(exitCode));
            }
            if (exitCode == PatchExecutor.EXIT_PERMISSION_DENIED_1
                    || exitCode == PatchExecutor.EXIT_PERMISSION_DENIED_2) {
                log.error("HPatchZ failed: Permission Denied (exit code {})", exitCode);
                return new PatchApplyResult(false, null, null,
                        PatchExecutor.ERROR_CODE_FILE_PERMISSION_DENY,
                        PatchExecutor.getExitCodeMessage(exitCode));
            }

            // Step 7: Verify MD5 of patched files
            // C# KRPatchApplyTask.cs#L89-94: first emit state=2 100% progress,
            // then run CheckFilesMd5WithProgress with state=3 progress callback.
            if (progressCallback != null) {
                progressCallback.onProgress(2, totalSize, totalSize, totalCount, totalCount);
            }

            log.info("HPatchZ completed successfully, verifying patched files...");
            // C# uses KRResourceHelper.CheckFilesMd5WithProgress with state=3
            // progress events during the post-patch MD5 verification phase.
            // checkFilesMd5WithProgress is synchronous — callbacks fire inline.
            final PatchProgressCallback cb = progressCallback;
            @SuppressWarnings("unchecked")
            List<FileInfo>[] failHolder = (List<FileInfo>[]) new List<?>[1];
            failHolder[0] = new ArrayList<>();
            ResourceHelper.checkFilesMd5WithProgress(
                    patchDirPath,
                    diffFileInfo.entries,
                    (completedSize, totalMd5Size) -> {
                        if (cb != null) {
                            // C# KRPatchApplyTask.cs#L94: state=3 for MD5 check progress.
                            cb.onProgress(3, completedSize, totalMd5Size, 0L, 0L);
                        }
                    },
                    (success, failFileInfos) -> failHolder[0] = failFileInfos != null ? failFileInfos
                            : new ArrayList<>());
            List<FileInfo> failedFiles = failHolder[0];

            if (failedFiles.isEmpty()) {
                // All files verified, add to move records
                if (diffFileInfo.entries != null) {
                    // Filter out files that also exist in resource (BugFix handling)
                    Map<String, FileInfo> resourceMap = new HashMap<>();
                    if (indexFile.getResource() != null) {
                        for (FileInfo fi : indexFile.getResource()) {
                            resourceMap.put(fi.path.toLowerCase(), fi);
                        }
                    }

                    List<FileInfo> filesToMove = new ArrayList<>();
                    for (FileInfo entry : diffFileInfo.entries) {
                        if (resourceMap.containsKey(entry.path.toLowerCase())) {
                            log.warn("BugFix File: {}, Use Resource File", entry.path);
                        } else {
                            filesToMove.add(entry);
                        }
                    }
                    if (!filesToMove.isEmpty()) {
                        moveFileRecords.add(new MoveFileRecord(patchDirPath, filesToMove));
                    }
                }

                log.info("Patch applied and verified successfully");
                return new PatchApplyResult(true, moveFileRecords, null);
            } else {
                // Some files failed verification
                log.warn("Patch verification failed for {} files", failedFiles.size());

                // Delete failed files from temp dir
                for (FileInfo failFile : failedFiles) {
                    try {
                        FileUtils.deleteFile(PathUtils.combine(patchDirPath, failFile.path));
                    } catch (Exception e) {
                        log.error("Failed to delete failed temp file: {}", failFile.path, e);
                    }
                }

                // Add successful files to move records
                Map<String, FileInfo> failMap = new HashMap<>();
                for (FileInfo fi : failedFiles) {
                    failMap.put(fi.path, fi);
                }

                Map<String, FileInfo> resourceMap = new HashMap<>();
                if (indexFile.getResource() != null) {
                    for (FileInfo fi : indexFile.getResource()) {
                        resourceMap.put(fi.path.toLowerCase(), fi);
                    }
                }

                List<FileInfo> successFiles = new ArrayList<>();
                applyFailFileInfos = new ArrayList<>();
                long failSize = 0;

                if (diffFileInfo.entries != null) {
                    for (FileInfo entry : diffFileInfo.entries) {
                        if (resourceMap.containsKey(entry.path.toLowerCase())) {
                            log.warn("BugFix File: {}, Use Resource File", entry.path);
                        } else if (failMap.containsKey(entry.path)) {
                            applyFailFileInfos.add(entry);
                            failSize += entry.size;
                        } else {
                            successFiles.add(entry);
                        }
                    }
                }

                // C# KRPatchApplyTask.cs:151-152 always adds the move record
                // (even if list3 is empty). Match that behavior.
                moveFileRecords.add(new MoveFileRecord(patchDirPath, successFiles));

                log.warn("Patch completed: {} success, {} fail, fail total size: {}",
                        successFiles.size(), failedFiles.size(), failSize);

                return new PatchApplyResult(false, moveFileRecords, applyFailFileInfos,
                        UpdateResult.APPLY_CHECK_MD5_NOT_MATCH, "UnZip File, Check Md5 Fail");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("PatchApplyTask interrupted", e);
            return new PatchApplyResult(false, null, null, -1, "Interrupted");
        } catch (Exception e) {
            log.error("PatchApplyTask failed", e);
            return new PatchApplyResult(false, null, null, ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    /**
     * Verify MD5 of patched files against expected values.
     * Replaced by ResourceHelper.checkFilesMd5WithProgress with state=3
     * progress events (matches C# KRPatchApplyTask.cs#L90-94).
     */

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
        long remaining = deCompressSize - currentDeCompressSize;
        return remaining > 0 ? remaining : 0;
    }

    public interface PatchProgressCallback {
        void onProgress(int state, long completedSize, long totalSize, long completedCount, long totalCount);
    }

    public static class PatchApplyResult {
        public boolean success;
        public List<MoveFileRecord> moveRecords;
        public List<FileInfo> failFileInfos;
        public int errorCode;
        public String errorMessage;

        public PatchApplyResult(boolean success, List<MoveFileRecord> moveRecords,
                List<FileInfo> failFileInfos) {
            this(success, moveRecords, failFileInfos, 0, "");
        }

        public PatchApplyResult(boolean success, List<MoveFileRecord> moveRecords,
                List<FileInfo> failFileInfos, int errorCode, String errorMessage) {
            this.success = success;
            this.moveRecords = moveRecords;
            this.failFileInfos = failFileInfos;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }
}
