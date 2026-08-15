package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wwt.game.resource.DownloadManager;
import cn.tealc.wwt.game.resource.GameResourceDownloadService;
import cn.tealc.wwt.game.resource.model.DownloadState;
import cn.tealc.wwt.game.resource.model.game.FileInfo;
import cn.tealc.wwt.game.resource.model.launcher.UpdateData;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameResourceUpdateCoordinator;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceCheckState;
import cn.tealc.wwt.game.resource.model.ResourceOperationResult;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 统一「游戏资源管理」ViewModel：全量下载 + 增量更新 + 预下载 + 校验修复合一。
 *
 * <p>未安装时提供全量下载；已安装时自动检查更新，并据此提供更新、预下载和校验修复。</p>
 */
public class GameAssetViewModel extends BaseViewModel implements SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(GameAssetViewModel.class);
    private static final long NO_OPERATION = -1;

    public enum OperationState {
        IDLE,
        RUNNING,
        PAUSED,
        STOPPING
    }

    private enum OperationType {
        NONE,
        DOWNLOAD,
        UPDATE,
        PRE_DOWNLOAD,
        REPAIR
    }

    private static final class ActiveDownload {
        private final long operationId;
        private final DownloadManager manager;

        private ActiveDownload(long operationId, DownloadManager manager) {
            this.operationId = operationId;
            this.manager = manager;
        }
    }

    @Inject
    private GameResourceDownloadService downloadService;
    @Inject
    private GameUpdateService updateService;
    @Inject
    private TaskManageService taskManageService;
    @Inject
    private GameResourceUpdateCoordinator resourceUpdateCoordinator;

    private final BooleanProperty operating = new SimpleBooleanProperty(false);
    private final BooleanProperty pauseAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty stopAvailable = new SimpleBooleanProperty(false);
    private final ReadOnlyObjectWrapper<OperationState> operationState =
            new ReadOnlyObjectWrapper<>(OperationState.IDLE);

    private final StringProperty currentVersion = new SimpleStringProperty("-");
    private final StringProperty latestVersion = new SimpleStringProperty("-");
    private final StringProperty status = new SimpleStringProperty("就绪");

    private final BooleanProperty showDownload = new SimpleBooleanProperty(true);
    private final BooleanProperty showUpdate = new SimpleBooleanProperty(false);
    private final BooleanProperty showRepair = new SimpleBooleanProperty(false);
    private final BooleanProperty showPreDownload = new SimpleBooleanProperty(false);

    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty("0%");
    private final StringProperty tip = new SimpleStringProperty("");
    private final StringProperty downloadDir = new SimpleStringProperty();
    private final ObjectProperty<SourceType> downloadSource =
            new SimpleObjectProperty<>(SourceType.DEFAULT);

    private ResourceCheckResult checkResult;
    private boolean downloadSourceInitialized;

    private long operationSequence;
    private volatile long activeOperationId = NO_OPERATION;
    private volatile Task<?> activeTask;
    private OperationType activeOperation = OperationType.NONE;
    private final AtomicReference<ActiveDownload> activeDownload = new AtomicReference<>();

    private long checkSequence;
    private long activeCheckId = NO_OPERATION;
    private Task<ResourceCheckResult> checkTask;
    private boolean coordinatorListenersAttached;
    private final ChangeListener<GameResourceUpdateCoordinator.UpdateState> coordinatorStateListener =
            (observable, oldState, newState) -> syncResourceUpdateState(newState);
    private final ChangeListener<Number> coordinatorProgressListener =
            (observable, oldValue, newValue) -> {
                if (activeOperation == OperationType.UPDATE) {
                    progress.set(newValue.doubleValue());
                }
            };
    private final ChangeListener<String> coordinatorProgressTextListener =
            (observable, oldValue, newValue) -> {
                if (activeOperation == OperationType.UPDATE) {
                    progressText.set(newValue);
                }
            };
    private final ChangeListener<String> coordinatorDetailListener =
            (observable, oldValue, newValue) -> {
                if (activeOperation == OperationType.UPDATE) {
                    tip.set(newValue);
                }
            };

    public GameAssetViewModel() {
        downloadSource.addListener((observable, oldSource, newSource) -> {
            SourceType normalizedSource = normalizeDownloadSource(newSource);
            if (newSource != normalizedSource) {
                downloadSource.set(normalizedSource);
                return;
            }
            if (downloadSourceInitialized && GameResourcesManager.getGameExeBase() == null) {
                Config.setting().setGameRootDirSource(normalizedSource);
                Config.setting().save();
            }
        });
    }

    public void initialize() {
        syncResourceUpdateState(resourceUpdateCoordinator.stateProperty().get());
    }

    @Override
    public void onViewAdded() {
        attachCoordinatorListeners();
        syncResourceUpdateState(resourceUpdateCoordinator.stateProperty().get());
        SourceType configuredSource = Config.setting().gameRootDirSourceProperty().get();
        downloadSource.set(normalizeDownloadSource(configuredSource));
        downloadSourceInitialized = true;
        if (downloadDir.get() == null || downloadDir.get().isBlank()) {
            downloadDir.set(defaultDownloadDir());
        }
        if (!operating.get()) {
            refreshInstalledState();
        }
    }

    private void attachCoordinatorListeners() {
        if (coordinatorListenersAttached) {
            return;
        }
        resourceUpdateCoordinator.stateProperty().addListener(coordinatorStateListener);
        resourceUpdateCoordinator.progressProperty().addListener(coordinatorProgressListener);
        resourceUpdateCoordinator.progressTextProperty().addListener(coordinatorProgressTextListener);
        resourceUpdateCoordinator.detailTextProperty().addListener(coordinatorDetailListener);
        coordinatorListenersAttached = true;
    }

    private void detachCoordinatorListeners() {
        if (!coordinatorListenersAttached) {
            return;
        }
        resourceUpdateCoordinator.stateProperty().removeListener(coordinatorStateListener);
        resourceUpdateCoordinator.progressProperty().removeListener(coordinatorProgressListener);
        resourceUpdateCoordinator.progressTextProperty().removeListener(coordinatorProgressTextListener);
        resourceUpdateCoordinator.detailTextProperty().removeListener(coordinatorDetailListener);
        coordinatorListenersAttached = false;
    }

    private void refreshInstalledState() {
        boolean installed = GameResourcesManager.getGameExeBase() != null;
        showDownload.set(!installed);
        showUpdate.set(false);
        showPreDownload.set(false);
        tip.set("");

        if (installed) {
            String installedVersion = readInstalledVersion();
            currentVersion.set(installedVersion.isBlank() ? "-" : installedVersion);
            showRepair.set(false);
            status.set(LanguageManager.getString("ui.game_manager.asset.ready"));
            resourceUpdateCoordinator.checkForUpdates();
            checkUpdate();
        } else {
            cancelCheckTask();
            checkResult = null;
            currentVersion.set("-");
            latestVersion.set("-");
            showRepair.set(false);
            status.set(LanguageManager.getString("ui.game_manager.asset.not_installed"));
        }
    }

    private String readInstalledVersion() {
        try {
            String version = updateService.getInstalledVersion();
            return version != null ? version : "";
        } catch (Exception e) {
            LOG.warn("读取已安装版本失败", e);
            return "";
        }
    }

    private String defaultDownloadDir() {
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir == null) {
            return "";
        }
        return new File(gameDir, "WwtBackup/" + downloadSource.get().name().toLowerCase()).getAbsolutePath();
    }

    /** 重新检查游戏版本；运行资源操作时忽略重复检查。 */
    public void checkUpdate() {
        if (operating.get() || checkTask != null || GameResourcesManager.getGameExeBase() == null) {
            return;
        }

        long checkId = ++checkSequence;
        activeCheckId = checkId;
        status.set(LanguageManager.getString("ui.game_manager.asset.checking"));

        Task<ResourceCheckResult> task = new Task<>() {
            @Override
            protected ResourceCheckResult call() {
                return updateService.checkUpdate();
            }
        };
        checkTask = task;
        task.setOnSucceeded(event -> {
            if (!finishCheck(checkId, task)) {
                return;
            }
            boolean success = handleCheckResult(task.getValue());
            status.set(LanguageManager.getString(success
                    ? "ui.game_manager.asset.ready"
                    : "ui.game_manager.asset.check_fail"));
            syncResourceUpdateState(resourceUpdateCoordinator.stateProperty().get());
        });
        task.setOnFailed(event -> {
            if (!finishCheck(checkId, task)) {
                return;
            }
            LOG.warn("检查游戏更新失败", task.getException());
            clearCheckResult();
            status.set(LanguageManager.getString("ui.game_manager.asset.check_fail"));
            syncResourceUpdateState(resourceUpdateCoordinator.stateProperty().get());
        });
        task.setOnCancelled(event -> finishCheck(checkId, task));
        taskManageService.execute(task);
    }

    private boolean finishCheck(long checkId, Task<ResourceCheckResult> task) {
        if (activeCheckId != checkId || checkTask != task) {
            return false;
        }
        activeCheckId = NO_OPERATION;
        checkTask = null;
        return true;
    }

    private void cancelCheckTask() {
        activeCheckId = NO_OPERATION;
        Task<ResourceCheckResult> task = checkTask;
        checkTask = null;
        if (task != null) {
            task.cancel(true);
        }
    }

    private boolean handleCheckResult(ResourceCheckResult result) {
        if (result == null || !result.isSuccessful()) {
            clearCheckResult();
            return false;
        }

        checkResult = result;
        latestVersion.set(hasText(result.latestVersion()) ? result.latestVersion() : "-");
        if (hasText(result.installedVersion())) {
            currentVersion.set(result.installedVersion());
        }

        boolean needsUpdate = result.isUpdateAvailable()
                || result.state() == ResourceCheckState.REPAIR_REQUIRED;
        showRepair.set(result.isRepairAvailable());
        showPreDownload.set(result.hasUpdatePlan() && updateService.isPreDownloadAvailable(result));
        tip.set(needsUpdate
                ? LanguageManager.getString("ui.game_manager.asset.tip_update") + "  " + result.latestVersion()
                : LanguageManager.getString("ui.game_manager.asset.tip_up_to_date"));
        return true;
    }

    private void clearCheckResult() {
        checkResult = null;
        latestVersion.set("-");
        showUpdate.set(false);
        showRepair.set(false);
        showPreDownload.set(false);
        tip.set("");
    }

    private void syncResourceUpdateState(GameResourceUpdateCoordinator.UpdateState state) {
        String coordinatorCurrent = resourceUpdateCoordinator.currentVersionProperty().get();
        String coordinatorLatest = resourceUpdateCoordinator.latestVersionProperty().get();
        if (hasText(coordinatorCurrent) && !"-".equals(coordinatorCurrent)) {
            currentVersion.set(coordinatorCurrent);
        }
        if (hasText(coordinatorLatest) && !"-".equals(coordinatorLatest)) {
            latestVersion.set(coordinatorLatest);
        }

        boolean updateRunning = switch (state) {
            case PREPARING, DOWNLOADING, PAUSED, APPLYING -> true;
            default -> false;
        };
        if (updateRunning) {
            if (activeOperation != OperationType.NONE && activeOperation != OperationType.UPDATE) {
                return;
            }
            cancelCheckTask();
            activeTask = null;
            activeOperationId = NO_OPERATION;
            activeOperation = OperationType.UPDATE;
            operating.set(true);
            pauseAvailable.set(state == GameResourceUpdateCoordinator.UpdateState.DOWNLOADING
                    || state == GameResourceUpdateCoordinator.UpdateState.PAUSED);
            stopAvailable.set(state != GameResourceUpdateCoordinator.UpdateState.APPLYING);
            operationState.set(state == GameResourceUpdateCoordinator.UpdateState.PAUSED
                    ? OperationState.PAUSED : OperationState.RUNNING);
            status.set(resourceUpdateCoordinator.statusTextProperty().get());
            tip.set(resourceUpdateCoordinator.detailTextProperty().get());
            progress.set(resourceUpdateCoordinator.progressProperty().get());
            progressText.set(resourceUpdateCoordinator.progressTextProperty().get());
            showUpdate.set(false);
            return;
        }

        if (activeOperation == OperationType.UPDATE) {
            activeOperation = OperationType.NONE;
            activeOperationId = NO_OPERATION;
            activeTask = null;
            operating.set(false);
            pauseAvailable.set(false);
            stopAvailable.set(false);
            operationState.set(OperationState.IDLE);
        }

        switch (state) {
            case CHECKING -> {
                status.set(resourceUpdateCoordinator.statusTextProperty().get());
                tip.set(resourceUpdateCoordinator.detailTextProperty().get());
                showUpdate.set(false);
            }
            case UPDATE_AVAILABLE -> {
                status.set(resourceUpdateCoordinator.statusTextProperty().get());
                tip.set(resourceUpdateCoordinator.detailTextProperty().get());
                showUpdate.set(true);
            }
            case UP_TO_DATE, COMPLETED -> {
                status.set(resourceUpdateCoordinator.statusTextProperty().get());
                tip.set(resourceUpdateCoordinator.detailTextProperty().get());
                showUpdate.set(false);
                if (state == GameResourceUpdateCoordinator.UpdateState.COMPLETED) {
                    progress.set(1);
                    progressText.set("100%");
                }
            }
            case FAILED, CANCELED -> {
                status.set(resourceUpdateCoordinator.statusTextProperty().get());
                tip.set(resourceUpdateCoordinator.detailTextProperty().get());
                showUpdate.set(hasCoordinatorUpdate());
                progress.set(resourceUpdateCoordinator.progressProperty().get());
                progressText.set(resourceUpdateCoordinator.progressTextProperty().get());
            }
            case IDLE -> {
            }
            default -> {
            }
        }
    }

    private boolean hasCoordinatorUpdate() {
        String current = resourceUpdateCoordinator.currentVersionProperty().get();
        String latest = resourceUpdateCoordinator.latestVersionProperty().get();
        return hasText(current) && hasText(latest)
                && !"-".equals(current) && !"-".equals(latest)
                && !current.equalsIgnoreCase(latest);
    }

    /** 全量下载到 {@link #downloadDir}。 */
    public void download() {
        if (operating.get()) {
            return;
        }
        String dir = downloadDir.get();
        if (dir == null || dir.isBlank()) {
            warnNoDownloadDir();
            return;
        }

        File saveDir = new File(dir);
        if ((!saveDir.exists() && !saveDir.mkdirs()) || !saveDir.isDirectory() || !saveDir.canWrite()) {
            warnNoDownloadDir();
            return;
        }

        SourceType selectedSource = downloadSource.get();
        long operationId = beginOperation(
                OperationType.DOWNLOAD,
                LanguageManager.getString("ui.game_manager.asset.downloading"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                runFullDownload(this, operationId, saveDir, selectedSource);
                return null;
            }
        };
        executeOperation(
                operationId,
                task,
                LanguageManager.getString("ui.game_manager.asset.done"),
                LanguageManager.getString("ui.game_manager.asset.fail"),
                null);
    }

    private void runFullDownload(
            Task<?> task,
            long operationId,
            File saveDir,
            SourceType selectedSource) throws Exception {
        if (task.isCancelled() || !isCurrentTask(operationId, task)) {
            return;
        }

        var launcherRes = downloadService.getLatestUpdate(selectedSource.toGameDownloadSource());
        if (launcherRes == null || launcherRes.getCode() != 200 || launcherRes.getData() == null) {
            throw new IllegalStateException("获取下载配置失败");
        }
        var updateData = launcherRes.getData();
        var fileInfos = loadFileInfos(updateData);
        if (fileInfos.isEmpty()) {
            throw new IllegalStateException("无文件可下载");
        }

        DownloadManager manager = downloadService.createDownloadManager(
                saveDir.toPath(), updateData, fileInfos);
        ActiveDownload handle = new ActiveDownload(operationId, manager);
        AtomicReference<String> failure = new AtomicReference<>();
        manager.setProgressListener((done, total) -> updateProgressByData(operationId, done, total));
        manager.setStateListener((state, error) -> {
            if (state == DownloadState.FAILED) {
                String message = hasText(error) ? error : "下载失败";
                failure.compareAndSet(null, message);
                LOG.warn("全量下载失败: {}", message);
            }
        });

        activeDownload.set(handle);
        if (task.isCancelled() || !isCurrentTask(operationId, task)) {
            manager.stop();
            activeDownload.compareAndSet(handle, null);
            return;
        }

        try {
            manager.run();
        } finally {
            activeDownload.compareAndSet(handle, null);
        }
        if (task.isCancelled() || !isCurrentTask(operationId, task)) {
            return;
        }
        if (failure.get() != null) {
            throw new IllegalStateException(failure.get());
        }
    }

    /** 启动增量更新。 */
    public void update() {
        if (operating.get()) {
            return;
        }
        resourceUpdateCoordinator.startUpdate();
    }

    /** 校验并修复游戏文件。 */
    public void repair() {
        if (operating.get()) {
            return;
        }
        if (checkResult == null || !checkResult.isRepairAvailable()) {
            warnCheckFirst();
            return;
        }

        long operationId = beginOperation(
                OperationType.REPAIR,
                LanguageManager.getString("ui.game_manager.asset.repairing"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                awaitUpdateResult(
                        completion -> updateService.repair(checkResult,
                                progressInfo -> {
                                    updateProgressByData(operationId, progressInfo.completedBytes(),
                                            progressInfo.totalBytes());
                                },
                                value -> completion.accept(value)),
                        "校验修复未返回结果",
                        "校验修复失败");
                return null;
            }
        };
        executeOperation(
                operationId,
                task,
                LanguageManager.getString("ui.game_manager.asset.repair_done"),
                LanguageManager.getString("ui.game_manager.asset.repair_fail"),
                this::refreshAfterAssetChange);
    }

    /** 下载下一版本的预下载资源。 */
    public void preDownload() {
        if (operating.get()) {
            return;
        }
        ResourceCheckResult result = checkResult;
        if (result == null || !result.hasUpdatePlan() || !updateService.isPreDownloadAvailable(result)) {
            warnCheckFirst();
            return;
        }

        long operationId = beginOperation(
                OperationType.PRE_DOWNLOAD,
                LanguageManager.getString("ui.game_manager.asset.predownloading"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                awaitUpdateResult(
                        completion -> updateService.preDownload(result,
                                progressInfo -> {
                                    updateProgressByData(operationId, progressInfo.completedBytes(),
                                            progressInfo.totalBytes());
                                },
                                value -> completion.accept(value)),
                        "预下载未返回结果",
                        "预下载失败");
                return null;
            }
        };
        executeOperation(
                operationId,
                task,
                LanguageManager.getString("ui.game_manager.asset.predownload_done"),
                LanguageManager.getString("ui.game_manager.asset.fail"),
                this::refreshAfterAssetChange);
    }

    public void pause() {
        if (operationState.get() != OperationState.RUNNING || !pauseAvailable.get()) {
            return;
        }
        switch (activeOperation) {
            case DOWNLOAD -> {
                ActiveDownload download = activeDownload.get();
                if (download == null || download.operationId != activeOperationId) {
                    return;
                }
                download.manager.pause();
            }
            case UPDATE -> resourceUpdateCoordinator.pause();
            case PRE_DOWNLOAD -> updateService.pausePreDownload();
            case REPAIR -> updateService.pauseRepair();
            case NONE -> {
                return;
            }
        }
        operationState.set(OperationState.PAUSED);
    }

    public void resume() {
        if (operationState.get() != OperationState.PAUSED) {
            return;
        }
        switch (activeOperation) {
            case DOWNLOAD -> {
                ActiveDownload download = activeDownload.get();
                if (download == null || download.operationId != activeOperationId) {
                    return;
                }
                download.manager.resume();
            }
            case UPDATE -> resourceUpdateCoordinator.resume();
            case PRE_DOWNLOAD -> updateService.resumePreDownload();
            case REPAIR -> updateService.resumeRepair();
            case NONE -> {
                return;
            }
        }
        operationState.set(OperationState.RUNNING);
    }

    public void stop() {
        if (!operating.get() || !stopAvailable.get()
                || operationState.get() == OperationState.STOPPING) {
            return;
        }

        long operationId = activeOperationId;
        Task<?> task = activeTask;
        operationState.set(OperationState.STOPPING);
        try {
            switch (activeOperation) {
                case DOWNLOAD -> {
                    ActiveDownload download = activeDownload.get();
                    if (download != null && download.operationId == operationId) {
                        download.manager.stop();
                    }
                }
                case UPDATE -> resourceUpdateCoordinator.cancel();
                case PRE_DOWNLOAD -> updateService.stopPreDownload();
                case REPAIR -> updateService.stopRepair();
                case NONE -> {
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("停止资源操作失败", e);
        } finally {
            if (task != null && isCurrentTask(operationId, task)) {
                task.cancel(true);
            }
        }
    }

    private List<FileInfo> loadFileInfos(UpdateData updateData) {
        var response = downloadService.getResourceList(updateData);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }

    private long beginOperation(OperationType operation, String statusText) {
        cancelCheckTask();
        long operationId = ++operationSequence;
        activeOperationId = operationId;
        activeOperation = operation;
        operating.set(true);
        pauseAvailable.set(operation != OperationType.UPDATE && operation != OperationType.REPAIR);
        stopAvailable.set(true);
        operationState.set(OperationState.RUNNING);
        status.set(statusText);
        progress.set(0);
        progressText.set("0%");
        return operationId;
    }

    private void executeOperation(
            long operationId,
            Task<Void> task,
            String successText,
            String failureText,
            Runnable afterSuccess) {
        activeTask = task;
        task.setOnSucceeded(event -> {
            if (finishOperation(operationId, task, successText) && afterSuccess != null) {
                afterSuccess.run();
            }
        });
        task.setOnCancelled(event -> finishOperation(
                operationId,
                task,
                LanguageManager.getString("ui.game_manager.asset.stopped"),
                true));
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            String detail = exception != null && hasText(exception.getMessage())
                    ? ": " + exception.getMessage()
                    : "";
            finishOperation(operationId, task, failureText + detail);
        });
        taskManageService.execute(task);
    }

    private boolean finishOperation(long operationId, Task<?> task, String resultText) {
        return finishOperation(operationId, task, resultText, false);
    }

    private boolean finishOperation(long operationId, Task<?> task, String resultText, boolean resetProgress) {
        if (!isCurrentTask(operationId, task)) {
            return false;
        }
        activeTask = null;
        activeOperationId = NO_OPERATION;
        activeOperation = OperationType.NONE;
        operating.set(false);
        pauseAvailable.set(false);
        stopAvailable.set(false);
        operationState.set(OperationState.IDLE);
        status.set(resultText);
        if (resetProgress) {
            progress.set(0);
            progressText.set("0%");
        }
        return true;
    }

    private boolean isCurrentTask(long operationId, Task<?> task) {
        return activeOperationId == operationId && activeTask == task;
    }

    private static void awaitUpdateResult(
            Consumer<Consumer<ResourceOperationResult>> starter,
            String missingResultMessage,
            String defaultFailureMessage) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ResourceOperationResult> resultHolder = new AtomicReference<>();
        starter.accept(result -> {
            resultHolder.set(result);
            latch.countDown();
        });
        latch.await();

        ResourceOperationResult result = resultHolder.get();
        if (result == null) {
            throw new IllegalStateException(missingResultMessage);
        }
        if (!result.successful()) {
            throw new IllegalStateException(hasText(result.errorMessage())
                    ? result.errorMessage()
                    : defaultFailureMessage);
        }
    }

    private void refreshAfterAssetChange() {
        String installedVersion = readInstalledVersion();
        if (!installedVersion.isBlank()) {
            currentVersion.set(installedVersion);
        }
        Platform.runLater(this::checkUpdate);
    }

    /** 将底层工作线程的进度收敛到 FX 线程，并丢弃过期操作的回调。 */
    private void updateProgressByData(long operationId, long done, long total) {
        if (total <= 0) {
            return;
        }
        double value = Math.min(1.0, Math.max(0.0, (double) done / total));
        String text = String.format("%.1f%%", value * 100);
        Runnable update = () -> {
            if (activeOperationId != operationId
                    || operationState.get() == OperationState.IDLE
                    || operationState.get() == OperationState.STOPPING) {
                return;
            }
            progress.set(value);
            progressText.set(text);
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private void warnNoDownloadDir() {
        NotificationManager.message(MessageInfo.warning(
                LanguageManager.getString("ui.game_manager.asset.no_dir")));
    }

    private void warnCheckFirst() {
        NotificationManager.message(MessageInfo.warning(
                LanguageManager.getString("ui.game_manager.asset.check_first")));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static SourceType normalizeDownloadSource(SourceType source) {
        return source == SourceType.BILIBILI || source == SourceType.GLOBAL
                ? source : SourceType.DEFAULT;
    }

    public void setDownloadDir(String dir) {
        downloadDir.set(dir);
    }

    public BooleanProperty operatingProperty() {
        return operating;
    }

    public BooleanProperty pauseAvailableProperty() {
        return pauseAvailable;
    }

    public BooleanProperty stopAvailableProperty() {
        return stopAvailable;
    }

    public ReadOnlyObjectProperty<OperationState> operationStateProperty() {
        return operationState.getReadOnlyProperty();
    }

    public StringProperty currentVersionProperty() {
        return currentVersion;
    }

    public StringProperty latestVersionProperty() {
        return latestVersion;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public BooleanProperty showDownloadProperty() {
        return showDownload;
    }

    public BooleanProperty showUpdateProperty() {
        return showUpdate;
    }

    public BooleanProperty showRepairProperty() {
        return showRepair;
    }

    public BooleanProperty showPreDownloadProperty() {
        return showPreDownload;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public StringProperty progressTextProperty() {
        return progressText;
    }

    public StringProperty tipProperty() {
        return tip;
    }

    public StringProperty downloadDirProperty() {
        return downloadDir;
    }

    public ObjectProperty<SourceType> downloadSourceProperty() {
        return downloadSource;
    }

    @Override
    public void onViewRemoved() {
        cancelCheckTask();
        detachCoordinatorListeners();
    }
}
