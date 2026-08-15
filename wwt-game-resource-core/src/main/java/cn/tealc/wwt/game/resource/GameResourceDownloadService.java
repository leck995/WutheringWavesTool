package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.ChunkInfo;
import cn.tealc.wwt.game.resource.model.DownloadInfo;
import cn.tealc.wwt.game.resource.internal.GameResourceApiClient;
import cn.tealc.wwt.game.resource.model.game.FileInfo;
import cn.tealc.wwt.game.resource.model.launcher.UpdateData;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;

/**
 * 游戏资源下载门面。
 *
 * <p>统一负责官方更新配置、资源清单和 CDN 文件地址的获取与组装；调用方仅提供
 * 下载目标目录、发行渠道及待下载的资源项。</p>
 */
public final class GameResourceDownloadService {
    private final GameResourceApiClient apiClient;

    public GameResourceDownloadService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.apiClient = new GameResourceApiClient(httpClient, objectMapper);
    }

    /** 获取指定发行渠道的最新资源更新配置。 */
    public GameDownloadResponse<UpdateData> getLatestUpdate(GameDownloadSource source) {
        return apiClient.getLatestUpdate(source);
    }

    /** 根据更新配置获取资源清单，资源清单 URL 由该模块内部计算。 */
    public GameDownloadResponse<List<FileInfo>> getResourceList(UpdateData updateData) {
        if (!hasCdnAddress(updateData)) {
            return GameDownloadResponse.failure("下载配置缺少 CDN 地址");
        }
        GameDownloadResponse<cn.tealc.wwt.game.resource.model.game.GameResourceList> response =
                apiClient.getResourceList(updateData);
        if (!response.isSuccessful() || response.getData().getResource() == null) {
            return GameDownloadResponse.failure(response.isSuccessful()
                    ? "获取游戏资源清单失败" : response.getMessage());
        }
        return GameDownloadResponse.success(response.getData().getResource());
    }

    /**
     * 使用更新配置构建下载器。CDN 基址和每个文件的最终请求地址由本模块生成。
     */
    public DownloadManager createDownloadManager(Path destRoot, UpdateData updateData,
            List<FileInfo> fileInfos) {
        if (!hasCdnAddress(updateData)) {
            throw new IllegalArgumentException("下载配置缺少 CDN 地址");
        }
        if (updateData.getResourcesBasePath() == null || updateData.getResourcesBasePath().isBlank()) {
            throw new IllegalArgumentException("下载配置缺少资源基路径");
        }
        List<String> cdnBaseUrls = updateData.getCdnList().stream()
                .map(cdn -> cdn != null ? cdn.getUrl() : null)
                .filter(GameResourceDownloadService::hasText)
                .map(cdnUrl -> appendPath(cdnUrl, updateData.getResourcesBasePath()))
                .toList();
        if (cdnBaseUrls.isEmpty()) {
            throw new IllegalArgumentException("下载配置缺少有效 CDN 地址");
        }
        List<DownloadInfo> infos = fileInfos.stream()
                .map(GameResourceDownloadService::toDownloadInfo)
                .toList();
        return new DownloadManagerBuilder(infos, destRoot)
                .cdnBaseUrls(cdnBaseUrls)
                .maxParallel(4)
                .maxRetry(5)
                .build();
    }

    private static String appendPath(String host, String path) {
        String normalizedHost = host.strip();
        while (normalizedHost.endsWith("/")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        String normalizedPath = path.strip();
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        while (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        if (!hasText(normalizedHost) || !hasText(normalizedPath)) {
            throw new IllegalArgumentException("下载配置包含无效资源路径");
        }
        return normalizedHost + "/" + normalizedPath + "/";
    }

    private static boolean hasCdnAddress(UpdateData updateData) {
        return updateData != null && updateData.getCdnList() != null
                && updateData.getCdnList().stream()
                        .anyMatch(cdn -> cdn != null && hasText(cdn.getUrl()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static DownloadInfo toDownloadInfo(FileInfo fileInfo) {
        String dest = fileInfo.getDest();
        List<ChunkInfo> chunks = fileInfo.getChunkInfos() == null ? List.of()
                : fileInfo.getChunkInfos().stream()
                        .map(chunk -> new ChunkInfo(chunk.getStart(), chunk.getEnd(), chunk.getMd5()))
                        .toList();
        return new DownloadInfo(dest, dest, fileInfo.getSize() != null ? fileInfo.getSize() : 0,
                fileInfo.getMd5(), null, chunks);
    }
}
