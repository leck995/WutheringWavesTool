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
import cn.tealc.wutheringwavestool.thread.game.download.AbstractGameDownloadTask;
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
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
 * <p>核心设计：两个 Task 属性（{@link #activeResourceTask} / {@link #activeDownloadTask}）
 * 是操作状态的唯一事实来源。进度、速度、文案、按钮显隐等全部从这两个 Task 的
 * 内置属性（{@code progress / message / title / state}）和 {@link AbstractGameDownloadTask}
 * 的 {@code speedText / paused} 派生，不再手动拷贝。</p>
 */
public class GameAssetViewModel extends BaseViewModel implements SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(GameAssetViewModel.class);
    private static final long NO_CHECK = -1;

    public enum OperationState { IDLE, RUNNING, PAUSED, STOPPING }

    @Inject
    private GameUpdateService updateService;
    @Inject
    private TaskManageService taskManageService;
    @Inject
    private GameServerSwitchCoordinator serverSwitchCoordinator;
    @Inject
    private GameInstallationManager installationManager;

    // ==================== Task 事实来源 ====================

    /** 资源操作区当前任务（更新/修复/预下载）。null 表示空闲。 */
    private final ReadOnlyObjectWrapper<Task<?>> activeResourceTask = new ReadOnlyObjectWrapper<>();
    /** 全量下载区当前任务。null 表示空闲。 */
    private final ReadOnlyObjectWrapper<Task<?>> activeDownloadTask = new ReadOnlyObjectWrapper<>();

    // ==================== 操作状态（从 Task 派生） ====================

    private final BooleanProperty operating = new SimpleBooleanProperty(false);
    private final BooleanProperty pauseAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty stopAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty retryAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty fullDownloadOperating = new SimpleBooleanProperty(false);
    private final BooleanProperty resourceOperationOperating = new SimpleBooleanProperty(false);
    private final ReadOnlyObjectWrapper<OperationState> operationState =
            new ReadOnlyObjectWrapper<>(OperationState.IDLE);

    // ==================== 进度/状态属性（从 Task 派生） ====================

    private final StringProperty currentVersion = new SimpleStringProperty("-");
    private final StringProperty latestVersion = new SimpleStringProperty("-");
    private final StringProperty status = new SimpleStringProperty("就绪");

    // 资源操作区
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty("0%");
    private final StringProperty downloadSpeed = new SimpleStringProperty("");
    private final StringProperty tip = new SimpleStringProperty("");

    // 全量下载区
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(0);
    private final StringProperty downloadProgressText = new SimpleStringProperty("0%");
    private final StringProperty downloadDownloadSpeed = new SimpleStringProperty("");
    private final StringProperty downloadTip = new SimpleStringProperty("");

    // ==================== 业务状态 ====================

    private final BooleanProperty showDownload = new SimpleBooleanProperty(true);
    private final BooleanProperty showDownloadSourceHint = new SimpleBooleanProperty(false);
    private final BooleanProperty showUpdate = new SimpleBooleanProperty(false);
    private final BooleanProperty showRepair = new SimpleBooleanProperty(false);
    private final BooleanProperty showPreDownload = new SimpleBooleanProperty(false);
    private final BooleanProperty preDownloadComplete = new SimpleBooleanProperty(false);

    private final ObjectProperty<SourceType> downloadSource =
            new SimpleObjectProperty<>(SourceType.DEFAULT);

    private ResourceCheckResult checkResult;
    private long checkSequence;
    private long activeCheckId = NO_CHECK;
    private Task<ResourceCheckResult> checkTask;

    /** 全量下载磁盘空间预检阶段（checkUpdate+prepareSize），此时 real task 尚未创建。 */
    private boolean preparingForDownload;

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

        // activeResourceTask 变化时自动 rebind 所有资源区属性
        activeResourceTask.addListener((obs, oldTask, newTask) -> {
            rebindResourceProperties(newTask);
            updateOperatingState();
            if (newTask != null) {
                newTask.stateProperty().addListener((o, a, state) -> onResourceTaskStateChanged(newTask, state));
            }
        });

        // activeDownloadTask 变化时自动 rebind 所有下载区属性
        activeDownloadTask.addListener((obs, oldTask, newTask) -> {
            rebindDownloadProperties(newTask);
            updateOperatingState();
            if (newTask != null) {
                newTask.stateProperty().addListener((o, a, state) -> onDownloadTaskStateChanged(newTask, state));
            }
        });
    }

    public void initialize() {
        restoreRunningTasks();
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
        restoreRunningTasks();
        SourceType configuredSource = Config.setting().gameRootDirSourceProperty().get();
        downloadSource.set(normalizeDownloadSource(configuredSource));
        refreshInstalledState();
    }

    @Override
    public void onViewRemoved() {
        cancelCheckTask();
    }

    /** 恢复已在后台运行的任务（从其他页面发起）。 */
    private void restoreRunningTasks() {
        ManagedTask managed = taskManageService.get(GameResourceUpdateTask.TASK_ID);
        if (managed != null) {
            GameResourceUpdateTask task = (GameResourceUpdateTask) managed.getTask();
            if (task.isRunning() && activeResourceTask.get() == null) {
                activeResourceTask.set(task);
            }
        }
        managed = taskManageService.get(GameFullDownloadTask.TASK_ID);
        if (managed != null) {
            GameFullDownloadTask task = (GameFullDownloadTask) managed.getTask();
            if (task.isRunning() && activeDownloadTask.get() == null) {
                activeDownloadTask.set(task);
            }
        }
    }

    // ==================== 属性 rebind（核心：Task 切换时自动绑定/解绑） ====================

    private void rebindResourceProperties(Task<?> task) {
        // 进度
        progress.unbind();
        progressText.unbind();
        if (task != null) {
            progress.bind(task.progressProperty());
            progressText.bind(javafx.beans.binding.Bindings.createStringBinding(
                    () -> {
                        double p = task.getProgress();
                        return p >= 0 ? String.format("%.1f%%", p * 100) : "0%";
                    }, task.progressProperty()));
        } else {
            progress.set(0);
            progressText.set("0%");
        }
        // 操作阶段提示（进度条下方）：合并 Task title 和 message（阶段 + 进度数据）
        tip.unbind();
        if (task != null) {
            tip.bind(javafx.beans.binding.Bindings.createStringBinding(
                    () -> {
                        String t = task.getTitle();
                        String m = task.getMessage();
                        if (t == null || t.isBlank()) return m != null ? m : "";
                        if (m == null || m.isBlank()) return t;
                        return t + "  " + m;
                    },
                    task.titleProperty(), task.messageProperty()));
        } else {
            tip.set("");
        }
        // 速度
        downloadSpeed.unbind();
        if (task instanceof AbstractGameDownloadTask<?> g) {
            downloadSpeed.bind(g.speedTextProperty());
        } else {
            downloadSpeed.set("");
        }
        // 状态摘要：不再绑定 Task title，改为静态文本
        status.unbind();
        if (task != null) {
            status.set("资源操作中");
        }
    }

    private void rebindDownloadProperties(Task<?> task) {
        downloadProgress.unbind();
        downloadProgressText.unbind();
        if (task != null) {
            downloadProgress.bind(task.progressProperty());
            downloadProgressText.bind(javafx.beans.binding.Bindings.createStringBinding(
                    () -> {
                        double p = task.getProgress();
                        return p >= 0 ? String.format("%.1f%%", p * 100) : "0%";
                    }, task.progressProperty()));
        } else {
            downloadProgress.set(0);
            downloadProgressText.set("0%");
        }
        // 操作阶段提示（进度条下方）：合并 Task title 和 message（阶段 + 进度数据）
        downloadTip.unbind();
        if (task != null) {
            downloadTip.bind(javafx.beans.binding.Bindings.createStringBinding(
                    () -> {
                        String t = task.getTitle();
                        String m = task.getMessage();
                        if (t == null || t.isBlank()) return m != null ? m : "";
                        if (m == null || m.isBlank()) return t;
                        return t + "  " + m;
                    },
                    task.titleProperty(), task.messageProperty()));
        } else {
            downloadTip.set("");
        }
        downloadDownloadSpeed.unbind();
        if (task instanceof AbstractGameDownloadTask<?> g) {
            downloadDownloadSpeed.bind(g.speedTextProperty());
        } else {
            downloadDownloadSpeed.set("");
        }
        // 状态摘要：不再绑定 Task title，改为静态文本
        if (task != null) {
            status.set("下载中");
        }
    }

    // ==================== 操作状态派生 ====================

    private void updateOperatingState() {
        boolean resActive = activeResourceTask.get() != null;
        boolean dlActive = activeDownloadTask.get() != null || preparingForDownload;
        operating.set(resActive || dlActive);
        resourceOperationOperating.set(resActive);
        fullDownloadOperating.set(dlActive);

        if (!resActive && !dlActive) {
            pauseAvailable.set(false);
            stopAvailable.set(false);
            retryAvailable.set(false);
            operationState.set(OperationState.IDLE);
        }
    }

    private void onResourceTaskStateChanged(Task<?> task, Worker.State state) {
        if (task != activeResourceTask.get()) return; // stale
        switch (state) {
            case SUCCEEDED -> {
                status.set("资源操作完成");
                activeResourceTask.set(null);
                refreshAfterAssetChange();
            }
            case CANCELLED -> {
                status.set("资源操作已取消");
                activeResourceTask.set(null);
            }
            case FAILED -> {
                // 保留在 activeResourceTask 中展示失败信息，允许重试
                status.set("资源操作失败");
                retryAvailable.set(true);
                pauseAvailable.set(false);
                stopAvailable.set(false);
                operationState.set(OperationState.IDLE);
            }
            default -> {
                if (task instanceof AbstractGameDownloadTask<?> g && g.isPaused()) {
                    operationState.set(OperationState.PAUSED);
                } else {
                    operationState.set(OperationState.RUNNING);
                }
                pauseAvailable.set(task instanceof TaskControl tc && tc.supportsPause()
                        && !(task instanceof AbstractGameDownloadTask<?> g && g.isPaused()));
                stopAvailable.set(true);
                retryAvailable.set(false);
            }
        }
    }

    private void onDownloadTaskStateChanged(Task<?> task, Worker.State state) {
        if (task != activeDownloadTask.get()) return;
        switch (state) {
            case SUCCEEDED -> {
                status.set("下载完成");
                activeDownloadTask.set(null);
                refreshInstalledState();
            }
            case CANCELLED -> {
                status.set("下载已取消");
                activeDownloadTask.set(null);
            }
            case FAILED -> {
                status.set("下载失败");
                retryAvailable.set(true);
                pauseAvailable.set(false);
                stopAvailable.set(false);
                operationState.set(OperationState.IDLE);
            }
            default -> {
                if (task instanceof AbstractGameDownloadTask<?> g && g.isPaused()) {
                    operationState.set(OperationState.PAUSED);
                } else {
                    operationState.set(OperationState.RUNNING);
                }
                pauseAvailable.set(task instanceof TaskControl tc && tc.supportsPause()
                        && !(task instanceof AbstractGameDownloadTask<?> g && g.isPaused()));
                stopAvailable.set(true);
                retryAvailable.set(false);
            }
        }
    }

    // ==================== 全量下载 ====================

    public void download(String dir) {
        if (activeDownloadTask.get() != null) return;
        endPreparingForDownload();
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
        SourceType selectedSource = downloadSource.get();
        installationManager.configureInstallation(selectedSource, saveDir.toPath(), true);
        Config.setting().setGameInstalledVersion("");
        Config.setting().save();
        serverSwitchCoordinator.refresh();

        cancelCheckTask();
        GameFullDownloadTask task = new GameFullDownloadTask(updateService);
        task.setOnFailed(e -> LOG.warn("全量下载失败", task.getException()));
        activeDownloadTask.set(task);
        taskManageService.submit(GameFullDownloadTask.TASK_ID,
                LanguageManager.getString("ui.game_manager.asset.download"),
                task, task, ManagedTask.TaskCategory.DOWNLOAD);
    }

    // ==================== 增量更新 ====================

    public void update() {
        if (activeResourceTask.get() != null) return;
        if (checkResult == null) {
            warnCheckFirst();
            return;
        }
        // 检查是否有 HomeViewModel 已创建的任务
        ManagedTask managed = taskManageService.get(GameResourceUpdateTask.TASK_ID);
        if (managed != null) {
            GameResourceUpdateTask existing = (GameResourceUpdateTask) managed.getTask();
            if (existing.isRunning()) {
                activeResourceTask.set(existing);
                return;
            }
        }
        GameResourceUpdateTask task = new GameResourceUpdateTask(updateService);
        task.setCheckResult(checkResult);
        task.setOnFailed(e -> LOG.warn("增量更新失败", task.getException()));
        activeResourceTask.set(task);
        taskManageService.submit(GameResourceUpdateTask.TASK_ID,
                LanguageManager.getString("ui.game_manager.asset.update"),
                task, task, ManagedTask.TaskCategory.UPDATE);
    }

    // ==================== 校验修复 ====================

    public void repair() {
        if (activeResourceTask.get() != null) return;
        if (checkResult == null || !checkResult.isRepairAvailable()) {
            warnCheckFirst();
            return;
        }
        GameRepairDownloadTask task = new GameRepairDownloadTask(updateService, checkResult);
        task.setOnFailed(e -> LOG.warn("校验修复失败", task.getException()));
        activeResourceTask.set(task);
        taskManageService.submit(task.getClass().getSimpleName(), "校验修复", task, task,
                ManagedTask.TaskCategory.REPAIR);
    }

    // ==================== 预下载 ====================

    public void preDownload() {
        if (activeResourceTask.get() != null) return;
        ResourceCheckResult result = checkResult;
        if (result == null) {
            warnCheckFirst();
            return;
        }
        if (result.isPreDownloadComplete()) return;
        if (!result.hasUpdatePlan() || !updateService.isPreDownloadAvailable(result)) {
            warnCheckFirst();
            return;
        }
        GamePreDownloadTask task = new GamePreDownloadTask(updateService, result);
        task.setOnFailed(e -> LOG.warn("预下载失败", task.getException()));
        activeResourceTask.set(task);
        taskManageService.submit(task.getClass().getSimpleName(), "预下载", task, task,
                ManagedTask.TaskCategory.PREDOWNLOAD);
    }

    // ==================== 磁盘空间校验 ====================

    public void checkDiskSpaceAndUpdate(BiConsumer<Long, Long> onInsufficient) {
        if (activeResourceTask.get() != null || checkResult == null) {
            warnCheckFirst();
            return;
        }
        runDiskSpaceCheck(false, this::update, onInsufficient);
    }

    public void checkDiskSpaceAndRepair() {
        repair();
    }

    public void checkDiskSpaceAndPreDownload(BiConsumer<Long, Long> onInsufficient) {
        if (activeResourceTask.get() != null || checkResult == null) {
            warnCheckFirst();
            return;
        }
        if (checkResult.isPreDownloadComplete()) return;
        if (!checkResult.hasUpdatePlan() || !updateService.isPreDownloadAvailable(checkResult)) {
            warnCheckFirst();
            return;
        }
        runDiskSpaceCheck(true, this::preDownload, onInsufficient);
    }

    public void checkDiskSpaceAndDownload(String dir, BiConsumer<Long, Long> onInsufficient) {
        if (activeDownloadTask.get() != null) return;
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
        beginPreparingForDownload();
        SourceType selectedSource = downloadSource.get();
        installationManager.configureInstallation(selectedSource, saveDir.toPath(), true);
        Config.setting().setGameInstalledVersion("");
        Config.setting().save();
        serverSwitchCoordinator.refresh();
        cancelCheckTask();

        Task<ResourceCheckResult> checkTask = new Task<>() {
            @Override
            protected ResourceCheckResult call() {
                return updateService.checkUpdate();
            }
        };
        checkTask.setOnSucceeded(e -> {
            ResourceCheckResult result = checkTask.getValue();
            if (result == null || !result.isSuccessful()) {
                endPreparingForDownload();
                download(dir);
                return;
            }
            GameAssetViewModel.this.checkResult = result;
            runDiskSpaceCheck(false, () -> {
                endPreparingForDownload();
                download(dir);
            }, onInsufficient);
        });
        checkTask.setOnFailed(e -> {
            endPreparingForDownload();
            download(dir);
        });
        taskManageService.execute(checkTask);
    }

    private void beginPreparingForDownload() {
        preparingForDownload = true;
        operating.set(true);
        fullDownloadOperating.set(true);
        downloadProgress.set(0);
        downloadProgressText.set("0%");
        downloadTip.set(LanguageManager.getString("ui.game_manager.asset.prepare_download"));
        status.set(LanguageManager.getString("ui.game_manager.asset.prepare_download"));
    }

    private void endPreparingForDownload() {
        if (!preparingForDownload) return;
        preparingForDownload = false;
        operating.set(false);
        fullDownloadOperating.set(false);
        downloadTip.set("");
    }

    private void runDiskSpaceCheck(boolean isPreDownload, Runnable onSufficient,
            BiConsumer<Long, Long> onInsufficient) {
        ResourceCheckResult result = checkResult;
        Task<Long> checkTask = new Task<>() {
            @Override
            protected Long call() {
                return updateService.prepareSize(result, isPreDownload);
            }
        };
        checkTask.setOnSucceeded(e -> {
            endPreparingForDownload();
            long requiredSize = checkTask.getValue();
            if (requiredSize <= 0) {
                onSufficient.run();
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
            endPreparingForDownload();
            onSufficient.run();
        });
        taskManageService.execute(checkTask);
    }

    // ==================== 控制：统一转发给当前任务 ====================

    /** 暂停当前任务，成功后更新操作状态。 */
    public void pause() {
        Task<?> task = activeResourceTask.get() != null ? activeResourceTask.get() : activeDownloadTask.get();
        if (task instanceof TaskControl tc && tc.supportsPause() && task.isRunning()) {
            if (tc.pauseTask()) {
                operationState.set(OperationState.PAUSED);
            }
        }
    }

    /** 恢复当前任务，成功后更新操作状态。 */
    public void resume() {
        Task<?> task = activeResourceTask.get() != null ? activeResourceTask.get() : activeDownloadTask.get();
        if (task instanceof TaskControl tc && tc.supportsPause() && task.isRunning()) {
            if (tc.resumeTask()) {
                operationState.set(OperationState.RUNNING);
            }
        }
    }

    public void stop() {
        Task<?> task = activeResourceTask.get() != null ? activeResourceTask.get() : activeDownloadTask.get();
        if (task == null) return;
        operationState.set(OperationState.STOPPING);
        if (task instanceof TaskControl tc) {
            tc.cancelTask();
        }
    }

    /** 重试失败的操作。从 activeTask 类型判断重试路径。 */
    public void retry() {
        if (!retryAvailable.get()) return;
        // 资源操作区
        Task<?> resTask = activeResourceTask.get();
        if (resTask != null && resTask.getState() == Worker.State.FAILED) {
            activeResourceTask.set(null);
            if (resTask instanceof GameResourceUpdateTask) update();
            else if (resTask instanceof GameRepairDownloadTask) repair();
            else if (resTask instanceof GamePreDownloadTask) preDownload();
            return;
        }
        // 下载区
        Task<?> dlTask = activeDownloadTask.get();
        if (dlTask != null && dlTask.getState() == Worker.State.FAILED) {
            activeDownloadTask.set(null);
            retryFullDownload();
        }
    }

    private void retryFullDownload() {
        SourceType source = downloadSource.get();
        Path dir = installationManager.gameDirectory(
                GameInstallationManager.editionOf(source)).orElse(null);
        if (dir == null) {
            warnNoDownloadDir();
            return;
        }
        download(dir.toString());
    }

    // ==================== 检查更新 ====================

    public void checkUpdate() {
        if (activeResourceTask.get() != null || activeDownloadTask.get() != null
                || checkTask != null || !isActiveInstallationInstalled()
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
            if (!finishCheck(checkId, task)) return;
            boolean success = handleCheckResult(task.getValue());
            status.set(LanguageManager.getString(success
                    ? "ui.game_manager.asset.ready"
                    : "ui.game_manager.asset.check_fail"));
        });
        task.setOnFailed(event -> {
            if (!finishCheck(checkId, task)) return;
            LOG.warn("检查游戏更新失败", task.getException());
            clearCheckResult();
            status.set(LanguageManager.getString("ui.game_manager.asset.check_fail"));
        });
        task.setOnCancelled(event -> finishCheck(checkId, task));
        taskManageService.execute(task);
    }

    private boolean finishCheck(long checkId, Task<ResourceCheckResult> task) {
        if (activeCheckId != checkId || checkTask != task) return false;
        activeCheckId = NO_CHECK;
        checkTask = null;
        return true;
    }

    private void cancelCheckTask() {
        activeCheckId = NO_CHECK;
        Task<ResourceCheckResult> task = checkTask;
        checkTask = null;
        if (task != null) task.cancel(true);
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
        if (!isActiveInstallationInstalled()) {
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

    // ==================== 工具 ====================

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

    public BooleanProperty operatingProperty() { return operating; }
    public BooleanProperty pauseAvailableProperty() { return pauseAvailable; }
    public BooleanProperty stopAvailableProperty() { return stopAvailable; }
    public ReadOnlyBooleanProperty retryAvailableProperty() { return retryAvailable; }
    public ReadOnlyBooleanProperty fullDownloadOperatingProperty() { return fullDownloadOperating; }
    public ReadOnlyBooleanProperty resourceOperationOperatingProperty() { return resourceOperationOperating; }
    public ReadOnlyObjectProperty<OperationState> operationStateProperty() { return operationState.getReadOnlyProperty(); }

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
    public ReadOnlyDoubleProperty serverSwitchProgressProperty() {
        return serverSwitchCoordinator.progressProperty();
    }
    public ReadOnlyStringProperty serverSwitchStatusTextProperty() {
        return serverSwitchCoordinator.statusTextProperty();
    }
    public ReadOnlyStringProperty serverSwitchDetailTextProperty() {
        return serverSwitchCoordinator.detailTextProperty();
    }

    public StringProperty currentVersionProperty() { return currentVersion; }
    public StringProperty latestVersionProperty() { return latestVersion; }
    public StringProperty statusProperty() { return status; }
    public BooleanProperty showDownloadProperty() { return showDownload; }
    public BooleanProperty showDownloadSourceHintProperty() { return showDownloadSourceHint; }
    public BooleanProperty showUpdateProperty() { return showUpdate; }
    public BooleanProperty showRepairProperty() { return showRepair; }
    public BooleanProperty showPreDownloadProperty() { return showPreDownload; }
    public BooleanProperty preDownloadCompleteProperty() { return preDownloadComplete; }

    public DoubleProperty progressProperty() { return progress; }
    public StringProperty progressTextProperty() { return progressText; }
    public StringProperty downloadSpeedProperty() { return downloadSpeed; }
    public StringProperty tipProperty() { return tip; }
    public DoubleProperty downloadProgressProperty() { return downloadProgress; }
    public StringProperty downloadProgressTextProperty() { return downloadProgressText; }
    public StringProperty downloadDownloadSpeedProperty() { return downloadDownloadSpeed; }
    public StringProperty downloadTipProperty() { return downloadTip; }

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

    public ObjectProperty<SourceType> downloadSourceProperty() { return downloadSource; }
    public StringProperty customDownloadCacheDirProperty() {
        return Config.setting().customDownloadCacheDirProperty();
    }
    public void setCustomDownloadCacheDir(String cacheDir) {
        Config.setting().setCustomDownloadCacheDir(cacheDir);
        Config.setting().save();
    }
}