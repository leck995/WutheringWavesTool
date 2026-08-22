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
import java.nio.file.Path;
import java.util.function.BiConsumer;

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

    /** 当前操作的种类，用于失败后的重试路径与失败文案。 */
    private enum OperationKind {
        FULL_DOWNLOAD,
        UPDATE,
        REPAIR,
        PREDOWNLOAD
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
    private final BooleanProperty retryAvailable = new SimpleBooleanProperty(false);
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
    private final BooleanProperty preDownloadComplete = new SimpleBooleanProperty(false);

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

    private final ObjectProperty<SourceType> downloadSource =
            new SimpleObjectProperty<>(SourceType.DEFAULT);

    private ResourceCheckResult checkResult;
    private OperationKind lastFailedKind;
    /** 全量下载前 checkUpdate + prepareSize 的准备阶段（尚未真正开始下载）。 */
    private boolean preparingDownload;

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
        refreshInstalledState();
    }

    @Override
    public void onViewRemoved() {
        cancelCheckTask();
        detachCoordinatorListeners();
    }

    // ==================== 全量下载 ====================

    /** 全量下载：校验空目录后登记并启动 {@link GameFullDownloadTask}。 */
    public void download(String dir) {
        // 准备阶段（checkUpdate+prepareSize）进行中时允许直接进入下载。
        if (operating.get() && !preparingDownload) {
            return;
        }
        endFullDownloadPreparing(); // 结束任何残留准备态，避免占用 operating。
        if (dir == null || dir.isBlank()) {
            warnNoDownloadDir();
            return;
        }
        File saveDir = new File(dir);
        if ((!saveDir.exists() && !saveDir.mkdirs()) || !saveDir.isDirectory() || !saveDir.canWrite()) {
            warnNoDownloadDir();
            return;
        }
        // 全量下载仅允许全新空目录：目标目录若已有安装登记，则提示改选空目录。
        if (hasText(updateService.getInstalledVersion(saveDir.toPath()))) {
            NotificationManager.message(MessageInfo.warning(
                    LanguageManager.getString("ui.game_manager.asset.dir_not_empty")));
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
        attachOperation(task, true, OperationKind.FULL_DOWNLOAD, this::refreshInstalledState, null);
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
        attachOperation(task, false, OperationKind.UPDATE, this::refreshAfterAssetChange, null);
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
        attachOperation(task, false, OperationKind.REPAIR, this::refreshAfterAssetChange, null);
        taskManageService.submit(managedId(task), managedName(task), task, task,
                ManagedTask.TaskCategory.REPAIR);
    }

    // ==================== 预下载 ====================

    public void preDownload() {
        if (operating.get()) {
            return;
        }
        ResourceCheckResult result = checkResult;
        if (result == null) {
            warnCheckFirst();
            return;
        }
        if (result.isPreDownloadComplete()) {
            return;
        }
        if (!result.hasUpdatePlan() || !updateService.isPreDownloadAvailable(result)) {
            warnCheckFirst();
            return;
        }
        status.set(LanguageManager.getString("ui.game_manager.asset.predownloading"));
        GamePreDownloadTask task = new GamePreDownloadTask(updateService, result);
        bindResourceTask(task);
        attachOperation(task, false, OperationKind.PREDOWNLOAD, this::refreshAfterAssetChange, null);
        taskManageService.submit(managedId(task), managedName(task), task, task,
                ManagedTask.TaskCategory.PREDOWNLOAD);
    }

    // ==================== 磁盘空间校验 ====================

    /**
     * 磁盘空间校验 + 增量更新。先跑 PrepareTask 获取精确下载大小，校验磁盘空间，
     * 不足时回调 View 弹确认框（携带所需空间与可用空间）。
     */
    public void checkDiskSpaceAndUpdate(BiConsumer<Long, Long> onInsufficient) {
        if (operating.get() || checkResult == null) {
            warnCheckFirst();
            return;
        }
        runDiskSpaceCheck(false, this::update, onInsufficient);
    }

    /**
     * 校验修复：跳过磁盘空间预检。
     * 修复的真实下载清单来自 CheckFileTask 的 MD5 校验结果（wrongFileInfos），
     * 只有校验完成后才能算出真实所需空间；而校验本身耗时很长，不宜在预检阶段重跑。
     * CDN 层内部仍有空间兜底检查。
     */
    public void checkDiskSpaceAndRepair() {
        repair();
    }

    /**
     * 磁盘空间校验 + 预下载。
     */
    public void checkDiskSpaceAndPreDownload(BiConsumer<Long, Long> onInsufficient) {
        if (operating.get() || checkResult == null) {
            warnCheckFirst();
            return;
        }
        if (checkResult.isPreDownloadComplete()) {
            return;
        }
        if (!checkResult.hasUpdatePlan() || !updateService.isPreDownloadAvailable(checkResult)) {
            warnCheckFirst();
            return;
        }
        runDiskSpaceCheck(true, this::preDownload, onInsufficient);
    }

    /**
     * 磁盘空间校验 + 全量下载。
     * 全量下载无现成 checkResult，需先 checkUpdate 获取，再 prepare 取大小。
     * 先复用 download() 的目录校验与安装登记逻辑，再做空间预检。
     */
    public void checkDiskSpaceAndDownload(String dir, BiConsumer<Long, Long> onInsufficient) {
        if (operating.get()) {
            return;
        }
        if (dir == null || dir.isBlank()) {
            warnNoDownloadDir();
            return;
        }
        File saveDir = new File(dir);
        if ((!saveDir.exists() && !saveDir.mkdirs()) || !saveDir.isDirectory() || !saveDir.canWrite()) {
            warnNoDownloadDir();
            return;
        }
        if (hasText(updateService.getInstalledVersion(saveDir.toPath()))) {
            NotificationManager.message(MessageInfo.warning(
                    LanguageManager.getString("ui.game_manager.asset.dir_not_empty")));
            return;
        }
        // 全量下载前的准备阶段：checkUpdate + prepareSize 可能耗时较长，先给出 UI 反馈，
        // 避免「选择目录后无反应」的观感。真正进入下载后再由 download() 接管展示态。
        beginFullDownloadPreparing();
        // 登记并激活目录：清空已装版本，使 legacy 检查进入 STATE_NEED_DOWNLOAD（全量下载）。
        SourceType selectedSource = downloadSource.get();
        installationManager.configureInstallation(selectedSource, saveDir.toPath(), true);
        Config.setting().setGameInstalledVersion("");
        Config.setting().save();
        serverSwitchCoordinator.refresh();
        cancelCheckTask();
        // 全量下载无现成 checkResult，需先 check 再 prepare。
        Task<ResourceCheckResult> checkTask = new Task<>() {
            @Override
            protected ResourceCheckResult call() {
                return updateService.checkUpdate();
            }
        };
        checkTask.setOnSucceeded(e -> {
            ResourceCheckResult result = checkTask.getValue();
            if (result == null || !result.isSuccessful()) {
                endFullDownloadPreparing();
                download(dir); // check 失败，直接走现有流程
                return;
            }
            this.checkResult = result;
            // prepare 阶段仍保持「准备中」展示；空间充足/继续下载时才结束准备态并真正下载。
            runDiskSpaceCheck(false, () -> {
                endFullDownloadPreparing();
                download(dir);
            }, onInsufficient);
        });
        checkTask.setOnFailed(e -> {
            endFullDownloadPreparing();
            download(dir);
        });
        taskManageService.execute(checkTask);
    }

    /** 进入全量下载准备阶段：置忙并显示下载区「准备中」，避免准备期间无 UI 反馈。 */
    private void beginFullDownloadPreparing() {
        preparingDownload = true;
        operating.set(true);
        fullDownloadOperating.set(true);
        downloadProgress.set(0);
        downloadProgressText.set("0%");
        downloadTip.set(LanguageManager.getString("ui.game_manager.asset.prepare_download"));
        status.set(LanguageManager.getString("ui.game_manager.asset.prepare_download"));
    }

    /** 结束全量下载准备阶段：复位准备态与下载区展示，交由真正下载接管。 */
    private void endFullDownloadPreparing() {
        preparingDownload = false;
        operating.set(false);
        fullDownloadOperating.set(false);
        downloadTip.set("");
    }

    /**
     * 公共方法：后台跑 PrepareTask 获取下载大小，校验磁盘空间。
     *
     * @param isPreDownload 预下载 vs 更新/修复/全量
     * @param onSufficient   空间足够时执行（调用现有 update/repair/preDownload/download）
     * @param onInsufficient 空间不足时回调 View（携带所需空间与可用空间，弹确认框）
     */
    private void runDiskSpaceCheck(boolean isPreDownload, Runnable onSufficient,
            BiConsumer<Long, Long> onInsufficient) {        ResourceCheckResult result = checkResult;
        Task<Long> checkTask = new Task<>() {
            @Override
            protected Long call() {
                return updateService.prepareSize(result, isPreDownload);
            }
        };
        checkTask.setOnSucceeded(e -> {
            // 准备阶段（checkUpdate+prepareSize）到此结束；无论空间是否充足都复位，
            // 避免用户取消弹窗后停留在残留的「准备中」展示。
            endFullDownloadPreparing();
            long requiredSize = checkTask.getValue();
            if (requiredSize <= 0) {
                onSufficient.run(); // prepare 失败，不阻塞用户
                return;
            }
            long freeSpace = updateService.getCacheAvailableSpace();
            if (freeSpace >= requiredSize) {
                onSufficient.run();
            } else {
                onInsufficient.accept(requiredSize, freeSpace);
            }
        });
        checkTask.setOnFailed(e -> {
            endFullDownloadPreparing();
            onSufficient.run();
        });
        taskManageService.execute(checkTask);
    }

    // ==================== 统一操作生命周期 ====================

    /**
     * 挂接一个操作任务：设置运行态；成功时复位并回调，失败/取消时保留失败态以便展示原因与重试按钮。
     * 失败具体文案由各 Task 上抛（FAILED 阶段 / 异常），此处只负责让面板在失败后停住而非消失。
     */
    private void attachOperation(Task<?> task, boolean download, OperationKind kind,
            Runnable onSuccess, Runnable onFailure) {
        boolean canPause = task instanceof TaskControl control && control.supportsPause();
        activeTask = task;
        lastFailedKind = null;
        operating.set(true);
        fullDownloadOperating.set(download);
        resourceOperationOperating.set(!download);
        pauseAvailable.set(canPause);
        stopAvailable.set(true);
        retryAvailable.set(false);
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
                    onOperationEnded(kind, download, newState, onFailure);
                }
            }
        });
    }

    /**
     * 操作失败/取消后的显示态。FAILED 保留进度区展示失败原因并提供「重试」，由用户主动重试后才复位；
     * CANCELLED（用户主动停止）直接复位到就绪。
     */
    private void onOperationEnded(OperationKind kind, boolean download, Worker.State state,
            Runnable onFailure) {
        if (state == Worker.State.CANCELLED) {
            detachOperation();
            return;
        }
        activeTask = null;
        lastFailedKind = kind;
        operating.set(true);
        fullDownloadOperating.set(download);
        resourceOperationOperating.set(!download);
        pauseAvailable.set(false);
        stopAvailable.set(false);
        retryAvailable.set(true);
        operationState.set(OperationState.IDLE);
        downloadSpeed.set("");
        downloadDownloadSpeed.set("");
        if (onFailure != null) {
            onFailure.run();
        }
    }

    private void detachOperation() {
        activeTask = null;
        lastFailedKind = null;
        operating.set(false);
        fullDownloadOperating.set(false);
        resourceOperationOperating.set(false);
        pauseAvailable.set(false);
        stopAvailable.set(false);
        retryAvailable.set(false);
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

    /**
     * 重试失败的操作。先复位失败显示态，再按失败类型重新发起任务；
     * 重试是否可执行由 {@link #retryAvailable} 决定（仅在失败的显示态下为 true）。
     */
    public void retry() {
        if (!retryAvailable.get()) {
            return;
        }
        OperationKind kind = lastFailedKind;
        if (kind == null) {
            return;
        }
        // 退出失败显示态，使 update/repair/preDownload/download 的 operating 校验放行。
        detachOperation();
        switch (kind) {
            case FULL_DOWNLOAD -> retryFullDownload();
            case UPDATE -> update();
            case REPAIR -> repair();
            case PREDOWNLOAD -> preDownload();
        }
    }

    /** 全量下载重试：复用已登记的目标目录重新发起下载。 */
    private void retryFullDownload() {
        SourceType source = downloadSource.get();
        Path dir = installationManager.gameDirectory(GameInstallationManager.editionOf(source)).orElse(null);
        if (dir == null) {
            warnNoDownloadDir();
            return;
        }
        download(dir.toString());
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
        showUpdate.set(needsUpdate);
        showRepair.set(result.isRepairAvailable());
        boolean pdComplete = result.isPreDownloadComplete();
        boolean pdAvailable = result.hasUpdatePlan() && updateService.isPreDownloadAvailable(result);
        preDownloadComplete.set(pdComplete);
        showPreDownload.set(pdAvailable || pdComplete);
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
        preDownloadComplete.set(false);
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
            preDownloadComplete.set(false);
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

    /** 刷新全量下载入口：下载按钮始终可见，仅在已安装时展示来源提示。 */
    private void refreshFullDownloadState() {
        boolean downloadTargetInstalled = installationManager.isConfigured(downloadSource.get());
        boolean downloadTargetRegistered = installationManager
                .gameDirectory(GameInstallationManager.editionOf(downloadSource.get()))
                .map(path -> hasText(updateService.getInstalledVersion(path)))
                .orElse(false);
        showDownloadSourceHint.set(downloadTargetInstalled || downloadTargetRegistered);
        showDownload.set(true);
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
            case FAILED -> {
                // 失败保留更新操作区：展示原因并提供重试（由 attachOperation 的终态监听统一复位）。
                showUpdate.set(false);
                lastFailedKind = OperationKind.UPDATE;
                operating.set(true);
                fullDownloadOperating.set(false);
                resourceOperationOperating.set(true);
                retryAvailable.set(true);
                status.set(updateTask.statusTextProperty().get());
                tip.set(updateTask.detailTextProperty().get());
                progress.set(updateTask.progressProperty().get());
                progressText.set(updateTask.progressTextProperty().get());
            }
            case CANCELED -> {
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
                pauseAvailable.set(true);
                stopAvailable.set(true);
                operationState.set(OperationState.PAUSED);
            }
            case COMPLETED, FAILED, CANCELED -> {
                // 运行态复位与 onSuccess 统一由 attachOperation 的 state 监听负责，
                // 这里只收起下载专属区的视觉表现，避免与 state 监听竞争导致 onSuccess 丢失。
                fullDownloadOperating.set(false);
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

    public ReadOnlyBooleanProperty retryAvailableProperty() {
        return retryAvailable;
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

    public BooleanProperty preDownloadCompleteProperty() {
        return preDownloadComplete;
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