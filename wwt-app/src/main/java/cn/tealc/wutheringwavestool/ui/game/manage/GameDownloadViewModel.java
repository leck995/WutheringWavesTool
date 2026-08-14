package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.download.DownloadManager;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameDownloadService;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.google.inject.Inject;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.UpdateData;
import com.kuro.model.ResponseBody;
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

/**
 * 游戏下载管理 ViewModel：设置下载目录 → 点击开始下载（进度条展示总进度）。
 */
public class GameDownloadViewModel extends BaseViewModel implements SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(GameDownloadViewModel.class);

    @Inject
    private GameDownloadService downloadService;
    @Inject
    private TaskManageService taskManageService;

    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty progressText = new SimpleStringProperty("0%");
    private final StringProperty status = new SimpleStringProperty("等待开始下载");
    private final StringProperty downloadDir = new SimpleStringProperty();

    private DownloadManager manager;
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
        if (manager != null && manager.isRunning()) {
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
        try {
            ResponseBody<LauncherResource> res = downloadService.getLauncherResource(sourceType);
            if (res.getCode() != 200 || res.getData() == null) {
                NotificationManager.message(MessageInfo.error(LanguageManager.getString("ui.game_manager.download.fetch_failed")));
                return;
            }
            UpdateData updateData = res.getData().getUpdateData();
            List<String> bases = downloadService.cdnBaseUrls(updateData);
            List<com.kuro.game.model.game.FileInfo> fileInfos = loadFileInfos(updateData);
            if (fileInfos.isEmpty()) {
                NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.download.empty")));
                return;
            }
            applyDownloadDir();
            manager = downloadService.buildDownloadManager(saveDir.toPath(), bases, fileInfos);
            Task<?> task = downloadService.wrapDownloadManager(manager,
                    null,
                    (state, error) -> Platform.runLater(() -> {
                        switch (state) {
                            case DOWNLOADING -> status.set("下载中");
                            case PAUSED -> status.set("已暂停");
                            case COMPLETE -> status.set("下载完成");
                            case FAILED -> status.set("下载失败: " + error);
                            case CANCELED -> status.set("已取消");
                            default -> { }
                        }
                    }),
                    (done, total) -> Platform.runLater(() -> {
                        if (total > 0) {
                            double p = done * 1.0 / total;
                            progress.set(p);
                            progressText.set(String.format("%.1f%%", p * 100));
                        }
                    }));
            taskManageService.execute(task);
            status.set("下载中");
        } catch (Exception e) {
            LOG.error("启动下载失败", e);
            NotificationManager.message(MessageInfo.error(LanguageManager.getString("ui.game_manager.download.start_failed")));
        }
    }

    private List<com.kuro.game.model.game.FileInfo> loadFileInfos(UpdateData updateData) {
        String indexUrl = updateData.getResourceJsonUrl();
        ResponseBody<com.kuro.game.model.game.GameResourceList> resourceRes = downloadService.getGameResourceList(indexUrl);
        if (resourceRes.getCode() == 200 && resourceRes.getData() != null) {
            return resourceRes.getData().getResource();
        }
        return List.of();
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