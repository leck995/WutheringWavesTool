package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.DownloadPhase;
import cn.tealc.wwt.game.resource.model.DownloadInfo;
import cn.tealc.wwt.game.resource.model.DownloadState;
import cn.tealc.wwt.game.resource.util.FileUtils;
import cn.tealc.wwt.game.resource.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多文件并行下载编排器。
 *
 * <p>能力：CDN 选线（返回按优先级排序的基址，首个为主 CDN，其余作为备用）、多文件并发下载
 * （{@code maxParallel} 个线程）、总体进度聚合上报、整体暂停/恢复/停止、磁盘空间预检。</p>
 *
 * <p>所有回调均在 worker 线程触发，调用方需自行收敛（如 Platform.runLater）。</p>
 */
public class DownloadManager {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadManager.class);

    private final List<DownloadInfo> infos;
    private final Path destRoot;
    private final List<String> cdnBaseUrls;
    private final int maxParallel;
    private final int maxRetry;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final BandwidthLimiter bandwidthLimiter;

    private DownloadListeners.StateListener stateListener;
    private DownloadListeners.ProgressListener progressListener;
    private DownloadListeners.Md5CheckListener md5CheckListener;
    private DownloadListeners.PhaseListener phaseListener;

    private final List<DownloadItem> items = new ArrayList<>();
    private final long[] itemDownloaded;
    private volatile boolean running = false;
    private final long totalBytes;

    DownloadManager(List<DownloadInfo> infos, Path destRoot, List<String> cdnBaseUrls,
            int maxParallel, int maxRetry, int connectTimeoutMs, int readTimeoutMs,
            long speedLimitBytesPerSecond) {
        this.infos = infos;
        this.destRoot = destRoot;
        this.cdnBaseUrls = cdnBaseUrls == null ? List.of() : List.copyOf(cdnBaseUrls);
        this.maxParallel = maxParallel;
        this.maxRetry = maxRetry;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.bandwidthLimiter = new BandwidthLimiter(speedLimitBytesPerSecond);
        this.totalBytes = infos.stream().mapToLong(DownloadInfo::fileSize).sum();
        this.itemDownloaded = new long[infos.size()];
    }

    public void setStateListener(DownloadListeners.StateListener listener) {
        this.stateListener = listener;
    }

    public void setProgressListener(DownloadListeners.ProgressListener listener) {
        this.progressListener = listener;
    }

    public void setMd5CheckListener(DownloadListeners.Md5CheckListener listener) {
        this.md5CheckListener = listener;
    }

    public void setPhaseListener(DownloadListeners.PhaseListener listener) {
        this.phaseListener = listener;
    }

    public long totalBytes() {
        return totalBytes;
    }

    /**
     * 阻塞执行：预检磁盘 → 解析 CDN URL → 并发下载所有文件。
     * 任一文件失败即整体终止（其余文件标记停止）。
     */
    public void run() throws IOException {
        if (infos.isEmpty()) {
            notifyState(DownloadState.COMPLETE, null);
            return;
        }
        running = true;

        long free = FileUtils.freeSpaceBytes(destRoot);
        if (free < totalBytes) {
            notifyState(DownloadState.FAILED,
                    "磁盘空间不足：需要 " + totalBytes + " 字节，剩余 " + free + " 字节");
            running = false;
            return;
        }

        items.clear();
        AtomicInteger completedFiles = new AtomicInteger();
        for (int i = 0; i < infos.size(); i++) {
            DownloadInfo info = infos.get(i);
            List<String> urls = resolveUrlsByBase(info, cdnBaseUrls);
            final int idx = i;
            // 以解析后的主 URL 构造 DownloadInfo
            DownloadInfo resolved = urls.isEmpty()
                    ? info
                    : new DownloadInfo(urls.get(0), info.destPath(), info.fileSize(),
                            info.md5(), info.basePath(), info.chunkInfoList());
            DownloadItem item = new DownloadItem(resolved, destRoot, urls.size() > 1 ? urls.subList(1, urls.size()) : List.of(),
                    maxRetry, connectTimeoutMs, readTimeoutMs, bandwidthLimiter);
            item.setStateListener((state, err) -> {
                if (state == DownloadState.FAILED && running) {
                    stop();
                }
                if (state == DownloadState.COMPLETE) {
                    completedFiles.incrementAndGet();
                }
            });
            item.setProgressListener((done, total) -> {
                itemDownloaded[idx] = done;
                notifyAggregatedProgress();
            });
            item.setMd5CheckListener(md5CheckListener);
            item.setPhaseListener((phase, relativePath, ignoredCompleted, ignoredTotal) ->
                    notifyPhase(phase, relativePath, completedFiles.get(), infos.size()));
            items.add(item);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("构建下载任务 {} 个，目标目录 {}", items.size(), destRoot);
        }

        notifyState(DownloadState.DOWNLOADING, null);
        CountDownLatch latch = new CountDownLatch(items.size());
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(maxParallel, Math.max(1, items.size())));
        for (DownloadItem item : items) {
            pool.submit(() -> {
                try {
                    item.run();
                } finally {
                    latch.countDown();
                }
            });
        }
        pool.shutdown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop();
        }
        running = false;

        notifyAggregatedProgress();
        if (anyFailed()) {
            notifyState(DownloadState.FAILED, "存在下载失败的文件");
        } else if (anyCanceled()) {
            notifyState(DownloadState.CANCELED, null);
        } else {
            notifyState(DownloadState.COMPLETE, null);
        }
    }

    public void pause() {
        for (DownloadItem item : items) {
            item.pause();
        }
    }

    public void resume() {
        for (DownloadItem item : items) {
            item.resume();
        }
    }

    public void stop() {
        running = false;
        for (DownloadItem item : items) {
            item.stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public List<DownloadItem> getItems() {
        return List.copyOf(items);
    }

    // --------------------------------------------------------------------- URL 解析

    /**
     * 为某文件按各 CDN 基址生成候选 URL 列表（首个即主 URL）。
     * 基址列表为空时退化为使用 info.url() 本身。
     */
    static List<String> resolveUrlsByBase(DownloadInfo info, List<String> bases) {
        if (bases == null || bases.isEmpty()) {
            return List.of(info.url());
        }
        List<String> urls = new ArrayList<>();
        for (String base : bases) {
            urls.add(UrlUtils.join(base, info.url()));
        }
        return urls;
    }

    // --------------------------------------------------------------------- 聚合

    private boolean anyFailed() {
        for (DownloadItem item : items) {
            if (item.getState() == DownloadState.FAILED) {
                return true;
            }
        }
        return false;
    }

    private boolean anyCanceled() {
        for (DownloadItem item : items) {
            if (item.getState() == DownloadState.CANCELED) {
                return true;
            }
        }
        return false;
    }

    private void notifyAggregatedProgress() {
        if (progressListener == null) {
            return;
        }
        long sum = 0;
        for (long v : itemDownloaded) {
            sum += v;
        }
        progressListener.onProgress(sum, totalBytes);
    }

    private void notifyState(DownloadState state, String error) {
        if (stateListener != null) {
            stateListener.onStateChanged(state, error);
        }
    }

    private void notifyPhase(DownloadPhase phase,
            String relativePath, int completedFiles, int totalFiles) {
        if (phaseListener != null) {
            phaseListener.onPhaseChanged(phase, relativePath, completedFiles, totalFiles);
        }
    }
}
