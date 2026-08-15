package cn.tealc.wwt.game.resource.internal.legacy.task;

import cn.tealc.wwt.game.resource.internal.legacy.model.FileInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.IndexFile;
import cn.tealc.wwt.game.resource.internal.legacy.model.MoveFileRecord;
import cn.tealc.wwt.game.resource.internal.legacy.model.UpdateInfo;
import cn.tealc.wwt.game.resource.internal.legacy.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Abstract base class for all apply tasks (Patch, Zip, Group, Nop).
 * Corresponds to KRResources/KRApplyTask.cs.
 *
 * Subclasses implement {@link #applyFiles()} and {@link #getNeedDiskSize()}.
 * The Run() method executes applyFiles() asynchronously and routes results
 * through the progress/result callbacks.
 *
 * Existing Java tasks (PatchApplyTask, ZipApplyTask, GroupApplyTask) were
 * implemented before this base class and have their own callback interfaces.
 * This base class is used by NopApplyTask and can be adopted by other tasks
 * in future refactoring.
 */
public abstract class ApplyTask {
    private static final Logger log = LoggerFactory.getLogger(ApplyTask.class);

    protected boolean isFinished;
    protected String gameDirPath;
    protected IndexFile updateIndexFile;
    protected final UpdateInfo updateInfo;
    protected final String downloadResPath;
    protected List<MoveFileRecord> moveFileRecords;
    protected List<FileInfo> applyFailFileInfos;
    protected List<FileInfo> updateInfoFileList;
    protected long totalSize;
    protected long totalCount;

    private boolean success;
    private int errorCode;
    private String errorMessage = "";

    private final ApplyProgressCallBack progressCallback;
    private final ApplyResultCallback resultCallback;

    public ApplyTask(UpdateInfo updateInfo, String gameDirPath, String downloadResPath,
            IndexFile updateIndexFile, List<FileInfo> updateInfoFileList,
            ApplyProgressCallBack progressCallback, ApplyResultCallback resultCallback) {
        this.updateInfo = updateInfo;
        this.gameDirPath = gameDirPath;
        this.downloadResPath = downloadResPath;
        this.updateIndexFile = updateIndexFile;
        this.progressCallback = progressCallback;
        this.resultCallback = resultCallback;
        this.updateInfoFileList = updateInfoFileList;
    }

    /**
     * Run the apply task. If already finished, replays the cached result.
     */
    public void run() {
        if (isFinished) {
            onApplyProgressCallback(2, totalSize, totalSize, totalCount, totalCount);
            onApplyResultCallback(success, moveFileRecords, applyFailFileInfos, errorCode, errorMessage);
            return;
        }
        new Thread(() -> {
            success = false;
            moveFileRecords = null;
            applyFailFileInfos = null;
            try {
                applyFiles();
            } catch (Exception e) {
                onApplyResultCallback(false, null, null, ExceptionUtils.getHResult(e), e.getMessage());
            }
        }, "ApplyTask").start();
    }

    /**
     * Get available disk space for the download path.
     */
    protected long getDiskAvailableSize() {
        try {
            File file = new File(downloadResPath);
            return file.getUsableSpace();
        } catch (Exception e) {
            log.warn("Check Disk Size Fail: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Subclasses implement the actual apply logic.
     */
    public abstract void applyFiles();

    /**
     * Subclasses return the remaining disk space needed.
     */
    public abstract long getNeedDiskSize();

    protected void onApplyProgressCallback(int state, long completedSize, long totalSize,
            long completedCount, long totalCount) {
        if (progressCallback != null) {
            progressCallback.onProgress(state, completedSize, totalSize, completedCount, totalCount);
        }
    }

    protected void onApplyResultCallback(boolean succ, List<MoveFileRecord> moveFileRecords,
            List<FileInfo> applyFailFileInfoList, int errorCode, String errorMessage) {
        this.moveFileRecords = moveFileRecords;
        this.applyFailFileInfos = applyFailFileInfoList;
        this.success = succ;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        if (resultCallback != null) {
            resultCallback.onResult(succ, moveFileRecords, applyFailFileInfoList, errorCode, errorMessage);
        }
    }

    // ==================== Callbacks ====================

    public interface ApplyProgressCallBack {
        void onProgress(int state, long completedSize, long totalSize, long completedCount, long totalCount);
    }

    public interface ApplyResultCallback {
        void onResult(boolean succ, List<MoveFileRecord> moveFileRecords,
                List<FileInfo> applyFailFileInfoList, int errorCode, String errorMessage);
    }
}
