package com.kr.launcher.task;

import com.kr.launcher.model.*;
import com.kr.launcher.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Zip decompression apply task.
 * Corresponds to KRZipApplyTask.cs (185 lines).
 *
 * Flow:
 * 1. Separate .krzip files from regular resource files; add regular files to
 * move records
 * 2. Delete and recreate krzip_temp directory
 * 3. Check disk space against deCompressSize
 * 4. Decompress each zip with progress tracking
 * 5. Verify MD5 of all decompressed files via CheckFilesMd5WithProgress
 * 6. Filter BugFix files (entries that also exist in resource list)
 * 7. On MD5 failure: delete failed files, move successful files, report error
 */
public class ZipApplyTask {
    private static final Logger log = LoggerFactory.getLogger(ZipApplyTask.class);

    private final UpdateInfo updateInfo;
    private final String gameDirPath;
    private final String downloadResPath;
    private final IndexFile indexFile;
    private final long deCompressSize;
    private long currentDeCompressSize = 0;
    private long totalSize = 0;
    private long totalCount = 0;

    public ZipApplyTask(UpdateInfo updateInfo, String gameDirPath, String downloadResPath,
            IndexFile indexFile, long deCompressSize) {
        this.updateInfo = updateInfo;
        this.gameDirPath = gameDirPath;
        this.downloadResPath = downloadResPath;
        this.indexFile = indexFile;
        this.deCompressSize = deCompressSize;
    }

    /**
     * Apply zip files by decompressing them.
     * Corresponds to KRZipApplyTask.ApplyFiles() (line 22-156)
     */
    public ApplyResult run(ApplyProgressCallback progressCallback) {
        try {
            log.info("=== ZipApplyTask: Starting ===");

            List<MoveFileRecord> moveRecords = new ArrayList<>();

            // Step 1: Separate .krzip files from regular resource files (C# lines 26-47).
            // Regular (non-.krzip) resource files go directly to move records.
            List<FileInfo> regularFiles = new ArrayList<>();
            if (indexFile.getResource() != null) {
                for (FileInfo fi : indexFile.getResource()) {
                    if (fi.path != null && fi.path.endsWith(".krzip")) {
                        log.info("Skip KRZip File Move, Path: {}", fi.path);
                    } else {
                        regularFiles.add(fi);
                    }
                }
            }
            if (!regularFiles.isEmpty()) {
                moveRecords.add(new MoveFileRecord(downloadResPath, regularFiles));
            }

            // Step 2: Delete and recreate krzip_temp directory (C# lines 48-50).
            // CRITICAL: must use "krzip_temp" (not "krdiff_temp") to match what
            // PrepareTask's moving-state recovery expects.
            String decompressFileTempPath = PathUtils.combine(downloadResPath, "krzip_temp");
            FileUtils.deleteDirectory(decompressFileTempPath);
            FileUtils.ensureDir(decompressFileTempPath);

            // Step 3: Check disk space (C# lines 51-56).
            long availableDisk = getAvailableDiskSize();
            if (availableDisk > 0 && availableDisk < deCompressSize) {
                return new ApplyResult(false, null, null,
                        UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE,
                        "Disk Not Enough Space, AvailableSize: " + availableDisk
                                + ", DeCompressSize: " + deCompressSize);
            }

            // Step 4: Calculate total decompress count/size for progress (C# lines 58-69).
            List<MixedFileInfo> zipInfos = indexFile.zipInfos;
            if (zipInfos == null || zipInfos.isEmpty()) {
                log.info("No zipInfos, returning with direct move records only");
                return new ApplyResult(true, moveRecords, null);
            }

            long zipFileDeCompressTotalCount = 0;
            long zipFileDeCompressTotalSize = 0;
            List<FileInfo> deCompressFileInfoList = new ArrayList<>();
            for (MixedFileInfo zipInfo : zipInfos) {
                if (zipInfo.entries != null) {
                    for (FileInfo entry : zipInfo.entries) {
                        zipFileDeCompressTotalCount++;
                        zipFileDeCompressTotalSize += entry.size;
                        deCompressFileInfoList.add(entry);
                    }
                }
            }
            totalSize = zipFileDeCompressTotalSize;
            totalCount = zipFileDeCompressTotalCount;

            // Step 5: Decompress each zip (C# lines 70-101).
            // C# pattern: only abort on retryable errors (disk-full, permission);
            // for non-retryable errors, log and continue to next zip.
            long currentDeCompressCount = 0;
            long currentDeCompressSize = 0;
            for (MixedFileInfo mixFileInfo : zipInfos) {
                String archiveFile = PathUtils.combine(downloadResPath, mixFileInfo.dest);
                log.info("Decompressing zip: {}", archiveFile);

                try {
                    decompressZip(archiveFile, decompressFileTempPath,
                            progressCallback, currentDeCompressSize, zipFileDeCompressTotalSize,
                            currentDeCompressCount, zipFileDeCompressTotalCount);
                } catch (IOException e) {
                    int errorCode = mapIOExceptionToErrorCode(e);
                    log.error("Decompress failed for zip: {}, errorCode={}", archiveFile, errorCode, e);
                    // Only abort on retryable errors (disk-full, permission). Other errors
                    // continue to the next zip (matches C# CanRetry logic).
                    if (canRetry(errorCode)) {
                        // C# KRZipApplyTask.cs:92: OnApplyResultCallback(succ: false, null,
                        // null, errorCode, errorMessage) — base class sets
                        // _moveFileRecords = null, discarding the regular-files move
                        // record added earlier. Match C# by passing null.
                        return new ApplyResult(false, null, null, errorCode,
                                "Decompress failed: " + e.getMessage());
                    }
                    // Non-retryable: skip this zip, but still advance progress counters
                    // (C# KRZipApplyTask.cs#L87-100 updates counters for both success
                    // and non-retryable failure in the else branch).
                    currentDeCompressCount += getEntriesCount(mixFileInfo);
                    currentDeCompressSize += mixFileInfo.getEntriesSize();
                    this.currentDeCompressSize = currentDeCompressSize;
                    if (progressCallback != null) {
                        progressCallback.onProgress(2, currentDeCompressSize, zipFileDeCompressTotalSize,
                                currentDeCompressCount, zipFileDeCompressTotalCount);
                    }
                    continue;
                } catch (Exception e) {
                    log.error("Decompress threw for zip: {}", archiveFile, e);
                    // Non-retryable: still advance progress counters.
                    currentDeCompressCount += getEntriesCount(mixFileInfo);
                    currentDeCompressSize += mixFileInfo.getEntriesSize();
                    this.currentDeCompressSize = currentDeCompressSize;
                    if (progressCallback != null) {
                        progressCallback.onProgress(2, currentDeCompressSize, zipFileDeCompressTotalSize,
                                currentDeCompressCount, zipFileDeCompressTotalCount);
                    }
                    continue;
                }

                currentDeCompressCount += getEntriesCount(mixFileInfo);
                currentDeCompressSize += mixFileInfo.getEntriesSize();
                this.currentDeCompressSize = currentDeCompressSize;
                if (progressCallback != null) {
                    progressCallback.onProgress(2, currentDeCompressSize, zipFileDeCompressTotalSize,
                            currentDeCompressCount, zipFileDeCompressTotalCount);
                }
            }

            // Notify decompress complete (C# line 102).
            if (progressCallback != null) {
                progressCallback.onProgress(2, zipFileDeCompressTotalCount, zipFileDeCompressTotalCount,
                        zipFileDeCompressTotalCount, zipFileDeCompressTotalCount);
            }

            // Step 6: Verify MD5 of all decompressed files (C# lines 103-155).
            log.info("Verifying MD5 of {} decompressed files...", deCompressFileInfoList.size());
            // C# KRZipApplyTask does NOT pre-emit a state=3 (0, totalSize) event;
            // the first state=3 progress comes from inside CheckFilesMd5WithProgress's
            // callback. Match C# by not emitting here.

            // C# KRZipApplyTask uses KRResourceHelper.CheckFilesMd5WithProgress
            // for post-decompress verification with state=3 progress events.
            @SuppressWarnings("unchecked")
            List<FileInfo>[] failHolder = (List<FileInfo>[]) new List<?>[1];
            failHolder[0] = new ArrayList<>();
            final ApplyProgressCallback zipCb = progressCallback;
            ResourceHelper.checkFilesMd5WithProgress(
                    decompressFileTempPath,
                    deCompressFileInfoList,
                    (completedSize, totalMd5Size) -> {
                        if (zipCb != null) {
                            zipCb.onProgress(3, completedSize, totalMd5Size, 0, 0);
                        }
                    },
                    (success, failFileInfos) -> failHolder[0] = failFileInfos != null ? failFileInfos
                            : new ArrayList<>());
            List<FileInfo> failedFiles = failHolder[0];

            if (progressCallback != null) {
                progressCallback.onProgress(3, zipFileDeCompressTotalCount, zipFileDeCompressTotalCount, 0, 0);
            }

            if (failedFiles.isEmpty()) {
                // All files verified - add to move records (C# lines 109-127).
                Map<String, FileInfo> resourceMap = buildResourceMapCaseInsensitive();
                List<FileInfo> filesToMove = new ArrayList<>();
                for (FileInfo entry : deCompressFileInfoList) {
                    if (resourceMap.containsKey(entry.path.toLowerCase())) {
                        log.warn("BugFix File: {}, Use Resource File", entry.path);
                    } else {
                        filesToMove.add(entry);
                    }
                }
                // C# KRZipApplyTask.cs:124-125 always adds the move record
                // (even if list2 is empty). Match that behavior.
                moveRecords.add(new MoveFileRecord(decompressFileTempPath, filesToMove));
                log.info("Zip apply and MD5 verification completed successfully");
                return new ApplyResult(true, moveRecords, null);
            } else {
                // Some files failed verification (C# lines 129-154).
                log.warn("Zip MD5 verification failed for {} files", failedFiles.size());

                // C# does NOT delete failed files from the temp dir — they are
                // simply excluded from the move record and left behind for
                // later cleanup when the temp dir is deleted.

                // Categorize: BugFix files, failed files, successful files.
                Map<String, FileInfo> resourceMap = buildResourceMapCaseInsensitive();
                Map<String, FileInfo> failMap = new HashMap<>();
                for (FileInfo fi : failedFiles) {
                    failMap.put(fi.path, fi);
                }

                List<FileInfo> successFiles = new ArrayList<>();
                List<FileInfo> applyFailFileInfos = new ArrayList<>();

                for (FileInfo entry : deCompressFileInfoList) {
                    if (resourceMap.containsKey(entry.path.toLowerCase())) {
                        log.warn("BugFix File: {}, Use Resource File", entry.path);
                    } else if (failMap.containsKey(entry.path)) {
                        applyFailFileInfos.add(entry);
                    } else {
                        successFiles.add(entry);
                    }
                }

                // C# always adds the move record (even when successFiles is
                // empty) — KRMoveFileTask iterates an empty list as a no-op.
                moveRecords.add(new MoveFileRecord(decompressFileTempPath, successFiles));

                log.warn("Zip apply completed: {} success, {} fail",
                        successFiles.size(), applyFailFileInfos.size());

                return new ApplyResult(false, moveRecords, applyFailFileInfos,
                        UpdateResult.APPLY_CHECK_MD5_NOT_MATCH, "UnZip File, Check Md5 Fail");
            }

        } catch (Exception e) {
            log.error("ZipApplyTask failed", e);
            return new ApplyResult(false, null, null, ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    /**
     * Decompress a zip file to the target directory with progress tracking.
     * Returns [decompressCount, decompressSize].
     */
    private long[] decompressZip(String zipPath, String destDir,
            ApplyProgressCallback progressCallback,
            long currentSize, long totalSize,
            long currentCount, long totalCount) throws IOException {
        long decompressCount = 0;
        long decompressSize = 0;

        try (ZipFile zip = new ZipFile(zipPath)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryFile = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                    continue;
                }

                entryFile.getParentFile().mkdirs();
                try (InputStream is = zip.getInputStream(entry);
                        FileOutputStream fos = new FileOutputStream(entryFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        decompressSize += read;
                    }
                }

                decompressCount++;
                if (progressCallback != null) {
                    progressCallback.onProgress(2,
                            currentSize + decompressSize, totalSize,
                            currentCount + decompressCount, totalCount);
                }
            }
        }

        return new long[] { decompressCount, decompressSize };
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

    private int getEntriesCount(MixedFileInfo info) {
        return info.entries != null ? info.entries.size() : 0;
    }

    /**
     * Map an IOException during decompression to an error code matching C#
     * KRZipApplyTask.CanRetry semantics. Disk-full and permission errors are
     * retryable; other errors are non-retryable.
     */
    private int mapIOExceptionToErrorCode(IOException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (e instanceof java.nio.file.AccessDeniedException
                || msg.contains("permission") || msg.contains("access") || msg.contains("denied")) {
            return UpdateResult.ERROR_CODE_FILE_PERMISSION_DENY;
        }
        if (msg.contains("no space") || msg.contains("disk full") || msg.contains("not enough space")) {
            return UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE;
        }
        // Default: non-retryable, generic occupancy code.
        return UpdateResult.ERROR_CODE_FILE_OCCUPANCY;
    }

    /**
     * Whether the apply pipeline should abort on this error code.
     * Matches C# KRZipApplyTask.CanRetry: true only for disk-full and permission.
     */
    private boolean canRetry(int errorCode) {
        return errorCode == UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE
                || errorCode == UpdateResult.ERROR_CODE_FILE_PERMISSION_DENY;
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
        long remaining = deCompressSize - currentDeCompressSize;
        return remaining > 0 ? remaining : 0;
    }

    public interface ApplyProgressCallback {
        void onProgress(int state, long completedSize, long totalSize, long completedCount, long totalCount);
    }

    public static class ApplyResult {
        public boolean success;
        public List<MoveFileRecord> moveRecords;
        public List<FileInfo> failFileInfos;
        public int errorCode;
        public String errorMessage;

        public ApplyResult(boolean success, List<MoveFileRecord> moveRecords, List<FileInfo> failFileInfos) {
            this(success, moveRecords, failFileInfos, 0, "");
        }

        public ApplyResult(boolean success, List<MoveFileRecord> moveRecords, List<FileInfo> failFileInfos,
                int errorCode, String errorMessage) {
            this.success = success;
            this.moveRecords = moveRecords;
            this.failFileInfos = failFileInfos;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }
}
