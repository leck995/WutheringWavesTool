package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.thread.game.download.GameResourceUpdateTask;
import cn.tealc.wutheringwavestool.service.GameInstallationManager;
import cn.tealc.wutheringwavestool.service.GameServerSwitchCoordinator;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.ManagedTask;
import cn.tealc.wutheringwavestool.service.TaskControl;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.thread.game.download.GameFullDownloadTask;
import cn.tealc.wutheringwavestool.thread.game.download.GamePreDownloadTask;
import cn.tealc.wutheringwavestool.thread.game.download.GameRepairDownloadTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceCheckState;
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

    @Inject
    private GameUpdateService updateService;
    @Inject
    private TaskManageService taskManageService;
    private GameResourceUpdateTask updateTask;
    private GameFullDownloadTask fullDownloadTask;
    @Inject
    private GameServerSwitchCoordinator serverSwitchCoordinator;
    @Inject
    private GameInstallationManager installationManager;

    private final BooleanProperty operating = new SimpleBooleanProperty(false);
    private final BooleanProperty pauseAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty stopAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty fullDownloadOperating = new SimpleBooleanProperty(false);
    private final BooleanProperty resourceOperationOperating = new SimpleBooleanProperty(false);
    private final ReadOnlyObjectWrapper<OperationState> operationState =
            new ReadOnlyObjectWrapper<>(OperationState.IDLE);

    private final StringProperty currentVersion = new SimpleStringProperty("-");
    private final StringProperty latestVersion = new SimpleStringProperty("-");
    private final StringProperty status = new SimpleStringProperty("就绪");

    private final BooleanProperty showDownload = new SimpleBooleanProperty(true);
    private final BooleanProperty showDownloadSourceHint = new SimpleBooleanProperty(false);
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

    // 全量下载独立的进度展示属性，与资源操作（更新/修复/预下载）隔离，避免互相污染。
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(0);
    private final StringProperty downloadProgressText = new SimpleStringProperty("0%");
    private final StringProperty downloadDownloadSpeed = new SimpleStringProperty("");
    private final StringProperty downloadTip = new SimpleStringProperty("");

    private ResourceCheckResult checkResult;

    private long operationSequence;
    private volatile long activeOperationId = NO_OPERATION;
    private volatile Task<?> activeTask;
    private OperationType activeOperation = OperationType.NONE;

    private long checkSequence;
    private long activeCheckId = NO_OPERATION;
    private Task<ResourceCheckResult> checkTask;
    private boolean coordinatorListenersAttached;
    private final ChangeListener<GameResourceUpdateTask.UpdateState> coordinatorStateListener =
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
                refreshFullDownloadState();
            }
        });
    }

    public void initialize() {
        currentUpdateTask();
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

    public ReadOnlyBooleanProperty mainlandServerSwitchReadyProperty() {
        return serverSwitchCoordinator.mainlandTargetReadyProperty();
    }

    public ReadOnlyBooleanProperty bilibiliServerSwitchReadyProperty() {
        return serverSwitchCoordinator.bilibiliTargetReadyProperty();
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
        currentUpdateTask();
        // 重新进入页面时，若存在进行中的全量下载任务，恢复其进度展示。
        GameFullDownloadTask full = currentFullDownloadTask();
        if (full != null && isFullDownloadRunning(full.phaseProperty().get())) {
            syncFullDownloadState(full.phaseProperty().get());
        }
        SourceType configuredSource = Config.setting().gameRootDirSourceProperty().get();
        downloadSource.set(normalizeDownloadSource(configuredSource));
        if (downloadDir.get() == null || downloadDir.get().isBlank()) {
            downloadDir.set(defaultDownloadDir());
        }
        if (!operating.get()) {
            refreshInstalledState();
        }
    }

    private static boolean isFullDownloadRunning(GameFullDownloadTask.Phase phase) {
        return phase == GameFullDownloadTask.Phase.CHECKING
                || phase == GameFullDownloadTask.Phase.DOWNLOADING
                || phase == GameFullDownloadTask.Phase.VERIFYING
                || phase == GameFullDownloadTask.Phase.APPLYING
                || phase == GameFullDownloadTask.Phase.PAUSED;
    }

    /**
     * 从 {@link TaskManageService} 查询当前更新任务（固定 id 复用），
     * 若任务实例变化则重绑进度监听，并把最新阶段同步到本界面的操作状态。
     */
    private GameResourceUpdateTask currentUpdateTask() {
        ManagedTask managed = taskManageService.get(GameResourceUpdateTask.TASK_ID);
        GameResourceUpdateTask task = managed != null ? (GameResourceUpdateTask) managed.getTask() : null;
        if (task != updateTask) {
            rebindCoordinator(task);
            updateTask = task;
        }
        if (task != null) {
            syncResourceUpdateState(task.phaseProperty().get());
        }
        return task;
    }

    private void rebindCoordinator(GameResourceUpdateTask task) {
        detachCoordinatorListeners();
        if (task != null) {
            task.phaseProperty().addListener(coordinatorStateListener);
            task.progressProperty().addListener(coordinatorProgressListener);
            task.progressTextProperty().addListener(coordinatorProgressTextListener);
            task.speedTextProperty().addListener(coordinatorSpeedListener);
            task.detailTextProperty().addListener(coordinatorDetailListener);
        }
        coordinatorListenersAttached = task != null;
    }

    private void detachCoordinatorListeners() {
        if (!coordinatorListenersAttached || updateTask == null) {
            coordinatorListenersAttached = false;
            return;
        }
        updateTask.phaseProperty().removeListener(coordinatorStateListener);
        updateTask.progressProperty().removeListener(coordinatorProgressListener);
        updateTask.progressTextProperty().removeListener(coordinatorProgressTextListener);
        updateTask.speedTextProperty().removeListener(coordinatorSpeedListener);
        updateTask.detailTextProperty().removeListener(coordinatorDetailListener);
        coordinatorListenersAttached = false;
    }

    private void refreshInstalledState() {
        refreshFullDownloadState();

        boolean activeInstallationInstalled = isActiveInstallationInstalled();
        if (!activeInstallationInstalled) {
            cancelCheckTask();
            checkResult = null;
            currentVersion.set("-");
            latestVersion.set("-");
            tip.set("");
            showRepair.set(false);
            showUpdate.set(false);
            showPreDownload.set(false);
            status.set(LanguageManager.getString("ui.game_manager.asset.not_installed"));
            return;
        }

        // 已安装但缺少 launcherDownloadConfig.json（未登记版本）时，仍执行更新检查，
        // 以便获取最新版本并开放校验修复（修复流程会自行重建该文件）。
        boolean activeInstallationRegistered = GameResourcesManager.getGameDir() != null
                && hasText(updateService.getInstalledVersion(GameResourcesManager.getGameDir().toPath()));
        tip.set(activeInstallationRegistered ? "" : LanguageManager
                .getString("ui.game_manager.asset.registration_detail"));
        status.set(LanguageManager.getString("ui.game_manager.asset.checking"));
        checkUpdate();
    }

    /** 仅根据全量下载来源刷新右侧下载入口，不影响当前服务器的资源状态。 */
    private void refreshFullDownloadState() {
        boolean downloadTargetInstalled = installationManager.isConfigured(downloadSource.get());
        boolean downloadTargetRegistered = installationManager
                .gameDirectory(GameInstallationManager.editionOf(downloadSource.get()))
                .map(path -> hasText(updateService.getInstalledVersion(path)))
                .orElse(false);
        showDownloadSourceHint.set(downloadTargetInstalled || downloadTargetRegistered);
        // 国际服使用已配置的独立游戏目录，不在资源管理中重新全量下载。
        boolean shouldShowFullDownload = downloadSource.get() != SourceType.GLOBAL || !downloadTargetInstalled;
        showDownload.set(shouldShowFullDownload && (!downloadTargetInstalled || !downloadTargetRegistered));
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
        if (operating.get() || checkTask != null || !isActiveInstallationInstalled()
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
            syncUpdateStateFromCurrentTask();
        });
        task.setOnFailed(event -> {
            if (!finishCheck(checkId, task)) {
                return;
            }
            LOG.warn("检查游戏更新失败", task.getException());
            clearCheckResult();
            status.set(LanguageManager.getString("ui.game_manager.asset.check_fail"));
            syncUpdateStateFromCurrentTask();
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
        if (result.state() == ResourceCheckState.REPAIR_REQUIRED) {
            tip.set(LanguageManager.getString("ui.game_manager.asset.tip_repair"));
        } else {
            tip.set(needsUpdate
                    ? LanguageManager.getString("ui.game_manager.asset.tip_update") + "  " + result.latestVersion()
                    : LanguageManager.getString("ui.game_manager.asset.tip_up_to_date"));
        }
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

    /** 同步更新任务阶段到界面操作状态，无进行中的任务时调用。 */
    private void syncUpdateStateFromCurrentTask() {
        GameResourceUpdateTask task = currentUpdateTask();
        if (task != null) {
            syncResourceUpdateState(task.phaseProperty().get());
        }
    }

    private void syncResourceUpdateState(GameResourceUpdateTask.UpdateState state) {
        if (!isActiveInstallationInstalled()) {
            return;
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
            fullDownloadOperating.set(false);
            resourceOperationOperating.set(true);
            pauseAvailable.set(state == GameResourceUpdateTask.UpdateState.DOWNLOADING
                    || state == GameResourceUpdateTask.UpdateState.PAUSED);
            stopAvailable.set(state != GameResourceUpdateTask.UpdateState.APPLYING);
            operationState.set(state == GameResourceUpdateTask.UpdateState.PAUSED
                    ? OperationState.PAUSED : OperationState.RUNNING);
            status.set(updateTask.statusTextProperty().get());
            tip.set(updateTask.detailTextProperty().get());
            progress.set(updateTask.progressProperty().get());
            progressText.set(updateTask.progressTextProperty().get());
            downloadSpeed.set(updateTask.speedTextProperty().get());
            showUpdate.set(false);
            return;
        }

        if (activeOperation == OperationType.UPDATE) {
            activeOperation = OperationType.NONE;
            activeOperationId = NO_OPERATION;
            activeTask = null;
            operating.set(false);
            resourceOperationOperating.set(false);
            pauseAvailable.set(false);
            stopAvailable.set(false);
            operationState.set(OperationState.IDLE);
            downloadSpeed.set("");
        }

        switch (state) {
            case COMPLETED -> {
                status.set(updateTask.statusTextProperty().get());
                tip.set(updateTask.detailTextProperty().get());
                showUpdate.set(false);
                progress.set(1);
                progressText.set("100%");
                String newVersion = updateTask.currentVersionProperty().get();
                if (hasText(newVersion) && !"-".equals(newVersion)) {
                    currentVersion.set(newVersion);
                    latestVersion.set(newVersion);
                }
            }
            case FAILED, CANCELED -> {
                status.set(updateTask.statusTextProperty().get());
                tip.set(updateTask.detailTextProperty().get());
                showUpdate.set(false);
                progress.set(updateTask.progressProperty().get());
                progressText.set(updateTask.progressTextProperty().get());
            }
            default -> {
            }
        }
    }

/** 全量下载：登记下载目录后启动 {@link GameFullDownloadTask}，走 legacy 的 STATE_NEED_DOWNLOAD 全量下载分支。 */
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
        // 登记并激活目录：清空已装版本，使 legacy 检查进入 STATE_NEED_DOWNLOAD（全量下载）。
        installationManager.configureInstallation(selectedSource, saveDir.toPath(), true);
        Config.setting().setGameInstalledVersion("");
        Config.setting().save();
        serverSwitchCoordinator.refresh();

        cancelCheckTask();
        GameFullDownloadTask task = currentFullDownloadTask();
        long operationId = ++operationSequence;
        if (task == null) {
            task = new GameFullDownloadTask(updateService);
            fullDownloadTask = task;
            bindFullDownloadTask(task);
            taskManageService.submit(GameFullDownloadTask.TASK_ID,
                    LanguageManager.getString("ui.game_manager.asset.download"),
                    task, task, ManagedTask.TaskCategory.DOWNLOAD);
        }
        final GameFullDownloadTask active = task;
        activeOperationId = operationId;
        activeTask = active;
        activeOperation = OperationType.DOWNLOAD;
        task.setOnSucceeded(event -> {
            if (isCurrentTask(operationId, active)) {
                activeOperation = OperationType.NONE;
                activeOperationId = NO_OPERATION;
                activeTask = null;
                refreshInstalledState();
            }
        });
        task.setOnFailed(event -> {
            if (isCurrentTask(operationId, active)) {
                activeOperation = OperationType.NONE;
                activeOperationId = NO_OPERATION;
                activeTask = null;
            }
        });
        task.setOnCancelled(event -> {
            if (isCurrentTask(operationId, active)) {
                activeOperation = OperationType.NONE;
                activeOperationId = NO_OPERATION;
                activeTask = null;
            }
        });
        beginFullDownloadState(task);
    }

    /**
     * 设置全量下载运行态（区别于资源操作区）。进入块状下载阶段时调用，
     * 由 {@link GameFullDownloadTask#phaseProperty()} 变化经 {@link #syncFullDownloadState}
     * 持续驱动暂停/恢复/停止可用性与进度展示。
     */
    private void beginFullDownloadState(GameFullDownloadTask task) {
        operating.set(true);
        resourceOperationOperating.set(false);
        pauseAvailable.set(true);
        stopAvailable.set(true);
        syncFullDownloadState(task.phaseProperty().get());
    }

    /** 从任务管理器按固定 id 复用全量下载任务（重新进入页面时恢复进度展示）。 */
    private GameFullDownloadTask currentFullDownloadTask() {
        ManagedTask managed = taskManageService.get(GameFullDownloadTask.TASK_ID);
        GameFullDownloadTask task = managed != null ? (GameFullDownloadTask) managed.getTask() : null;
        if (task != fullDownloadTask) {
            fullDownloadTask = task;
            if (task != null) {
                bindFullDownloadTask(task);
            }
        }
        return task;
    }

    /** 将全量下载任务的进度/阶段属性绑定到下载专属区块。 */
    private void bindFullDownloadTask(GameFullDownloadTask task) {
        task.progressProperty().addListener((observable, oldValue, newValue) -> {
            double v = newValue != null ? newValue.doubleValue() : 0;
            if (v >= 0) {
                downloadProgress.set(v);
                downloadProgressText.set(String.format("%.1f%%", v * 100));
            }
        });
        task.progressTextProperty().addListener((o, a, n) -> downloadProgressText.set(n != null ? n : "0%"));
        task.speedTextProperty().addListener((o, a, n) -> downloadDownloadSpeed.set(n != null ? n : ""));
        task.detailTextProperty().addListener((o, a, n) -> downloadTip.set(n != null ? n : ""));
        task.phaseProperty().addListener((o, a, n) -> syncFullDownloadState(n));
    }

    private void syncFullDownloadState(GameFullDownloadTask.Phase phase) {
        if (phase == null) {
            return;
        }
        switch (phase) {
            case CHECKING, DOWNLOADING, VERIFYING, APPLYING -> {
                fullDownloadOperating.set(true);
                resourceOperationOperating.set(false);
                operating.set(true);
                activeOperation = OperationType.DOWNLOAD;
                pauseAvailable.set(phase == GameFullDownloadTask.Phase.DOWNLOADING);
                stopAvailable.set(phase != GameFullDownloadTask.Phase.APPLYING);
                operationState.set(OperationState.RUNNING);
            }
            case PAUSED -> {
                fullDownloadOperating.set(true);
                resourceOperationOperating.set(false);
                operating.set(true);
                activeOperation = OperationType.DOWNLOAD;
                pauseAvailable.set(false);
                stopAvailable.set(true);
                operationState.set(OperationState.PAUSED);
            }
            case COMPLETED, FAILED, CANCELED -> {
                fullDownloadOperating.set(false);
                resourceOperationOperating.set(false);
                if (activeOperation == OperationType.DOWNLOAD) {
                    activeOperation = OperationType.NONE;
                    activeOperationId = NO_OPERATION;
                    activeTask = null;
                }
                operating.set(false);
                pauseAvailable.set(false);
                stopAvailable.set(false);
                operationState.set(OperationState.IDLE);
                downloadDownloadSpeed.set("");
            }
            case IDLE -> {
            }
        }
    }

    /** 启动增量更新。 */
    public void update() {
        if (operating.get()) {
            return;
        }
        if (checkResult == null) {
            warnCheckFirst();
            return;
        }
        GameResourceUpdateTask task = currentUpdateTask();
        if (task == null) {
            task = new GameResourceUpdateTask(updateService);
            updateTask = task;
            rebindCoordinator(task);
            task.setCheckResult(checkResult);
            taskManageService.submit(GameResourceUpdateTask.TASK_ID,
                    LanguageManager.getString("ui.game_manager.asset.update"),
                    task, task, ManagedTask.TaskCategory.UPDATE);
        }
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
        GameRepairDownloadTask task = new GameRepairDownloadTask(updateService, checkResult);
        bindResourceTask(task);
        executeOperation(
                operationId,
                task,
                LanguageManager.getString("ui.game_manager.asset.repair_done"),
                LanguageManager.getString("ui.game_manager.asset.repair_fail"),
                this::refreshAfterAssetChange);
    }

    /** 将资源操作 Task(修复/预下载) 的进度/文案绑定到左侧共享属性。 */
    private void bindResourceTask(Task<?> task) {
        task.progressProperty().addListener((observable, oldValue, newValue) -> {
            double v = newValue != null ? newValue.doubleValue() : 0;
            if (v >= 0) {
                progress.set(v);
                progressText.set(String.format("%.1f%%", v * 100));
            }
        });
        task.messageProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                tip.set(newValue);
            }
        });
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
        GamePreDownloadTask task = new GamePreDownloadTask(updateService, result);
        bindResourceTask(task);
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
                GameFullDownloadTask full = currentFullDownloadTask();
                if (full != null) {
                    full.pause();
                }
            }
            case UPDATE -> {
                GameResourceUpdateTask task = currentUpdateTask();
                if (task != null) {
                    task.pauseUpdate();
                }
            }
            case PRE_DOWNLOAD -> updateService.pausePreDownload();
            case REPAIR -> updateService.pauseRepair();
            case NONE -> {
                return;
            }
        }
        operationState.set(OperationState.PAUSED);
        if (activeOperation == OperationType.DOWNLOAD) {
            downloadDownloadSpeed.set("");
            downloadTip.set("资源操作已暂停");
        } else {
            downloadSpeed.set("");
            tip.set("资源操作已暂停");
        }
    }

    public void resume() {
        if (operationState.get() != OperationState.PAUSED) {
            return;
        }
        switch (activeOperation) {
            case DOWNLOAD -> {
                GameFullDownloadTask full = currentFullDownloadTask();
                if (full != null) {
                    full.resume();
                }
            }
            case UPDATE -> {
                GameResourceUpdateTask task = currentUpdateTask();
                if (task != null) {
                    task.resumeUpdate();
                }
            }
            case PRE_DOWNLOAD -> updateService.resumePreDownload();
            case REPAIR -> updateService.resumeRepair();
            case NONE -> {
                return;
            }
        }
        operationState.set(OperationState.RUNNING);
        if (activeOperation == OperationType.DOWNLOAD) {
            downloadTip.set("正在继续下载资源");
        } else {
            tip.set(resumeDetail(activeOperation));
        }
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
                    GameFullDownloadTask full = currentFullDownloadTask();
                    if (full != null) {
                        full.cancelTask();
                    }
                }
                case UPDATE -> {
                    GameResourceUpdateTask updateTask = currentUpdateTask();
                    if (updateTask != null) {
                        updateTask.cancelUpdate();
                    }
                }
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

    private long beginOperation(OperationType operation, String statusText) {
        cancelCheckTask();
        long operationId = ++operationSequence;
        activeOperationId = operationId;
        activeOperation = operation;
        operating.set(true);
        resourceOperationOperating.set(true);
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
            Task<?> task,
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
        TaskControl control = task instanceof TaskControl ? (TaskControl) task : null;
        ManagedTask.TaskCategory category = classify(task);
        taskManageService.submit(managedId(task), managedName(task), task, control, category);
    }

    private static String managedId(Task<?> task) {
        return task.getClass().getSimpleName();
    }

    private static String managedName(Task<?> task) {
        if (task instanceof cn.tealc.wutheringwavestool.thread.game.download.GameRepairDownloadTask) return "校验修复";
        if (task instanceof cn.tealc.wutheringwavestool.thread.game.download.GamePreDownloadTask) return "预下载";
        return task.getClass().getSimpleName();
    }

    private static ManagedTask.TaskCategory classify(Task<?> task) {
        if (task instanceof cn.tealc.wutheringwavestool.thread.game.download.GameRepairDownloadTask) return ManagedTask.TaskCategory.REPAIR;
        if (task instanceof cn.tealc.wutheringwavestool.thread.game.download.GamePreDownloadTask) return ManagedTask.TaskCategory.PREDOWNLOAD;
        return ManagedTask.TaskCategory.OTHER;
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
        resourceOperationOperating.set(false);
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

    private void refreshAfterAssetChange() {
        String installedVersion = readInstalledVersion();
        if (!installedVersion.isBlank()) {
            currentVersion.set(installedVersion);
        }
        Platform.runLater(this::checkUpdate);
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

    private boolean isActiveInstallationInstalled() {
        SourceType activeSource = Config.setting().getGameRootDirSource();
        return installationManager.isConfigured(activeSource != null ? activeSource : SourceType.DEFAULT);
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

    public ReadOnlyBooleanProperty fullDownloadOperatingProperty() {
        return fullDownloadOperating;
    }

    public ReadOnlyBooleanProperty resourceOperationOperatingProperty() {
        return resourceOperationOperating;
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

    public BooleanProperty showDownloadSourceHintProperty() {
        return showDownloadSourceHint;
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

    public DoubleProperty downloadProgressProperty() {
        return downloadProgress;
    }

    public StringProperty downloadProgressTextProperty() {
        return downloadProgressText;
    }

    public StringProperty downloadDownloadSpeedProperty() {
        return downloadDownloadSpeed;
    }

    public StringProperty downloadTipProperty() {
        return downloadTip;
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

    public StringProperty customDownloadCacheDirProperty() {
        return Config.setting().customDownloadCacheDirProperty();
    }

    public void setCustomDownloadCacheDir(String cacheDir) {
        Config.setting().setCustomDownloadCacheDir(cacheDir);
        Config.setting().save();
    }

    @Override
    public void onViewRemoved() {
        cancelCheckTask();
        detachCoordinatorListeners();
    }
}
