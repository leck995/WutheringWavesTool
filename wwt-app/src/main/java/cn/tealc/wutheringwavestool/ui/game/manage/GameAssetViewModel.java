package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.download.model.DownloadState;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameDownloadService;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.google.inject.Inject;
import com.kr.launcher.model.CheckUpdateResult;
import com.kr.launcher.model.ResStateInfo;
import com.kr.launcher.model.UpdateInfo;
import com.kr.launcher.model.UpdateResult;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 统一「游戏资源管理」ViewModel：全量下载 + 增量更新 + 预下载 + 校验修复合一。
 *
 * <p>状态机：<ul>
 * <li>未安装 → 仅提供全量下载；</li>
 * <li>已安装 → 打开时自动 checkUpdate，据此提供更新 / 预下载 / 校验修复。</li></ul>
 * 单一进度条 {@link #progressProperty}，下方文本 {@link #tipProperty}。
 */
public class GameAssetViewModel extends BaseViewModel implements SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(GameAssetViewModel.class);

    @Inject
    private GameDownloadService downloadService;
    @Inject
    private GameUpdateService updateService;
    @Inject
    private TaskManageService taskManageService;

    // ---- 安装态 ----
    private final BooleanProperty installed = new SimpleBooleanProperty(false);

    // ---- 操作态（互斥） ----
    private final BooleanProperty operating = new SimpleBooleanProperty(false);
    private final StringProperty operationName = new SimpleStringProperty("");

    // ---- 版本与状态 ----
    private final StringProperty currentVersion = new SimpleStringProperty("-");
    private final StringProperty latestVersion = new SimpleStringProperty("-");
    private final StringProperty status = new SimpleStringProperty("就绪");

    // ---- 按钮可见性 ----
    private final BooleanProperty showDownload = new SimpleBooleanProperty(true);      // 全量下载
    private final BooleanProperty showUpdate = new SimpleBooleanProperty(false);        // 更新
    private final BooleanProperty showRepair = new SimpleBooleanProperty(false);        // 校验修复
    private final BooleanProperty showPreDownload = new SimpleBooleanProperty(false);   // 预下载

    // ---- 统一进度 ----
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty("0%");
    private final StringProperty tip = new SimpleStringProperty("");

    // ---- 下载目录 ----
    private final StringProperty downloadDir = new SimpleStringProperty();

    private CheckUpdateResult checkResult;
    private SourceType sourceType;

    /** 当前活动操作类型：download / update / predownload / repair */
    private String activeOp = "";

    /** 当前正在运行的 JavaFX Task，用于 stop() 时取消以解除 latch 阻塞。 */
    private Task<?> activeTask;

    /** 防止 onViewAdded 被 mvvmfx 多次触发导致重复初始化/检查更新。 */
    private boolean initialized = false;

    @Override
    public void onViewAdded() {
        sourceType = Config.setting().gameRootDirSourceProperty().get();
        if (sourceType == null) {
            sourceType = SourceType.DEFAULT;
        }
        downloadDir.set(defaultDownloadDir());
        if (initialized) {
            // 视图可能被 mvvmfx 多次 add，避免重复初始化/重复检查更新
            return;
        }
        initialized = true;
        refreshInstalledState();
    }

    private void refreshInstalledState() {
        boolean isInstalled = GameResourcesManager.getGameExeBase() != null;
        installed.set(isInstalled);
        if (isInstalled) {
            showDownload.set(true);
            showRepair.set(true);
            currentVersion.set(readInstalledVersion());
            status.set(LanguageManager.getString("ui.game_manager.asset.ready"));
            // 已安装 → 打开即自动检查更新
            checkUpdateSilently();
        } else {
            showDownload.set(true);
            showUpdate.set(false);
            showRepair.set(false);
            showPreDownload.set(false);
            status.set(LanguageManager.getString("ui.game_manager.asset.not_installed"));
        }
    }

    private String readInstalledVersion() {
        try {
            return updateService.getInstalledVersion();
        } catch (Exception e) {
            LOG.warn("读取已安装版本失败", e);
            return "";
        }
    }

    private String defaultDownloadDir() {
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir != null) {
            return new File(gameDir, "WwtBackup/" + sourceType.name().toLowerCase()).getAbsolutePath();
        }
        return "";
    }

    /** 打开已安装界面时静默检查更新（失败不打扰，仅更新状态）。 */
    private void checkUpdateSilently() {
        status.set(LanguageManager.getString("ui.game_manager.asset.checking"));
        Task<CheckUpdateResult> task = new Task<>() {
            @Override
            protected CheckUpdateResult call() {
                return updateService.checkUpdate();
            }
        };
        task.setOnSucceeded(e -> {
            handleCheckResult(task.getValue());
            if (status.get().equals(LanguageManager.getString("ui.game_manager.asset.checking"))) {
                status.set(LanguageManager.getString("ui.game_manager.asset.ready"));
            }
        });
        task.setOnFailed(e -> status.set(LanguageManager.getString("ui.game_manager.asset.check_fail")));
        taskManageService.execute(task);
    }

    private void handleCheckResult(CheckUpdateResult result) {
        this.checkResult = result;
        if (result == null || !result.succ || result.stateInfo == null) {
            showUpdate.set(false);
            showPreDownload.set(false);
            return;
        }
        ResStateInfo state = result.stateInfo;
        if (state.newVersion != null && !state.newVersion.isEmpty()) {
            latestVersion.set(state.newVersion);
        }
        if (state.usingVersion != null && !state.usingVersion.isEmpty()) {
            currentVersion.set(state.usingVersion);
        }

        switch (state.state) {
            case ResStateInfo.STATE_NEED_DOWNLOAD,
                 ResStateInfo.STATE_DOWNLOADING,
                 ResStateInfo.STATE_PRE_DOWNLOAD,
                 ResStateInfo.STATE_REPAIRING -> {
                showUpdate.set(true);
                tip.set(LanguageManager.getString("ui.game_manager.asset.tip_update")
                        + "  " + state.newVersion);
            }
            default -> {
                showUpdate.set(false);
                tip.set(LanguageManager.getString("ui.game_manager.asset.tip_up_to_date"));
            }
        }
        showRepair.set(true);
        showPreDownload.set(updateService.isPreDownloadAvailable(result));
    }

    // ==================== 操作 ====================

    /** 全量下载到 {{@link #downloadDir}}。 */
    public void download() {
        if (operating.get()) {
            return;
        }
        String dir = downloadDir.get();
        if (dir == null || dir.isBlank()) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.asset.no_dir")));
            return;
        }
        File saveDir = new File(dir);
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.asset.no_dir")));
            return;
        }
        setOperating("download", LanguageManager.getString("ui.game_manager.asset.downloading"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                runFullDownload(saveDir);
                return null;
            }
        };
        task.setOnFailed(e -> {
            if (task.isCancelled()) {
                endOperating(LanguageManager.getString("ui.game_manager.asset.stopped"));
                return;
            }
            endOperating(LanguageManager.getString("ui.game_manager.asset.fail"));
        });
        task.setOnSucceeded(e -> {
            endOperating(LanguageManager.getString("ui.game_manager.asset.done"));
        });
        task.setOnCancelled(e -> endOperating(LanguageManager.getString("ui.game_manager.asset.stopped")));
        activeTask = task;
        taskManageService.execute(task);
    }

    private void runFullDownload(File saveDir) throws Exception {
        var launcherRes = downloadService.getLauncherResource(sourceType);
        if (launcherRes.getCode() != 200 || launcherRes.getData() == null) {
            throw new IllegalStateException("获取下载配置失败");
        }
        var updateData = launcherRes.getData().getUpdateData();
        List<String> bases = downloadService.cdnBaseUrls(updateData);
        var fileInfos = loadFileInfos(updateData);
        if (fileInfos == null || fileInfos.isEmpty()) {
            throw new IllegalStateException("无文件可下载");
        }
        var manager = downloadService.buildDownloadManager(saveDir.toPath(), bases, fileInfos);
        manager.setProgressListener((done, total) -> Platform.runLater(() -> {
            updateProgressByData(done, total);
        }));
        manager.setStateListener((state, error) -> {
            if (state == DownloadState.FAILED) {
                LOG.warn("全量下载失败: {}", error);
            }
        });
        manager.run();
    }

    /** 启动增量更新。 */
    public void update() {
        if (operating.get() || checkResult == null || checkResult.updateInfo == null) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.asset.check_first")));
            return;
        }
        setOperating("update", LanguageManager.getString("ui.game_manager.asset.updating"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                CountDownLatch latch = new CountDownLatch(1);
                UpdateResult[] resultHolder = new UpdateResult[1];
                updateService.runUpdate(checkResult,
                        (state, doneSize, totalSize, doneCount, totalCount) -> {
                            updateProgressByData(doneSize, totalSize);
                            updateMessage("更新 " + percent(doneSize, totalSize));
                        },
                        r -> {
                            resultHolder[0] = r;
                            latch.countDown();
                        });
                latch.await();
                UpdateResult r = resultHolder[0];
                if (r != null && !r.success) {
                    throw new IllegalStateException(r.errorMessage != null ? r.errorMessage : "更新失败");
                }
                if (r == null) {
                    throw new IllegalStateException("更新未返回结果");
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            endOperating(LanguageManager.getString("ui.game_manager.asset.done"));
            String v = updateService.getInstalledVersion();
            if (v != null && !v.isEmpty()) {
                currentVersion.set(v);
            }
            showUpdate.set(false);
        });
        task.setOnCancelled(e -> endOperating(LanguageManager.getString("ui.game_manager.asset.stopped")));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (task.isCancelled()) {
                endOperating(LanguageManager.getString("ui.game_manager.asset.stopped"));
                return;
            }
            endOperating(LanguageManager.getString("ui.game_manager.asset.update_fail")
                    + (ex != null && ex.getMessage() != null ? ": " + ex.getMessage() : ""));
        });
        activeTask = task;
        taskManageService.execute(task);
    }

    /** 校验并修复（RepairFlow）。 */
    public void repair() {
        if (operating.get()) {
            return;
        }
        if (checkResult == null || checkResult.updateInfo == null) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.asset.check_first")));
            return;
        }
        setOperating("repair", LanguageManager.getString("ui.game_manager.asset.repairing"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // RepairFlow 内部 CheckFileTask 等可能异步回完成回调，
                // 必须用 CountDownLatch 阻塞等待真正完成，否则 call() 会提前返回误报“完成”。
                CountDownLatch latch = new CountDownLatch(1);
                UpdateResult[] resultHolder = new UpdateResult[1];
                updateService.repair(
                        (state, progressInfo) -> {
                            updateProgressByData(progressInfo.completedSize, progressInfo.totalSize);
                            updateMessage(repairStateText(state, progressInfo));
                        },
                        r -> {
                            resultHolder[0] = r;
                            latch.countDown();
                        });
                latch.await();
                UpdateResult r = resultHolder[0];
                if (r != null && !r.success) {
                    throw new IllegalStateException(r.errorMessage != null ? r.errorMessage : "校验修复失败");
                }
                if (r == null) {
                    throw new IllegalStateException("校验修复未返回结果");
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> endOperating(LanguageManager.getString("ui.game_manager.asset.repair_done")));
        task.setOnCancelled(e -> endOperating(LanguageManager.getString("ui.game_manager.asset.stopped")));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (task.isCancelled()) {
                endOperating(LanguageManager.getString("ui.game_manager.asset.stopped"));
                return;
            }
            endOperating(LanguageManager.getString("ui.game_manager.asset.repair_fail")
                    + (ex != null && ex.getMessage() != null ? ": " + ex.getMessage() : ""));
        });
        activeTask = task;
        taskManageService.execute(task);
    }

    /** 将 RepairFlow 的阶段码(state)映射为中文阶段名。 */
    private static String repairStateText(int state, com.kr.launcher.model.UpdateProgressInfo info) {
        String pct = info != null && info.totalSize > 0
                ? String.format("%.1f%%", info.completedSize * 100.0 / info.totalSize)
                : "0%";
        String stage = switch (state) {
            case 0 -> "校验 MD5";
            case 1 -> "重新下载";
            case 5 -> "移动文件";
            case 9 -> "复校 MD5";
            default -> "校验";
        };
        return stage + " " + pct;
    }

    /** 预下载。 */
    public void preDownload() {
        if (operating.get()) {
            return;
        }
        if (checkResult == null) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.asset.check_first")));
            return;
        }
        setOperating("predownload", LanguageManager.getString("ui.game_manager.asset.predownloading"));
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                CountDownLatch latch = new CountDownLatch(1);
                UpdateResult[] resultHolder = new UpdateResult[1];
                updateService.preDownload(
                        (state, progressInfo) -> {
                            updateProgressByData(progressInfo.completedSize, progressInfo.totalSize);
                            updateMessage("预下载 " + percent(progressInfo.completedSize, progressInfo.totalSize));
                        },
                        r -> {
                            resultHolder[0] = r;
                            latch.countDown();
                        });
                latch.await();
                UpdateResult r = resultHolder[0];
                if (r != null && !r.success) {
                    throw new IllegalStateException(r.errorMessage != null ? r.errorMessage : "预下载失败");
                }
                if (r == null) {
                    throw new IllegalStateException("预下载未返回结果");
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            endOperating(LanguageManager.getString("ui.game_manager.asset.predownload_done"));
            showPreDownload.set(false);
        });
        task.setOnCancelled(e -> endOperating(LanguageManager.getString("ui.game_manager.asset.stopped")));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (task.isCancelled()) {
                endOperating(LanguageManager.getString("ui.game_manager.asset.stopped"));
                return;
            }
            endOperating(LanguageManager.getString("ui.game_manager.asset.fail")
                    + (ex != null && ex.getMessage() != null ? ": " + ex.getMessage() : ""));
        });
        activeTask = task;
        taskManageService.execute(task);
    }

    // ==================== 暂停/继续/停止 ====================

    public void pause() {
        switch (activeOp) {
            case "update" -> updateService.pause();
            case "predownload" -> updateService.pausePreDownload();
            case "repair" -> updateService.pauseRepair();
            default -> { /* 全量下载的 DownloadManager 由 run() 阻塞，不做暂停/继续 */ }
        }
    }

    public void resume() {
        switch (activeOp) {
            case "update" -> updateService.resume();
            case "predownload" -> updateService.resumePreDownload();
            case "repair" -> updateService.resumeRepair();
            default -> { }
        }
    }

    public void stop() {
        // 先转发到底层流程（更新/预下载/修复），尽量中止下载
        switch (activeOp) {
            case "update" -> updateService.stop();
            case "predownload" -> updateService.stopPreDownload();
            case "repair" -> updateService.stopRepair();
            default -> { /* 全量下载直接取消 Task */ }
        }
        // 取消 JavaFX Task，中断 latch.await()，使后台线程退出
        if (activeTask != null) {
            activeTask.cancel(true);
        }
        endOperating(LanguageManager.getString("ui.game_manager.asset.stopped"));
    }

    // ==================== 工具 ====================

    private List<com.kuro.game.model.game.FileInfo> loadFileInfos(
            com.kuro.game.model.launcher.item.UpdateData updateData) {
        var res = downloadService.getGameResourceList(updateData.getResourceJsonUrl());
        if (res.getCode() == 200 && res.getData() != null) {
            return res.getData().getResource();
        }
        return List.of();
    }

    private void setOperating(String op, String statusText) {
        this.activeOp = op;
        operating.set(true);
        operationName.set(statusText);
        status.set(statusText);
        progress.set(0);
        progressText.set("0%");
    }

    private void endOperating(String doneText) {
        operating.set(false);
        activeOp = "";
        status.set(doneText);
    }

    /** 更新进度（线程安全：底层流程可能在非 FX 线程回调进度，包 runLater 收敛）。 */
    private void updateProgressByData(long done, long total) {
        if (total <= 0) {
            return;
        }
        double p = Math.min(1.0, Math.max(0.0, (double) done / total));
        String text = String.format("%.1f%%", p * 100);
        if (Platform.isFxApplicationThread()) {
            progress.set(p);
            progressText.set(text);
        } else {
            Platform.runLater(() -> {
                progress.set(p);
                progressText.set(text);
            });
        }
    }

    private static String percent(long done, long total) {
        return total > 0 ? String.format("%.1f%%", done * 100.0 / total) : "0%";
    }

    public void setDownloadDir(String dir) {
        downloadDir.set(dir);
    }

    // ==================== 访问器 ====================

    public BooleanProperty installedProperty() {
        return installed;
    }

    public BooleanProperty operatingProperty() {
        return operating;
    }

    public String getOperationName() {
        return operationName.get();
    }

    public StringProperty operationNameProperty() {
        return operationName;
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
    }
}