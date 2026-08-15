package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.download.DownloadManager;
import cn.tealc.download.GameResourceDownloadService;
import cn.tealc.download.model.DownloadState;
import cn.tealc.download.model.game.FileInfo;
import cn.tealc.download.model.launcher.UpdateData;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 游戏下载管理 ViewModel：设置下载目录 → 点击开始下载（进度条展示总进度）。
 */
public class GameDownloadViewModel extends BaseViewModel implements SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(GameDownloadViewModel.class);

    @Inject
    private GameResourceDownloadService downloadService;
    @Inject
    private TaskManageService taskManageService;

    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty("0%");
    private final StringProperty status = new SimpleStringProperty("等待开始下载");
    private final StringProperty downloadDir = new SimpleStringProperty();

    private volatile DownloadManager manager;
    private Task<DownloadState> downloadTask;
    private SourceType sourceType;

    public void init() {
        sourceType = Config.setting().gameRootDirSourceProperty().get();
        if (sourceType == null) {
            sourceType = SourceType.DEFAULT;
        }
        // 下载目录：优先使用自定义配置，否则默认到游戏根目录 WwtBackup/<区服>
        String saved = Config.setting().getGameDownloadDir();
        if (saved != null && !saved.isBlank()) {
            downloadDir.set(saved);
        } else {
            downloadDir.set(defaultDownloadDir());
        }
    }

    /** 自定义下载目录，并持久化到 settings.json。 */
    public void applyDownloadDir() {
        Config.setting().setGameDownloadDir(downloadDir.get() != null ? downloadDir.get().trim() : "");
        Config.setting().save();
    }

    private String defaultDownloadDir() {
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir != null) {
            return new File(gameDir, "WwtBackup/" + sourceType.name().toLowerCase()).getAbsolutePath();
        }
        return "";
    }

    /** 使用当前清单构建并启动下载。 */
    public void startDownload() {
        if (downloadTask != null && !downloadTask.isDone()) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.download.running")));
            return;
        }
        String dir = downloadDir.get();
        if (dir == null || dir.isBlank()) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.download.no_dir")));
            return;
        }
        File saveDir = new File(dir);
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.download.no_dir")));
            return;
        }
        SourceType selectedSource = sourceType;
        applyDownloadDir();
        Task<DownloadState> task = new Task<>() {
            @Override
            protected DownloadState call() throws Exception {
                var updateResponse = downloadService.getLatestUpdate(selectedSource.toGameDownloadSource());
                if (updateResponse == null || updateResponse.getCode() != 200
                        || updateResponse.getData() == null) {
                    throw new IllegalStateException("获取下载配置失败");
                }
                UpdateData updateData = updateResponse.getData();
                var resourceResponse = downloadService.getResourceList(updateData);
                if (resourceResponse == null || resourceResponse.getCode() != 200
                        || resourceResponse.getData() == null) {
                    throw new IllegalStateException("获取游戏资源清单失败");
                }
                List<FileInfo> fileInfos = resourceResponse.getData();
                if (fileInfos.isEmpty()) {
                    throw new IllegalStateException("无文件可下载");
                }

                DownloadManager newManager = downloadService.createDownloadManager(
                        saveDir.toPath(), updateData, fileInfos);
                manager = newManager;
                AtomicReference<DownloadState> terminalState = new AtomicReference<>();
                AtomicReference<String> failure = new AtomicReference<>();
                newManager.setStateListener((state, error) -> {
                    if (state == DownloadState.COMPLETE || state == DownloadState.FAILED
                            || state == DownloadState.CANCELED) {
                        terminalState.set(state);
                        failure.set(error);
                    }
                    Platform.runLater(() -> updateStatus(state, error));
                });
                newManager.setProgressListener((done, total) -> Platform.runLater(() -> {
                    if (total > 0) {
                        double value = done * 1.0 / total;
                        progress.set(value);
                        progressText.set(String.format("%.1f%%", value * 100));
                    }
                }));
                newManager.run();
                if (terminalState.get() == DownloadState.FAILED) {
                    throw new IllegalStateException(failure.get() != null ? failure.get() : "下载失败");
                }
                if (terminalState.get() != DownloadState.COMPLETE
                        && terminalState.get() != DownloadState.CANCELED) {
                    throw new IllegalStateException("下载未完成");
                }
                return terminalState.get();
            }
        };
        downloadTask = task;
        task.setOnSucceeded(event -> {
            if (downloadTask != task) {
                return;
            }
            DownloadState terminalState = task.getValue();
            if (terminalState == DownloadState.CANCELED) {
                status.set("已取消");
            } else {
                progress.set(1);
                progressText.set("100%");
                status.set("下载完成");
            }
            manager = null;
            downloadTask = null;
        });
        task.setOnFailed(event -> {
            if (downloadTask != task) {
                return;
            }
            LOG.error("下载失败", task.getException());
            status.set("下载失败");
            manager = null;
            downloadTask = null;
            NotificationManager.message(MessageInfo.error(LanguageManager.getString("ui.game_manager.download.start_failed")));
        });
        task.setOnCancelled(event -> {
            if (downloadTask == task) {
                manager = null;
                downloadTask = null;
                status.set("已取消");
            }
        });
        taskManageService.execute(task);
        status.set("下载中");
    }

    private void updateStatus(DownloadState state, String error) {
        switch (state) {
            case DOWNLOADING -> status.set("下载中");
            case PAUSED -> status.set("已暂停");
            case COMPLETE -> status.set("下载完成");
            case FAILED -> status.set("下载失败" + (error != null ? ": " + error : ""));
            case CANCELED -> status.set("已取消");
            default -> {
            }
        }
    }

    public void pauseDownload() {
        if (manager != null) {
            manager.pause();
        }
    }

    public void resumeDownload() {
        if (manager != null) {
            manager.resume();
        }
    }

    public void stopDownload() {
        if (manager != null) {
            manager.stop();
        }
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public String getProgressText() {
        return progressText.get();
    }

    public StringProperty progressTextProperty() {
        return progressText;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public String getDownloadDir() {
        return downloadDir.get();
    }

    public StringProperty downloadDirProperty() {
        return downloadDir;
    }

    public void setDownloadDir(String dir) {
        downloadDir.set(dir);
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceTypeName) {
        try {
            sourceType = SourceType.valueOf(sourceTypeName.toUpperCase());
            applyDownloadDir();
        } catch (Exception e) {
            LOG.warn("未知区服: {}", sourceTypeName);
        }
    }

    @Override
    public void onViewAdded() {
    }

    @Override
    public void onViewRemoved() {
        if (manager != null) {
            manager.stop();
        }
        applyDownloadDir();
    }
}
