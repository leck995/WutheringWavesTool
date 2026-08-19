package cn.tealc.wutheringwavestool.thread.game.download;

import cn.tealc.wwt.game.resource.ResourceCompletionListener;
import cn.tealc.wwt.game.resource.ResourceProgressListener;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceOperationResult;
import cn.tealc.wwt.game.resource.model.ResourceProgress;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** 预下载任务：驱动 {@link GameUpdateService#preDownload} 下载下一版本资源，进度经 Task 上抛。 */
public class GamePreDownloadTask extends Task<ResourceOperationResult> {
    private static final Logger LOG = LoggerFactory.getLogger(GamePreDownloadTask.class);

    private final GameUpdateService updateService;
    private final ResourceCheckResult checkResult;

    public GamePreDownloadTask(GameUpdateService updateService, ResourceCheckResult checkResult) {
        this.updateService = updateService;
        this.checkResult = checkResult;
    }

    @Override
    protected ResourceOperationResult call() throws Exception {
        updateTitle("正在预下载资源");
        AtomicReference<ResourceOperationResult> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        ResourceProgressListener progressListener = this::handleProgress;
        ResourceCompletionListener completionListener = result -> {
            resultRef.set(result);
            latch.countDown();
        };
        updateService.preDownload(checkResult, progressListener, completionListener);
        latch.await();
        ResourceOperationResult result = resultRef.get();
        if (result == null) {
            throw new IllegalStateException("预下载未返回结果");
        }
        if (!result.successful()) {
            throw new IllegalStateException(result.errorMessage() != null && !result.errorMessage().isBlank()
                    ? result.errorMessage() : "预下载失败");
        }
        updateProgress(1, 1);
        return result;
    }

    private void handleProgress(ResourceProgress value) {
        long completed = value.completedBytes();
        long total = value.totalBytes();
        if (total > 0) {
            updateProgress(completed, total);
        } else {
            updateProgress(-1, 0);
        }
        updateMessage(String.format(Locale.ROOT, "%s / %s",
                formatBytes(completed), formatBytes(total)));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        updateService.stopPreDownload();
        return super.cancel(mayInterruptIfRunning);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        do {
            value /= 1024;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }
}