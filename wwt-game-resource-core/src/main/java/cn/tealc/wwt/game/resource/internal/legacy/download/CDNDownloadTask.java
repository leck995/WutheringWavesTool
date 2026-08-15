package cn.tealc.wwt.game.resource.internal.legacy.download;

import cn.tealc.wwt.game.resource.internal.legacy.model.DownloadInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.FileInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.CdnConfig;
import cn.tealc.wwt.game.resource.internal.legacy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CDN selection and download orchestration.
 * Corresponds to KRCDNDownloadTask.
 *
 * Features:
 * 1. CDN selection via speed test (bytes/ms * K1 - P * K2)
 * 2. Non-selected CDNs passed as backup URLs to each DownloadTask
 * 3. Disk space check before download (with diskSpaceCalculationRatio)
 * 4. Configurable CDN test duration and read block timeout
 * 5. Retry count = maxRetryCount * cdnCount - 1
 */
public class CDNDownloadTask {
    private static final Logger log = LoggerFactory.getLogger(CDNDownloadTask.class);

    private final List<CdnConfig> cdnConfigList;
    private final List<DownloadInfo> downloadInfoList;
    private final String basePath;
    private final String baseDestPath;
    private final AtomicBoolean pauseFlag = new AtomicBoolean(false);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private ProgressCallback progressCallback;
    private StateCallback stateCallback;
    private DownloadTask.Md5CheckProgressCallback md5CheckProgressCallback;
    // Aggregate state mirrored from per-file DownloadTask callbacks so pause()
    // and resume() can emit PAUSED/DOWNLOADING synchronously without waiting
    // for the next state callback from the in-flight task.
    private DownloadState state = DownloadState.IDLE;
    private int selectedCDNIndex = 0;
    // CDN url → speed-test score, populated by selectCDN().
    // Corresponds to C# KRCDNDownloadTask.cdnIdentityMap. Used by
    // buildBackupBaseUrls() to emit backups in descending-score order
    // (C# KRCDNDownloadTask.cs:190-202 OrderByDescending).
    private final Map<String, Double> cdnScoreMap = new LinkedHashMap<>();
    // All per-file DownloadTasks built for the current run (for pause/resume/stop
    // delegation). Corresponds to C# KRMultiDownloadTask.DownloadTasks.
    // Populated before parallel submission, cleared after run completes.
    private final List<DownloadTask> allTasks = new ArrayList<>();
    // Maximum number of files downloaded in parallel.
    // Corresponds to C# KRDownloadDispatcher._maxParallelRunningCount = 4.
    private int maxParallelCount = 4;
    // Progress notification throttle (matches C# KRMultiDownloadTask which
    // throttles by ProgressNotifyIntervalMillis = 100ms).
    private static final long PROGRESS_NOTIFY_INTERVAL_MS = 100L;
    private final Object progressNotifyLock = new Object();
    private long lastProgressNotifyTimeMs = 0L;
    private long lastNotifiedReceivedBytes = -1L;

    private int maxRetryCount = Integer.MAX_VALUE;
    private long cdnSelectTestDuration = 3000L;
    private double diskSpaceCalculationRatio = 1.2;
    // HTTP read timeout (ms) propagated to per-file DownloadTask instances.
    // Corresponds to C# KRCDNDownloadTask readBlockTimeout.
    private long readBlockTimeout = 0L;
    // Last error code from a failed download (propagated from DownloadTask or
    // set directly for disk-space check failure). Used by UpdateFlow for error
    // type mapping (NETWORK vs RETRY vs DISK_FULL vs UNKNOWN).
    private int lastErrorCode = 0;
    // C# HRESULT from the underlying exception (propagated from DownloadTask).
    // Corresponds to C# KRDownloadStateChangedEventArgs.CSharpErrorCode.
    // Used by ResourcesDownloadTask to detect proxy exceptions for retry.
    private int lastCSharpErrorCode = 0;
    // Tracks whether the multi-file download has completed successfully.
    // Corresponds to C# KRCDNDownloadTask.IsDownloadFinish().
    private boolean isFinished = false;
    // Propagated to each DownloadTask after proxy exception retry.
    private boolean disableProxy = false;

    public void setDisableProxy(boolean disable) {
        this.disableProxy = disable;
    }

    public CDNDownloadTask(List<CdnConfig> cdnList, List<DownloadInfo> downloadInfoList,
            String basePath, String baseDestPath) {
        this.cdnConfigList = new ArrayList<>(cdnList);
        this.downloadInfoList = downloadInfoList;
        this.basePath = basePath;
        this.baseDestPath = baseDestPath;
    }

    public void setProgressCallback(ProgressCallback cb) {
        this.progressCallback = cb;
    }

    public void setStateCallback(StateCallback cb) {
        this.stateCallback = cb;
    }

    /**
     * Set the MD5 check progress callback.
     * Corresponds to C# KRCDNDownloadTask.SetMd5CheckProgressCallback.
     * Aggregates per-file MD5 progress across all files in downloadInfoList.
     */
    public void setMd5CheckProgressCallback(DownloadTask.Md5CheckProgressCallback cb) {
        this.md5CheckProgressCallback = cb;
    }

    public void setMaxRetryCount(int max) {
        this.maxRetryCount = max;
    }

    public void setCdnSelectTestDuration(long duration) {
        this.cdnSelectTestDuration = duration;
    }

    public void setDiskSpaceCalculationRatio(double ratio) {
        this.diskSpaceCalculationRatio = ratio;
    }

    public void setReadBlockTimeout(long timeout) {
        this.readBlockTimeout = timeout;
    }

    public long getReadBlockTimeout() {
        return readBlockTimeout;
    }

    /**
     * Returns true if the multi-file download has completed successfully.
     * Corresponds to C# KRCDNDownloadTask.IsDownloadFinish().
     */
    public boolean isDownloadFinish() {
        return isFinished;
    }

    /**
     * Get the remaining disk size required for this download.
     * Corresponds to C# KRMultiDownloadTask.GetNeedDiskSize() which returns
     * the REMAINING size (totalSize - alreadyDownloaded), not the total.
     *
     * When download is in progress: returns totalSize - sum(downloadedBytes).
     * When download is finished: returns sum of file sizes for tasks whose
     * state is not COMPLETE (files still being verified).
     * Returns 0 if negative (defensive clamp matching C# line 815-818).
     */
    public long getNeedDiskSize() {
        long totalSize = 0;
        for (DownloadInfo info : downloadInfoList) {
            totalSize += info.fileSize;
        }

        if (isFinished) {
            // C# line 795-803: when IsDownloadFinish, sum file sizes for tasks
            // whose state is NOT COMPLETE (e.g., still in MD5 check).
            long need = 0;
            synchronized (allTasks) {
                for (int i = 0; i < allTasks.size() && i < downloadInfoList.size(); i++) {
                    DownloadTask task = allTasks.get(i);
                    if (task.getState() != DownloadState.COMPLETE) {
                        need += downloadInfoList.get(i).fileSize;
                    }
                }
            }
            return Math.max(0, need);
        }

        // C# line 806-813: when in progress, remaining = total - downloaded.
        long downloaded = 0;
        synchronized (allTasks) {
            for (DownloadTask task : allTasks) {
                downloaded += task.getDownloadedBytes();
            }
        }
        long remaining = totalSize - downloaded;
        return Math.max(0, remaining);
    }

    /**
     * Get a stable task ID. Corresponds to C# KRCDNDownloadTask.Id.
     */
    public String getId() {
        return "CDNDownloadTask-" + System.identityHashCode(this);
    }

    /**
     * Pause the multi-file download. Emits PAUSED state immediately if currently
     * DOWNLOADING, matching C# KRMultiDownloadTask.Pause() which calls
     * OnDownloadStateChanged synchronously rather than waiting for the in-flight
     * DownloadTask's read loop to notice the pause flag.
     */
    public void pause() {
        pauseFlag.set(true);
        synchronized (allTasks) {
            for (DownloadTask task : allTasks) {
                task.pause();
            }
        }
        if (state == DownloadState.DOWNLOADING) {
            state = DownloadState.PAUSED;
            if (stateCallback != null) {
                stateCallback.onStateChanged(DownloadState.PAUSED, null);
            }
        }
    }

    /**
     * Resume the multi-file download. Emits DOWNLOADING state immediately if
     * currently PAUSED, matching C# KRMultiDownloadTask.Resume().
     */
    public void resume() {
        pauseFlag.set(false);
        synchronized (allTasks) {
            for (DownloadTask task : allTasks) {
                task.resume();
            }
        }
        if (state == DownloadState.PAUSED) {
            state = DownloadState.DOWNLOADING;
            if (stateCallback != null) {
                stateCallback.onStateChanged(DownloadState.DOWNLOADING, null);
            }
        }
    }

    public void stop() {
        stopFlag.set(true);
        resume();
        synchronized (allTasks) {
            for (DownloadTask task : allTasks) {
                task.stop();
            }
        }
    }

    /**
     * Get the last error code (matches C# KRDownloadError values).
     * Returns 0 if no error occurred.
     */
    public int getErrorCode() {
        return lastErrorCode;
    }

    /**
     * Get the last C# HRESULT error code (0 if none).
     * Corresponds to C# KRDownloadStateChangedEventArgs.CSharpErrorCode.
     * Propagated from the underlying DownloadTask.
     */
    public int getCSharpErrorCode() {
        return lastCSharpErrorCode;
    }

    /**
     * CDN selection algorithm.
     * Corresponds to KRCDNDownloadTask.StartCDNSelectDownload + CalculateCDNValue.
     *
     * C# creates a KRMultiDownloadTask with the ENTIRE _downloadInfoList (all
     * files) for each CDN, runs it in parallel for _cdnSelectTestDuration ms,
     * then stops and records total bytes downloaded. Score =
     * (bytesDownloaded / timeMs) * K1 - P * K2. Using only the first file (as the
     * previous Java port did) misjudges CDNs that throttle single connections
     * but parallelize well, and under-tests CDN throughput on multi-file
     * updates.
     */
    private int selectCDN() {
        // C# KRCDNDownloadTask.cs:225-233: if no CDN is selected (empty
        // cdnIdentityMap), emits FAILED with UNKNOWN error and "BaseUrl is
        // Empty" message. Return -1 to signal this to the caller.
        if (cdnConfigList.isEmpty())
            return -1;
        if (cdnConfigList.size() == 1)
            return 0;

        log.info("Starting CDN selection with {} CDNs, {} test files",
                cdnConfigList.size(), downloadInfoList.size());

        // Shuffle CDN list (matches C# ShufferCdnConfigList).
        List<CdnConfig> shuffled = new ArrayList<>(cdnConfigList);
        ListUtils.shuffle(shuffled);

        double[] scores = new double[shuffled.size()];
        cdnScoreMap.clear();

        for (int i = 0; i < shuffled.size(); i++) {
            CdnConfig cdn = shuffled.get(i);
            try {
                long[] result = testCDNWithAllFiles(cdn);
                long bytesDownloaded = result[0];
                long elapsed = result[1];

                // Match C# exactly: clamp time to >=1, integer division for speed (bytes/ms).
                long timeMs = elapsed <= 0 ? 1L : elapsed;
                long speed = bytesDownloaded / timeMs;
                // Integer arithmetic: long * int = long, int * int = int, long - int = long
                double score = (double) (speed * cdn.k1 - cdn.p * cdn.k2);
                scores[i] = score;
                cdnScoreMap.put(cdn.url, score);
                log.info("CDN {} score: speed(bytes/ms)={}, K1={}, P={}, K2={}, score={}",
                        cdn.url, speed, cdn.k1, cdn.p, cdn.k2, score);
            } catch (Exception e) {
                log.warn("CDN test failed for {}: {}", cdn.url, e.getMessage());
                scores[i] = Double.NEGATIVE_INFINITY;
                cdnScoreMap.put(cdn.url, Double.NEGATIVE_INFINITY);
            }
        }

        // Find best CDN (highest score wins).
        int bestIndex = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                bestIndex = i;
            }
        }

        // Map back to original index
        CdnConfig bestCdn = shuffled.get(bestIndex);
        int originalIndex = cdnConfigList.indexOf(bestCdn);
        log.info("Selected CDN: {} (score: {})", bestCdn.url, bestScore);
        return Math.max(0, originalIndex);
    }

    /**
     * Test a single CDN by downloading ALL files in parallel for
     * cdnSelectTestDuration ms, then stopping. Returns {totalBytes, elapsedMs}.
     * Corresponds to C# KRCDNDownloadTask.StartCDNSelectDownload which builds
     * a KRMultiDownloadTask with the full _downloadInfoList.
     *
     * Downloaded bytes are NOT written to disk (matches C# which sets
     * SkipMergeChunk=true and discards the partial data after stopping).
     */
    private long[] testCDNWithAllFiles(CdnConfig cdn) {
        int threadCount = Math.min(maxParallelCount, downloadInfoList.size());
        if (threadCount <= 0)
            threadCount = 1;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "CDNTest-" + cdn.url);
            t.setDaemon(true);
            return t;
        });
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicBoolean stopTest = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>();
        for (DownloadInfo info : downloadInfoList) {
            String effectiveBasePath = info.basePath != null ? info.basePath : basePath;
            String testUrl = UrlUtils.appendPath(
                    UrlUtils.appendPath(cdn.url, effectiveBasePath), info.url);
            futures.add(pool.submit(() -> {
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) URI.create(UrlUtils.encodeUrlPath(testUrl)).toURL().openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.setRequestProperty("Range", "bytes=0-");
                    conn.setRequestProperty("Accept-Encoding", "identity");
                    try (InputStream is = conn.getInputStream()) {
                        byte[] buffer = new byte[65536];
                        while (!stopTest.get()) {
                            int read = is.read(buffer);
                            if (read <= 0)
                                break;
                            totalBytes.addAndGet(read);
                        }
                    }
                } catch (Exception e) {
                    log.debug("CDN test stream ended for {}: {}", testUrl, e.getMessage());
                } finally {
                    if (conn != null) {
                        try {
                            conn.disconnect();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }));
        }

        // Wait for the test duration, then signal stop.
        try {
            Thread.sleep(cdnSelectTestDuration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        stopTest.set(true);

        // Forcefully interrupt any blocked reads.
        pool.shutdownNow();
        try {
            pool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        return new long[] { totalBytes.get(), elapsed };
    }

    /**
     * Build list of backup CDN URLs sorted by descending score.
     * Corresponds to C# KRCDNDownloadTask.cs:190-202 — iterates cdnIdentityMap
     * in OrderByDescending order, skipping the selected (primary) CDN. Result
     * is [2nd-best, 3rd-best, ..., worst] so failover tries the next-fastest
     * CDN first.
     */
    private List<String> buildBackupBaseUrls() {
        String primaryUrl = cdnConfigList.get(selectedCDNIndex).url;
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(cdnScoreMap.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> backups = new ArrayList<>();
        for (Map.Entry<String, Double> e : sorted) {
            if (!e.getKey().equals(primaryUrl)) {
                backups.add(e.getKey());
            }
        }
        return backups;
    }

    /**
     * Check disk space before download.
     * Corresponds to C# KRCDNDownloadTask disk space check (lines 291-318).
     */
    private boolean checkDiskSpace(long requiredSize) {
        if (baseDestPath == null || baseDestPath.isEmpty()) {
            return true;
        }
        try {
            File destDir = new File(baseDestPath);
            long usableSpace = destDir.getUsableSpace();
            if (usableSpace <= 0) {
                return true; // Can't determine, allow download
            }
            long needSize = (long) (requiredSize * diskSpaceCalculationRatio);
            if (usableSpace - needSize <= 0) {
                log.error("Disk space check fail: usable={}, required={} (with ratio {})",
                        usableSpace, needSize, diskSpaceCalculationRatio);
                return false;
            }
            log.info("Disk space check pass: usable={}, required={}", usableSpace, needSize);
            return true;
        } catch (Exception e) {
            log.warn("Disk space check failed: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Execute downloads with CDN selection.
     */
    public void run() {
        // C# KRCDNDownloadTask emits DOWNLOADING state at start of Enqueue(),
        // before CDN selection. This ensures the UI reflects the active download
        // state during the (potentially slow) CDN speed test phase.
        state = DownloadState.DOWNLOADING;
        if (stateCallback != null) {
            stateCallback.onStateChanged(DownloadState.DOWNLOADING, null);
        }
        // Reset progress throttle state for this run.
        lastProgressNotifyTimeMs = 0L;
        lastNotifiedReceivedBytes = -1L;

        if (downloadInfoList.isEmpty()) {
            log.info("No files to download");
            isFinished = true;
            state = DownloadState.COMPLETE;
            if (stateCallback != null)
                stateCallback.onStateChanged(DownloadState.COMPLETE, null);
            return;
        }

        // Disk space check before download
        long totalRequiredSize = 0;
        for (DownloadInfo info : downloadInfoList) {
            totalRequiredSize += info.fileSize;
        }
        if (!checkDiskSpace(totalRequiredSize)) {
            // C# KRMultiDownloadTask.cs:354: sets both ErrorCode and CSharpErrorCode
            // to ERROR_DISK_FULL (-2147024784) on disk space check failure.
            lastErrorCode = DownloadError.DISK_SPACE_CHECK_FAIL;
            lastCSharpErrorCode = RetryHelper.ERROR_DISK_FULL;
            state = DownloadState.FAILED;
            if (stateCallback != null) {
                stateCallback.onStateChanged(DownloadState.FAILED, "Disk space not enough");
            }
            return;
        }

        // Select best CDN
        selectedCDNIndex = selectCDN();
        // C# KRCDNDownloadTask.cs:225-233: if no CDN selected (text == null),
        // emit FAILED with UNKNOWN error and "BaseUrl is Empty" message.
        if (selectedCDNIndex < 0) {
            lastErrorCode = DownloadError.UNKNOWN;
            lastCSharpErrorCode = 0;
            state = DownloadState.FAILED;
            if (stateCallback != null) {
                stateCallback.onStateChanged(DownloadState.FAILED, "BaseUrl is Empty");
            }
            return;
        }
        CdnConfig primaryCDN = cdnConfigList.get(selectedCDNIndex);
        List<String> backupBaseUrls = buildBackupBaseUrls();

        // Retry count = maxRetryCount * cdnCount - 1 (matches C# exactly, no clamp)
        int perFileRetryCount = maxRetryCount == Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : maxRetryCount * cdnConfigList.size() - 1;

        log.info("Starting download with CDN: {}, {} files, {} backup CDNs, maxRetries={}, parallel={}",
                primaryCDN.url, downloadInfoList.size(), backupBaseUrls.size(), perFileRetryCount,
                Math.min(maxParallelCount, downloadInfoList.size()));

        long totalBytes = 0;
        for (DownloadInfo info : downloadInfoList) {
            totalBytes += info.fileSize;
        }

        // Per-file progress slots for thread-safe aggregation across parallel
        // downloads. Corresponds to C# KRMultiDownloadTask._singleReceivedBytesSizeList
        // and _singleMd5CheckBytesSizeList.
        final AtomicLongArray perFileDownloaded = new AtomicLongArray(downloadInfoList.size());
        final AtomicLongArray perFileMd5Checked = new AtomicLongArray(downloadInfoList.size());

        synchronized (allTasks) {
            allTasks.clear();
        }

        // Build all per-file DownloadTasks upfront (matches C# BuildDownloadTasks).
        for (int i = 0; i < downloadInfoList.size(); i++) {
            DownloadInfo info = downloadInfoList.get(i);
            String effectiveBasePath = info.basePath != null ? info.basePath : basePath;
            String fileUrl = UrlUtils.appendPath(
                    UrlUtils.appendPath(primaryCDN.url, effectiveBasePath), info.url);
            // C# KRMultiDownloadTask.BuildDestFilePath always combines
            // BaseDestPath with the relative FilePath. Java must do the same:
            // info.destPath holds the RELATIVE path (set by
            // ResourceHelper.transformFileInfoToDownloadInfo), and baseDestPath
            // is the absolute destination directory.
            String relativePath = info.destPath != null ? info.destPath : info.url;
            String destPath = PathUtils.combine(baseDestPath, relativePath);

            List<String> backupFileUrls = new ArrayList<>();
            for (String backupBaseUrl : backupBaseUrls) {
                backupFileUrls.add(UrlUtils.appendPath(
                        UrlUtils.appendPath(backupBaseUrl, effectiveBasePath), info.url));
            }

            DownloadInfo dlInfo = new DownloadInfo(fileUrl, destPath, info.fileSize, info.md5);
            dlInfo.chunkInfoList = info.chunkInfoList;

            log.info("Built task [{}/{}]: {} -> {} ({} bytes, chunks={})",
                    i + 1, downloadInfoList.size(), info.url, destPath, info.fileSize,
                    info.chunkInfoList != null ? info.chunkInfoList.size() : 0);

            DownloadTask task = new DownloadTask(dlInfo, destPath, backupFileUrls, perFileRetryCount);
            task.setCheckContentLength(true);
            task.setCheckContentEncoding(true);
            task.setDisableProxy(disableProxy);
            if (readBlockTimeout > 0) {
                task.setReadBlockTimeout(readBlockTimeout);
            }

            final int fileIndex = i;
            final long finalTotalBytes = totalBytes;
            task.setProgressCallback((downloaded, total) -> {
                perFileDownloaded.set(fileIndex, downloaded);
                if (progressCallback != null) {
                    long sum = 0;
                    for (int j = 0; j < perFileDownloaded.length(); j++) {
                        sum += perFileDownloaded.get(j);
                    }
                    // C# KRMultiDownloadTask.RefreshAndNotifyDownloadProgressInfo
                    // throttles: only notify if (now - lastNotifyTime >
                    // ProgressNotifyIntervalMillis) OR (sum == totalDownloadSize),
                    // AND (sum != lastNotifiedReceivedBytes).
                    synchronized (progressNotifyLock) {
                        long now = System.currentTimeMillis();
                        if ((now - lastProgressNotifyTimeMs > PROGRESS_NOTIFY_INTERVAL_MS
                                || sum == finalTotalBytes)
                                && sum != lastNotifiedReceivedBytes) {
                            lastProgressNotifyTimeMs = now;
                            lastNotifiedReceivedBytes = sum;
                            progressCallback.onProgress(sum, finalTotalBytes);
                        }
                    }
                }
            });

            if (md5CheckProgressCallback != null) {
                final long md5Total = totalBytes;
                task.setMd5CheckProgressCallback((completed, fileTotal) -> {
                    perFileMd5Checked.set(fileIndex, completed);
                    long sum = 0;
                    for (int j = 0; j < perFileMd5Checked.length(); j++) {
                        sum += perFileMd5Checked.get(j);
                    }
                    md5CheckProgressCallback.onProgress(sum, md5Total);
                });
            }

            synchronized (allTasks) {
                allTasks.add(task);
            }
        }

        // Submit all tasks to a fixed thread pool (matches C# KRDownloadDispatcher
        // with _maxParallelRunningCount = 4).
        int threadCount = Math.min(maxParallelCount, downloadInfoList.size());
        if (threadCount <= 0)
            threadCount = 1;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "CDNDownload-worker");
            t.setDaemon(true);
            return t;
        });

        state = DownloadState.DOWNLOADING;

        // Track first failure for error reporting (matches C#
        // OnSingleDownloadStateChanged
        // which forwards the first FAILED state and calls InnerStop to cancel others).
        final AtomicBoolean failureLogged = new AtomicBoolean(false);
        final AtomicReference<DownloadState> firstFailureState = new AtomicReference<>(null);
        final AtomicInteger firstFailureCode = new AtomicInteger(0);
        final AtomicInteger firstFailureCSharpCode = new AtomicInteger(0);
        final AtomicReference<String> firstFailureUrl = new AtomicReference<>(null);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < allTasks.size(); i++) {
            final int fileIndex = i;
            final DownloadInfo info = downloadInfoList.get(fileIndex);
            final DownloadTask task = allTasks.get(fileIndex);
            futures.add(pool.submit(() -> {
                long fileStartTime = System.currentTimeMillis();
                int chunkCount = info.chunkInfoList != null ? info.chunkInfoList.size() : 0;
                log.info("[FILE START] [{}/{}] {} ({}{})",
                        fileIndex + 1, downloadInfoList.size(), info.url,
                        ByteUtils.byteConvert(info.fileSize),
                        chunkCount > 0 ? ", " + chunkCount + " chunks" : "");
                try {
                    task.run();
                } finally {
                    // Mark fully downloaded for accurate aggregate progress
                    // even if the task completed via cached/existing file path.
                    perFileDownloaded.set(fileIndex, info.fileSize);
                }
                long elapsed = System.currentTimeMillis() - fileStartTime;
                if (task.getState() == DownloadState.COMPLETE) {
                    log.info("[FILE DONE] [{}/{}] {} in {}",
                            fileIndex + 1, downloadInfoList.size(), info.url,
                            formatDuration(elapsed));
                } else if (task.getState() == DownloadState.FAILED) {
                    log.warn("[FILE FAILED] [{}/{}] {} after {}",
                            fileIndex + 1, downloadInfoList.size(), info.url,
                            formatDuration(elapsed));
                }
                if (task.getState() == DownloadState.FAILED || task.getState() == DownloadState.CANCELED) {
                    if (failureLogged.compareAndSet(false, true)) {
                        firstFailureState.set(task.getState());
                        firstFailureCode.set(task.getErrorCode());
                        firstFailureCSharpCode.set(task.getCSharpErrorCode());
                        firstFailureUrl.set(info.url);
                        // Emit FAILED/CANCELED state immediately (matches C#
                        // KRCDNDownloadTask.OnDownloadStateChanged which
                        // forwards the state synchronously via
                        // _downloadStateChanged?.Invoke, not deferred until all
                        // tasks complete).
                        lastErrorCode = firstFailureCode.get();
                        lastCSharpErrorCode = firstFailureCSharpCode.get();
                        state = firstFailureState.get();
                        if (stateCallback != null) {
                            stateCallback.onStateChanged(firstFailureState.get(),
                                    "Download failed: " + firstFailureUrl.get());
                        }
                        // Cancel all other in-flight tasks (matches C# InnerStop).
                        synchronized (allTasks) {
                            for (DownloadTask other : allTasks) {
                                if (other != task) {
                                    other.stop();
                                }
                            }
                        }
                    }
                }
            }));
        }

        // Wait for all tasks to complete.
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ee) {
                log.error("Worker threw", ee.getCause());
            }
        }
        pool.shutdown();
        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        synchronized (allTasks) {
            allTasks.clear();
        }

        // Report outcome.
        if (firstFailureState.get() != null) {
            // State already emitted immediately when the first failure was
            // detected (above). Only update error codes/state fields here.
            log.error("Download failed for: {}", firstFailureUrl.get());
            lastErrorCode = firstFailureCode.get();
            lastCSharpErrorCode = firstFailureCSharpCode.get();
            state = firstFailureState.get();
            return;
        }

        if (stopFlag.get()) {
            state = DownloadState.CANCELED;
            if (stateCallback != null) {
                stateCallback.onStateChanged(DownloadState.CANCELED, null);
            }
            return;
        }

        log.info("All downloads completed");
        isFinished = true;
        state = DownloadState.COMPLETE;
        if (stateCallback != null) {
            stateCallback.onStateChanged(DownloadState.COMPLETE, null);
        }
    }

    public interface ProgressCallback {
        void onProgress(long downloaded, long total);
    }

    public interface StateCallback {
        void onStateChanged(DownloadState state, String error);
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m" + remainingSeconds + "s";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h" + remainingMinutes + "m";
    }
}
