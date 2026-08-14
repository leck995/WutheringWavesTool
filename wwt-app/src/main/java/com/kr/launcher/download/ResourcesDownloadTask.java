package com.kr.launcher.download;

import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.model.CdnConfig;
import com.kr.launcher.model.DownloadInfo;
import com.kr.launcher.util.ResourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Multi-file download orchestrator wrapping CDNDownloadTask.
 * Corresponds to KRResourcesDownloadTask.cs.
 *
 * Adds retry-on-STREAM_READ_BLOCK_TIMEOUT (up to 5 retries) and proxy-exception
 * handling on top of CDNDownloadTask. Tracks cumulative progress and forwards
 * state changes via DownloadStateChangedEventArgs /
 * DownloadProgressChangedEventArgs
 * callbacks to match the C# event model.
 */
public class ResourcesDownloadTask {
    private static final Logger log = LoggerFactory.getLogger(ResourcesDownloadTask.class);

    public interface Callback {
        void invoke();
    }

    public interface ProgressChangedHandler {
        void onProgress(ResourcesDownloadTask sender, DownloadProgressChangedEventArgs progressInfo);
    }

    public interface StateChangedHandler {
        void onStateChanged(ResourcesDownloadTask sender, DownloadStateChangedEventArgs stateInfo);
    }

    public interface Md5CheckProgressHandler {
        void onProgress(ResourcesDownloadTask sender, DownloadMd5CheckProgressChangedEventArgs progressInfo);
    }

    private boolean isFinished = false;
    private final ResourceConfigManager configManager;
    private final List<DownloadInfo> downloadInfoList;
    private final ProgressChangedHandler progressChanged;
    private final StateChangedHandler stateChanged;
    private final String destPath;
    private CDNDownloadTask task;
    private final List<CdnConfig> cdnConfigList;
    private final String baseUrl;
    private Map<String, Object> trackExtInfos;
    private int readTimeoutRetryCount;
    private long lastReceivedBytes;
    private Md5CheckProgressHandler md5CheckProgressChanged;
    // C# KRResourcesDownloadTask: proxy exception retry is one-shot.
    // After ResetHttpClient(allowUseProxy:false), subsequently built tasks must
    // also use no proxy. This field persists across run() rebuilds.
    private boolean proxyRetryUsed = false;
    private boolean disableProxy = false;

    public ResourcesDownloadTask(ResourceConfigManager configManager,
            List<CdnConfig> cdnConfigList,
            List<DownloadInfo> downloadInfoList,
            String baseUrl,
            String downloadBasePath,
            ProgressChangedHandler progressChanged,
            StateChangedHandler stateChanged) {
        this.configManager = configManager;
        this.cdnConfigList = cdnConfigList;
        this.downloadInfoList = downloadInfoList;
        this.progressChanged = progressChanged;
        this.stateChanged = stateChanged;
        this.destPath = downloadBasePath;
        this.baseUrl = baseUrl;
    }

    public void setMd5CheckProgressCallback(Md5CheckProgressHandler md5CheckProgressChanged) {
        this.md5CheckProgressChanged = md5CheckProgressChanged;
    }

    public void setExtInfos(Map<String, Object> trackExtInfos) {
        this.trackExtInfos = trackExtInfos;
    }

    /**
     * Get the disk size required for this download.
     * Corresponds to C# KRResourcesDownloadTask.GetNeedDiskSize(ratio).
     * If the underlying task has finished, returns raw size; otherwise applies
     * the disk-space calculation ratio to allow for decompression overhead.
     */
    public long getNeedDiskSize(double diskSpaceCalculationRatio) {
        if (task != null) {
            if (task.isDownloadFinish()) {
                return task.getNeedDiskSize();
            }
            return (long) (task.getNeedDiskSize() * diskSpaceCalculationRatio);
        }
        return 0L;
    }

    /**
     * Returns true if the underlying CDNDownloadTask has finished successfully.
     */
    public boolean isDownloadFinish() {
        if (task != null) {
            return task.isDownloadFinish();
        }
        return false;
    }

    /**
     * Get the underlying CDNDownloadTask's last error code.
     * Used by callers (e.g., UpdateFlow) for error type mapping.
     */
    public int getErrorCode() {
        if (task != null) {
            return task.getErrorCode();
        }
        return 0;
    }

    /**
     * Run the resources download.
     * Corresponds to C# KRResourcesDownloadTask.Run().
     *
     * On STREAM_READ_BLOCK_TIMEOUT (retry count < 5): optionally reset HTTP
     * client per config, then re-run. On ERROR_PROXY_EXCEPTION (if proxy
     * allowed): reset HTTP client without proxy, then re-run. Otherwise forward
     * state to caller.
     */
    public void run() {
        if (isFinished && task != null) {
            log.info("resourceDownload task has finished, skip exec it");
            DownloadStateChangedEventArgs args = new DownloadStateChangedEventArgs(task.getId());
            args.state = DownloadState.COMPLETE;
            args.errorCode = DownloadError.NO_ERROR;
            args.errorMessage = "";
            stateChanged.onStateChanged(this, args);
            return;
        }
        task = null;

        CDNDownloadTaskBuilder builder = new CDNDownloadTaskBuilder(downloadInfoList, cdnConfigList);
        builder.withBasePath(baseUrl);
        builder.withBaseDestPath(destPath);
        builder.withMaxRetryCount(5);
        builder.withReadBlockTimeout(ResourceHelper.getReadBlockTimeout(configManager));
        builder.withCdnSelectTestDuration(ResourceHelper.getCdnSelectTestDuration(configManager));
        task = builder.build();
        // Propagate disableProxy to the newly built task.
        // C# KRResourcesDownloadTask.cs:51-54: ResetHttpClient(allowUseProxy:false)
        // resets the GLOBAL HttpClient so all subsequently built tasks use no proxy.
        task.setDisableProxy(disableProxy);

        double diskSpaceCalculationRatio = ResourceHelper.getDiskSpaceCalculationRatio(configManager);
        task.setDiskSpaceCalculationRatio(diskSpaceCalculationRatio);

        // Wire MD5 check progress callback to the CDN download task.
        // Corresponds to C# KRResourcesDownloadTask.SetMd5CheckProgressCallback.
        if (md5CheckProgressChanged != null) {
            task.setMd5CheckProgressCallback((completed, total) -> {
                DownloadMd5CheckProgressChangedEventArgs args = new DownloadMd5CheckProgressChangedEventArgs(
                        task.getId());
                args.completedBytesSize = completed;
                args.totalBytes = total;
                md5CheckProgressChanged.onProgress(this, args);
            });
        }

        log.info("Create CDN Download Task, TaskId: {}", task.getId());

        if (downloadInfoList.isEmpty()) {
            DownloadProgressChangedEventArgs progressArgs = new DownloadProgressChangedEventArgs(task.getId());
            progressArgs.totalBytesToReceive = 0L;
            progressArgs.receivedBytesSize = 0L;
            progressArgs.bytesPerSecondSpeed = 0.0;
            progressArgs.downloadedBytesSizeThisTime = 0L;
            progressChanged.onProgress(this, progressArgs);

            DownloadStateChangedEventArgs stateArgs = new DownloadStateChangedEventArgs(task.getId());
            stateArgs.state = DownloadState.COMPLETE;
            stateArgs.errorCode = DownloadError.NO_ERROR;
            stateArgs.errorMessage = "";
            stateChanged.onStateChanged(this, stateArgs);
            return;
        }

        // Wire progress callback: also tracks read-timeout retry reset.
        task.setProgressCallback((downloaded, total) -> {
            if (downloaded != lastReceivedBytes) {
                readTimeoutRetryCount = 0;
            }
            lastReceivedBytes = downloaded;
            DownloadProgressChangedEventArgs progressArgs = new DownloadProgressChangedEventArgs(task.getId());
            progressArgs.totalBytesToReceive = total;
            progressArgs.receivedBytesSize = downloaded;
            progressChanged.onProgress(this, progressArgs);
        });

        // Wire state callback with retry-on-timeout / proxy-exception logic.
        task.setStateCallback((state, error) -> {
            int errCode = task.getErrorCode();
            int cSharpErrorCode = task.getCSharpErrorCode();

            if (state == DownloadState.FAILED && cSharpErrorCode == RetryHelper.ERROR_PROXY_EXCEPTION
                    && !proxyRetryUsed) {
                // C# KRResourcesDownloadTask.cs:51-54: on proxy exception, call
                // ResetHttpClient(allowUseProxy:false) then retry once.
                // Subsequent proxy exceptions are treated as normal errors.
                log.info("Proxy exception, disabling proxy and retrying once");
                proxyRetryUsed = true;
                disableProxy = true;
                run();
                return;
            }
            if (state == DownloadState.FAILED && errCode == DownloadError.STREAM_READ_BLOCK_TIMEOUT
                    && readTimeoutRetryCount < 5) {
                readTimeoutRetryCount++;
                if (ResourceHelper.resetHttpClientWhenReadTimeout(configManager)) {
                    // C# KRResourcesDownloadTask.cs:56-65: ResetHttpClient(allowUseProxy:false)
                    // globally disables proxy for all subsequently built tasks. Persist
                    // disableProxy so the rebuilt CDNDownloadTask (and its nested
                    // DownloadTasks) skip the proxy on retry.
                    log.info("Reset HttpClient When Timeout (disable proxy)");
                    disableProxy = true;
                }
                run();
                return;
            }

            if (state == DownloadState.COMPLETE) {
                isFinished = true;
            }

            DownloadStateChangedEventArgs stateArgs = new DownloadStateChangedEventArgs(task.getId());
            stateArgs.state = state;
            stateArgs.errorCode = errCode;
            stateArgs.cSharpErrorCode = cSharpErrorCode;
            stateArgs.errorMessage = error != null ? error : "";
            stateChanged.onStateChanged(this, stateArgs);
        });

        task.run();
    }

    public void pause() {
        if (task != null) {
            log.info("taskId: {} Pause", task.getId());
            task.pause();
        }
    }

    public void resume() {
        if (task != null) {
            log.info("taskId: {} Resume", task.getId());
            task.resume();
        }
    }

    public void stop() {
        stop(null);
    }

    public void stop(Callback callback) {
        if (task != null) {
            log.info("taskId: {} Stop", task.getId());
            task.stop();
            task = null;
        }
        if (callback != null) {
            callback.invoke();
        }
    }
}
