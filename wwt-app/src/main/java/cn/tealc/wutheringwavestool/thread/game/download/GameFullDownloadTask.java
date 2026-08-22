package cn.tealc.wutheringwavestool.thread.game.download;

import cn.tealc.wwt.game.resource.ResourceCompletionListener;
import cn.tealc.wwt.game.resource.ResourceProgressListener;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceOperationPhase;
import cn.tealc.wwt.game.resource.model.ResourceOperationResult;
import cn.tealc.wwt.game.resource.model.ResourceProgress;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.TaskControl;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 全量下载任务（全新安装）：对一个「未登记、无 exe」的游戏目录执行完整资源下载。
 *
 * <p>与 {@link GameResourceUpdateTask}（下载 patch 并调用 hpatchz 合成、用于已安装版本的增量更新）
 * 不同，本任务面向「未安装」场景：先 {@link GameUpdateService#checkUpdate} 使 legacy 引擎进入
 * {@code STATE_NEED_DOWNLOAD}，再 {@link GameUpdateService#runUpdate} 走 {@code UpdateFlow}
 * 的「无 patch → 直接 move」分支，用完整文件清单直接下载，不涉及差分合成。</p>
 *
 * <p>资源管理界面通过固定 {@link #TASK_ID} 复用本任务，重新进入页面时可恢复进度展示；
 * 与 Home 页面无关。</p>
 */
public class GameFullDownloadTask extends AbstractGameDownloadTask<ResourceOperationResult>
        implements TaskControl {
    /** 全局唯一 id，资源管理界面通过它复用同一个全量下载任务。 */
    public static final String TASK_ID = "game-full-download";

    private final GameUpdateService updateService;

    private final long[] speedSample = {System.nanoTime(), 0L};
    private final double[] emaSpeed = {0};

    public GameFullDownloadTask(GameUpdateService updateService) {
        this.updateService = updateService;
    }

    @Override
    protected ResourceOperationResult call() throws Exception {
        try {
            updateTitle("正在检查更新");
            ResourceCheckResult checkResult = updateService.checkUpdate();
            if (checkResult == null || !checkResult.isSuccessful()) {
                throw new IllegalStateException("检查游戏更新失败");
            }
            return runFullDownload(checkResult);
        } catch (Exception e) {
            final String message = hasText(e.getMessage()) ? e.getMessage() : "全量下载失败";
            onFx(() -> updateMessage(message));
            throw e;
        }
    }

    private ResourceOperationResult runFullDownload(ResourceCheckResult checkResult) throws Exception {
        updateTitle("正在下载游戏资源");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ResourceOperationResult> resultRef = new AtomicReference<>();
        ResourceProgressListener progressListener = this::handleProgress;
        ResourceCompletionListener completionListener = result -> {
            resultRef.set(result);
            latch.countDown();
        };
        updateService.runUpdate(checkResult, progressListener, completionListener);
        latch.await();
        ResourceOperationResult result = resultRef.get();
        if (result == null) {
            throw new IllegalStateException("全量下载未返回结果");
        }
        if (!result.successful()) {
            throw new IllegalStateException(GameUpdateService.friendlyError(result.errorCode(),
                    hasText(result.errorMessage()) ? result.errorMessage() : "全量下载失败"));
        }
        updateProgress(1, 1);
        updateTitle("下载完成");
        updateMessage("");
        return result;
    }

    private void handleProgress(ResourceProgress value) {
        ResourceOperationPhase opPhase = value.operationPhase();
        if (opPhase == ResourceOperationPhase.VERIFYING) {
            updateTitle("正在校验文件");
        } else if (opPhase == ResourceOperationPhase.APPLYING) {
            updateTitle("正在合成文件");
        } else if (opPhase == ResourceOperationPhase.DOWNLOADING && !isPaused()) {
            updateTitle("正在下载游戏资源");
        }

        long completed = value.completedBytes();
        long total = value.totalBytes();
        if (total > 0) {
            updateProgress(completed, total);
        } else {
            updateProgress(-1, 0);
        }
        String speed = (opPhase == ResourceOperationPhase.DOWNLOADING && !isPaused())
                ? updateEmaSpeed(completed, speedSample, emaSpeed) : "";
        String msg = progressMessage(completed, total);
        onFx(() -> {
            speedText.set(speed);
            updateMessage(msg);
        });
    }

    // ---------------- 操作 ----------------

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        updateService.stop();
        return super.cancel(mayInterruptIfRunning);
    }

    // ---------------- TaskControl ----------------

    @Override
    public boolean pauseTask() {
        if (isRunning() && !isPaused()) {
            updateService.pause();
            onFx(() -> {
                paused.set(true);
                updateTitle("已暂停");
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean resumeTask() {
        if (isPaused()) {
            updateService.resume();
            onFx(() -> {
                paused.set(false);
                updateTitle("正在下载游戏资源");
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelTask() {
        return cancel(true);
    }

    @Override
    public boolean supportsPause() {
        return true;
    }

    // ---------------- 工具 ----------------

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}