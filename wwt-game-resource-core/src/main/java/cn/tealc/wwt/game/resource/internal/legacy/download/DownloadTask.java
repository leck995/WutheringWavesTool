package cn.tealc.wwt.game.resource.internal.legacy.download;

import cn.tealc.wwt.game.resource.BandwidthLimiter;
import cn.tealc.wwt.game.resource.internal.legacy.model.ChunkInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.DownloadInfo;
import cn.tealc.wwt.game.resource.internal.legacy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single file downloader with HTTP Range resume, backup URL failover,
 * content-length/encoding checks, and optional chunk-based download.
 * Corresponds to KRDownloadTask + KRDownloadCall.
 */
public class DownloadTask {
    private static final Logger log = LoggerFactory.getLogger(DownloadTask.class);

    private final DownloadInfo downloadInfo;
    private final String destPath;
    private final String primaryUrl;
    private final List<String> backUpUrls;
    private final BandwidthLimiter bandwidthLimiter;
    private DownloadState state = DownloadState.IDLE;
    private final AtomicBoolean pauseFlag = new AtomicBoolean(false);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private ProgressCallback progressCallback;
    private StateCallback stateCallback;
    private Md5CheckProgressCallback md5CheckProgressCallback;
    private long downloadedBytes = 0;
    // Throttle for MD5 check progress notifications (ms).
    // Corresponds to C# KRDownloadConfiguration.ProgressNotifyIntervalMillis =
    // 100L.
    private long lastMd5NotifyTimeMs = 0;
    private static final long MD5_NOTIFY_INTERVAL_MS = 100L;
    // Throttle for download progress notifications (ms).
    // Corresponds to C# KRMultiDownloadTask.RefreshAndNotifyDownloadProgressInfo
    // which checks (now - _lastNotifyProgressTime > ProgressNotifyIntervalMillis).
    private long lastProgressNotifyTimeMs = 0;
    private static final long PROGRESS_NOTIFY_INTERVAL_MS = 100L;
    // C# KRDownloadConfiguration.DownloadReadBufferSize = 65536 (64KB).
    // Using 8KB caused excessive read syscalls on high-bandwidth links.。
    private static final int READ_BUFFER_SIZE = 65536;

    private int maxRetryCount = Integer.MAX_VALUE;
    private boolean checkContentLength = false;
    private boolean checkContentEncoding = false;
    // C# KRDownloadTask.DisableDownloadRange — when true, do not send Range
    // header and do not attempt resume. Used for index file downloads.
    private boolean disableDownloadRange = false;
    // C# KRDownloadTask.DisableCheckFileSize — when true, skip post-download
    // file size verification. Used for index file downloads where size may
    // not be known ahead of time.
    private boolean disableCheckFileSize = false;
    // C# KRDownloadConfiguration.CheckMd5WhenSizeIsSame (default false).
    // When false, pre-existing files with matching size skip MD5 check and
    // emit COMPLETE immediately (matching C# KRDownloadTask.Enqueue line 124).
    // When true, the file goes through the normal download+MD5-check path.
    private boolean checkMd5WhenSizeIsSame = false;
    private int currentRetryCount = 0;
    private String currentUrl;
    // Last error code from a failed download (matches C# KRDownloadError).
    // 0 = no error. Used by CDNDownloadTask/UpdateFlow for error type mapping.
    private int lastErrorCode = 0;
    // C# HRESULT from the underlying exception (0 if none).
    // Corresponds to C# KRDownloadStateChangedEventArgs.CSharpErrorCode.
    // Used by ResourcesDownloadTask to detect proxy exceptions for retry.
    private int lastCSharpErrorCode = 0;
    // HTTP read timeout (ms) for stream reads. When > 0, overrides the default
    // 10s/30s timeouts. Corresponds to C# KRDownloadCall readBlockTimeout.
    private long readBlockTimeout = 0L;
    // When true, open connections with Proxy.NO_PROXY.
    // Set by ResourcesDownloadTask after a proxy exception to retry without proxy.
    private boolean disableProxy = false;

    public void setDisableProxy(boolean disable) {
        this.disableProxy = disable;
    }

    public DownloadTask(DownloadInfo info, String destPath) {
        this(info, destPath, null, Integer.MAX_VALUE);
    }

    public DownloadTask(DownloadInfo info, String destPath, List<String> backUpUrls, int maxRetryCount) {
        this(info, destPath, backUpUrls, maxRetryCount, new BandwidthLimiter(0));
    }

    public DownloadTask(DownloadInfo info, String destPath, List<String> backUpUrls, int maxRetryCount,
            BandwidthLimiter bandwidthLimiter) {
        this.downloadInfo = info;
        this.destPath = destPath;
        this.primaryUrl = info.url;
        this.currentUrl = info.url;
        // Match C# KRDownloadTask: BackUpUrls = [backup1, backup2, ..., primary].
        // GetRetryUrl indexes BackUpUrls[retryCount % Count] — retry 0 picks backup1.
        this.backUpUrls = new ArrayList<>();
        if (backUpUrls != null) {
            this.backUpUrls.addAll(backUpUrls);
        }
        this.backUpUrls.add(primaryUrl);
        this.maxRetryCount = maxRetryCount;
        this.bandwidthLimiter = bandwidthLimiter != null ? bandwidthLimiter : new BandwidthLimiter(0);
    }

    public void setProgressCallback(ProgressCallback cb) {
        this.progressCallback = cb;
    }

    public void setStateCallback(StateCallback cb) {
        this.stateCallback = cb;
    }

    /**
     * Set the MD5 check progress callback.
     * Corresponds to C# KRDownloadTask.OnMd5CheckProgressChanged event.
     * Invoked during post-download MD5 verification with (completedBytes,
     * totalBytes).
     */
    public void setMd5CheckProgressCallback(Md5CheckProgressCallback cb) {
        this.md5CheckProgressCallback = cb;
    }

    public void setCheckContentLength(boolean check) {
        this.checkContentLength = check;
    }

    public void setCheckContentEncoding(boolean check) {
        this.checkContentEncoding = check;
    }

    public void setDisableDownloadRange(boolean disable) {
        this.disableDownloadRange = disable;
    }

    public void setDisableCheckFileSize(boolean disable) {
        this.disableCheckFileSize = disable;
    }

    public void setCheckMd5WhenSizeIsSame(boolean check) {
        this.checkMd5WhenSizeIsSame = check;
    }

    public void setMaxRetryCount(int max) {
        this.maxRetryCount = max;
    }

    public void setReadBlockTimeout(long timeout) {
        this.readBlockTimeout = timeout;
    }

    public long getReadBlockTimeout() {
        return readBlockTimeout;
    }

    /**
     * Get the last C# HRESULT error code (0 if none).
     * Corresponds to C# KRDownloadStateChangedEventArgs.CSharpErrorCode.
     */
    public int getCSharpErrorCode() {
        return lastCSharpErrorCode;
    }

    /**
     * Get the task ID. Corresponds to C# KRDownloadTask.Id (MD5 hash of random).
     * Uses the destPath as a stable identifier since Java port doesn't generate
     * an explicit id field.
     */
    public String getId() {
        return destPath;
    }

    /**
     * Returns true if this task has successfully completed.
     * Corresponds to C# KRDownloadTask.IsDownloadFinish().
     */
    public boolean isDownloadFinish() {
        return state == DownloadState.COMPLETE;
    }

    /**
     * Get the disk size required for this download.
     * Corresponds to C# KRDownloadTask.GetNeedDiskSize().
     */
    public long getNeedDiskSize() {
        return downloadInfo.fileSize;
    }

    /**
     * Pause the download. Emits PAUSED state immediately if currently DOWNLOADING,
     * matching C# KRDownloadTask.Pause() / KRMultiDownloadTask.Pause() which call
     * OnDownloadStateChanged synchronously. Without the immediate notification
     * the UI does not reflect the pause until the next read-loop iteration.
     */
    public void pause() {
        pauseFlag.set(true);
        if (state == DownloadState.DOWNLOADING) {
            state = DownloadState.PAUSED;
            notifyState(DownloadState.PAUSED, null);
        }
    }

    /**
     * Resume the download. Emits DOWNLOADING state immediately if currently PAUSED,
     * matching C# KRDownloadTask.Resume() / KRMultiDownloadTask.Resume() which call
     * OnDownloadStateChanged synchronously.
     */
    public void resume() {
        pauseFlag.set(false);
        if (state == DownloadState.PAUSED) {
            state = DownloadState.DOWNLOADING;
            notifyState(DownloadState.DOWNLOADING, null);
        }
    }

    /**
     * Block the download thread while paused. Called from the read loop.
     * The PAUSED/DOWNLOADING state transitions are emitted by pause()/resume()
     * (matching C# synchronous notification); this method only waits for the
     * flag to clear. The in-loop state-emit fallback is retained for safety
     * in case pause() is invoked between the read-loop flag check and this call.
     */
    private void handlePauseInLoop() {
        if (pauseFlag.get() && !stopFlag.get() && state == DownloadState.DOWNLOADING) {
            state = DownloadState.PAUSED;
            notifyState(DownloadState.PAUSED, null);
        }
        while (pauseFlag.get() && !stopFlag.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (state == DownloadState.PAUSED && !stopFlag.get() && !pauseFlag.get()) {
            state = DownloadState.DOWNLOADING;
            notifyState(DownloadState.DOWNLOADING, null);
        }
    }

    public void stop() {
        stopFlag.set(true);
        resume();
    }

    public DownloadState getState() {
        return state;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    /**
     * Get the last error code (matches C# KRDownloadError values).
     * Returns 0 if no error occurred or if the error type was not classified.
     */
    public int getErrorCode() {
        return lastErrorCode;
    }

    /**
     * Classify an IOException into a C# KRDownloadError code.
     * Also sets lastCSharpErrorCode (HRESULT) for RetryHelper.canRetry() checks.
     * Used for error type mapping in UpdateFlow (NETWORK vs RETRY vs UNKNOWN).
     */
    private int classifyError(IOException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        // Set lastCSharpErrorCode (HRESULT) for RetryHelper.canRetry() checks.
        // Corresponds to C# setting CSharpErrorCode = ex.HResult.
        if (msg.contains("proxy") || isProxyException(e)) {
            lastCSharpErrorCode = RetryHelper.ERROR_PROXY_EXCEPTION;
        } else if (msg.contains("no space") || msg.contains("disk full") || msg.contains("not enough space")) {
            lastCSharpErrorCode = RetryHelper.ERROR_DISK_FULL;
        } else if (msg.contains("permission") || msg.contains("access denied") || msg.contains("access is denied")) {
            lastCSharpErrorCode = RetryHelper.ERROR_PERMISSION_DENY;
        } else {
            lastCSharpErrorCode = 0;
        }
        if (msg.contains("not support download range")) {
            return DownloadError.NOT_SUPPORT_DOWNLOAD_RANGE;
        }
        if (msg.contains("content-length") || msg.contains("content length")) {
            return DownloadError.GET_CONTENT_LENGTH_ERROR;
        }
        if (msg.contains("content-encoding") || msg.contains("content encoding")) {
            return DownloadError.CHECK_CONTENT_ENCODING_FAIL;
        }
        if (msg.contains("md5")) {
            return DownloadError.CHECK_MD5_FAILED;
        }
        if (msg.contains("timeout") || msg.contains("timed out")) {
            return DownloadError.STREAM_READ_BLOCK_TIMEOUT;
        }
        if (msg.contains("no space") || msg.contains("disk full")) {
            return DownloadError.DISK_SPACE_CHECK_FAIL;
        }
        // Default: network error (connect refused, reset, unreachable, etc.)
        return DownloadError.NETWORK;
    }

    /**
     * Check if the exception is proxy-related.
     * Java proxy exceptions include SocketException/ConnectException from proxy
     * connections, or IOExceptions with proxy-related messages.
     */
    private boolean isProxyException(Exception e) {
        if (e instanceof java.net.ConnectException || e instanceof java.net.SocketException) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            return msg.contains("proxy") || msg.contains("proxyserver");
        }
        return false;
    }

    /**
     * Get the URL for the current retry attempt.
     * Matches C# KRDownloadTask.GetRetryUrl():
     * - If currentRetryCount out of range, return primary Url.
     * - Otherwise index into backUpUrls (which already contains primary as last
     * entry).
     */
    private String getRetryUrl() {
        return getRetryUrl(currentRetryCount);
    }

    /**
     * Get the URL for a specific retry attempt.
     * Used by downloadWithChunks() where each chunk has its own local retry
     * counter (matching C# where each chunk is a separate KRDownloadTask with
     * its own _currentRetryCount).
     */
    private String getRetryUrl(int retryCount) {
        if (backUpUrls.isEmpty()) {
            return primaryUrl;
        }
        int num = retryCount % backUpUrls.size();
        if (num < 0 || num >= backUpUrls.size()) {
            return primaryUrl;
        }
        return backUpUrls.get(num);
    }

    public void run() {
        state = DownloadState.DOWNLOADING;
        try {
            List<ChunkInfo> chunks = downloadInfo.chunkInfoList;
            if (chunks != null && !chunks.isEmpty()) {
                downloadWithChunks(chunks);
            } else {
                downloadSingleFile();
            }
        } catch (Exception e) {
            log.error("Download failed: {}", currentUrl, e);
            state = DownloadState.FAILED;
            if (e instanceof IOException) {
                lastErrorCode = classifyError((IOException) e);
            } else {
                lastErrorCode = DownloadError.UNKNOWN;
                lastCSharpErrorCode = 0;
            }
            notifyState(DownloadState.FAILED, e.getMessage());
        }
    }

    /**
     * Download file as a single stream with HTTP Range resume and backup URL retry.
     * Corresponds to KRDownloadCall.
     */
    private void downloadSingleFile() throws Exception {
        File destFile = new File(destPath);
        destFile.getParentFile().mkdirs();

        // C# KRDownloadCall.cs:110-168: when FileSize < 0 and CheckFileSize is
        // true, do a separate GET request to fetch the content length. If the
        // content length is still unavailable, fail with
        // GET_CONTENT_LENGTH_ERROR. This is needed for downloads where the
        // expected file size is not known ahead of time (e.g. index files).
        if (downloadInfo.fileSize < 0 && !disableCheckFileSize) {
            long contentLength = fetchContentLength();
            if (contentLength < 0) {
                log.error("Failed to get content length for: {}", currentUrl);
                lastErrorCode = DownloadError.GET_CONTENT_LENGTH_ERROR;
                state = DownloadState.FAILED;
                notifyState(DownloadState.FAILED, "download fail, get content length error");
                return;
            }
            downloadInfo.fileSize = contentLength;
            log.info("Fetched content length: {} for {}", contentLength, currentUrl);
        }

        // Check existing file
        if (destFile.exists()) {
            long existingSize = destFile.length();
            if (existingSize == downloadInfo.fileSize) {
                // C# KRDownloadTask.Enqueue line 124: when
                // !CheckMd5WhenSizeIsSame (default), emit COMPLETE immediately
                // without MD5 check. When true, fall through to the download
                // path which re-verifies size and checks MD5 post-download.
                if (!checkMd5WhenSizeIsSame) {
                    log.info("File already downloaded with matching size (skipping MD5 check): {}", destPath);
                    downloadedBytes = existingSize;
                    state = DownloadState.COMPLETE;
                    notifyState(DownloadState.COMPLETE, null);
                    return;
                }
                // checkMd5WhenSizeIsSame=true: verify MD5, re-download on mismatch
                boolean existingMd5Ok;
                try {
                    existingMd5Ok = verifyFileMd5(destFile);
                } catch (IOException md5Ex) {
                    log.warn("MD5 computation failed for existing file, re-downloading: {}", destPath, md5Ex);
                    existingMd5Ok = false;
                }
                if (existingMd5Ok) {
                    log.info("File already downloaded with matching size and MD5: {}", destPath);
                    downloadedBytes = existingSize;
                    state = DownloadState.COMPLETE;
                    notifyState(DownloadState.COMPLETE, null);
                    return;
                }
                log.warn("Existing file MD5 mismatch, re-downloading: {}", destPath);
                try {
                    FileUtils.deleteFile(destPath);
                } catch (Exception ex) {
                    log.warn("Failed to delete existing file with MD5 mismatch: {}", ex.getMessage());
                }
                downloadedBytes = 0;
            } else if (existingSize > downloadInfo.fileSize) {
                // C# KRDownloadCall.cs:185-196: delete invalid (too large) file.
                // Logs warning on failure, does not fail the download.
                log.info("Existing file larger than expected, deleting: {}", destPath);
                try {
                    FileUtils.deleteFile(destPath);
                } catch (Exception ex) {
                    log.warn("File size invalid, but try delete file fail: {}", ex.getMessage());
                }
                downloadedBytes = 0;
            } else {
                downloadedBytes = existingSize;
                log.info("Resuming download from byte: {}", downloadedBytes);
            }
        }

        while (currentRetryCount <= maxRetryCount) {
            if (stopFlag.get()) {
                state = DownloadState.CANCELED;
                notifyState(DownloadState.CANCELED, null);
                return;
            }
            if (currentRetryCount == 0) {
                log.info("[SINGLE DL] {} | {}",
                        Paths.get(destPath).getFileName().toString(),
                        ByteUtils.byteConvert(downloadInfo.fileSize));
            }
            // Reset error code at the start of each attempt so stale codes
            // from previous iterations don't affect classifyError() logic.
            lastErrorCode = 0;
            lastCSharpErrorCode = 0;
            // First iteration uses primaryUrl (set in constructor); subsequent
            // iterations use the retry URL set in the catch block. Matches C#
            // where Enqueue uses Url=primary, and RetryEnqueue calls
            // GetRetryUrl() BEFORE incrementing _currentRetryCount.
            try {
                attemptDownload(destFile);
            } catch (IOException e) {
                log.warn("Download attempt {} failed from {}: {}",
                        currentRetryCount + 1, currentUrl, e.getMessage());
                // Only classify if error code not already set by the failure
                // point (e.g., DELETE_FILE_ERROR from attemptDownload).
                if (lastErrorCode == 0) {
                    lastErrorCode = classifyError(e);
                }
                // Check if error is retryable via RetryHelper.canRetry (C# KRRetryHelper).
                if (!RetryHelper.canRetry(lastCSharpErrorCode, !disableProxy)) {
                    log.error("Non-retryable error (code={}), failing download: {}", lastCSharpErrorCode,
                            e.getMessage());
                    state = DownloadState.FAILED;
                    notifyState(DownloadState.FAILED, e.getMessage());
                    throw e;
                }
                if (currentRetryCount >= maxRetryCount) {
                    state = DownloadState.FAILED;
                    notifyState(DownloadState.FAILED, e.getMessage());
                    throw e;
                }
                // Set retry URL BEFORE incrementing currentRetryCount, matching
                // C# RetryEnqueue which calls GetRetryUrl() then _currentRetryCount++.
                currentUrl = getRetryUrl();
                currentRetryCount++;
                downloadedBytes = destFile.exists() ? destFile.length() : 0;
                continue;
            }
            // MD5 verification — separate from download failure handling.
            // C# KRDownloadTask.DownloadStateChangedWrapper (lines 221-334):
            // - MD5 computation exception → CanRetry(NO_ERROR, ex.HResult) check
            // - Non-retryable → FAILED with CHECK_MD5_FAILED
            // - Retryable → text="" → treated as mismatch → delete + RetryEnqueue
            // - MD5 mismatch → delete + RetryEnqueue (NO CanRetry, only retry count)
            if (stopFlag.get()) {
                state = DownloadState.CANCELED;
                notifyState(DownloadState.CANCELED, null);
                return;
            }
            boolean md5Ok;
            try {
                md5Ok = verifyFileMd5(destFile);
            } catch (IOException md5Ex) {
                int hresult = ExceptionUtils.getHResult(md5Ex);
                if (!RetryHelper.canRetry(hresult, !disableProxy)) {
                    log.error("Non-retryable MD5 computation error (code={}): {}",
                            hresult, md5Ex.getMessage());
                    lastErrorCode = DownloadError.CHECK_MD5_FAILED;
                    lastCSharpErrorCode = hresult;
                    state = DownloadState.FAILED;
                    notifyState(DownloadState.FAILED,
                            "Get File Md5 Failed, ErrorMessage: " + md5Ex.getMessage());
                    return;
                }
                log.warn("Retryable MD5 computation error, treating as mismatch: {}",
                        md5Ex.getMessage());
                md5Ok = false;
            }
            if (!md5Ok) {
                if (currentRetryCount < maxRetryCount) {
                    try {
                        FileUtils.deleteFile(destPath);
                    } catch (Exception ex) {
                        log.error("MD5 mismatch and delete file failed: {}", destPath, ex);
                        lastErrorCode = DownloadError.DELETE_FILE_ERROR;
                        lastCSharpErrorCode = ExceptionUtils.getHResult(ex);
                        state = DownloadState.FAILED;
                        notifyState(DownloadState.FAILED,
                                "check md5 fail, delete old file fail: " + ex.getMessage());
                        return;
                    }
                    log.warn("MD5 mismatch after download, retrying: {}", destPath);
                    currentUrl = getRetryUrl();
                    currentRetryCount++;
                    downloadedBytes = 0;
                    continue;
                } else {
                    try {
                        FileUtils.deleteFile(destPath);
                    } catch (Exception ex) {
                        log.warn("Delete file failed after max retries: {}", destPath, ex);
                    }
                    downloadedBytes = 0;
                    lastErrorCode = DownloadError.CHECK_MD5_FAILED;
                    state = DownloadState.FAILED;
                    notifyState(DownloadState.FAILED, "retry max time, check md5 fail");
                    return;
                }
            }
            // MD5 verification passed → emit COMPLETE (C# DownloadStateChangedWrapper
            // intercepts COMPLETE and only forwards after MD5 verification).
            state = DownloadState.COMPLETE;
            notifyState(DownloadState.COMPLETE, null);
            return;
        }
    }

    private boolean verifyFileMd5(File file) throws IOException {
        log.info("verifyFileMd5: path={}, md5={}", file.getAbsolutePath(), downloadInfo.md5);
        if (downloadInfo.md5 == null || downloadInfo.md5.isEmpty()) {
            return true;
        }
        String actualMd5;
        if (md5CheckProgressCallback != null) {
            actualMd5 = MD5Utils.getFileMD5WithProgressOrThrow(file.getAbsolutePath(),
                    (completed, total) -> notifyMd5CheckProgress(completed, total));
        } else {
            actualMd5 = MD5Utils.getFileMD5OrThrow(file.getAbsolutePath());
        }
        return actualMd5.equalsIgnoreCase(downloadInfo.md5);
    }

    /**
     * Notify MD5 check progress with throttling.
     * Corresponds to C# KRDownloadTask.OnMd5CheckProgressChanged (lines 478-501).
     */
    private void notifyMd5CheckProgress(long completed, long total) {
        if (md5CheckProgressCallback == null) {
            return;
        }
        long now = System.currentTimeMillis();
        // C# KRDownloadTask.cs:485 uses <= (not <) for the skip condition:
        // if (diff <= interval && completed != total) return;
        if (now - lastMd5NotifyTimeMs <= MD5_NOTIFY_INTERVAL_MS && completed < total) {
            return;
        }
        lastMd5NotifyTimeMs = now;
        md5CheckProgressCallback.onProgress(completed, total);
    }

    /**
     * Notify download progress with time-based throttling (~100ms).
     * Corresponds to C# KRMultiDownloadTask.RefreshAndNotifyDownloadProgressInfo
     * which checks (now - _lastNotifyProgressTime > ProgressNotifyIntervalMillis)
     * OR (receivedBytes == totalBytes) before invoking the callback.
     * Without throttling, every 8KB read triggers a callback on high-bandwidth
     * links, flooding the UI thread.
     */
    private void notifyDownloadProgress() {
        if (progressCallback == null) {
            return;
        }
        long now = System.currentTimeMillis();
        // C# KRDownloadCall.cs:583 uses > (not >=) for the notify condition:
        // if (diff > interval || received == total) notify;
        // Equivalent skip condition: diff <= interval && received < total.
        if (now - lastProgressNotifyTimeMs <= PROGRESS_NOTIFY_INTERVAL_MS
                && downloadedBytes < downloadInfo.fileSize) {
            return;
        }
        lastProgressNotifyTimeMs = now;
        progressCallback.onProgress(downloadedBytes, downloadInfo.fileSize);
    }

    private void attemptDownload(File destFile) throws IOException {
        // C# KRResourcesDownloadTask: after proxy exception, ResetHttpClient disables
        // proxy. Java equivalent: open connection with Proxy.NO_PROXY.
        java.net.URL url = URI.create(UrlUtils.encodeUrlPath(currentUrl)).toURL();
        HttpURLConnection conn = disableProxy
                ? (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY)
                : (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        // C# KRMultiDownloadTask.SetReadBlockTimeout uses
        // TimeSpan.FromSeconds(seconds),
        // so the configured value is in SECONDS. Java's setReadTimeout expects millis.
        conn.setReadTimeout(readBlockTimeout > 0 ? (int) (readBlockTimeout * 1000) : 10000);
        conn.setRequestProperty("User-Agent", "KR-Launcher/1.0");
        conn.setRequestProperty("Accept-Encoding", "identity");

        // Range download for resume (disabled when DisableDownloadRange is set,
        // e.g. for index file downloads that should always fetch fresh).
        // C# KRDownloadCall: when UseDownloadRange()==false, always delete existing
        // file and download from scratch (no append, no resume).
        long rangeStart;
        boolean appendMode;
        if (disableDownloadRange) {
            if (destFile.exists()) {
                // C# KRDownloadCall.cs:264-281: when UseDownloadRange()==false,
                // delete existing file. If delete fails, emit FAILED with
                // DELETE_FILE_ERROR.
                try {
                    FileUtils.deleteFile(destPath);
                } catch (Exception ex) {
                    lastErrorCode = DownloadError.DELETE_FILE_ERROR;
                    lastCSharpErrorCode = ExceptionUtils.getHResult(ex);
                    throw new IOException("download not use range, delete exist file error: " + ex.getMessage(), ex);
                }
            }
            downloadedBytes = 0;
            rangeStart = 0;
            appendMode = false;
        } else {
            rangeStart = downloadedBytes;
            appendMode = downloadedBytes > 0;
        }
        if (rangeStart > 0) {
            conn.setRequestProperty("Range", "bytes=" + rangeStart + "-");
        }

        int responseCode = conn.getResponseCode();
        boolean isRangeResponse = (responseCode == 206);
        boolean isFullResponse = (responseCode == 200);

        if (!isRangeResponse && !isFullResponse) {
            conn.disconnect();
            throw new IOException("HTTP " + responseCode);
        }

        // If we requested a range but server returned 200 (full response) instead of
        // 206,
        // the server does not support Range requests. C# KRDownloadCall emits
        // NOT_SUPPORT_DOWNLOAD_RANGE error rather than silently restarting.
        if (rangeStart > 0 && isFullResponse && !isRangeResponse) {
            conn.disconnect();
            throw new IOException("NOT_SUPPORT_DOWNLOAD_RANGE: server returned 200 for range request");
        }

        // CheckContentLength: verify Content-Length matches expected remaining size
        if (checkContentLength && downloadInfo.fileSize > 0) {
            long contentLength = conn.getContentLengthLong();
            long expectedRemaining = downloadInfo.fileSize - downloadedBytes;
            if (contentLength > 0 && contentLength != expectedRemaining) {
                conn.disconnect();
                throw new IOException("Content-Length check fail: expected=" + expectedRemaining
                        + ", actual=" + contentLength);
            }
        }

        // CheckContentEncoding: reject compressed responses
        if (checkContentEncoding) {
            String encoding = conn.getContentEncoding();
            if (encoding != null && !encoding.isEmpty() && !encoding.equalsIgnoreCase("identity")) {
                conn.disconnect();
                throw new IOException("Content-Encoding check fail: " + encoding);
            }
        }

        try (InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(destFile, appendMode)) {
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                if (pauseFlag.get()) {
                    handlePauseInLoop();
                }
                if (stopFlag.get()) {
                    conn.disconnect();
                    state = DownloadState.CANCELED;
                    notifyState(DownloadState.CANCELED, null);
                    return;
                }

                if (!bandwidthLimiter.acquire(bytesRead, stopFlag::get)) {
                    conn.disconnect();
                    state = DownloadState.CANCELED;
                    notifyState(DownloadState.CANCELED, null);
                    return;
                }

                fos.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                if (progressCallback != null) {
                    notifyDownloadProgress();
                }
            }
        } finally {
            conn.disconnect();
        }
        // COMPLETE is intentionally NOT emitted here. The caller (downloadSingleFile)
        // verifies file size + MD5 and emits COMPLETE only after both pass — matches
        // C# KRDownloadStateChangedWrapper which intercepts COMPLETE and only
        // forwards after MD5 verification succeeds.
    }

    /**
     * Fetch the Content-Length for the current URL via a headers-only request.
     * Corresponds to C# KRDownloadCall.cs:110-158 which does a GET with
     * ResponseHeadersRead to obtain the content length when FileSize is
     * unknown.
     *
     * @return content length, or -1 if unavailable
     */
    private long fetchContentLength() {
        HttpURLConnection conn = null;
        try {
            java.net.URL url = URI.create(UrlUtils.encodeUrlPath(currentUrl)).toURL();
            conn = disableProxy
                    ? (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY)
                    : (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "KR-Launcher/1.0");
            conn.setRequestProperty("Accept-Encoding", "identity");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return conn.getContentLengthLong();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch content length: {}", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return -1;
    }

    /**
     * Download file using chunk-based download with resume and MD5 retry.
     * Corresponds to KRChunkDownloadTask.
     */
    private void downloadWithChunks(List<ChunkInfo> chunks) throws Exception {
        String chunkDir = destPath + "_Chunks";

        long totalChunkBytes = 0;
        for (ChunkInfo chunk : chunks) {
            totalChunkBytes += chunk.length();
        }

        // C# KRChunkDownloadTask.Enqueue (line 377-477): if main file already
        // exists with correct size AND no chunk directory, skip download entirely.
        // Defense-in-depth — PrepareTask normally filters such files beforehand.
        File mainFile = new File(destPath);
        boolean chunkDirExists = new File(chunkDir).exists();
        if (mainFile.exists() && mainFile.length() == totalChunkBytes && !chunkDirExists) {
            if (downloadInfo.md5 == null || downloadInfo.md5.isEmpty()
                    || !checkMd5WhenSizeIsSame) {
                log.info("Main file already exists with correct size, skipping chunk download: {}", destPath);
                downloadedBytes = totalChunkBytes;
                state = DownloadState.COMPLETE;
                notifyState(DownloadState.COMPLETE, null);
                return;
            }
            boolean mainFileMd5Ok;
            try {
                mainFileMd5Ok = verifyFileMd5(mainFile);
            } catch (IOException md5Ex) {
                log.warn("MD5 computation failed for main file, re-downloading via chunks: {}", destPath, md5Ex);
                mainFileMd5Ok = false;
            }
            if (mainFileMd5Ok) {
                log.info("Main file already exists with correct size and MD5: {}", destPath);
                downloadedBytes = totalChunkBytes;
                state = DownloadState.COMPLETE;
                notifyState(DownloadState.COMPLETE, null);
                return;
            }
            log.warn("Main file MD5 mismatch, deleting and re-downloading via chunks: {}", destPath);
            try {
                FileUtils.deleteFile(destPath);
            } catch (Exception ex) {
                log.warn("Failed to delete main file with MD5 mismatch: {}", ex.getMessage());
            }
        }

        FileUtils.ensureDir(chunkDir);

        String filename = Paths.get(destPath).getFileName().toString();
        log.info("[CHUNK DL] {} | {} chunks, {} total", filename, chunks.size(),
                ByteUtils.byteConvert(totalChunkBytes));

        // Outer retry loop for merge failures and merged-file MD5 mismatches.
        // Corresponds to C# KRChunkDownloadTask.RetryEnqueue() which re-enqueues
        // ALL chunks. Good chunks are skipped on re-download because
        // CheckAndRemoveErrorChunk (run before retry) already deleted bad ones,
        // and the per-chunk loop below skips existing chunks with matching size.
        int outerRetry = 0;
        while (true) {
            if (stopFlag.get()) {
                state = DownloadState.CANCELED;
                notifyState(DownloadState.CANCELED, null);
                return;
            }
            downloadedBytes = 0;

            // Download each chunk with retry on MD5 mismatch.
            // Per-chunk failures (after exhausting chunkRetry) throw IOException
            // which propagates to run() — NOT caught by the outer try-catch below,
            // so individual chunk failures do NOT trigger an outer retry.
            for (int i = 0; i < chunks.size(); i++) {
                if (stopFlag.get()) {
                    state = DownloadState.CANCELED;
                    notifyState(DownloadState.CANCELED, null);
                    return;
                }

                ChunkInfo chunk = chunks.get(i);
                String chunkFile = PathUtils.combine(chunkDir, "chunk_" + i);
                File chunkFileObj = new File(chunkFile);

                // C# KRDownloadTask.Enqueue (line 124): when chunk file exists
                // with matching size AND !CheckMd5WhenSizeIsSame (default),
                // emit COMPLETE immediately WITHOUT MD5 check. When
                // CheckMd5WhenSizeIsSame=true, verify MD5 — if Md5 is empty,
                // skip (can't verify); if Md5 is not empty, verify and
                // re-download on mismatch.
                if (chunkFileObj.exists() && chunkFileObj.length() == chunk.length()) {
                    if (!checkMd5WhenSizeIsSame) {
                        downloadedBytes += chunk.length();
                        notifyDownloadProgress();
                        continue;
                    }
                    if (chunk.md5 == null || chunk.md5.isEmpty()) {
                        downloadedBytes += chunk.length();
                        notifyDownloadProgress();
                        continue;
                    }
                    String chunkMd5 = MD5Utils.getFileMD5(chunkFile);
                    if (chunkMd5 != null && chunkMd5.equalsIgnoreCase(chunk.md5)) {
                        log.debug("Chunk {} already downloaded and MD5 verified, skipping", i);
                        downloadedBytes += chunk.length();
                        notifyDownloadProgress();
                        continue;
                    }
                    log.warn("Chunk {} MD5 mismatch, re-downloading", i);
                    chunkFileObj.delete();
                }

                // C# creates a separate KRDownloadTask per chunk with Url=primary.
                currentUrl = primaryUrl;

                int chunkRetry = 0;
                while (true) {
                    if (stopFlag.get()) {
                        state = DownloadState.CANCELED;
                        notifyState(DownloadState.CANCELED, null);
                        return;
                    }
                    lastErrorCode = 0;
                    lastCSharpErrorCode = 0;
                    try {
                        downloadChunkWithResume(chunkFile, chunk.start, chunk.end, i);
                    } catch (IOException e) {
                        if (lastErrorCode == 0) {
                            lastErrorCode = classifyError(e);
                        }
                        if (!RetryHelper.canRetry(lastCSharpErrorCode, !disableProxy)) {
                            log.error("Non-retryable error (code={}), failing chunk download: {}",
                                    lastCSharpErrorCode, e.getMessage());
                            state = DownloadState.FAILED;
                            notifyState(DownloadState.FAILED, e.getMessage());
                            throw e;
                        }
                        if (chunkRetry >= maxRetryCount) {
                            state = DownloadState.FAILED;
                            notifyState(DownloadState.FAILED,
                                    "Chunk " + i + " failed after " + (chunkRetry + 1) + " retries: " + e.getMessage());
                            throw e;
                        }
                        currentUrl = getRetryUrl(chunkRetry);
                        chunkRetry++;
                        log.warn("Chunk {} retry {} from {}", i, chunkRetry, currentUrl);
                        continue;
                    }
                    // MD5 verification — separate from download failure handling.
                    // C# each chunk is a KRDownloadTask that goes through
                    // DownloadStateChangedWrapper:
                    // - MD5 computation exception → CanRetry(NO_ERROR, ex.HResult)
                    // - Non-retryable → FAILED with CHECK_MD5_FAILED
                    // - Retryable → text="" → treated as mismatch
                    // - MD5 mismatch → delete + RetryEnqueue (NO CanRetry, only retry count)
                    if (chunk.md5 != null && !chunk.md5.isEmpty()) {
                        String actualMd5;
                        try {
                            actualMd5 = MD5Utils.getFileMD5OrThrow(chunkFile);
                        } catch (IOException md5Ex) {
                            int hresult = ExceptionUtils.getHResult(md5Ex);
                            if (!RetryHelper.canRetry(hresult, !disableProxy)) {
                                chunkFileObj.delete();
                                lastErrorCode = DownloadError.CHECK_MD5_FAILED;
                                lastCSharpErrorCode = hresult;
                                state = DownloadState.FAILED;
                                notifyState(DownloadState.FAILED,
                                        "Get chunk Md5 Failed, ErrorMessage: " + md5Ex.getMessage());
                                throw md5Ex;
                            }
                            log.warn("Retryable chunk MD5 computation error, treating as mismatch: {}",
                                    md5Ex.getMessage());
                            actualMd5 = "";
                        }
                        if (!actualMd5.equalsIgnoreCase(chunk.md5)) {
                            log.warn("Chunk {} MD5 mismatch: expected={}, actual={}, retry={}",
                                    i, chunk.md5, actualMd5, chunkRetry);
                            chunkFileObj.delete();
                            if (chunkRetry < maxRetryCount) {
                                currentUrl = getRetryUrl(chunkRetry);
                                chunkRetry++;
                                continue;
                            } else {
                                lastErrorCode = DownloadError.CHECK_MD5_FAILED;
                                state = DownloadState.FAILED;
                                notifyState(DownloadState.FAILED,
                                        "Chunk " + i + " MD5 mismatch after max retries");
                                throw new IOException(
                                        "Chunk " + i + " MD5 mismatch after max retries");
                            }
                        }
                    }
                    break;
                }

                downloadedBytes += chunk.length();
                notifyDownloadProgress();
                int chunkThreshold = Math.max(1, chunks.size() / 100);
                if ((i + 1) % chunkThreshold == 0 || i + 1 == chunks.size()) {
                    log.info("[CHUNK DL] {} | {}/{} chunks ({}%)", filename, i + 1,
                            chunks.size(), (i + 1) * 100 / chunks.size());
                }
            }

            // Merge chunks + verify merged MD5.
            // C# KRChunkDownloadTask.MergeChunk (line 820-1037) has THREE distinct
            // paths, each with different CanRetry behavior:
            // - Merge exception (line 887-914): _currentRetryCount < MaxRetryCount &&
            // CanRetry(NO_ERROR, ex.HResult) → retry; else FAILED with MERGE_CHUNK_FAIL.
            // - MD5 computation exception (line 920-946, separate try-catch):
            // !CanRetry(NO_ERROR, ex.HResult) → FAILED with CHECK_MD5_FAILED;
            // retryable → text="" → mismatch path.
            // - MD5 mismatch (line 963-1009): _currentRetryCount < MaxRetryCount →
            // CheckAndRemoveErrorChunk + delete + RetryEnqueue (NO CanRetry check).
            File mergedFile = new File(destPath);
            try {
                log.info("[MERGE] {} | merging {} chunks", filename, chunks.size());
                FileUtils.ensureDir(PathUtils.getParentDir(destPath));
                try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
                    for (int i = 0; i < chunks.size(); i++) {
                        if (stopFlag.get()) {
                            state = DownloadState.CANCELED;
                            notifyState(DownloadState.CANCELED, null);
                            return;
                        }
                        String chunkFile = PathUtils.combine(chunkDir, "chunk_" + i);
                        try (FileInputStream fis = new FileInputStream(chunkFile)) {
                            byte[] buffer = new byte[READ_BUFFER_SIZE];
                            int read;
                            while ((read = fis.read(buffer)) != -1) {
                                fos.write(buffer, 0, read);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof IOException) {
                    if (lastErrorCode == 0) {
                        lastErrorCode = classifyError((IOException) e);
                    }
                    if (outerRetry < maxRetryCount
                            && RetryHelper.canRetry(lastCSharpErrorCode, !disableProxy)) {
                        log.warn("Merge failed, retrying (retry {}): {}", outerRetry + 1, e.getMessage());
                        new File(destPath).delete();
                        outerRetry++;
                        lastErrorCode = 0;
                        lastCSharpErrorCode = 0;
                        continue;
                    }
                }
                if (lastErrorCode == 0) {
                    lastErrorCode = DownloadError.MERGE_CHUNK_FAIL;
                }
                state = DownloadState.FAILED;
                notifyState(DownloadState.FAILED, "merge chunk fail: " + e.getMessage());
                throw e;
            }

            // Verify merged file MD5 — separate from merge exception handling.
            if (downloadInfo.md5 != null && !downloadInfo.md5.isEmpty()) {
                String mergedMd5 = "";
                try {
                    if (md5CheckProgressCallback != null) {
                        mergedMd5 = MD5Utils.getFileMD5WithProgressOrThrow(destPath,
                                (completed, total) -> notifyMd5CheckProgress(completed, total));
                    } else {
                        mergedMd5 = MD5Utils.getFileMD5OrThrow(destPath);
                    }
                } catch (IOException md5Ex) {
                    int hresult = ExceptionUtils.getHResult(md5Ex);
                    if (!RetryHelper.canRetry(hresult, !disableProxy)) {
                        mergedFile.delete();
                        lastErrorCode = DownloadError.CHECK_MD5_FAILED;
                        lastCSharpErrorCode = hresult;
                        state = DownloadState.FAILED;
                        notifyState(DownloadState.FAILED,
                                "Get File Md5 Failed, ErrorMessage: " + md5Ex.getMessage());
                        return;
                    }
                    log.warn("Retryable merged MD5 computation error, treating as mismatch: {}",
                            md5Ex.getMessage());
                }
                if (!mergedMd5.equalsIgnoreCase(downloadInfo.md5)) {
                    log.error("Merged file MD5 mismatch: expected={}, actual={}, outerRetry={}",
                            downloadInfo.md5, mergedMd5, outerRetry);
                    if (outerRetry < maxRetryCount) {
                        // C# CheckAndRemoveErrorChunk: re-verify each chunk
                        // and delete bad ones. Chunks with empty MD5 are also
                        // deleted (can't verify them).
                        for (int i = 0; i < chunks.size(); i++) {
                            ChunkInfo chunk = chunks.get(i);
                            String chunkFile = PathUtils.combine(chunkDir, "chunk_" + i);
                            if (chunk.md5 == null || chunk.md5.isEmpty()) {
                                new File(chunkFile).delete();
                            } else {
                                String actualMd5 = MD5Utils.getFileMD5(chunkFile);
                                if (actualMd5 == null || !actualMd5.equalsIgnoreCase(chunk.md5)) {
                                    log.warn("Found bad chunk {} during re-verify, deleting", i);
                                    new File(chunkFile).delete();
                                }
                            }
                        }
                        mergedFile.delete();
                        outerRetry++;
                        log.warn("Retrying whole chunk download after merged MD5 mismatch (retry {})",
                                outerRetry);
                        continue;
                    } else {
                        mergedFile.delete();
                        lastErrorCode = DownloadError.CHECK_MD5_FAILED;
                        state = DownloadState.FAILED;
                        notifyState(DownloadState.FAILED,
                                "Merge Chunk, Check MD5 fail, expect: "
                                        + downloadInfo.md5 + ", actual: " + mergedMd5);
                        return;
                    }
                }
            }

            break;
        }

        try {
            FileUtils.deleteDirectory(chunkDir);
        } catch (Exception e) {
            log.warn("Failed to clean up chunk directory: {}", chunkDir, e);
        }

        state = DownloadState.COMPLETE;
        notifyState(DownloadState.COMPLETE, null);
    }

    /**
     * Download a single chunk with Range header and resume support.
     * If the chunk file exists with partial data, resumes from the partial offset.
     */
    private void downloadChunkWithResume(String chunkFile, long start, long end, int chunkIndex) throws IOException {
        File chunkFileObj = new File(chunkFile);
        chunkFileObj.getParentFile().mkdirs();

        long chunkSize = end - start + 1;
        long existingSize = chunkFileObj.exists() ? chunkFileObj.length() : 0;

        // If already complete, skip
        if (existingSize == chunkSize) {
            return;
        }
        // If larger than expected, restart
        if (existingSize > chunkSize) {
            chunkFileObj.delete();
            existingSize = 0;
        }

        java.net.URL chunkUrl = URI.create(UrlUtils.encodeUrlPath(currentUrl)).toURL();
        HttpURLConnection conn = disableProxy
                ? (HttpURLConnection) chunkUrl.openConnection(java.net.Proxy.NO_PROXY)
                : (HttpURLConnection) chunkUrl.openConnection();
        conn.setConnectTimeout(10000);
        // C# KRMultiDownloadTask.SetReadBlockTimeout uses
        // TimeSpan.FromSeconds(seconds).
        conn.setReadTimeout(readBlockTimeout > 0 ? (int) (readBlockTimeout * 1000) : 30000);
        conn.setRequestProperty("User-Agent", "KR-Launcher/1.0");
        conn.setRequestProperty("Accept-Encoding", "identity");

        // Range: resume from (start + existingSize) to end
        long rangeStart = start + existingSize;
        conn.setRequestProperty("Range", "bytes=" + rangeStart + "-" + end);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 206) {
            conn.disconnect();
            throw new IOException("HTTP " + responseCode + " for chunk " + chunkIndex);
        }

        // C# KRDownloadCall.cs:374-382: when a Range request is sent and server
        // returns 200 (not 206), the server does not support Range requests.
        // Fail with NOT_SUPPORT_DOWNLOAD_RANGE rather than silently writing wrong
        // data (full file instead of the requested byte range).
        if (responseCode == 200 && chunkSize > 0) {
            conn.disconnect();
            lastErrorCode = DownloadError.NOT_SUPPORT_DOWNLOAD_RANGE;
            throw new IOException("NOT_SUPPORT_DOWNLOAD_RANGE: server returned 200 for chunk range request");
        }

        // CheckContentLength: verify Content-Length matches expected remaining size.
        // C# KRDownloadCall.cs:340-348. This catches empty/short responses early
        // (e.g. CDN returns 206 with 0 bytes) instead of relying solely on MD5.
        if (checkContentLength && chunkSize > 0) {
            long contentLength = conn.getContentLengthLong();
            long expectedRemaining = chunkSize - existingSize;
            if (contentLength > 0 && contentLength != expectedRemaining) {
                conn.disconnect();
                throw new IOException("Content-Length check fail for chunk " + chunkIndex
                        + ": expected=" + expectedRemaining + ", actual=" + contentLength);
            }
        }

        // CheckContentEncoding
        if (checkContentEncoding) {
            String encoding = conn.getContentEncoding();
            if (encoding != null && !encoding.isEmpty() && !encoding.equalsIgnoreCase("identity")) {
                conn.disconnect();
                throw new IOException("Content-Encoding check fail for chunk " + chunkIndex + ": " + encoding);
            }
        }

        try (InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(chunkFile, existingSize > 0)) {
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            int read;
            while ((read = is.read(buffer)) != -1) {
                if (pauseFlag.get()) {
                    handlePauseInLoop();
                }
                if (stopFlag.get()) {
                    conn.disconnect();
                    state = DownloadState.CANCELED;
                    notifyState(DownloadState.CANCELED, null);
                    return;
                }
                if (!bandwidthLimiter.acquire(read, stopFlag::get)) {
                    conn.disconnect();
                    state = DownloadState.CANCELED;
                    notifyState(DownloadState.CANCELED, null);
                    return;
                }
                fos.write(buffer, 0, read);
            }
        } finally {
            conn.disconnect();
        }
    }

    private void notifyState(DownloadState state, String error) {
        if (stateCallback != null) {
            stateCallback.onStateChanged(state, error);
        }
    }

    public interface ProgressCallback {
        void onProgress(long downloaded, long total);
    }

    public interface StateCallback {
        void onStateChanged(DownloadState state, String error);
    }

    /**
     * Callback for MD5 check progress.
     * Corresponds to C# KRDownloadMd5CheckProgressChanged event.
     * Reports (completedBytes, totalBytes) during post-download MD5 verification.
     */
    public interface Md5CheckProgressCallback {
        void onProgress(long completed, long total);
    }
}
