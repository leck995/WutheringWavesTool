package cn.tealc.wutheringwavestool.thread.game.download;

import cn.tealc.wwt.game.resource.ResourceCompletionListener;
import cn.tealc.wwt.game.resource.ResourceProgressListener;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceOperationPhase;
import cn.tealc.wwt.game.resource.model.ResourceOperationResult;
import cn.tealc.wwt.game.resource.model.ResourceProgress;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.TaskControl;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 游戏资源更新执行任务：接收检测结果 {@link ResourceCheckResult}，驱动
 * {@link GameUpdateService#runUpdate} 做差分合成更新。
 *
 * <p>检测（check）由 {@link GameResourceCheckTask} 负责；本任务只执行更新。
 * 进度/状态/文案均使用 Task 内置属性：{@link #progressProperty()}、
 * {@link #messageProperty()}、{@link #titleProperty()}、{@link #stateProperty()}。</p>
 */
public class GameResourceUpdateTask extends AbstractGameDownloadTask<ResourceOperationResult>
        implements TaskControl {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourceUpdateTask.class);

    /** 全局唯一 id，供两个界面通过 {@code TaskManageService.get(id)} 复用同一个更新任务。 */
    public static final String TASK_ID = "game-resource-update";

    private final GameUpdateService updateService;
    private volatile ResourceCheckResult checkResult;

    private final ReadOnlyStringWrapper currentVersion = new ReadOnlyStringWrapper("-");

    private final long[] speedSample = {System.nanoTime(), 0L};
    private final double[] emaSpeed = {0};

    public GameResourceUpdateTask(GameUpdateService updateService) {
        this.updateService = updateService;
    }

    public void setCheckResult(ResourceCheckResult checkResult) {
        this.checkResult = checkResult;
    }

    @Override
    protected ResourceOperationResult call() throws Exception {
        try {
            return runUpdate();
        } catch (Exception e) {
            onFx(() -> {
                updateTitle(LanguageManager.getString("ui.home.resource.update_failed"));
                updateMessage(hasText(e.getMessage()) ? e.getMessage()
                        : LanguageManager.getString("ui.home.resource.update_failed_detail"));
            });
            throw e;
        }
    }

    private ResourceOperationResult runUpdate() throws Exception {
        updateTitle(LanguageManager.getString("ui.home.resource.preparing"));
        updateMessage(LanguageManager.getString("ui.home.resource.preparing_detail"));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ResourceOperationResult> resultRef = new AtomicReference<>();
        updateService.runUpdate(checkResult, this::handleUpdateProgress, result -> {
            resultRef.set(result);
            latch.countDown();
        });
        latch.await();
        ResourceOperationResult result = resultRef.get();
        if (result == null) {
            throw new IllegalStateException("更新未返回结果");
        }
        if (!result.successful()) {
            throw new IllegalStateException(GameUpdateService.friendlyError(result.errorCode(),
                    hasText(result.errorMessage()) ? result.errorMessage() : "更新失败"));
        }
        String newVersion = hasText(checkResult.latestVersion()) ? checkResult.latestVersion() : "-";
        cacheInstalledVersion(newVersion);
        onFx(() -> currentVersion.set(newVersion));
        updateProgress(1, 1);
        updateTitle(LanguageManager.getString("ui.home.resource.update_complete"));
        updateMessage(String.format(LanguageManager.getString("ui.home.resource.current_version"), newVersion));
        return result;
    }

    private void handleUpdateProgress(ResourceProgress value) {
        ResourceOperationPhase opPhase = value.operationPhase();
        if (opPhase == ResourceOperationPhase.DOWNLOADING && !isPaused()) {
            updateTitle(LanguageManager.getString("ui.home.resource.downloading"));
            updateMessage(LanguageManager.getString("ui.home.resource.download_start"));
        } else if (opPhase == ResourceOperationPhase.APPLYING) {
            updateTitle(LanguageManager.getString("ui.home.resource.applying"));
            updateMessage(LanguageManager.getString("ui.home.resource.applying_detail"));
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
        updateMessage(progressMessage(completed, total));
        onFx(() -> speedText.set(speed));
    }

    private void cacheInstalledVersion(String version) {
        try {
            Config.setting().setGameInstalledVersion(version);
            Config.setting().save();
        } catch (Exception e) {
            LOG.warn("保存游戏资源版本失败: {}", version, e);
        }
    }

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
                updateTitle(LanguageManager.getString("ui.home.resource.update_paused"));
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
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelTask() {
        if (isPaused()) {
            // 暂停状态下允许取消
            updateService.stop();
            cancel(true);
            return true;
        }
        return cancel(true);
    }

    @Override
    public boolean supportsPause() { return true; }

    // ---------------- 对外属性 ----------------

    public ReadOnlyStringProperty currentVersionProperty() { return currentVersion.getReadOnlyProperty(); }

    // ---------------- 工具 ----------------

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}