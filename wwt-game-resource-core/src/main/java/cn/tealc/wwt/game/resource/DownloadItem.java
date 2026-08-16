package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.error.DownloadError;
import cn.tealc.wwt.game.resource.error.RetryHelper;
import cn.tealc.wwt.game.resource.model.ChunkInfo;
import cn.tealc.wwt.game.resource.model.DownloadPhase;
import cn.tealc.wwt.game.resource.model.DownloadInfo;
import cn.tealc.wwt.game.resource.model.DownloadState;
import cn.tealc.wwt.game.resource.util.FileUtils;
import cn.tealc.wwt.game.resource.util.HttpUtils;
import cn.tealc.wwt.game.resource.util.MD5Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单文件下载器。
 *
 * <p>能力：HTTP Range 断点续传、服务器预定义分块下载（逐块存 <dest>_Chunks/ 后合并）并逐块 MD5、
 * 整文件 MD5 校验、备用 URL（备份 CDN）轮换重试、暂停/恢复/停止、进度节流上报。</p>
 *
 * <p>阻塞运行：{@link #run()} 需在工作线程调用，进度/状态经 {@link DownloadListeners} 回调上报
 * （回调在调用线程触发）。</p>
 */
public class DownloadItem {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadItem.class);
    private static final int READ_BUFFER_SIZE = 65536;
    private static final long PROGRESS_NOTIFY_INTERVAL_MS = 100;

    private final DownloadInfo info;
    private final Path destRoot;
    private final List<String> backUpUrls;
    private final int maxRetryCount;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final BandwidthLimiter bandwidthLimiter;

    private DownloadListeners.StateListener stateListener;
    private DownloadListeners.ProgressListener progressListener;
    private DownloadListeners.Md5CheckListener md5CheckListener;
    private DownloadListeners.PhaseListener phaseListener;

    private volatile DownloadState state = DownloadState.IDLE;
    private final AtomicBoolean pauseFlag = new AtomicBoolean(false);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private volatile long downloadedBytes = 0;

    private long lastProgressNotifyMs = 0;
    private int lastErrorCode = DownloadError.NO_ERROR;
    private String lastError;
    private volatile int retryCount = 0;

    public DownloadItem(DownloadInfo info, Path destRoot, List<String> backUpUrls, int maxRetryCount,
            int connectTimeoutMs, int readTimeoutMs) {
        this(info, destRoot, backUpUrls, maxRetryCount, connectTimeoutMs, readTimeoutMs,
                new BandwidthLimiter(0));
    }

    public DownloadItem(DownloadInfo info, Path destRoot, List<String> backUpUrls, int maxRetryCount,
            int connectTimeoutMs, int readTimeoutMs, BandwidthLimiter bandwidthLimiter) {
        this.info = info;
        this.destRoot = destRoot;
        // 备用 URL 之前追加主 URL，与之对应 kr 的 BackUpUrls=[backup..., primary]，按 retry 轮换。
        this.backUpUrls = new ArrayList<>();
        if (backUpUrls != null) {
            this.backUpUrls.addAll(backUpUrls);
        }
        this.backUpUrls.add(primaryUrl());
        this.maxRetryCount = maxRetryCount;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.bandwidthLimiter = bandwidthLimiter != null ? bandwidthLimiter : new BandwidthLimiter(0);
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

    public DownloadState getState() {
        return state;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public int getErrorCode() {
        return lastErrorCode;
    }

    public String getError() {
        return lastError;
    }

    public boolean isFinished() {
        return state == DownloadState.COMPLETE;
    }

    /** 本次下载目标文件绝对路径。 */
    public Path destPath() {
        return destRoot.resolve(info.destPath());
    }

    private String primaryUrl() {
        return info.url();
    }

    public void pause() {
        pauseFlag.set(true);
        if (state == DownloadState.DOWNLOADING) {
            setState(DownloadState.PAUSED, null);
        }
    }

    public void resume() {
        pauseFlag.set(false);
        if (state == DownloadState.PAUSED) {
            setState(DownloadState.DOWNLOADING, null);
        }
    }

    public void stop() {
        stopFlag.set(true);
        resume();
    }

    /**
     * 阻塞执行下载。成功置 COMPLETE；失败/取消终结为 FAILED/CANCELED。
     */
    public void run() {
        if (stopFlag.get()) {
            setState(DownloadState.CANCELED, null);
            return;
        }
        if (downloadInfoEmpty()) {
            setState(DownloadState.COMPLETE, null);
            return;
        }
        setState(DownloadState.WAITING, null);
        notifyPhase(DownloadPhase.PREPARING);
        try {
            if (info.chunkInfoList().isEmpty()) {
                runSingleFile();
            } else {
                runWithChunks();
            }
        } catch (IOException e) {
            if (stopFlag.get()) {
                setState(DownloadState.CANCELED, null);
            } else {
                lastErrorCode = classifyError(e);
                lastError = e.getMessage();
                setState(DownloadState.FAILED, e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (stopFlag.get()) {
                setState(DownloadState.CANCELED, null);
            } else {
                setState(DownloadState.FAILED, e.getMessage());
            }
        } catch (RuntimeException e) {
            if (stopFlag.get()) {
                setState(DownloadState.CANCELED, null);
            } else {
                String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                lastError = message;
                setState(DownloadState.FAILED, "下载任务异常: " + message);
            }
        }
        if (state == DownloadState.DOWNLOADING || state == DownloadState.WAITING) {
            setState(DownloadState.COMPLETE, null);
        }
    }

    private boolean downloadInfoEmpty() {
        return info.fileSize() <= 0 && (info.md5() == null || info.md5().isEmpty());
    }

    // --------------------------------------------------------------------- 整文件下载

    private void runSingleFile() throws IOException, InterruptedException {
        Path dest = destPath();
        FileUtils.createParents(dest);

        long baseOffset = 0;
        if (Files.exists(dest)) {
            long existing = Files.size(dest);
            if (existing >= info.fileSize() && info.fileSize() > 0) {
                // 大小一致：未要求 md5 或 md5 校验通过则直接完成
                if (info.md5() == null || info.md5().isEmpty() || verifyFileMd5(dest)) {
                    downloadedBytes = existing;
                    notifyProgress();
                    setState(DownloadState.COMPLETE, null);
                    return;
                }
                Files.deleteIfExists(dest);
            } else {
                baseOffset = existing;
            }
        }

        int retry = 0;
        boolean completed = false;
        while (!completed && !stopFlag.get()) {
            try {
                attemptWholeFile(dest, baseOffset);
                if (stopFlag.get()) {
                    setState(DownloadState.CANCELED, null);
                    return;
                }
                // 下载完成后校验整文件 MD5
                if (info.md5() != null && !info.md5().isEmpty() && !verifyFileMd5(dest)) {
                    Files.deleteIfExists(dest);
                    baseOffset = 0;
                    downloadedBytes = 0;
                    retry++;
                    retryCount++;
                    if (retry > maxRetryCount) {
                        throw new IOException("文件MD5校验失败: " + info.destPath());
                    }
                    lastError = "文件MD5校验失败，重试 " + info.destPath();
                    continue;
                }
                completed = true;
            } catch (IOException e) {
                if (stopFlag.get()) {
                    setState(DownloadState.CANCELED, null);
                    return;
                }
                lastErrorCode = classifyError(e);
                lastError = e.getMessage();
                if (retry >= maxRetryCount || !RetryHelper.canRetry(e)) {
                    throw e;
                }
                retry++;
                retryCount++;
                LOG.debug("下载重试 {}/{}: {}/{}", retry, maxRetryCount, info.destPath(), e.getMessage());
                // 目标改为完整重下，避免基于残缺文件续传
                baseOffset = 0;
                downloadedBytes = 0;
                Files.deleteIfExists(dest);
                waitIfNeeded(retry);
            }
        }
        if (completed && !stopFlag.get()) {
            setState(DownloadState.COMPLETE, null);
        }
    }

    private void attemptWholeFile(Path dest, long baseOffset) throws IOException, InterruptedException {
        String url = urlForRetry();
        HttpURLConnection conn = HttpUtils.openConnection(url, connectTimeoutMs, readTimeoutMs, false);
        if (baseOffset > 0) {
            conn.setRequestProperty("Range", "bytes=" + baseOffset + "-");
        }
        int code = conn.getResponseCode();
        if (baseOffset > 0 && code == HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new IOException("not support download range");
        }
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
            conn.disconnect();
            throw new IOException("HTTP " + code + " for " + url);
        }
        setState(DownloadState.DOWNLOADING, null);
        notifyPhase(DownloadPhase.DOWNLOADING);
        boolean append = baseOffset > 0 && code == HttpURLConnection.HTTP_PARTIAL;
        downloadedBytes = append ? baseOffset : 0;
        try (InputStream in = conn.getInputStream();
             var out = Files.newOutputStream(dest, append ? new java.nio.file.StandardOpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND} : new java.nio.file.StandardOpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING})) {
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (stopFlag.get()) {
                    setState(DownloadState.CANCELED, null);
                    conn.disconnect();
                    return;
                }
                if (pauseFlag.get()) {
                    handlePauseInLoop();
                }
                if (!bandwidthLimiter.acquire(read, stopFlag::get)) {
                    setState(DownloadState.CANCELED, null);
                    conn.disconnect();
                    return;
                }
                out.write(buffer, 0, read);
                downloadedBytes += read;
                maybeNotifyProgress();
            }
        }
    }

    // --------------------------------------------------------------------- 分块下载

    private void runWithChunks() throws IOException, InterruptedException {
        Path dest = destPath();
        if (Files.isRegularFile(dest) && Files.size(dest) >= info.fileSize()
                && (info.md5() == null || info.md5().isEmpty() || verifyFileMd5(dest))) {
            downloadedBytes = Files.size(dest);
            notifyProgress();
            setState(DownloadState.COMPLETE, null);
            return;
        }
        Path chunkDir = Path.of(dest.getFileName() + "_Chunks");
        Path chunkRoot = dest.getParent() != null ? dest.getParent().resolve(chunkDir) : chunkDir;
        FileUtils.createParents(chunkRoot);

        List<ChunkInfo> chunks = info.chunkInfoList();

        int retry = 0;
        boolean verified = false;
        while (!verified && !stopFlag.get()) {
            setState(DownloadState.DOWNLOADING, null);
            notifyPhase(DownloadPhase.DOWNLOADING);
            // 1. 逐块下载
            for (ChunkInfo chunk : chunks) {
                if (stopFlag.get()) {
                    setState(DownloadState.CANCELED, null);
                    return;
                }
                downloadChunk(chunkRoot, chunk);
            }
            if (stopFlag.get()) {
                setState(DownloadState.CANCELED, null);
                return;
            }
            // 2. 合并
            notifyPhase(DownloadPhase.MERGING);
            mergeChunks(chunkRoot, chunks, dest);
            // 3. 整文件校验
            if (info.md5() != null && !info.md5().isEmpty()) {
                verified = verifyFileMd5(dest);
                if (!verified) {
                    // 精简版：删除坏块目录，整体重下
                    cleanBadChunks(chunkRoot);
                    FileUtils.deleteRecursively(chunkRoot);
                    retry++;
                    if (retry > maxRetryCount) {
                        throw new IOException("分块下载MD5校验失败: " + info.destPath());
                    }
                    continue;
                }
            } else {
                verified = true;
            }
        }
        FileUtils.deleteRecursively(chunkRoot);
        setState(DownloadState.COMPLETE, null);
    }

    private void downloadChunk(Path chunkRoot, ChunkInfo chunk) throws IOException, InterruptedException {
        Path chunkFile = chunkRoot.resolve("chunk_" + chunk.start());
        long existing = Files.exists(chunkFile) ? Files.size(chunkFile) : 0;
        if (existing > chunk.length()) {
            Files.deleteIfExists(chunkFile);
            existing = 0;
        }
        if (existing == chunk.length()) {
            // 大小一致，逐块 md5 优先校验（有 md5 且开启二次校验则校验）
            if (chunk.md5() == null || chunk.md5().isEmpty() || verifyChunkMd5(chunkFile, chunk)) {
                downloadedBytes += chunk.length();
                maybeNotifyProgress();
                return;
            }
            Files.deleteIfExists(chunkFile);
            existing = 0;
        }

        long offset = chunk.start() + existing;
        String url = urlForRetry();
        HttpURLConnection conn = HttpUtils.openConnection(url, connectTimeoutMs, readTimeoutMs, false);
        if (offset > 0) {
            conn.setRequestProperty("Range", "bytes=" + offset + "-" + chunk.end());
        }
        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new IOException("HTTP " + code + " for " + url);
        }
        try (InputStream in = conn.getInputStream();
             var out = Files.newOutputStream(chunkFile,
                     existing > 0 ? new java.nio.file.StandardOpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND} : new java.nio.file.StandardOpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING})) {
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (stopFlag.get()) {
                    setState(DownloadState.CANCELED, null);
                    conn.disconnect();
                    return;
                }
                if (pauseFlag.get()) {
                    handlePauseInLoop();
                }
                if (!bandwidthLimiter.acquire(read, stopFlag::get)) {
                    setState(DownloadState.CANCELED, null);
                    conn.disconnect();
                    return;
                }
                out.write(buffer, 0, read);
                downloadedBytes += read;
                maybeNotifyProgress();
            }
        }
    }

    private void mergeChunks(Path chunkRoot, List<ChunkInfo> chunks, Path dest) throws IOException {
        FileUtils.createParents(dest);
        try (var out = Files.newOutputStream(dest)) {
            for (ChunkInfo chunk : chunks) {
                Path chunkFile = chunkRoot.resolve("chunk_" + chunk.start());
                try (var in = Files.newInputStream(chunkFile)) {
                    byte[] buffer = new byte[READ_BUFFER_SIZE];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private void cleanBadChunks(Path chunkRoot) {
        // 占位：本实现整体重下，若需精确坏块判定可扩展
    }

    // --------------------------------------------------------------------- 校验与工具

    private boolean verifyChunkMd5(Path chunkFile, ChunkInfo chunk) throws IOException {
        String actual = MD5Utils.md5(chunkFile);
        return actual != null && actual.equalsIgnoreCase(chunk.md5());
    }

    private boolean verifyFileMd5(Path dest) throws IOException {
        if (info.md5() == null || info.md5().isEmpty()) {
            return true;
        }
        setState(DownloadState.WAITING, null);
        notifyPhase(DownloadPhase.VERIFYING);
        String actual = MD5Utils.md5(dest, (completed, total) -> {
            if (md5CheckListener != null) {
                md5CheckListener.onMd5Check(completed, total);
            }
            if (stopFlag.get()) {
                // 校验阶段也响应停止，但不改状态（由 run 收敛）
            }
        });
        if (actual == null) {
            return false;
        }
        boolean ok = actual.equalsIgnoreCase(info.md5());
        if (!ok) {
            lastErrorCode = DownloadError.CHECK_MD5_FAILED;
            lastError = "文件MD5校验失败: " + info.destPath();
        }
        return ok;
    }

    private String urlForRetry() {
        // 按重试次数在 [备用1..备用N, 主URL] 间轮换，优先使用备用 CDN。
        List<String> urls = backUpUrls; // [backup..., primary]
        if (urls.isEmpty()) {
            return primaryUrl();
        }
        return urls.get(retryCount % urls.size());
    }

    private void waitIfNeeded(int retry) throws InterruptedException {
        long delay = (long) Math.pow(2, Math.min(retry, 6)) * 500;
        Thread.sleep(delay);
    }

    private void handlePauseInLoop() throws InterruptedException {
        if (pauseFlag.get() && !stopFlag.get() && state == DownloadState.DOWNLOADING) {
            setState(DownloadState.PAUSED, null);
        }
        while (pauseFlag.get() && !stopFlag.get()) {
            Thread.sleep(100);
        }
        if (state == DownloadState.PAUSED && !stopFlag.get() && !pauseFlag.get()) {
            setState(DownloadState.DOWNLOADING, null);
        }
    }

    private void maybeNotifyProgress() {
        long now = System.currentTimeMillis();
        if (now - lastProgressNotifyMs >= PROGRESS_NOTIFY_INTERVAL_MS) {
            notifyProgress();
            lastProgressNotifyMs = now;
        }
    }

    private void notifyProgress() {
        if (progressListener != null) {
            progressListener.onProgress(downloadedBytes, info.fileSize());
        }
    }

    private void setState(DownloadState newState, String error) {
        this.state = newState;
        if (stateListener != null) {
            stateListener.onStateChanged(newState, error);
        }
    }

    private void notifyPhase(DownloadPhase phase) {
        if (phaseListener != null) {
            phaseListener.onPhaseChanged(phase, info.destPath(), 0, 0);
        }
    }

    private int classifyError(IOException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
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
        return DownloadError.NETWORK;
    }
}
