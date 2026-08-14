package com.kr.launcher.task;

import com.kr.launcher.model.FileChunkInfo;
import com.kr.launcher.model.FileInfo;
import com.kr.launcher.model.FileModifyTimeEntry;
import com.kr.launcher.model.ResourceError;
import com.kr.launcher.util.MD5Utils;
import com.kr.launcher.util.PathUtils;
import com.kr.launcher.util.ResourceModifyHelper;
import com.kr.launcher.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Chunk-level MD5 verification with timeout, white-list, and modify-time cache.
 * Corresponds to KRFileChunkCheckTask.cs.
 *
 * Three-stage verification:
 * 1. File size check (if enabled)
 * 2. Key file full MD5 check (parallel, 4 threads)
 * 3. Random chunk MD5 check (parallel, 4 threads) — skips files whose modify
 * time is unchanged since last check
 *
 * A timeout (default 10s) cancels all checks and returns success (matches C#
 * behavior: OnResultCallback(success=true, ...) on timeout).
 */
public class FileChunkCheckTask {
    private static final Logger log = LoggerFactory.getLogger(FileChunkCheckTask.class);

    public interface FileChunkCheckProgressCallback {
        void onProgress(int progressPercent);
    }

    public interface FileChunkCheckResultCallback {
        void onResult(boolean success, int errCode, String errMessage, String path);
    }

    private final String baseDestPath;
    private final String modifyTimeCacheFilePath;
    private final List<String> keyFileCheckList;
    private final List<FileInfo> allFileInfoList;
    private final int timeOut;
    private final boolean fileChunkCheckSwitch;
    private final boolean fileSizeCheckSwitch;
    private final boolean fileModifyTimeCheckSwitch;
    private final String fileCheckWhiteListConfig;
    private final FileChunkCheckProgressCallback progressCallback;
    private final FileChunkCheckResultCallback resultCallback;

    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private List<String> fileCheckWhiteList;

    public FileChunkCheckTask(String baseDestPath,
            String modifyTimeCacheFilePath,
            List<String> keyFileCheckList,
            List<FileInfo> fileInfoList,
            int timeOut,
            boolean fileChunkCheckSwitch,
            boolean fileSizeCheckSwitch,
            String fileCheckWhiteListConfig,
            boolean fileModifyTimeCheckSwitch,
            FileChunkCheckProgressCallback progressCallback,
            FileChunkCheckResultCallback resultCallback) {
        this.baseDestPath = baseDestPath;
        this.modifyTimeCacheFilePath = modifyTimeCacheFilePath;
        this.keyFileCheckList = keyFileCheckList;
        this.allFileInfoList = fileInfoList;
        this.timeOut = timeOut;
        this.fileChunkCheckSwitch = fileChunkCheckSwitch;
        this.fileSizeCheckSwitch = fileSizeCheckSwitch;
        this.fileModifyTimeCheckSwitch = fileModifyTimeCheckSwitch;
        this.fileCheckWhiteListConfig = fileCheckWhiteListConfig;
        this.progressCallback = progressCallback;
        this.resultCallback = resultCallback;
    }

    public void run() {
        if (fileCheckWhiteListConfig != null && !fileCheckWhiteListConfig.isEmpty()) {
            fileCheckWhiteList = Arrays.asList(fileCheckWhiteListConfig.split(":"));
        }

        Thread thread = new Thread(() -> {
            ExecutorService scheduler = Executors.newFixedThreadPool(4);
            ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
            AtomicBoolean cancelled = new AtomicBoolean(false);

            final int[] indexHolder = { 0 };
            timer.scheduleAtFixedRate(() -> {
                indexHolder[0]++;
                log.info("File Chunk Check Cost: {}s, Callback Progress", indexHolder[0]);
                onProgress((int) ((double) indexHolder[0] / timeOut * 100.0));
                if (indexHolder[0] >= timeOut) {
                    log.info("Check Res TimeOut: {}s", timeOut);
                    cancelled.set(true);
                    timer.shutdownNow();
                    onResult(true, 0, "TimeOut", null);
                }
            }, 1, 1, TimeUnit.SECONDS);

            try {
                // Stage 1: file size check
                if (fileSizeCheckSwitch) {
                    log.info("Start Check File Size");
                    for (FileInfo fi : allFileInfoList) {
                        if (cancelled.get())
                            break;
                        if (fileCheckWhiteList != null && fileCheckWhiteList.contains(fi.path)) {
                            log.info("Skip White File Size Check, Path: {}", fi.path);
                        } else {
                            File file = new File(PathUtils.combine(baseDestPath, fi.path));
                            if (!file.exists()) {
                                log.error("Target File Not Exist, Path is {}", fi.path);
                                timer.shutdownNow();
                                cancelled.set(true);
                                onResult(false, ResourceError.CHUNK_CHECK_ERROR_FILE_NOT_EXIST,
                                        "File Not Exist", fi.path);
                                return;
                            }
                            if (file.length() != fi.size) {
                                log.error("Target File Size Error, Path is {}, Expect: {}, Actual: {}",
                                        fi.path, fi.size, file.length());
                                timer.shutdownNow();
                                cancelled.set(true);
                                onResult(false, ResourceError.CHUNK_CHECK_ERROR_FILE_SIZE_NOT_MATCH,
                                        "File MD5 Not Match", fi.path);
                                return;
                            }
                        }
                    }
                }

                // Stage 2: key file full MD5 check
                Map<String, FileInfo> allFileInfoMap = new HashMap<>();
                if (allFileInfoList != null) {
                    for (FileInfo fi : allFileInfoList) {
                        allFileInfoMap.put(fi.path, fi);
                    }
                }
                List<Future<?>> keyFileFutures = new ArrayList<>();
                for (String keyFilePath : keyFileCheckList) {
                    if (cancelled.get())
                        break;
                    final String keyPath = keyFilePath;
                    keyFileFutures.add(scheduler.submit(() -> {
                        if (allFileInfoMap.containsKey(keyPath)) {
                            FileInfo fi = allFileInfoMap.get(keyPath);
                            String filePath = PathUtils.combine(baseDestPath, fi.path);
                            String md5;
                            try {
                                md5 = MD5Utils.getFileMD5(filePath);
                            } catch (Exception e) {
                                timer.shutdownNow();
                                cancelled.set(true);
                                onResult(false, ExceptionUtils.getHResult(e), "Get File Md5 Fail", fi.path);
                                return;
                            }
                            if (md5 == null || !md5.equalsIgnoreCase(fi.md5)) {
                                timer.shutdownNow();
                                cancelled.set(true);
                                onResult(false, ResourceError.CHUNK_CHECK_ERROR_FILE_MD5_NOT_MATCH,
                                        "File Md5 Not Match", fi.path);
                            } else {
                                log.info("Key File Check Succ, Path is {}", keyPath);
                            }
                        } else {
                            log.error("Key File FileInfo Not Exist, Skip Check This File, Path is {}", keyPath);
                        }
                    }));
                }
                try {
                    for (Future<?> f : keyFileFutures) {
                        if (cancelled.get())
                            break;
                        f.get();
                    }
                } catch (Exception ignored) {
                }

                // Stage 3: chunk MD5 check
                if (fileChunkCheckSwitch && !cancelled.get()) {
                    log.info("Start Check File Chunk Md5");
                    List<FileModifyTimeEntry> modifyTimeEntries = new ArrayList<>();
                    Map<String, Long> modifyTimeDict = new HashMap<>();
                    if (fileModifyTimeCheckSwitch) {
                        modifyTimeEntries = ResourceModifyHelper.loadModifyTimeEntries(modifyTimeCacheFilePath);
                        if (modifyTimeEntries != null) {
                            for (FileModifyTimeEntry e : modifyTimeEntries) {
                                modifyTimeDict.put(e.path, e.modifyTime);
                            }
                        }
                    }

                    List<Future<?>> chunkFutures = new ArrayList<>();
                    for (FileInfo targetFileInfo : allFileInfoList) {
                        if (cancelled.get())
                            break;
                        if (fileCheckWhiteList != null && fileCheckWhiteList.contains(targetFileInfo.path)) {
                            log.info("Skip White File Chunk Check, Path: {}", targetFileInfo.path);
                            continue;
                        }
                        final FileInfo target = targetFileInfo;
                        final String filePath = PathUtils.combine(baseDestPath, target.path);
                        final Long[] modifyTimestamp = new Long[1];
                        if (fileModifyTimeCheckSwitch) {
                            try {
                                modifyTimestamp[0] = new File(filePath).lastModified();
                            } catch (Exception e) {
                                log.error("Failed to get last write time of file: {}", filePath, e);
                            }
                            if (modifyTimestamp[0] != null
                                    && modifyTimeDict.containsKey(target.path)
                                    && modifyTimeDict.get(target.path).equals(modifyTimestamp[0])) {
                                log.info("The file modification time has not changed, skip File Chunk Check, Path: {}",
                                        target.path);
                                continue;
                            }
                        }
                        chunkFutures.add(scheduler.submit(() -> {
                            if (target.chunkInfos != null && target.chunkInfos.size() > 0) {
                                int count = target.chunkInfos.size();
                                int chunkIndex = new Random().nextInt(count);
                                FileChunkInfo chunkInfo = target.chunkInfos.get(chunkIndex);
                                String chunkMd5 = MD5Utils.getFileChunkMd5(filePath, chunkInfo.start, chunkInfo.end);
                                if (chunkMd5 == null || !chunkMd5.equalsIgnoreCase(chunkInfo.md5)) {
                                    log.error("Check File Chunk Md5 Fail, Path is {}, ChunkIndex: {}", target.path,
                                            chunkIndex);
                                    timer.shutdownNow();
                                    cancelled.set(true);
                                    onResult(false, ResourceError.CHUNK_CHECK_ERROR_FILE_MD5_NOT_MATCH,
                                            "File Chunk Md5 Not Match", target.path);
                                } else {
                                    if (modifyTimestamp[0] != null) {
                                        modifyTimeDict.put(target.path, modifyTimestamp[0]);
                                    }
                                    log.info("Check File Chunk Md5 Succ, Path is {}, ChunkIndex: {}", target.path,
                                            chunkIndex);
                                }
                            } else {
                                String fullMd5 = MD5Utils.safeGetFileMd5(filePath);
                                if (fullMd5 == null || !fullMd5.equalsIgnoreCase(target.md5)) {
                                    timer.shutdownNow();
                                    cancelled.set(true);
                                    onResult(false, ResourceError.CHUNK_CHECK_ERROR_FILE_MD5_NOT_MATCH,
                                            "File Md5 Not Match", target.path);
                                } else {
                                    if (modifyTimestamp[0] != null) {
                                        modifyTimeDict.put(target.path, modifyTimestamp[0]);
                                    }
                                    log.info("Check File Md5 Succ, Path is {}", target.path);
                                }
                            }
                        }));
                    }
                    try {
                        for (Future<?> f : chunkFutures) {
                            if (cancelled.get())
                                break;
                            f.get();
                        }
                    } catch (Exception ignored) {
                    }
                    if (fileModifyTimeCheckSwitch && !chunkFutures.isEmpty()) {
                        log.info("Save modify time cache file {}", modifyTimeCacheFilePath);
                        List<FileModifyTimeEntry> entries = new ArrayList<>();
                        for (Map.Entry<String, Long> kv : modifyTimeDict.entrySet()) {
                            FileModifyTimeEntry entry = new FileModifyTimeEntry();
                            entry.path = kv.getKey();
                            entry.modifyTime = kv.getValue();
                            entries.add(entry);
                        }
                        ResourceModifyHelper.saveModifyTimeEntries(modifyTimeCacheFilePath, entries);
                    }
                }

                if (!isFinished.get() && !cancelled.get()) {
                    onProgress(100);
                    timer.shutdownNow();
                    onResult(true, 0, "", null);
                }
            } finally {
                scheduler.shutdownNow();
                if (!timer.isShutdown()) {
                    timer.shutdownNow();
                }
            }
        }, "FileChunkCheckTask");
        thread.setDaemon(true);
        thread.start();
    }

    private void onProgress(int percent) {
        if (progressCallback != null) {
            progressCallback.onProgress(percent);
        }
    }

    private void onResult(boolean success, int errCode, String errMessage, String path) {
        // Use compareAndSet to ensure only the first caller delivers the result.
        // Prevents duplicate delivery from timeout + check-failure race.
        if (isFinished.compareAndSet(false, true)) {
            log.info("Res Check Result: succ: {}, code: {}, message: {}, path: {}", success, errCode, errMessage, path);
            if (resultCallback != null) {
                resultCallback.onResult(success, errCode, errMessage, path);
            }
        }
    }
}
