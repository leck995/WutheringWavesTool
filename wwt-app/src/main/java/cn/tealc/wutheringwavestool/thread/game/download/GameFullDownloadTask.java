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
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.Locale;
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

    /** 全量下载阶段。 */
    public enum Phase {
        IDLE, CHECKING, DOWNLOADING, VERIFYING, APPLYING, PAUSED, COMPLETED, FAILED, CANCELED
    }

    private final GameUpdateService updateService;

    private final ReadOnlyObjectWrapper<Phase> phase = new ReadOnlyObjectWrapper<>(Phase.IDLE);
    private final ReadOnlyStringWrapper progressText = new ReadOnlyStringWrapper("0%");
    private final ReadOnlyStringWrapper speedText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper detailText = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper pauseVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper resumeVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper cancelVisible = new ReadOnlyBooleanWrapper(false);

    private final long[] speedSample = {System.nanoTime(), 0L};
    private final double[] emaSpeed = {0};

    public GameFullDownloadTask(GameUpdateService updateService) {
        this.updateService = updateService;
    }

    @Override
    protected ResourceOperationResult call() throws Exception {
        setPhase(Phase.CHECKING, "正在检查更新");
        ResourceCheckResult checkResult = updateService.checkUpdate();
        if (checkResult == null || !checkResult.isSuccessful()) {
            throw new IllegalStateException("检查游戏更新失败");
        }
        return runFullDownload(checkResult);
    }

    private ResourceOperationResult runFullDownload(ResourceCheckResult checkResult) throws Exception {
        setPhase(Phase.DOWNLOADING, "正在下载游戏资源");
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
            throw new IllegalStateException(hasText(result.errorMessage())
                    ? result.errorMessage() : "全量下载失败");
        }
        updateProgress(1, 1);
        setPhase(Phase.COMPLETED, "下载完成");
        return result;
    }

    private void handleProgress(ResourceProgress value) {
        ResourceOperationPhase opPhase = value.operationPhase();
        Phase stage = mapPhase(opPhase);
        // 阶段切换时同步 phase（含暂停状态保持）。
        if (stage == Phase.DOWNLOADING && phase.get() != Phase.DOWNLOADING
                && phase.get() != Phase.PAUSED) {
            setPhase(Phase.DOWNLOADING, "正在下载游戏资源");
        } else if (stage == Phase.VERIFYING && phase.get() != Phase.VERIFYING) {
            setPhase(Phase.VERIFYING, "正在校验文件");
        } else if (stage == Phase.APPLYING && phase.get() != Phase.APPLYING) {
            setPhase(Phase.APPLYING, "正在合成文件");
        }

        long completed = value.completedBytes();
        long total = value.totalBytes();
        if (total > 0) {
            updateProgress(completed, total);
        } else {
            updateProgress(-1, 0);
        }
        String speed = (stage == Phase.DOWNLOADING && phase.get() == Phase.DOWNLOADING)
                ? updateEmaSpeed(completed, speedSample, emaSpeed) : "";
        onFx(() -> {
            speedText.set(speed);
            double p = getProgress() >= 0 ? getProgress() : 0;
            progressText.set(String.format(Locale.ROOT, "%.1f%%", p * 100));
            detailText.set(progressMessage(completed, total, speed));
        });
    }

    private void setPhase(Phase newPhase, String detail) {
        onFx(() -> {
            onPhaseChanged(newPhase, detail);
        });
    }

    /** 在 FX 线程内更新 phase 派生的只读属性。 */
    private void onPhaseChanged(Phase newPhase, String detail) {
        phase.set(newPhase);
        if (detail != null) {
            detailText.set(detail);
        }
        boolean progressing = newPhase == Phase.CHECKING || newPhase == Phase.DOWNLOADING
                || newPhase == Phase.VERIFYING || newPhase == Phase.APPLYING
                || newPhase == Phase.PAUSED;
        pauseVisible.set(newPhase == Phase.DOWNLOADING);
        resumeVisible.set(newPhase == Phase.PAUSED);
        cancelVisible.set(progressing);
        if (!progressing) {
            progressText.set(newPhase == Phase.COMPLETED ? "100%" : "0%");
            speedText.set("");
        }
    }

    // ---------------- 操作 ----------------

    public void pause() {
        if (phase.get() == Phase.DOWNLOADING) {
            updateService.pause();
            onFx(() -> phase.set(Phase.PAUSED));
        }
    }

    public void resume() {
        if (phase.get() == Phase.PAUSED) {
            updateService.resume();
            onFx(() -> phase.set(Phase.DOWNLOADING));
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
        pause();
        return true;
    }

    @Override
    public boolean resumeTask() {
        resume();
        return true;
    }

    @Override
    public boolean cancelTask() {
        cancel(true);
        return true;
    }

    @Override
    public boolean supportsPause() {
        return true;
    }

    // ---------------- 面向资源管理界面暴露的只读属性 ----------------

    public ReadOnlyObjectProperty<Phase> phaseProperty() {
        return phase.getReadOnlyProperty();
    }

    public Phase getPhase() {
        return phase.get();
    }

    public ReadOnlyStringProperty progressTextProperty() {
        return progressText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty speedTextProperty() {
        return speedText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty detailTextProperty() {
        return detailText.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty pauseVisibleProperty() {
        return pauseVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty resumeVisibleProperty() {
        return resumeVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty cancelVisibleProperty() {
        return cancelVisible.getReadOnlyProperty();
    }

    // ---------------- 工具 ----------------

    private static Phase mapPhase(ResourceOperationPhase phase) {
        return switch (phase) {
            case VERIFYING -> Phase.VERIFYING;
            case DOWNLOADING -> Phase.DOWNLOADING;
            case APPLYING -> Phase.APPLYING;
            default -> Phase.DOWNLOADING;
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void onFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}