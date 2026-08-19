package cn.tealc.wutheringwavestool.thread.game.download;

import cn.tealc.wwt.game.resource.DownloadManager;
import cn.tealc.wwt.game.resource.GameResourceDownloadService;
import cn.tealc.wwt.game.resource.GameResourceInstallService;
import cn.tealc.wwt.game.resource.model.DownloadState;
import cn.tealc.wwt.game.resource.model.game.FileInfo;
import cn.tealc.wwt.game.resource.model.launcher.UpdateData;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.TaskControl;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** 全量下载任务：拉取清单 → 阻塞下载 → 注册安装。进度经 Task 的 progress/title/message 上抛。 */
public class GameFullDownloadTask extends AbstractGameDownloadTask<Void> implements TaskControl {
    private static final Logger LOG = LoggerFactory.getLogger(GameFullDownloadTask.class);

    private final GameResourceDownloadService downloadService;
    private final GameResourceInstallService installService;
    private final File saveDir;
    private final SourceType source;
    private final int maxParallel;
    private final long speedLimitBytesPerSecond;

    private volatile DownloadManager manager;

    public GameFullDownloadTask(GameResourceDownloadService downloadService,
            GameResourceInstallService installService, File saveDir, SourceType source,
            int maxParallel, long speedLimitBytesPerSecond) {
        this.downloadService = downloadService;
        this.installService = installService;
        this.saveDir = saveDir;
        this.source = source;
        this.maxParallel = maxParallel;
        this.speedLimitBytesPerSecond = speedLimitBytesPerSecond;
    }

    @Override
    protected Void call() throws Exception {
        updateTitle("正在获取下载配置");
        var launcherRes = downloadService.getLatestUpdate(source.toGameDownloadSource());
        if (launcherRes == null || launcherRes.getCode() != 200 || launcherRes.getData() == null) {
            throw new IllegalStateException("获取下载配置失败");
        }
        UpdateData updateData = launcherRes.getData();

        updateTitle("正在读取资源清单");
        var response = downloadService.getResourceList(updateData);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new IllegalStateException("获取资源清单失败");
        }
        List<FileInfo> fileInfos = response.getData();
        if (fileInfos.isEmpty()) {
            throw new IllegalStateException("无文件可下载");
        }

        updateTitle("正在检查磁盘空间并准备下载");
        DownloadManager mgr = downloadService.createDownloadManager(
                saveDir.toPath(), updateData, fileInfos,
                new cn.tealc.wwt.game.resource.DownloadOptions(maxParallel, speedLimitBytesPerSecond));
        this.manager = mgr;
        AtomicReference<String> failure = new AtomicReference<>();
        mgr.setProgressListener((done, total) -> {
            if (isCancelled()) return;
            updateProgress(done, total);
        });
        mgr.setPhaseListener((phase, relativePath, completedFiles, totalFiles) ->
                updateMessage(fullDownloadDetail(phase, relativePath, completedFiles, totalFiles)));
        mgr.setStateListener((state, error) -> {
            if (state == DownloadState.FAILED) {
                String message = (error != null && !error.isBlank()) ? error : "下载失败";
                failure.compareAndSet(null, message);
                LOG.warn("全量下载失败: {}", message);
            }
        });

        try {
            mgr.run();
        } finally {
            // 引擎持有关系由 manager 自行维护
        }
        if (isCancelled()) {
            return null;
        }
        if (failure.get() != null) {
            throw new IllegalStateException(failure.get());
        }
        updateTitle("正在登记下载资源");
        installService.registerInstalledRelease(saveDir.toPath(), updateData.getVersion(), fileInfos);
        updateProgress(1, 1);
        return null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        DownloadManager mgr = manager;
        if (mgr != null) {
            mgr.stop();
        }
        return super.cancel(mayInterruptIfRunning);
    }

    public void pause() {
        DownloadManager mgr = manager;
        if (mgr != null) {
            mgr.pause();
        }
    }

    // ---------------- TaskControl ----------------

    @Override
    public boolean pauseTask() { pause(); return true; }

    @Override
    public boolean resumeTask() {
        DownloadManager mgr = manager;
        if (mgr != null) {
            mgr.resume();
        }
        return true;
    }

    @Override
    public boolean cancelTask() { cancel(true); return true; }

    @Override
    public boolean supportsPause() { return true; }

    public Path getSaveDir() {
        return saveDir.toPath();
    }

    public SourceType getSource() {
        return source;
    }

    private static String fullDownloadDetail(cn.tealc.wwt.game.resource.model.DownloadPhase phase,
            String relativePath, int completedFiles, int totalFiles) {
        String action = switch (phase) {
            case PREPARING -> "正在准备下载";
            case DOWNLOADING -> "正在下载";
            case VERIFYING -> "正在校验文件";
            case MERGING -> "正在合成文件";
        };
        String count = totalFiles > 0
                ? "（" + (int) Math.min(totalFiles, completedFiles + 1) + "/" + totalFiles + "）"
                : "";
        String path = displayPath(relativePath);
        return path.isEmpty() ? action + count : action + count + "：" + path;
    }

    private static String displayPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        return normalized.length() <= 96 ? normalized : "..." + normalized.substring(normalized.length() - 93);
    }
}