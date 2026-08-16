package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wwt.game.resource.DownloadManager;
import cn.tealc.wwt.game.resource.DownloadOptions;
import cn.tealc.wwt.game.resource.GameResourceDownloadService;
import cn.tealc.wwt.game.resource.GameResourceInstallService;
import cn.tealc.wwt.game.resource.model.DownloadPhase;
import cn.tealc.wwt.game.resource.model.DownloadState;
import cn.tealc.wwt.game.resource.model.game.FileInfo;
import cn.tealc.wwt.game.resource.model.launcher.UpdateData;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameResourceUpdateCoordinator;
import cn.tealc.wutheringwavestool.service.GameInstallationManager;
import cn.tealc.wutheringwavestool.service.GameServerSwitchCoordinator;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceCheckState;
import cn.tealc.wwt.game.resource.model.ResourceOperationResult;
import cn.tealc.wwt.game.resource.model.ResourceOperationPhase;
import cn.tealc.wwt.game.resource.model.ResourceProgress;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
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

    private record DownloadDetail(DownloadPhase phase, String relativePath,
            int completedFiles, int totalFiles) {
    }

    private static final class ThroughputTracker {
        private static final long SAMPLE_INTERVAL_NANOS = 250_000_000L;

        private long lastBytes = -1;
        private long lastSampleNanos = System.nanoTime();
        private long bytesPerSecond;

        synchronized String update(long completedBytes) {
            long now = System.nanoTime();
            if (lastBytes < 0 || completedBytes < lastBytes) {
                lastBytes = completedBytes;
                lastSampleNanos = now;
                bytesPerSecond = 0;
                return "";
            }
            long elapsed = now - lastSampleNanos;
            if (elapsed >= SAMPLE_INTERVAL_NANOS) {
                bytesPerSecond = Math.max(0, Math.round((completedBytes - lastBytes)
                        * 1_000_000_000D / elapsed));
                lastBytes = completedBytes;
                lastSampleNanos = now;
            }
            return bytesPerSecond > 0 ? formatBytesPerSecond(bytesPerSecond) : "";
        }
    }

    @Inject
    private GameResourceDownloadService downloadService;
    @Inject
    private GameResourceInstallService installService;
    @Inject
    private GameUpdateService updateService;
    @Inject
    private TaskManageService taskManageService;
    @Inject
    private GameResourceUpdateCoordinator resourceUpdateCoordinator;
    @Inject
    private GameServerSwitchCoordinator serverSwitchCoordinator;
    @Inject
    private GameInstallationManager installationManager;

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
    private final StringProperty downloadSpeed = new SimpleStringProperty("");
    private final StringProperty tip = new SimpleStringProperty("");
    private final StringProperty downloadDir = new SimpleStringProperty();
    private final ObjectProperty<SourceType> downloadSource =
            new SimpleObjectProperty<>(SourceType.DEFAULT);

    private ResourceCheckResult checkResult;

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
    private final ChangeListener<String> coordinatorSpeedListener =
            (observable, oldValue, newValue) -> {
                if (activeOperation == OperationType.UPDATE) {
                    downloadSpeed.set(newValue);
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
            if (!operating.get()) {
                downloadDir.set(defaultDownloadDir());
                refreshInstalledState();
            }
        });
    }

    public void initialize() {
        syncResourceUpdateState(resourceUpdateCoordinator.stateProperty().get());
        serverSwitchCoordinator.refresh();
    }

    public boolean changeServer(SourceType source) {
        if (source != SourceType.DEFAULT && source != SourceType.BILIBILI) {
            return false;
        }
        serverSwitchCoordinator.switchTo(source);
        return true;
    }

    public void redownloadServerFiles(SourceType source) {
        if (source == SourceType.DEFAULT || source == SourceType.BILIBILI) {
            serverSwitchCoordinator.redownloadRequiredFiles(source);
        }
    }

    public void deleteServerCache(SourceType source) {
        if (source == SourceType.DEFAULT || source == SourceType.BILIBILI) {
            serverSwitchCoordinator.deleteCache(source);
        }
    }

    public void refreshServerSwitchState() {
        serverSwitchCoordinator.refresh();
    }

    public ReadOnlyObjectProperty<SourceType> currentGameSourceProperty() {
        return Config.setting().gameRootDirSourceProperty();
    }

    public ReadOnlyBooleanProperty serverSwitchOperatingProperty() {
        return serverSwitchCoordinator.operatingProperty();
    }

    public ReadOnlyBooleanProperty serverSwitchAvailableProperty() {
        return serverSwitchCoordinator.switchAvailableProperty();
    }

    public ReadOnlyDoubleProperty serverSwitchProgressProperty() {
        return serverSwitchCoordinator.progressProperty();
    }

    public ReadOnlyStringProperty serverSwitchStatusTextProperty() {
        return serverSwitchCoordinator.statusTextProperty();
    }

    public ReadOnlyStringProperty serverSwitchDetailTextProperty() {
        return serverSwitchCoordinator.detailTextProperty();
    }

    @Override
    public void onViewAdded() {
        serverSwitchCoordinator.refresh();
        attachCoordinatorListeners();
        syncResourceUpdateState(resourceUpdateCoordinator.stateProperty().get());
        SourceType configuredSource = Config.setting().gameRootDirSourceProperty().get();
        downloadSource.set(normalizeDownloadSource(configuredSource));
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
        resourceUpdateCoordinator.speedTextProperty().addListener(coordinatorSpeedListener);
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
        resourceUpdateCoordinator.speedTextProperty().removeListener(coordinatorSpeedListener);
        resourceUpdateCoordinator.detailTextProperty().removeListener(coordinatorDetailListener);
        coordinatorListenersAttached = false;
    }

    private void refreshInstalledState() {
        boolean downloadTargetInstalled = installationManager.isConfigured(downloadSource.get());
        boolean downloadTargetRegistered = installationManager
                .gameDirectory(GameInstallationManager.editionOf(downloadSource.get()))
                .map(path -> hasText(installService.readInstalledVersion(path)))
                .orElse(false);
        boolean activeInstallationInstalled = GameResourcesManager.getGameExeBase() != null;
        showDownload.set(!downloadTargetInstalled || !downloadTargetRegistered);
        showUpdate.set(false);
        showPreDownload.set(false);
        tip.set("");

        if (!downloadTargetInstalled) {
            cancelCheckTask();
            checkResult = null;
            currentVersion.set("-");
            latestVersion.set("-");
            showRepair.set(false);
            status.set(LanguageManager.getString("ui.game_manager.asset.not_installed"));
        } else if (!downloadTargetRegistered) {
            cancelCheckTask();
            checkResult = null;
            currentVersion.set("-");
            latestVersion.set("-");
            showRepair.set(false);
            status.set(LanguageManager.getString("ui.game_manager.asset.registration_required"));
            tip.set(LanguageManager.getString("ui.game_manager.asset.registration_detail"));
        } else if (activeInstallationInstalled) {
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
        return installationManager.gameDirectory(GameInstallationManager.editionOf(downloadSource.get()))
                .map(path -> path.toString())
                .orElse("");
    }

    /** 重新检查游戏版本；运行资源操作时忽略重复检查。 */
    public void checkUpdate() {
        if (operating.get() || checkTask != null || !isDownloadTargetInstalled()
                || GameResourcesManager.getGameExeBase() == null) {
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
        if (!isDownloadTargetInstalled()) {
            return;
        }
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
            downloadSpeed.set(resourceUpdateCoordinator.speedTextProperty().get());
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
            downloadSpeed.set("");
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
        updateOperationDetail(operationId, "正在获取下载配置");
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
                () -> {
                    tip.set("正在登记" + downloadSourceName(selectedSource) + "游戏目录");
                    installationManager.configureInstallation(selectedSource, saveDir.toPath(), true);
                    String installedVersion = installService.readInstalledVersion(saveDir.toPath());
                    if (hasText(installedVersion)) {
                        Config.setting().setGameInstalledVersion(installedVersion);
                    }
                    Config.setting().save();
                    serverSwitchCoordinator.refresh();
                    refreshInstalledState();
                    tip.set(downloadSourceName(selectedSource) + "游戏目录已登记");
                });
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
        updateOperationDetail(operationId, "正在读取资源清单");
        var fileInfos = loadFileInfos(updateData);
        if (fileInfos.isEmpty()) {
            throw new IllegalStateException("无文件可下载");
        }

        updateOperationDetail(operationId, "正在检查磁盘空间并准备下载");
        DownloadManager manager = downloadService.createDownloadManager(
                saveDir.toPath(), updateData, fileInfos, downloadOptions());
        ActiveDownload handle = new ActiveDownload(operationId, manager);
        AtomicReference<String> failure = new AtomicReference<>();
        AtomicReference<DownloadDetail> activeDetail = new AtomicReference<>();
        ThroughputTracker throughputTracker = new ThroughputTracker();
        manager.setProgressListener((done, total) -> {
            updateProgressByData(operationId, done, total);
            DownloadDetail detail = activeDetail.get();
            if (detail != null && detail.phase() == DownloadPhase.DOWNLOADING) {
                updateDownloadSpeed(operationId, throughputTracker.update(done));
                updateOperationDetail(operationId, fullDownloadDetail(detail));
            }
        });
        manager.setPhaseListener((phase, relativePath, completedFiles, totalFiles) ->
                {
                    DownloadDetail detail = new DownloadDetail(phase, relativePath, completedFiles, totalFiles);
                    activeDetail.set(detail);
                    if (phase != DownloadPhase.DOWNLOADING) {
                        updateDownloadSpeed(operationId, "");
                    }
                    updateOperationDetail(operationId, fullDownloadDetail(detail));
                });
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
        updateOperationDetail(operationId, "正在登记下载资源");
        installService.registerInstalledRelease(saveDir.toPath(), updateData.getVersion(), fileInfos);
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
        updateOperationDetail(operationId, "正在校验游戏文件");
        ThroughputTracker throughputTracker = new ThroughputTracker();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                awaitUpdateResult(
                        completion -> updateService.repair(checkResult,
                                progressInfo -> {
                                    updateProgressByData(operationId, progressInfo.completedBytes(),
                                            progressInfo.totalBytes());
                                    String speed = progressInfo.operationPhase()
                                            == ResourceOperationPhase.DOWNLOADING
                                            ? throughputTracker.update(progressInfo.completedBytes())
                                            : "";
                                    updateDownloadSpeed(operationId, speed);
                                    updateOperationDetail(operationId,
                                            resourceOperationDetail(OperationType.REPAIR, progressInfo));
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
        updateOperationDetail(operationId, "正在准备预下载资源");
        ThroughputTracker throughputTracker = new ThroughputTracker();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                awaitUpdateResult(
                        completion -> updateService.preDownload(result,
                                progressInfo -> {
                                    updateProgressByData(operationId, progressInfo.completedBytes(),
                                            progressInfo.totalBytes());
                                    String speed = progressInfo.operationPhase()
                                            == ResourceOperationPhase.DOWNLOADING
                                            ? throughputTracker.update(progressInfo.completedBytes())
                                            : "";
                                    updateDownloadSpeed(operationId, speed);
                                    updateOperationDetail(operationId,
                                            resourceOperationDetail(OperationType.PRE_DOWNLOAD, progressInfo));
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
        downloadSpeed.set("");
        tip.set("资源操作已暂停");
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
        tip.set(resumeDetail(activeOperation));
    }

    public void stop() {
        if (!operating.get() || !stopAvailable.get()
                || operationState.get() == OperationState.STOPPING) {
            return;
        }

        long operationId = activeOperationId;
        Task<?> task = activeTask;
        operationState.set(OperationState.STOPPING);
        downloadSpeed.set("");
        tip.set("正在停止资源操作");
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
        downloadSpeed.set("");
        tip.set("");
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
            tip.set(failureText + detail);
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
        downloadSpeed.set("");
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

    private void updateOperationDetail(long operationId, String detail) {
        if (!hasText(detail)) {
            return;
        }
        Runnable update = () -> {
            if (activeOperationId != operationId
                    || operationState.get() == OperationState.IDLE
                    || operationState.get() == OperationState.STOPPING) {
                return;
            }
            tip.set(detail);
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private void updateDownloadSpeed(long operationId, String speed) {
        Runnable update = () -> {
            if (activeOperationId != operationId
                    || operationState.get() == OperationState.IDLE
                    || operationState.get() == OperationState.STOPPING) {
                return;
            }
            downloadSpeed.set(speed != null ? speed : "");
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private static String fullDownloadDetail(DownloadDetail detail) {
        String action = switch (detail.phase()) {
            case PREPARING -> "正在准备下载";
            case DOWNLOADING -> "正在下载";
            case VERIFYING -> "正在校验文件";
            case MERGING -> "正在合成文件";
        };
        String count = detail.totalFiles() > 0
                ? "（" + Math.min(detail.totalFiles(), detail.completedFiles() + 1)
                + "/" + detail.totalFiles() + "）"
                : "";
        String path = displayPath(detail.relativePath());
        return path.isEmpty() ? action + count : action + count + "：" + path;
    }

    private static String resourceOperationDetail(OperationType operation, ResourceProgress progressInfo) {
        String action = switch (operation) {
            case REPAIR -> switch (progressInfo.operationPhase()) {
                case VERIFYING -> "正在校验游戏文件";
                case DOWNLOADING -> "正在下载缺失文件";
                case APPLYING -> "正在安装修复文件";
                case UNKNOWN -> "正在校验修复游戏文件";
            };
            case PRE_DOWNLOAD -> switch (progressInfo.operationPhase()) {
                case VERIFYING -> "正在校验预下载文件";
                case DOWNLOADING -> "正在下载预下载资源";
                case APPLYING -> "正在整理预下载资源";
                case UNKNOWN -> "正在处理预下载资源";
            };
            default -> "正在处理游戏资源";
        };
        String count = progressInfo.totalFiles() > 0
                ? "（" + Math.min(progressInfo.completedFiles() + 1, progressInfo.totalFiles())
                + "/" + progressInfo.totalFiles() + "）"
                : "";
        return action + count;
    }

    private static String formatBytesPerSecond(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        }
        double value = bytesPerSecond;
        String[] units = {"KB/s", "MB/s", "GB/s", "TB/s"};
        int unit = -1;
        do {
            value /= 1024;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format("%.1f %s", value, units[unit]);
    }

    private static String resumeDetail(OperationType operation) {
        return switch (operation) {
            case DOWNLOAD -> "正在继续下载资源";
            case REPAIR -> "正在继续校验修复游戏文件";
            case PRE_DOWNLOAD -> "正在继续预下载资源";
            case UPDATE -> "正在继续更新游戏资源";
            case NONE -> "";
        };
    }

    private static String displayPath(String path) {
        if (!hasText(path)) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        return normalized.length() <= 96 ? normalized : "..." + normalized.substring(normalized.length() - 93);
    }

    private static String downloadSourceName(SourceType source) {
        return switch (source) {
            case GLOBAL -> "国际服";
            case BILIBILI -> "Bilibili服";
            case DEFAULT, WE_GAME -> "国服";
        };
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

    private boolean isDownloadTargetInstalled() {
        return installationManager.isConfigured(downloadSource.get());
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

    public StringProperty downloadSpeedProperty() {
        return downloadSpeed;
    }

    public StringProperty tipProperty() {
        return tip;
    }

    public StringProperty downloadDirProperty() {
        return downloadDir;
    }

    public IntegerProperty downloadParallelCountProperty() {
        return Config.setting().downloadParallelCountProperty();
    }

    public LongProperty downloadSpeedLimitBytesPerSecondProperty() {
        return Config.setting().downloadSpeedLimitBytesPerSecondProperty();
    }

    public void setDownloadParallelCount(int parallelCount) {
        Config.setting().setDownloadParallelCount(parallelCount);
        Config.setting().save();
    }

    public void setDownloadSpeedLimitBytesPerSecond(long bytesPerSecond) {
        Config.setting().setDownloadSpeedLimitBytesPerSecond(bytesPerSecond);
        Config.setting().save();
    }

    public ObjectProperty<SourceType> downloadSourceProperty() {
        return downloadSource;
    }

    private static DownloadOptions downloadOptions() {
        return new DownloadOptions(Config.setting().getDownloadParallelCount(),
                Config.setting().getDownloadSpeedLimitBytesPerSecond());
    }

    @Override
    public void onViewRemoved() {
        cancelCheckTask();
        detachCoordinatorListeners();
    }
}
