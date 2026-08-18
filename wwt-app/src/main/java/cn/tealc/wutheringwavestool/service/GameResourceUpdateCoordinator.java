package cn.tealc.wutheringwavestool.service;

import cn.tealc.wwt.game.resource.ResourceCompletionListener;
import cn.tealc.wwt.game.resource.ResourceProgressListener;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceCheckState;
import cn.tealc.wwt.game.resource.model.ResourceOperationPhase;
import cn.tealc.wwt.game.resource.model.ResourceOperationResult;
import cn.tealc.wwt.game.resource.model.ResourceProgress;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JavaFX adapter for the game-resource update pipeline.
 *
 * <p>内部驱动老链路 {@link GameUpdateService}（差分合成更新），对外保留统一的
 * 呈现状态机（状态、版本、进度、速度、各控件可见性），供 Home 与资源管理界面复用。</p>
 */
@Singleton
public class GameResourceUpdateCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourceUpdateCoordinator.class);

    public enum UpdateState {
        IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, PREPARING, DOWNLOADING, PAUSED,
        APPLYING, COMPLETED, FAILED, CANCELED
    }

    private static final double EMA_ALPHA = 0.3;
    private static final long SAMPLE_INTERVAL_NANOS = 500_000_000L;

    private final GameUpdateService updateService;
    private final TaskManageService taskManageService;

    private final ReadOnlyObjectWrapper<UpdateState> state = new ReadOnlyObjectWrapper<>(UpdateState.IDLE);
    private final ReadOnlyStringWrapper statusText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper detailText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper currentVersion = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper latestVersion = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper actionText = new ReadOnlyStringWrapper("");
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0);
    private final ReadOnlyStringWrapper progressText = new ReadOnlyStringWrapper("0%");
    private final ReadOnlyStringWrapper speedText = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper statusVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper retryVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper updateActionVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper progressVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper pauseVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper resumeVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper cancelVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper operating = new ReadOnlyBooleanWrapper(false);

    private volatile Task<?> activeTask;
    private ResourceCheckResult checkResult;
    private boolean startAfterCheck;

    // 速率平滑采样（老链路进度回调驱动）
    private final long[] speedSample = {System.nanoTime(), 0L};
    private final double[] emaSpeed = {0};

    @Inject
    public GameResourceUpdateCoordinator(GameUpdateService updateService,
            TaskManageService taskManageService) {
        this.updateService = updateService;
        this.taskManageService = taskManageService;
        actionText.set(LanguageManager.getString("ui.home.button.start_update"));
    }

    public void checkForUpdates() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::checkForUpdates);
            return;
        }
        if (activeTask != null) {
            return;
        }
        if (GameResourcesManager.getGameExeBase() == null) {
            checkResult = null;
            setState(UpdateState.IDLE, "", "");
            return;
        }

        setState(UpdateState.CHECKING,
                LanguageManager.getString("ui.home.resource.checking"),
                LanguageManager.getString("ui.home.resource.checking_detail"));
        Task<ResourceCheckResult> task = new Task<>() {
            @Override
            protected ResourceCheckResult call() {
                updateTitle(LanguageManager.getString("ui.home.resource.task"));
                return updateService.checkUpdate();
            }
        };
        activeTask = task;
        task.setOnSucceeded(event -> completeCheck(task));
        task.setOnFailed(event -> failCheck(task));
        task.setOnCancelled(event -> {
            if (finishTask(task)) {
                startAfterCheck = false;
                setState(UpdateState.CANCELED,
                        LanguageManager.getString("ui.game_manager.asset.stopped"), "");
            }
        });
        taskManageService.execute(task);
    }

    public void startUpdate() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::startUpdate);
            return;
        }
        if (activeTask != null) {
            return;
        }
        if (checkResult == null) {
            startAfterCheck = true;
            checkForUpdates();
            return;
        }
        if (checkResult.installedVersion().equalsIgnoreCase(checkResult.latestVersion())) {
            showUpToDate(checkResult.installedVersion());
            return;
        }
        if (GameAppListener.getInstance().isRunning()) {
            setState(UpdateState.FAILED,
                    LanguageManager.getString("ui.home.resource.update_failed"),
                    LanguageManager.getString("ui.home.resource.close_game"));
            return;
        }

        UpdateTask task = new UpdateTask(checkResult);
        activeTask = task;
        progress.unbind();
        progress.bind(task.progressProperty());
        task.messageProperty().addListener((observable, oldValue, newValue) -> detailText.set(newValue));
        task.progressProperty().addListener((observable, oldValue, newValue) -> {
            double value = newValue != null ? newValue.doubleValue() : 0;
            progressText.set(value < 0 ? "--" : String.format(Locale.ROOT, "%.1f%%", value * 100));
        });
        setState(UpdateState.PREPARING,
                LanguageManager.getString("ui.home.resource.preparing"),
                LanguageManager.getString("ui.home.resource.preparing_detail"));
        task.setOnSucceeded(event -> completeUpdate(task));
        task.setOnFailed(event -> failUpdate(task));
        task.setOnCancelled(event -> cancelUpdate(task));
        taskManageService.execute(task);
    }

    public void retry() {
        if (checkResult != null
                && !checkResult.installedVersion().equalsIgnoreCase(checkResult.latestVersion())) {
            startUpdate();
        } else {
            checkForUpdates();
        }
    }

    public void pause() {
        if (state.get() == UpdateState.DOWNLOADING) {
            updateService.pause();
            setState(UpdateState.PAUSED,
                    LanguageManager.getString("ui.home.resource.update_paused"),
                    "");
        }
    }

    public void resume() {
        if (state.get() == UpdateState.PAUSED) {
            updateService.resume();
        }
    }

    public void cancel() {
        if (activeTask == null || state.get() == UpdateState.APPLYING) {
            return;
        }
        updateService.stop();
        activeTask.cancel(true);
    }

    private void completeCheck(Task<ResourceCheckResult> task) {
        if (!finishTask(task)) {
            return;
        }
        ResourceCheckResult result = task.getValue();
        if (result == null || !result.isSuccessful()) {
            startAfterCheck = false;
            setState(UpdateState.FAILED,
                    LanguageManager.getString("ui.home.resource.check_failed"),
                    LanguageManager.getString("ui.home.resource.check_failed_detail"));
            return;
        }
        checkResult = result;
        currentVersion.set(hasText(result.installedVersion()) ? result.installedVersion() : "-");
        latestVersion.set(hasText(result.latestVersion()) ? result.latestVersion() : "-");
        boolean upToDate = result.state() == ResourceCheckState.UP_TO_DATE
                || result.installedVersion().equalsIgnoreCase(result.latestVersion());
        if (upToDate) {
            startAfterCheck = false;
            showUpToDate(result.installedVersion());
            return;
        }
        setState(UpdateState.UPDATE_AVAILABLE,
                LanguageManager.getString("ui.home.resource.update_available"),
                String.format(LanguageManager.getString("ui.home.resource.version_diff"),
                        result.installedVersion(), result.latestVersion()));
        if (startAfterCheck) {
            startAfterCheck = false;
            startUpdate();
        }
    }

    private void failCheck(Task<ResourceCheckResult> task) {
        if (!finishTask(task)) {
            return;
        }
        startAfterCheck = false;
        LOG.warn("检查游戏资源更新失败", task.getException());
        setState(UpdateState.FAILED,
                LanguageManager.getString("ui.home.resource.check_failed"),
                LanguageManager.getString("ui.home.resource.check_failed_detail"));
    }

    private void completeUpdate(UpdateTask task) {
        if (!finishTask(task)) {
            return;
        }
        progress.unbind();
        progress.set(1);
        progressText.set("100%");
        // 老链路更新成功后本地版本即最新版本
        String newVersion = checkResult != null && hasText(checkResult.latestVersion())
                ? checkResult.latestVersion()
                : currentVersion.get();
        cacheInstalledVersion(newVersion);
        currentVersion.set(newVersion);
        latestVersion.set(newVersion);
        setState(UpdateState.COMPLETED,
                LanguageManager.getString("ui.home.resource.update_complete"),
                String.format(LanguageManager.getString("ui.home.resource.current_version"), newVersion));
    }

    private void failUpdate(UpdateTask task) {
        if (!finishTask(task)) {
            return;
        }
        progress.unbind();
        Throwable exception = task.getException();
        String message = exception != null && hasText(exception.getMessage())
                ? exception.getMessage()
                : LanguageManager.getString("ui.home.resource.update_failed_detail");
        LOG.error("游戏资源更新失败", exception);
        setState(UpdateState.FAILED,
                LanguageManager.getString("ui.home.resource.update_failed"), message);
    }

    private void cancelUpdate(UpdateTask task) {
        if (!finishTask(task)) {
            return;
        }
        progress.unbind();
        progress.set(0);
        progressText.set("0%");
        setState(UpdateState.CANCELED,
                LanguageManager.getString("ui.game_manager.asset.stopped"),
                LanguageManager.getString("ui.home.resource.update_canceled_detail"));
    }

    private void showUpToDate(String version) {
        setState(UpdateState.UP_TO_DATE,
                LanguageManager.getString("ui.home.resource.up_to_date"),
                String.format(LanguageManager.getString("ui.home.resource.current_version"), version));
    }

    private boolean finishTask(Task<?> task) {
        if (activeTask != task) {
            return false;
        }
        activeTask = null;
        return true;
    }

    private void setState(UpdateState newState, String status, String detail) {
        Runnable update = () -> {
            statusText.set(status != null ? status : "");
            detailText.set(detail != null ? detail : "");
            statusVisible.set(newState != UpdateState.IDLE);
            retryVisible.set(newState == UpdateState.FAILED || newState == UpdateState.CANCELED);
            updateActionVisible.set(newState == UpdateState.UPDATE_AVAILABLE);
            progressVisible.set(isProgressState(newState));
            pauseVisible.set(newState == UpdateState.DOWNLOADING);
            resumeVisible.set(newState == UpdateState.PAUSED);
            cancelVisible.set(newState == UpdateState.PREPARING
                    || newState == UpdateState.DOWNLOADING || newState == UpdateState.PAUSED);
            operating.set(isOperatingState(newState));
            actionText.set(newState == UpdateState.UPDATE_AVAILABLE
                    ? String.format(LanguageManager.getString("ui.home.button.update_to"), latestVersion.get())
                    : LanguageManager.getString("ui.home.button.start_update"));
            if (!isProgressState(newState) && newState != UpdateState.COMPLETED) {
                progressText.set("0%");
            }
            if (newState != UpdateState.DOWNLOADING) {
                speedText.set("");
            }
            state.set(newState);
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private static boolean isProgressState(UpdateState value) {
        return value == UpdateState.PREPARING || value == UpdateState.DOWNLOADING
                || value == UpdateState.PAUSED || value == UpdateState.APPLYING;
    }

    private static boolean isOperatingState(UpdateState value) {
        return value == UpdateState.CHECKING || isProgressState(value);
    }

    private void cacheInstalledVersion(String version) {
        try {
            Config.setting().setGameInstalledVersion(version);
            Config.setting().save();
        } catch (Exception e) {
            LOG.warn("保存游戏资源版本失败: {}", version, e);
        }
    }

    /** 驱动老链路 {@link GameUpdateService#runUpdate}，并把 legacy 进度映射到 JavaFX 状态机。 */
    private final class UpdateTask extends Task<ResourceOperationResult> {
        private final ResourceCheckResult initialResult;

        private UpdateTask(ResourceCheckResult initialResult) {
            this.initialResult = initialResult;
        }

        @Override
        protected ResourceOperationResult call() throws InterruptedException {
            updateTitle(LanguageManager.getString("ui.home.resource.update_task"));
            AtomicReference<ResourceOperationResult> resultRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            ResourceProgressListener progressListener = this::handleLegacyProgress;
            ResourceCompletionListener completionListener = result -> {
                resultRef.set(result);
                latch.countDown();
            };
            updateService.runUpdate(initialResult, progressListener, completionListener);
            latch.await();
            ResourceOperationResult result = resultRef.get();
            if (result == null) {
                throw new IllegalStateException("更新未返回结果");
            }
            if (!result.successful()) {
                throw new IllegalStateException(hasText(result.errorMessage())
                        ? result.errorMessage() : "更新失败");
            }
            return result;
        }

        private void handleLegacyProgress(ResourceProgress value) {
            UpdateState progressState = mapProgressState(value.operationPhase());
            if (progressState == UpdateState.DOWNLOADING && state.get() != UpdateState.DOWNLOADING) {
                setState(UpdateState.DOWNLOADING,
                        LanguageManager.getString("ui.home.resource.downloading"),
                        LanguageManager.getString("ui.home.resource.download_start"));
            } else if (progressState == UpdateState.APPLYING
                    && state.get() != UpdateState.APPLYING) {
                setState(UpdateState.APPLYING,
                        LanguageManager.getString("ui.home.resource.applying"),
                        LanguageManager.getString("ui.home.resource.applying_detail"));
            }

            long completed = value.completedBytes();
            long total = value.totalBytes();
            if (total > 0) {
                // Note: completed may exceed total (e.g. placeholder), clamp via Task anyway.
                updateProgress(completed, total);
            } else {
                updateProgress(-1, 0);
            }
            if (progressState == UpdateState.DOWNLOADING) {
                long now = System.nanoTime();
                long elapsed = now - speedSample[0];
                if (elapsed >= SAMPLE_INTERVAL_NANOS) {
                    double instant = Math.max(0, (completed - speedSample[1])
                            / (elapsed / 1_000_000_000.0));
                    emaSpeed[0] = (emaSpeed[0] == 0) ? instant
                            : EMA_ALPHA * instant + (1 - EMA_ALPHA) * emaSpeed[0];
                    speedSample[0] = now;
                    speedSample[1] = completed;
                    updateSpeedText(formatBytes((long) Math.max(0, emaSpeed[0])) + "/s");
                }
            }
            updateMessage(String.format(Locale.ROOT, "%s / %s  ·  %s/s",
                    formatBytes(completed), formatBytes(total),
                    formatBytes((long) Math.max(0, emaSpeed[0]))));
        }

        private UpdateState mapProgressState(ResourceOperationPhase phase) {
            return switch (phase) {
                case VERIFYING -> UpdateState.PREPARING;
                case DOWNLOADING -> UpdateState.DOWNLOADING;
                case APPLYING -> UpdateState.APPLYING;
                default -> UpdateState.PREPARING;
            };
        }
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

    private void updateSpeedText(String value) {
        Runnable update = () -> {
            if (state.get() == UpdateState.DOWNLOADING) {
                speedText.set(value);
            }
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public ReadOnlyObjectProperty<UpdateState> stateProperty() { return state.getReadOnlyProperty(); }
    public ReadOnlyStringProperty statusTextProperty() { return statusText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty detailTextProperty() { return detailText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty currentVersionProperty() { return currentVersion.getReadOnlyProperty(); }
    public ReadOnlyStringProperty latestVersionProperty() { return latestVersion.getReadOnlyProperty(); }
    public ReadOnlyStringProperty actionTextProperty() { return actionText.getReadOnlyProperty(); }
    public ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }
    public ReadOnlyStringProperty progressTextProperty() { return progressText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty speedTextProperty() { return speedText.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty statusVisibleProperty() { return statusVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty retryVisibleProperty() { return retryVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty updateActionVisibleProperty() { return updateActionVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty progressVisibleProperty() { return progressVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty pauseVisibleProperty() { return pauseVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty resumeVisibleProperty() { return resumeVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty cancelVisibleProperty() { return cancelVisible.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty operatingProperty() { return operating.getReadOnlyProperty(); }
}