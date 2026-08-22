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
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 游戏资源更新执行任务：接收检测结果 {@link ResourceCheckResult}，驱动
 * {@link GameUpdateService#runUpdate} 做差分合成更新。
 *
 * <p>检测（check）由 {@link GameResourceCheckTask} 负责；本任务只执行更新，并把更新阶段的展示状态
 * （阶段、版本、可见性、进度文本）以 JavaFX 只读属性暴露，供 Home / 资源管理界面绑定。</p>
 *
 * <p>"阶段"（phase）为业务更新阶段，与 {@link javafx.concurrent.Worker.State} 区分，用 {@link #phaseProperty()}。</p>
 */
public class GameResourceUpdateTask extends AbstractGameDownloadTask<ResourceOperationResult>
        implements TaskControl {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourceUpdateTask.class);

    /** 全局唯一 id，供两个界面通过 {@code TaskManageService.get(id)} 复用同一个更新任务。 */
    public static final String TASK_ID = "game-resource-update";

    public enum UpdateState {
        IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, PREPARING, DOWNLOADING, PAUSED,
        APPLYING, COMPLETED, FAILED, CANCELED
    }

    private final GameUpdateService updateService;
    private volatile ResourceCheckResult checkResult;

    private final ReadOnlyObjectWrapper<UpdateState> phase = new ReadOnlyObjectWrapper<>(UpdateState.IDLE);
    private final ReadOnlyStringWrapper statusText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper detailText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper currentVersion = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper progressText = new ReadOnlyStringWrapper("0%");
    private final ReadOnlyStringWrapper speedText = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper statusVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper retryVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper progressVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper pauseVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper resumeVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper cancelVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper operating = new ReadOnlyBooleanWrapper(false);

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
            onFx(() -> setPhase(UpdateState.FAILED,
                    LanguageManager.getString("ui.home.resource.update_failed"),
                    hasText(e.getMessage()) ? e.getMessage()
                            : LanguageManager.getString("ui.home.resource.update_failed_detail")));
            throw e;
        }
    }

    private ResourceOperationResult runUpdate() throws Exception {
        setPhase(UpdateState.PREPARING,
                LanguageManager.getString("ui.home.resource.preparing"),
                LanguageManager.getString("ui.home.resource.preparing_detail"));
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
        onFx(() -> {
            currentVersion.set(newVersion);
        });
        updateProgress(1, 1);
        setPhase(UpdateState.COMPLETED,
                LanguageManager.getString("ui.home.resource.update_complete"),
                String.format(LanguageManager.getString("ui.home.resource.current_version"), newVersion));
        return result;
    }

    private void handleUpdateProgress(ResourceProgress value) {
        UpdateState stage = mapProgressState(value.operationPhase());
        if (stage == UpdateState.DOWNLOADING && phase.get() != UpdateState.DOWNLOADING) {
            setPhase(UpdateState.DOWNLOADING,
                    LanguageManager.getString("ui.home.resource.downloading"),
                    LanguageManager.getString("ui.home.resource.download_start"));
        } else if (stage == UpdateState.APPLYING && phase.get() != UpdateState.APPLYING) {
            setPhase(UpdateState.APPLYING,
                    LanguageManager.getString("ui.home.resource.applying"),
                    LanguageManager.getString("ui.home.resource.applying_detail"));
        }
        long completed = value.completedBytes();
        long total = value.totalBytes();
        if (total > 0) {
            updateProgress(completed, total);
        } else {
            updateProgress(-1, 0);
        }
        String speed = (stage == UpdateState.DOWNLOADING)
                ? updateEmaSpeed(completed, speedSample, emaSpeed) : "";
        updateMessage(progressMessage(completed, total));
        onFx(() -> {
            this.speedText.set(speed);
            double p = getProgress() >= 0 ? getProgress() : 0;
            progressText.set(String.format(Locale.ROOT, "%.1f%%", p * 100));
        });
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
        if (phase.get() == UpdateState.DOWNLOADING) {
            updateService.pause();
            setPhase(UpdateState.PAUSED,
                    LanguageManager.getString("ui.home.resource.update_paused"), "");
            return true;
        }
        return false;
    }

    @Override
    public boolean resumeTask() {
        if (phase.get() == UpdateState.PAUSED) {
            updateService.resume();
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelTask() {
        if (phase.get() == UpdateState.APPLYING) {
            return true;
        }
        updateService.stop();
        cancel(true);
        return true;
    }

    @Override
    public boolean supportsPause() { return true; }

    // ---------------- 状态 ----------------

    private static UpdateState mapProgressState(ResourceOperationPhase phase) {
        return switch (phase) {
            case VERIFYING -> UpdateState.PREPARING;
            case DOWNLOADING -> UpdateState.DOWNLOADING;
            case APPLYING -> UpdateState.APPLYING;
            default -> UpdateState.PREPARING;
        };
    }

    private void setPhase(UpdateState newPhase, String status, String detail) {
        onFx(() -> {
            statusText.set(status != null ? status : "");
            detailText.set(detail != null ? detail : "");
            statusVisible.set(newPhase != UpdateState.IDLE);
            retryVisible.set(newPhase == UpdateState.FAILED || newPhase == UpdateState.CANCELED);
            progressVisible.set(isProgressState(newPhase));
            pauseVisible.set(newPhase == UpdateState.DOWNLOADING);
            resumeVisible.set(newPhase == UpdateState.PAUSED);
            cancelVisible.set(newPhase == UpdateState.PREPARING
                    || newPhase == UpdateState.DOWNLOADING || newPhase == UpdateState.PAUSED);
            operating.set(isOperatingState(newPhase));
            if (!isProgressState(newPhase) && newPhase != UpdateState.COMPLETED) {
                progressText.set("0%");
            }
            if (newPhase != UpdateState.DOWNLOADING) {
                speedText.set("");
            }
            phase.set(newPhase);
        });
    }

    private static void onFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private static boolean isProgressState(UpdateState value) {
        return value == UpdateState.PREPARING || value == UpdateState.DOWNLOADING
                || value == UpdateState.PAUSED || value == UpdateState.APPLYING;
    }

    private static boolean isOperatingState(UpdateState value) {
        return isProgressState(value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // ---------------- 对外属性 ----------------

    public ReadOnlyObjectProperty<UpdateState> phaseProperty() { return phase.getReadOnlyProperty(); }
    public UpdateState getPhase() { return phase.get(); }
    public ReadOnlyStringProperty statusTextProperty() { return statusText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty detailTextProperty() { return detailText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty currentVersionProperty() { return currentVersion.getReadOnlyProperty(); }
    public ReadOnlyStringProperty progressTextProperty() { return progressText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty speedTextProperty() { return speedText.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty statusVisibleProperty() { return statusVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty retryVisibleProperty() { return retryVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty progressVisibleProperty() { return progressVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty pauseVisibleProperty() { return pauseVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty resumeVisibleProperty() { return resumeVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty cancelVisibleProperty() { return cancelVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty operatingProperty() { return operating.getReadOnlyProperty(); }
}