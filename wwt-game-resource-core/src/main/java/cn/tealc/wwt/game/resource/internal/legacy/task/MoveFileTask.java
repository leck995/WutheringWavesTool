package cn.tealc.wwt.game.resource.internal.legacy.task;

import cn.tealc.wwt.game.resource.internal.legacy.config.ResourceConfigManager;
import cn.tealc.wwt.game.resource.internal.legacy.model.*;
import cn.tealc.wwt.game.resource.internal.legacy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.util.*;

/**
 * File move task: moves downloaded files from cache to game directory.
 * Corresponds to KRMoveFileTask.cs.
 *
 * Critical behavior:
 * 1. After moving, persists the moved file list to LocalGameResources.json
 * via GameResourceRecord. Always persists (even if list is empty) to match C#.
 * 2. Continues iterating on FILE_MISSING errors (logs them). The first
 * non-FILE_MISSING exception is reported as the failure cause. A
 * FILE_MISSING exception is "sticky": once set, subsequent exceptions
 * are logged but do not replace it.
 * 3. Skips files already marked moved in the MoveFileRecord (resume support).
 * 4. Maps IOException subtypes to proper error codes:
 * - AccessDeniedException → ERROR_CODE_FILE_PERMISSION_DENY
 * - FileSystemException (lock/sharing) → ERROR_CODE_FILE_OCCUPANCY
 * - NoSuchFileException → ERROR_CODE_FILE_MISSING
 * 5. Progress notification throttling via configurable interval.
 */
public class MoveFileTask {
    private static final Logger log = LoggerFactory.getLogger(MoveFileTask.class);

    private final String gameDirPath;
    private final List<MoveFileRecord> moveRecords;
    private long progressNotifyIntervalMillis = 0;
    private long lastNotifyProgressTime = 0;

    public MoveFileTask(String gameDirPath, List<MoveFileRecord> moveRecords) {
        this.gameDirPath = gameDirPath;
        this.moveRecords = moveRecords != null ? moveRecords : new ArrayList<>();
    }

    public void setProgressNotifyIntervalMillis(long interval) {
        this.progressNotifyIntervalMillis = interval;
    }

    /**
     * Map an IOException to the appropriate error code.
     * Matches C# FileUtils.Move which throws with HRESULT matching
     * ERROR_CODE_FILE_OCCUPANCY, ERROR_CODE_FILE_PERMISSION_DENY, etc.
     */
    private int mapIOExceptionToErrorCode(Exception e) {
        if (e instanceof NoSuchFileException || e instanceof java.io.FileNotFoundException) {
            return UpdateResult.ERROR_CODE_FILE_MISSING;
        }
        if (e instanceof AccessDeniedException) {
            return UpdateResult.ERROR_CODE_FILE_PERMISSION_DENY;
        }
        if (e instanceof FileSystemException) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            // Check for sharing/lock violations (file occupancy)
            if (msg.contains("being used by another process")
                    || msg.contains("share violation")
                    || msg.contains("lock")
                    || msg.contains("occupied")) {
                return UpdateResult.ERROR_CODE_FILE_OCCUPANCY;
            }
            // Check for disk full
            if (msg.contains("no space") || msg.contains("disk full")
                    || msg.contains("not enough space")) {
                return UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE;
            }
            // Check for permission
            if (msg.contains("permission") || msg.contains("access") || msg.contains("denied")) {
                return UpdateResult.ERROR_CODE_FILE_PERMISSION_DENY;
            }
        }
        return UpdateResult.ERROR_CODE_FILE_OCCUPANCY; // Default for IO errors during move
    }

    /**
     * Move files from source directories to game directory.
     *
     * Progress metric is FILE COUNT (not byte size), matching C# KRMoveFileTask
     * which reports (numMoved, totalToMove) as integer counts.
     */
    public MoveResult run(MoveProgressCallback progressCallback) {
        try {
            // First pass: count files that still need to be moved (skip already-moved).
            long totalToMove = 0;
            for (MoveFileRecord record : moveRecords) {
                if (record.fileInfos == null)
                    continue;
                for (FileInfo fi : record.fileInfos) {
                    if (!record.getMoveFileResult(fi.path)) {
                        totalToMove++;
                    }
                }
            }

            MoveException moveException = null;
            FileInfo failFileInfo = null;
            long movedCount = 0;
            List<FileInfo> movedFiles = new ArrayList<>();

            // Second pass: attempt each pending move.
            for (MoveFileRecord record : moveRecords) {
                if (record.fileInfos == null)
                    continue;
                for (FileInfo fi : record.fileInfos) {
                    if (record.getMoveFileResult(fi.path)) {
                        // Already moved in a previous run; just notify progress.
                        notifyProgress(progressCallback, movedCount, totalToMove);
                        continue;
                    }

                    String srcPath = PathUtils.combine(record.srcDir, fi.path);
                    String dstPath = PathUtils.combine(gameDirPath, fi.path);
                    try {
                        FileUtils.ensureDir(PathUtils.getParentDir(dstPath));
                        Files.move(Path.of(srcPath), Path.of(dstPath), StandardCopyOption.REPLACE_EXISTING);
                        movedCount++;
                        record.updateMoveFileRecord(fi.path, true);
                        movedFiles.add(fi);
                        notifyProgress(progressCallback, movedCount, totalToMove);
                    } catch (Exception e) {
                        int code = mapIOExceptionToErrorCode(e);
                        if (moveException != null
                                && moveException.errorCode == UpdateResult.ERROR_CODE_FILE_MISSING) {
                            // Sticky FILE_MISSING: log only, don't replace.
                            log.info("Move File Has Fail (FILE_MISSING already set), ErrorMessage: {}, Path: {}",
                                    e.getMessage(), fi.path);
                        } else {
                            moveException = new MoveException(code, e.getMessage(), fi);
                            failFileInfo = fi;
                            if (code == UpdateResult.ERROR_CODE_FILE_MISSING) {
                                log.warn("Move file missing: {} -> {}", srcPath, dstPath);
                            } else {
                                log.error("Failed to move file (code={}): {} -> {}", code, srcPath, dstPath, e);
                            }
                        }
                    }
                }
            }

            // Persist moved files to LocalGameResources.json.
            // C# KRMoveFileTask.cs lines 122-135: always loads/creates record and saves,
            // even if list is empty.
            String configPath = PathUtils.combine(gameDirPath, ResourceConfigManager.LOCAL_INDEX_FILE_NAME);
            try {
                GameResourceRecord record = GameResourceRecord.get(configPath);
                if (record == null) {
                    record = new GameResourceRecord(movedFiles);
                } else {
                    record.updateResource(movedFiles);
                }
                GameResourceRecord.save(record, configPath);
                log.info("Updated LocalGameResources.json with {} moved files", movedFiles.size());
            } catch (Exception e) {
                log.warn("Failed to update LocalGameResources.json: {}", e.getMessage());
            }

            log.info("Move completed: {} files moved", movedCount);

            if (moveException == null) {
                return new MoveResult(true, 0, "", null);
            }
            return new MoveResult(false, moveException.errorCode,
                    moveException.message != null ? moveException.message : "",
                    moveException.failFile != null ? moveException.failFile.path : null);
        } catch (Exception e) {
            log.error("MoveFileTask failed", e);
            return new MoveResult(false, mapIOExceptionToErrorCode(e), e.getMessage(), null);
        }
    }

    /**
     * Notify progress with optional throttling.
     * Matches C# OnMoveProgressChanged which throttles by time interval.
     * Reports file COUNT (not byte size).
     */
    private void notifyProgress(MoveProgressCallback callback, long movedCount, long totalCount) {
        if (callback == null)
            return;
        if (progressNotifyIntervalMillis > 0) {
            long now = System.currentTimeMillis();
            if (now - lastNotifyProgressTime <= progressNotifyIntervalMillis
                    && movedCount != totalCount) {
                return; // Skip, too soon
            }
            lastNotifyProgressTime = now;
        }
        callback.onProgress(movedCount, totalCount);
    }

    public interface MoveProgressCallback {
        /** Reports file COUNT progress (movedCount, totalCount). */
        void onProgress(long movedCount, long totalCount);
    }

    public static class MoveResult {
        public boolean success;
        public int errorCode;
        public String errorMessage;
        public String failFilePath;

        public MoveResult(boolean success, int errorCode, String errorMessage, String failFilePath) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.failFilePath = failFilePath;
        }
    }

    /** Internal holder for the first captured move exception. */
    private static class MoveException {
        final int errorCode;
        final String message;
        final FileInfo failFile;

        MoveException(int errorCode, String message, FileInfo failFile) {
            this.errorCode = errorCode;
            this.message = message;
            this.failFile = failFile;
        }
    }
}
