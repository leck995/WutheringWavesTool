package cn.tealc.wutheringwavestool.service;

import cn.tealc.download.DownloadManager;
import cn.tealc.download.DownloadManagerBuilder;
import cn.tealc.download.model.DownloadInfo;
import cn.tealc.download.model.DownloadState;
import cn.tealc.wutheringwavestool.model.SourceType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.game.GameManager;
import com.kuro.game.model.Type;
import com.kuro.game.model.game.FileInfo;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.UpdateData;
import com.kuro.model.ResponseBody;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * 游戏下载 Service：拉取启动器 + 游戏资源清单，构建 {@link DownloadManager} 并包装为
 * JavaFX {@link Task} 供 UI / {@link TaskManageService} 使用。
 */
@Singleton
public class GameDownloadService {
    private static final Logger LOG = LoggerFactory.getLogger(GameDownloadService.class);

    private final GameManager gameManager;

    @Inject
    public GameDownloadService(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /** 获取启动器资源（含 CDN 列表 / 资源基路径 / 清单 URL）。 */
    public ResponseBody<LauncherResource> getLauncherResource(SourceType sourceType) {
        return gameManager.getLauncherResource(toKuroType(sourceType));
    }

    /** 获取游戏资源清单。 */
    public ResponseBody<GameResourceList> getGameResourceList(String indexUrl) {
        return gameManager.getGameResourceList(indexUrl);
    }

    /**
     * 构建下载管理器。
     *
     * @param destRoot     保存根目录
     * @param cdnBaseUrls  CDN 基址列表（首个为主）
     * @param fileInfos    服务器清单项
     */
    public DownloadManager buildDownloadManager(Path destRoot, List<String> cdnBaseUrls, List<FileInfo> fileInfos) {
        List<DownloadInfo> infos = fileInfos.stream()
                .map(GameDownloadService::toDownloadInfo)
                .toList();
        return new DownloadManagerBuilder(infos, destRoot)
                .cdnBaseUrls(cdnBaseUrls)
                .maxParallel(4)
                .maxRetry(5)
                .build();
    }

    /**
     * 由资源更新信息构建 CDN 下载基址列表（各 CDN url + resourcesBasePath，已按 ping 排序）。
     */
    public List<String> cdnBaseUrls(UpdateData updateData) {
        String basePath = updateData.getResourcesBasePath();
        return updateData.getCdnList().stream()
                .map(cdn -> cdn.getUrl() + basePath + "/")
                .toList();
    }

    /**
     * 将下载引擎包装为 JavaFX Task（引擎回调在 worker 线程触发，Task 的 updateMessage/updateProgress
     * 线程安全）。成功 / 失败 / 取消分别进入对应终止状态。
     */
    public Task<Void> wrapDownloadManager(DownloadManager manager, Runnable onProgress,
            java.util.function.BiConsumer<cn.tealc.download.model.DownloadState, String> onState,
            java.util.function.BiConsumer<Long, Long> onDownloadProgress) {
        return new Task<>() {
            @Override
            protected Void call() {
                manager.setProgressListener((done, total) -> {
                    if (total > 0) {
                        updateProgress(done, total);
                    }
                    if (onDownloadProgress != null) {
                        onDownloadProgress.accept(done, total);
                    }
                    if (onProgress != null) {
                        onProgress.run();
                    }
                });
                manager.setStateListener((state, error) -> {
                    if (onState != null) {
                        onState.accept(state, error);
                    }
                    switch (state) {
                        case DOWNLOADING -> updateMessage("下载中");
                        case PAUSED -> updateMessage("已暂停");
                        case COMPLETE -> updateMessage("下载完成");
                        case FAILED -> updateMessage("下载失败: " + error);
                        case CANCELED -> updateMessage("已取消");
                        default -> { /* 忽略 WAITING 等中间态 */ }
                    }
                });
                try {
                    manager.run();
                } catch (Exception e) {
                    LOG.error("下载失败", e);
                    updateMessage("下载失败: " + e.getMessage());
                    throw new IllegalStateException(e);
                }
                return null;
            }
        };
    }

    private static DownloadInfo toDownloadInfo(FileInfo fi) {
        String dest = fi.getDest();
        List<cn.tealc.download.model.ChunkInfo> chunkInfos = fi.getChunkInfos() == null ? List.of()
                : fi.getChunkInfos().stream()
                        .map(c -> new cn.tealc.download.model.ChunkInfo(c.getStart(), c.getEnd(), c.getMd5()))
                        .toList();
        // url 使用相对 dest 路径，交由 DownloadManager 拼接 CDN 基址
        return new DownloadInfo(dest, dest, fi.getSize() != null ? fi.getSize() : 0,
                fi.getMd5(), null, chunkInfos);
    }

    private Type toKuroType(SourceType sourceType) {
        return switch (sourceType) {
            case BILIBILI -> Type.BILIBILI;
            case GLOBAL -> Type.GLOBAL;
            default -> Type.CN;
        };
    }
}