package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.download.DownloadManager;
import cn.tealc.download.model.DownloadState;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameDownloadService;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import com.kr.launcher.model.CheckUpdateResult;
import com.kr.launcher.model.ResStateInfo;
import com.kr.launcher.model.UpdateResult;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
    private GameDownloadService downloadService;
    @Inject
    private GameUpdateService updateService;
    @Inject
    private TaskManageService taskManageService;

    private final BooleanProperty operating = new SimpleBooleanProperty(false);
    private final BooleanProperty pauseAvailable = new SimpleBooleanProperty(false);
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

    private CheckUpdateResult checkResult;
    private SourceType sourceType = SourceType.DEFAULT;

    private long operationSequence;
    private volatile long activeOperationId = NO_OPERATION;
    private volatile Task<?> activeTask;
    private OperationType activeOperation = OperationType.NONE;
    private final AtomicReference<ActiveDownload> activeDownload = new AtomicReference<>();

    private long checkSequence;
    private long activeCheckId = NO_OPERATION;
    private Task<CheckUpdateResult> checkTask;

    @Override
    public void onViewAdded() {
        SourceType configuredSource = Config.setting().gameRootDirSourceProperty().get();
        sourceType = configuredSource != null ? configuredSource : SourceType.DEFAULT;
        if (downloadDir.get() == null || downloadDir.get().isBlank()) {
            downloadDir.set(defaultDownloadDir());
        }
        if (!operating.get()) {
            refreshInstalledState();
        }
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
        return new File(gameDir, "WwtBackup/" + sourceType.name().toLowerCase()).getAbsolutePath();
    }

    /** 重新检查游戏版本；运行资源操作时忽略重复检查。 */
    public void checkUpdate() {
        if (operating.get() || checkTask != null || GameResourcesManager.getGameExeBase() == null) {
            return;
        }

        long checkId = ++checkSequence;
        activeCheckId = checkId;
        status.set(LanguageManager.getString("ui.game_manager.asset.checking"));

        Task<CheckUpdateResult> task = new Task<>() {
            @Override
            protected CheckUpdateResult call() {
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
        });
        task.setOnFailed(event -> {
            if (!finishCheck(checkId, task)) {
                return;
            }
            LOG.warn("检查游戏更新失败", task.getException());
            clearCheckResult();
            status.set(LanguageManager.getString("ui.game_manager.asset.check_fail"));
        });
        task.setOnCancelled(event -> finishCheck(checkId, task));
        taskManageService.execute(task);
    }

    private boolean finishCheck(long checkId, Task<CheckUpdateResult> task) {
        if (activeCheckId != checkId || checkTask != task) {
            return false;
        }
        activeCheckId = NO_OPERATION;
        checkTask = null;
        return true;
    }

    private void cancelCheckTask() {
        activeCheckId = NO_OPERATION;
        Task<CheckUpdateResult> task = checkTask;
        checkTask = null;
        if (task != null) {
            task.cancel(true);
        }
    }

    private boolean handleCheckResult(CheckUpdateResult result) {
        if (result == null || !result.succ || result.stateInfo == null) {
            clearCheckResult();
            return false;
        }

        checkResult = result;
        ResStateInfo state = result.stateInfo;
        latestVersion.set(hasText(state.newVersion) ? state.newVersion : "-");
        if (hasText(state.usingVersion)) {
            currentVersion.set(state.usingVersion);
        }

        boolean needsUpdate = switch (state.state) {
            case ResStateInfo.STATE_NEED_DOWNLOAD,
                 ResStateInfo.STATE_DOWNLOADING,
                 ResStateInfo.STATE_PRE_DOWNLOAD,
                 ResStateInfo.STATE_REPAIRING -> true;
            default -> false;
        };
        boolean hasUpdateInfo = result.updateInfo != null;
        showUpdate.set(needsUpdate && hasUpdateInfo);
        showRepair.set(hasUpdateInfo);
        showPreDownload.set(hasUpdateInfo && updateService.isPreDownloadAvailable(result));
        tip.set(needsUpdate
                ? LanguageManager.getString("ui.game_manager.asset.tip_update") + "  " + state.newVersion
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

        SourceType selectedSource = sourceType;
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

        var launcherRes = downloadService.getLauncherResource(selectedSource);
        if (launcherRes == null || launcherRes.getCode() != 200 || launcherRes.getData() == null) {
            throw new IllegalStateException("获取下载配置失败");
        }
        var updateData = launcherRes.getData().getUpdateData();
        if (updateData == null) {
            throw new IllegalStateException("下载配置缺少更新数据");
        }
        List<String> bases = downloadService.cdnBaseUrls(updateData);
        if (bases == null || bases.isEmpty()) {
            throw new IllegalStateException("下载配置缺少 CDN 地址");
        }
        var fileInfos = loadFileInfos(updateData);
        if (fileInfos.isEmpty()) {
            throw new IllegalStateException("无文件可下载");
        }

        DownloadManager manager = downloadService.buildDownloadManager(saveDir.toPath(), bases, fileInfos);
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
        CheckUpdateResult result = checkResult;
        if (result == null || result.updateInfo == null) {
            warnCheckFirst();
            return;
        }

        long operationId = beginOperation(
                OperationType.UPDATE,
                LanguageManager.getString("ui.game_manager.asset.updating"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                awaitUpdateResult(
                        completion -> updateService.runUpdate(
                                result,
                                (state, doneSize, totalSize, doneCount, totalCount) ->
                                        updateProgressByData(operationId, doneSize, totalSize),
                                value -> completion.accept(value)),
                        "更新未返回结果",
                        "更新失败");
                return null;
            }
        };
        executeOperation(
                operationId,
                task,
                LanguageManager.getString("ui.game_manager.asset.done"),
                LanguageManager.getString("ui.game_manager.asset.update_fail"),
                this::refreshAfterAssetChange);
    }

    /** 校验并修复游戏文件。 */
    public void repair() {
        if (operating.get()) {
            return;
        }
        if (checkResult == null || checkResult.updateInfo == null) {
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
                        completion -> updateService.repair(
                                (state, info) -> {
                                    if (info != null) {
                                        updateProgressByData(operationId, info.completedSize, info.totalSize);
                                    }
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
        CheckUpdateResult result = checkResult;
        if (result == null || result.updateInfo == null || !updateService.isPreDownloadAvailable(result)) {
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
                        completion -> updateService.preDownload(
                                (state, info) -> {
                                    if (info != null) {
                                        updateProgressByData(operationId, info.completedSize, info.totalSize);
                                    }
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
            case UPDATE -> updateService.pause();
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
            case UPDATE -> updateService.resume();
            case PRE_DOWNLOAD -> updateService.resumePreDownload();
            case REPAIR -> updateService.resumeRepair();
            case NONE -> {
                return;
            }
        }
        operationState.set(OperationState.RUNNING);
    }

    public void stop() {
        if (!operating.get() || operationState.get() == OperationState.STOPPING) {
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
                case UPDATE -> updateService.stop();
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

    private List<com.kuro.game.model.game.FileInfo> loadFileInfos(
            com.kuro.game.model.launcher.item.UpdateData updateData) {
        var response = downloadService.getGameResourceList(updateData.getResourceJsonUrl());
        if (response == null || response.getCode() != 200 || response.getData() == null
                || response.getData().getResource() == null) {
            return List.of();
        }
        return response.getData().getResource();
    }

    private long beginOperation(OperationType operation, String statusText) {
        cancelCheckTask();
        long operationId = ++operationSequence;
        activeOperationId = operationId;
        activeOperation = operation;
        operating.set(true);
        pauseAvailable.set(operation != OperationType.UPDATE && operation != OperationType.REPAIR);
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
            Consumer<Consumer<UpdateResult>> starter,
            String missingResultMessage,
            String defaultFailureMessage) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UpdateResult> resultHolder = new AtomicReference<>();
        starter.accept(result -> {
            resultHolder.set(result);
            latch.countDown();
        });
        latch.await();

        UpdateResult result = resultHolder.get();
        if (result == null) {
            throw new IllegalStateException(missingResultMessage);
        }
        if (!result.success) {
            throw new IllegalStateException(hasText(result.errorMessage)
                    ? result.errorMessage
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

    public void setDownloadDir(String dir) {
        downloadDir.set(dir);
    }

    public BooleanProperty operatingProperty() {
        return operating;
    }

    public BooleanProperty pauseAvailableProperty() {
        return pauseAvailable;
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

    @Override
    public void onViewRemoved() {
        cancelCheckTask();
    }
}
