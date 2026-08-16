package cn.tealc.wutheringwavestool.service;

import cn.tealc.wwt.game.resource.GameResourceInstallService;
import cn.tealc.wwt.game.resource.GameResourceRelease;
import cn.tealc.wwt.game.resource.DownloadOptions;
import cn.tealc.wwt.game.resource.ResourceOperationListener;
import cn.tealc.wwt.game.resource.ResourceUpdateOperation;
import cn.tealc.wwt.game.resource.model.ResourceOperationState;
import cn.tealc.wwt.game.resource.model.ResourceProgress;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.model.SourceType;
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

import java.nio.file.Path;
import java.util.Locale;

/**
 * JavaFX adapter for the resource-core installation API. It owns presentation state and
 * application-specific process checks; the core owns manifest planning and file updates.
 */
@Singleton
public class GameResourceUpdateCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourceUpdateCoordinator.class);

    public enum UpdateState {
        IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, PREPARING, DOWNLOADING, PAUSED,
        APPLYING, COMPLETED, FAILED, CANCELED
    }

    private final GameResourceInstallService installService;
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
    private volatile ResourceUpdateOperation activeOperation;
    private GameResourceRelease remoteRelease;
    private boolean startAfterCheck;

    @Inject
    public GameResourceUpdateCoordinator(GameResourceInstallService installService,
            TaskManageService taskManageService) {
        this.installService = installService;
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
            remoteRelease = null;
            setState(UpdateState.IDLE, "", "");
            return;
        }

        SourceType configuredSource = Config.setting().getGameRootDirSource();
        SourceType source = configuredSource != null ? configuredSource : SourceType.DEFAULT;
        setState(UpdateState.CHECKING,
                LanguageManager.getString("ui.home.resource.checking"),
                LanguageManager.getString("ui.home.resource.checking_detail"));
        Task<GameResourceRelease> task = new Task<>() {
            @Override
            protected GameResourceRelease call() {
                updateTitle(LanguageManager.getString("ui.home.resource.task"));
                return installService.check(source.toGameDownloadSource(), getGameDir(),
                        Config.setting().getGameInstalledVersion());
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
        if (remoteRelease == null) {
            startAfterCheck = true;
            checkForUpdates();
            return;
        }
        if (remoteRelease.installedVersion().equalsIgnoreCase(remoteRelease.latestVersion())) {
            showUpToDate(remoteRelease.installedVersion());
            return;
        }
        if (GameAppListener.getInstance().isRunning()) {
            setState(UpdateState.FAILED,
                    LanguageManager.getString("ui.home.resource.update_failed"),
                    LanguageManager.getString("ui.home.resource.close_game"));
            return;
        }

        ResourceUpdateTask task = new ResourceUpdateTask(remoteRelease);
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
        if (remoteRelease != null
                && !remoteRelease.installedVersion().equalsIgnoreCase(remoteRelease.latestVersion())) {
            startUpdate();
        } else {
            checkForUpdates();
        }
    }

    public void pause() {
        if (state.get() == UpdateState.DOWNLOADING && activeOperation != null) {
            activeOperation.pause();
        }
    }

    public void resume() {
        if (state.get() == UpdateState.PAUSED && activeOperation != null) {
            activeOperation.resume();
        }
    }

    public void cancel() {
        if (activeTask == null || state.get() == UpdateState.APPLYING) {
            return;
        }
        ResourceUpdateOperation operation = activeOperation;
        if (operation != null) {
            operation.cancel();
        }
        activeTask.cancel(true);
    }

    private void completeCheck(Task<GameResourceRelease> task) {
        if (!finishTask(task)) {
            return;
        }
        remoteRelease = task.getValue();
        currentVersion.set(remoteRelease.installedVersion());
        latestVersion.set(remoteRelease.latestVersion());
        if (remoteRelease.installedVersion().equalsIgnoreCase(remoteRelease.latestVersion())) {
            startAfterCheck = false;
            showUpToDate(remoteRelease.installedVersion());
            return;
        }
        setState(UpdateState.UPDATE_AVAILABLE,
                LanguageManager.getString("ui.home.resource.update_available"),
                String.format(LanguageManager.getString("ui.home.resource.version_diff"),
                        remoteRelease.installedVersion(), remoteRelease.latestVersion()));
        if (startAfterCheck) {
            startAfterCheck = false;
            startUpdate();
        }
    }

    private void failCheck(Task<GameResourceRelease> task) {
        if (!finishTask(task)) {
            return;
        }
        startAfterCheck = false;
        LOG.warn("检查游戏资源更新失败", task.getException());
        setState(UpdateState.FAILED,
                LanguageManager.getString("ui.home.resource.check_failed"),
                LanguageManager.getString("ui.home.resource.check_failed_detail"));
    }

    private void completeUpdate(ResourceUpdateTask task) {
        if (!finishTask(task)) {
            return;
        }
        progress.unbind();
        progress.set(1);
        progressText.set("100%");
        GameResourceRelease completedRelease = task.getValue();
        String installedVersion = completedRelease.latestVersion();
        currentVersion.set(installedVersion);
        latestVersion.set(installedVersion);
        cacheInstalledVersion(installedVersion);
        remoteRelease = completedRelease;
        setState(UpdateState.COMPLETED,
                LanguageManager.getString("ui.home.resource.update_complete"),
                String.format(LanguageManager.getString("ui.home.resource.current_version"), installedVersion));
    }

    private void failUpdate(ResourceUpdateTask task) {
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

    private void cancelUpdate(ResourceUpdateTask task) {
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
        activeOperation = null;
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

    private static Path getGameDir() {
        var gameDir = GameResourcesManager.getGameDir();
        if (gameDir == null) {
            throw new IllegalStateException("游戏目录未设置");
        }
        return gameDir.toPath().toAbsolutePath().normalize();
    }

    private void cacheInstalledVersion(String version) {
        try {
            Config.setting().setGameInstalledVersion(version);
            Config.setting().save();
        } catch (Exception e) {
            LOG.warn("保存游戏资源版本失败: {}", version, e);
        }
    }

    private final class ResourceUpdateTask extends Task<GameResourceRelease> {
        private final GameResourceRelease initialRelease;
        private final long[] speedSample = {System.nanoTime(), 0L};
        private final double[] speed = {0};

        private ResourceUpdateTask(GameResourceRelease initialRelease) {
            this.initialRelease = initialRelease;
        }

        @Override
        protected GameResourceRelease call() throws Exception {
            updateTitle(LanguageManager.getString("ui.home.resource.update_task"));
            ResourceUpdateOperation operation = installService.createUpdate(getGameDir(), initialRelease,
                    new ResourceOperationListener() {
                        @Override
                        public void onStateChanged(ResourceOperationState value) {
                            handleOperationState(value);
                        }

                        @Override
                        public void onProgress(ResourceProgress value) {
                            handleProgress(value);
                        }
                    }, downloadOptions());
            activeOperation = operation;
            if (isCancelled()) {
                operation.cancel();
            }
            try {
                return operation.execute();
            } finally {
                if (activeOperation == operation) {
                    activeOperation = null;
                }
            }
        }

        private void handleOperationState(ResourceOperationState value) {
            switch (value) {
                case PREPARING -> phase(UpdateState.PREPARING,
                        LanguageManager.getString("ui.home.resource.preparing"),
                        LanguageManager.getString("ui.home.resource.preparing_detail"));
                case DOWNLOADING -> phase(UpdateState.DOWNLOADING,
                        LanguageManager.getString("ui.home.resource.downloading"),
                        LanguageManager.getString("ui.home.resource.download_start"));
                case PAUSED -> phase(UpdateState.PAUSED,
                        LanguageManager.getString("ui.home.resource.update_paused"), detailText.get());
                case APPLYING -> {
                    if (GameAppListener.getInstance().isRunning()) {
                        throw new IllegalStateException(LanguageManager.getString("ui.home.resource.close_game"));
                    }
                    phase(UpdateState.APPLYING,
                            LanguageManager.getString("ui.home.resource.applying"),
                            LanguageManager.getString("ui.home.resource.applying_detail"));
                }
                case CANCELED -> setState(UpdateState.CANCELED,
                        LanguageManager.getString("ui.game_manager.asset.stopped"),
                        LanguageManager.getString("ui.home.resource.update_canceled_detail"));
                case FAILED, COMPLETED -> {
                    // Task completion supplies final status and error details.
                }
            }
        }

        private void handleProgress(ResourceProgress value) {
            long completed = value.completedBytes();
            long total = value.totalBytes();
            updateProgress(completed, total);
            long now = System.nanoTime();
            long elapsed = now - speedSample[0];
            if (elapsed >= 500_000_000L) {
                speed[0] = (completed - speedSample[1]) / (elapsed / 1_000_000_000.0);
                speedSample[0] = now;
                speedSample[1] = completed;
                updateSpeedText(formatBytes((long) Math.max(0, speed[0])) + "/s");
            }
            updateMessage(String.format(Locale.ROOT, "%s / %s  ·  %s/s",
                    formatBytes(completed), formatBytes(total),
                    formatBytes((long) Math.max(0, speed[0]))));
        }

        private void phase(UpdateState updateState, String status, String detail) {
            if (updateState == UpdateState.APPLYING) {
                updateProgress(-1, -1);
            }
            updateMessage(detail);
            setState(updateState, status, detail);
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

    private static DownloadOptions downloadOptions() {
        return new DownloadOptions(Config.setting().getDownloadParallelCount(),
                Config.setting().getDownloadSpeedLimitBytesPerSecond());
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
