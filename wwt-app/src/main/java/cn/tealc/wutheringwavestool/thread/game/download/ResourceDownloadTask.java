package cn.tealc.wutheringwavestool.thread.game.download;

import cn.tealc.wutheringwavestool.base.AppInjector;
import com.kuro.game.model.game.FileInfo;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多线程下载任务，传入文件列表、URL前缀和保存目录进行并发下载，进度上报至 TaskManageService
 */
public class ResourceDownloadTask extends Task<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceDownloadTask.class);
    private static final int MAX_CONCURRENT = 4;
    private static final int MAX_RETRIES = 3;

    private final List<FileInfo> fileInfoList;
    private final String host;
    private final File gameDir;
    private final HttpClient httpClient;

    private volatile boolean paused = false;

    private final AtomicLong totalDownloadedBytes = new AtomicLong(0);
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final long totalBytes;

    public ResourceDownloadTask(List<FileInfo> fileInfoList, String host, File gameDir) {
        this.fileInfoList = fileInfoList;
        this.host = host;
        this.gameDir = gameDir;
        this.httpClient = AppInjector.getInstance(HttpClient.class);

        long tb = 0;
        for (FileInfo fi : fileInfoList) {
            tb += fi.getSize() != null ? fi.getSize() : 0;
        }
        this.totalBytes = tb;

        updateTitle("游戏资源下载");
    }

    @Override
    protected Void call() throws Exception {
        if (fileInfoList.isEmpty()) {
            updateMessage("无文件需要下载");
            return null;
        }

        Semaphore semaphore = new Semaphore(MAX_CONCURRENT);
        CountDownLatch latch = new CountDownLatch(fileInfoList.size());

        for (FileInfo fileInfo : fileInfoList) {
            semaphore.acquire();
            Thread.startVirtualThread(() -> {
                try {
                    downloadFile(fileInfo);
                } catch (Exception e) {
                    LOG.error("下载失败: {}", fileInfo.getDest(), e);
                } finally {
                    semaphore.release();
                    latch.countDown();
                }
            });
        }

        latch.await();

        if (isCancelled()) {
            updateMessage("下载已取消");
        } else {
            updateProgress(1.0, 1.0);
            updateMessage("下载完成");
        }
        return null;
    }

    private void downloadFile(FileInfo fileInfo) throws Exception {
        String dest = fileInfo.getDest();
        String url = host + dest.replace(" ", "%20");
        File outputFile = new File(gameDir, dest);
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 跳过已存在且大小一致的文件
        if (outputFile.exists() && outputFile.length() == fileInfo.getSize()) {
            totalDownloadedBytes.addAndGet(fileInfo.getSize());
            completedCount.incrementAndGet();
            updateOverallProgress();
            return;
        }

        IOException lastException = null;
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                downloadFileInternal(url, outputFile);
                return;
            } catch (IOException e) {
                lastException = e;
                Files.deleteIfExists(outputFile.toPath());
                if (isCancelled()) return;
                if (retry < MAX_RETRIES - 1) {
                    long delay = (long) Math.pow(2, retry) * 1000;
                    LOG.debug("下载重试 {}/{}，等待 {}ms: {}", retry + 1, MAX_RETRIES, delay, dest);
                    Thread.sleep(delay);
                }
            }
        }
        throw lastException;
    }

    private void downloadFileInternal(String url, File outputFile) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("下载被中断: " + url, e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }

        byte[] buffer = new byte[8192];
        int bytesRead;
        long lastUpdate = 0;
        try (InputStream is = response.body();
             OutputStream os = new FileOutputStream(outputFile)) {
            while (true) {
                if (isCancelled()) {
                    Files.deleteIfExists(outputFile.toPath());
                    return;
                }
                if (paused) {
                    Thread.sleep(200);
                    continue;
                }
                bytesRead = is.read(buffer);
                if (bytesRead == -1) break;
                os.write(buffer, 0, bytesRead);
                totalDownloadedBytes.addAndGet(bytesRead);

                long now = System.currentTimeMillis();
                if (now - lastUpdate > 1000) {
                    updateOverallProgress();
                    lastUpdate = now;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("下载被中断: " + url, e);
        }

        if (!isCancelled()) {
            completedCount.incrementAndGet();
        }
        updateOverallProgress();
    }

    private void updateOverallProgress() {
        if (totalBytes > 0) {
            double progress = Math.min((double) totalDownloadedBytes.get() / totalBytes, 1.0);
            int done = completedCount.get();
            int total = fileInfoList.size();
            updateProgress(progress, 1.0);
            updateMessage(String.format("已下载 %d/%d 个文件", done, total));
        }
    }

    public void pause() {
        paused = true;
        updateMessage("已暂停");
    }

    public void resume() {
        paused = false;
        updateMessage("下载中");
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = super.cancel(mayInterruptIfRunning);
        updateMessage("下载已取消");
        return result;
    }
}
