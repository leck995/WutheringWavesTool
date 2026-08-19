package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameInstallationManager;
import cn.tealc.wutheringwavestool.service.GameServerSwitchCoordinator;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.ManagedTask;
import cn.tealc.wutheringwavestool.service.TaskControl;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.thread.game.download.GameFullDownloadTask;
import cn.tealc.wutheringwavestool.thread.game.download.GamePreDownloadTask;
import cn.tealc.wutheringwavestool.thread.game.download.GameRepairDownloadTask;
import cn.tealc.wutheringwavestool.thread.game.download.GameResourceUpdateTask;
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
import javafx.concurrent.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 「游戏资源管理」ViewModel：全量下载 + 增量更新 + 预下载 + 校验修复合一。
 *
 * <p>操作状态不在此处自行维护状态机，而是以「当前操作任务」为唯一事实来源：
 * 暂停/恢复/停止直接转发给 {@link TaskControl}；进度、速度、文案、显隐均由对应 Task 的
 * 只读属性驱动，本类只做镜像绑定与按任务类型区分「下载专属区 / 资源操作区」。</p>
 */
public class GameAssetViewModel extends BaseViewModel implements SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(GameAssetViewModel.class);
    private static final long NO_CHECK = -1;

    public enum OperationState {
        IDLE,
        RUNNING,
        PAUSED,
        STOPPING
    }

    @Inject
    private GameUpdateService updateService;
    @Inject
    private TaskManageService taskManageService;
    @Inject
    private GameServerSwitchCoordinator serverSwitchCoordinator;
    @Inject
    private GameInstallationManager installationManager;

    /** 当前正在执行的操作任务；null 表示空闲。是否暂停由其 phase 属性表达。 */
    private Task<?> activeTask;
    private GameResourceUpdateTask updateTask;
    private GameFullDownloadTask fullDownloadTask;

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

    // 资源操作区进度（更新/修复/预下载）。
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty("0%");
    private final StringProperty downloadSpeed = new SimpleStringProperty("");
    private final StringProperty tip = new SimpleStringProperty("");

    // 下载专属区进度（全量下载）。
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(0);
    private final StringProperty downloadProgressText = new SimpleStringProperty("0%");
    private final StringProperty downloadDownloadSpeed = new SimpleStringProperty("");
    private final StringProperty downloadTip = new SimpleStringProperty("");

    private final StringProperty downloadDir = new SimpleStringProperty();
    private final ObjectProperty<SourceType> downloadSource =
            new SimpleObjectProperty<>(SourceType.DEFAULT);

    private ResourceCheckResult checkResult;

    private long checkSequence;
    private long activeCheckId = NO_CHECK;
    private Task<ResourceCheckResult> checkTask;

    private boolean coordinatorListenersAttached;
    private final ChangeListener<GameResourceUpdateTask.UpdateState> coordinatorStateListener =
            (observable, oldState, newState) -> syncUpdateState(newState);
    private final ChangeListener<Number> coordinatorProgressListener =
            (observable, oldValue, newValue) -> {
                if (activeTask instanceof GameResourceUpdateTask) {
                    progress.set(newValue.doubleValue());
                }
            };
    private final ChangeListener<String> coordinatorProgressTextListener =
            (observable, oldValue, newValue) -> {
                if (activeTask instanceof GameResourceUpdateTask) {
                    progressText.set(newValue);
                }
            };
    private final ChangeListener<String> coordinatorSpeedListener =
            (observable, oldValue, newValue) -> {
                if (activeTask instanceof GameResourceUpdateTask) {
                    downloadSpeed.set(newValue);
                }
            };
    private final ChangeListener<String> coordinatorDetailListener =
            (observable, oldValue, newValue) -> {
                if (activeTask instanceof GameResourceUpdateTask) {
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

    // ==================== 服务器切换 ====================

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

    // ==================== 生命周期 ====================

    @Override
    public void onViewAdded() {
        serverSwitchCoordinator.refresh();
        currentUpdateTask();
        restoreFullDownloadIfRunning();
        SourceType configuredSource = Config.setting().gameRootDirSourceProperty().get();
        downloadSource.set(normalizeDownloadSource(configuredSource));
        if (downloadDir.get() == null || downloadDir.get().isBlank()) {
            downloadDir.set(defaultDownloadDir());
        }
        if (!operating.get()) {
            refreshInstalledState();
        }
    }

    @Override
    public void onViewRemoved() {
        cancelCheckTask();
        detachCoordinatorListeners();
    }

    // ==================== 全量下载 ====================

    /** 全量下载：登记下载目录后启动 {@link GameFullDownloadTask}。 */
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
        if (task == null) {
            task = new GameFullDownloadTask(updateService);
            fullDownloadTask = task;
            bindFullDownloadTask(task);
            taskManageService.submit(GameFullDownloadTask.TASK_ID,
                    LanguageManager.getString("ui.game_manager.asset.download"),
                    task, task, ManagedTask.TaskCategory.DOWNLOAD);
        }
        attachOperation(task, true, this::refreshInstalledState);
        syncFullDownloadState(task.phaseProperty().get());
    }

    // ==================== 增量更新 ====================

    /** 启动增量更新（已安装版本的 patch 更新）。 */
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
        attachOperation(task, false, null);
        syncUpdateState(task.phaseProperty().get());
    }

    // ==================== 校验修复 ====================

    public void repair() {
        if (operating.get()) {
            return;
        }
        if (checkResult == null || !checkResult.isRepairAvailable()) {
            warnCheckFirst();
            return;
        }
        status.set(LanguageManager.getString("ui.game_manager.asset.repairing"));
        GameRepairDownloadTask task = new GameRepairDownloadTask(updateService, checkResult);
        bindResourceTask(task);
        attachOperation(task, false, this::refreshAfterAssetChange);
        taskManageService.submit(managedId(task), managedName(task), task, task,
                ManagedTask.TaskCategory.REPAIR);
    }

    // ==================== 预下载 ====================

    public void preDownload() {
        if (operating.get()) {
            return;
        }
        ResourceCheckResult result = checkResult;
        if (result == null || !result.hasUpdatePlan() || !updateService.isPreDownloadAvailable(result)) {
            warnCheckFirst();
            return;
        }
        status.set(LanguageManager.getString("ui.game_manager.asset.predownloading"));
        GamePreDownloadTask task = new GamePreDownloadTask(updateService, result);
        bindResourceTask(task);
        attachOperation(task, false, this::refreshAfterAssetChange);
        taskManageService.submit(managedId(task), managedName(task), task, task,
                ManagedTask.TaskCategory.PREDOWNLOAD);
    }

    // ==================== 统一操作生命周期 ====================

    /** 挂接一个操作任务：设置运行态，并在任务终结时复位。 */
    private void attachOperation(Task<?> task, boolean download, Runnable onSuccess) {
        boolean canPause = task instanceof TaskControl control && control.supportsPause();
        activeTask = task;
        operating.set(true);
        fullDownloadOperating.set(download);
        resourceOperationOperating.set(!download);
        pauseAvailable.set(canPause);
        stopAvailable.set(true);
        operationState.set(OperationState.RUNNING);
        task.stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                if (activeTask == task) {
                    detachOperation();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }
            } else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                if (activeTask == task) {
                    detachOperation();
                }
            }
        });
    }

    private void detachOperation() {
        activeTask = null;
        operating.set(false);
        fullDownloadOperating.set(false);
        resourceOperationOperating.set(false);
        pauseAvailable.set(false);
        stopAvailable.set(false);
        operationState.set(OperationState.IDLE);
        downloadSpeed.set("");
        downloadDownloadSpeed.set("");
    }

    // ==================== 控制：统一转发给当前任务 ====================

    public void pause() {
        if (activeTask instanceof TaskControl control && control.supportsPause()
                && operationState.get() == OperationState.RUNNING) {
            control.pauseTask();
        }
    }

    public void resume() {
        if (activeTask instanceof TaskControl control && control.supportsPause()
                && operationState.get() == OperationState.PAUSED) {
            control.resumeTask();
        }
    }

    public void stop() {
        Task<?> task = activeTask;
        if (task == null || !operating.get() || !stopAvailable.get()) {
            return;
        }
        operationState.set(OperationState.STOPPING);
        if (task instanceof TaskControl control) {
            control.cancelTask();
        }
    }

    // ==================== 检查更新 ====================

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
        activeCheckId = NO_CHECK;
        checkTask = null;
        return true;
    }

    private void cancelCheckTask() {
        activeCheckId = NO_CHECK;
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

    // ==================== 安装状态刷新 ====================

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
        boolean shouldShowFullDownload = downloadSource.get() != SourceType.GLOBAL || !downloadTargetInstalled;
        showDownload.set(shouldShowFullDownload && (!downloadTargetInstalled || !downloadTargetRegistered));
    }

    private void refreshAfterAssetChange() {
        String installedVersion = readInstalledVersion();
        if (!installedVersion.isBlank()) {
            currentVersion.set(installedVersion);
        }
        Platform.runLater(this::checkUpdate);
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

    // ==================== 更新任务（coordinator）镜像 ====================

    private GameResourceUpdateTask currentUpdateTask() {
        ManagedTask managed = taskManageService.get(GameResourceUpdateTask.TASK_ID);
        GameResourceUpdateTask task = managed != null ? (GameResourceUpdateTask) managed.getTask() : null;
        if (task != updateTask) {
            rebindCoordinator(task);
            updateTask = task;
        }
        if (task != null) {
            syncUpdateState(task.phaseProperty().get());
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

    private void syncUpdateStateFromCurrentTask() {
        GameResourceUpdateTask task = currentUpdateTask();
        if (task != null) {
            syncUpdateState(task.phaseProperty().get());
        }
    }

    private void syncUpdateState(GameResourceUpdateTask.UpdateState state) {
        if (!isActiveInstallationInstalled()) {
            return;
        }
        boolean updateRunning = state == GameResourceUpdateTask.UpdateState.PREPARING
                || state == GameResourceUpdateTask.UpdateState.DOWNLOADING
                || state == GameResourceUpdateTask.UpdateState.PAUSED
                || state == GameResourceUpdateTask.UpdateState.APPLYING;
        if (updateRunning) {
            activeTask = updateTask;
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

        if (activeTask == updateTask) {
            detachOperation();
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

    // ==================== 全量下载镜像 ====================

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

    private void restoreFullDownloadIfRunning() {
        GameFullDownloadTask full = currentFullDownloadTask();
        if (full != null && isFullDownloadRunning(full.phaseProperty().get())) {
            activeTask = full;
            syncFullDownloadState(full.phaseProperty().get());
        }
    }

    private static boolean isFullDownloadRunning(GameFullDownloadTask.Phase phase) {
        return phase == GameFullDownloadTask.Phase.CHECKING
                || phase == GameFullDownloadTask.Phase.DOWNLOADING
                || phase == GameFullDownloadTask.Phase.VERIFYING
                || phase == GameFullDownloadTask.Phase.APPLYING
                || phase == GameFullDownloadTask.Phase.PAUSED;
    }

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
                activeTask = fullDownloadTask;
                operating.set(true);
                fullDownloadOperating.set(true);
                resourceOperationOperating.set(false);
                pauseAvailable.set(phase == GameFullDownloadTask.Phase.DOWNLOADING);
                stopAvailable.set(phase != GameFullDownloadTask.Phase.APPLYING);
                operationState.set(OperationState.RUNNING);
            }
            case PAUSED -> {
                activeTask = fullDownloadTask;
                operating.set(true);
                fullDownloadOperating.set(true);
                resourceOperationOperating.set(false);
                pauseAvailable.set(false);
                stopAvailable.set(true);
                operationState.set(OperationState.PAUSED);
            }
            case COMPLETED, FAILED, CANCELED -> {
                detachOperation();
            }
            case IDLE -> {
            }
        }
    }

    /** 将资源操作 Task(修复/预下载) 的进度/文案绑定到资源操作区属性。 */
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

    // ==================== 工具 ====================

    private static String managedId(Task<?> task) {
        return task.getClass().getSimpleName();
    }

    private static String managedName(Task<?> task) {
        if (task instanceof GameRepairDownloadTask) return "校验修复";
        if (task instanceof GamePreDownloadTask) return "预下载";
        return task.getClass().getSimpleName();
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

    // ==================== 对外属性 ====================

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
}