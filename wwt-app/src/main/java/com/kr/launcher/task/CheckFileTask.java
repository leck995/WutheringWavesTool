package com.kr.launcher.task;

import com.kr.launcher.model.FileInfo;
import com.kr.launcher.util.ExceptionUtils;
import com.kr.launcher.util.ResourceHelper;

import java.util.List;

/**
 * Verifies file MD5s against the index file using KRResourceHelper.
 * Corresponds to KRCheckFileTask.cs.
 *
 * Wraps ResourceHelper.checkFilesMultiMd5WithProgress and caches results so
 * repeated Run() calls return the cached result without re-checking.
 */
public class CheckFileTask {
    public interface CheckFileProgressCallback {
        void onProgress(long completedSize, long totalSize);
    }

    public interface CheckFileResultCallback {
        void onResult(boolean success, List<FileInfo> failFileInfos, int cSharpErrorCode);
    }

    private final String baseDestPath;
    private final List<FileInfo> checkFileList;
    private final List<FileInfo> localFileList;
    private final List<String> resourcesExcludePathList;
    private final List<String> resourcesExcludeWhitePathList;
    private final CheckFileProgressCallback progressCallback;
    private final CheckFileResultCallback resultCallback;

    private boolean isFinished = false;
    private long cacheTotalSize;
    private boolean cacheResult = true;
    private List<FileInfo> cacheFailFileInfos;

    public CheckFileTask(String baseDestPath,
            List<FileInfo> checkFileList,
            List<FileInfo> localFileList,
            List<String> resourcesExcludePathList,
            List<String> resourcesExcludeWhitePathList,
            CheckFileProgressCallback progressCallback,
            CheckFileResultCallback resultCallback) {
        this.baseDestPath = baseDestPath;
        this.checkFileList = checkFileList;
        this.localFileList = localFileList;
        this.progressCallback = progressCallback;
        this.resultCallback = resultCallback;
        this.resourcesExcludePathList = resourcesExcludePathList;
        this.resourcesExcludeWhitePathList = resourcesExcludeWhitePathList;
    }

    public void run() {
        if (isFinished && cacheFailFileInfos != null) {
            progressCallback.onProgress(cacheTotalSize, cacheTotalSize);
            resultCallback.onResult(cacheResult, cacheFailFileInfos, 0);
            return;
        }
        isFinished = false;

        Thread thread = new Thread(() -> {
            try {
                ResourceHelper.checkFilesMultiMd5WithProgress(
                        baseDestPath,
                        checkFileList,
                        localFileList,
                        resourcesExcludePathList,
                        resourcesExcludeWhitePathList,
                        (completedSize, totalSize) -> {
                            cacheTotalSize = totalSize;
                            progressCallback.onProgress(completedSize, totalSize);
                        },
                        (success, failFileInfos) -> {
                            cacheResult = success;
                            cacheFailFileInfos = failFileInfos;
                            isFinished = true;
                            resultCallback.onResult(success, failFileInfos, 0);
                        });
            } catch (Exception e) {
                resultCallback.onResult(false, new java.util.ArrayList<>(), ExceptionUtils.getHResult(e));
            }
        }, "CheckFileTask");
        thread.setDaemon(true);
        thread.start();
    }
}
