package cn.tealc.wutheringwavestool.service;

import cn.tealc.wwt.game.resource.GameResourceUpdateService;
import cn.tealc.wwt.game.resource.DownloadOptions;
import cn.tealc.wwt.game.resource.GameDownloadSource;
import cn.tealc.wwt.game.resource.ResourceCompletionListener;
import cn.tealc.wwt.game.resource.ResourceContext;
import cn.tealc.wwt.game.resource.ResourceProgressListener;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceCheckState;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/** App adapter for the JavaFX-free game resource core. */
@Singleton
public class GameUpdateService {
    private static final Logger LOG = LoggerFactory.getLogger(GameUpdateService.class);

    private final GameResourceUpdateService resourceCore = new GameResourceUpdateService();

    public void ensureInit() {
        resourceCore.initialize(resourceContext());
    }

    public ResourceCheckResult checkUpdate() {
        ResourceCheckResult result = resourceCore.check(resourceContext());
        if (result.isSuccessful() && result.state() == ResourceCheckState.UP_TO_DATE) {
            persistVersion(result.latestVersion());
        }
        return result;
    }

    public void runUpdate(ResourceCheckResult checkResult, ResourceProgressListener progress,
            ResourceCompletionListener completion) {
        resourceCore.update(resourceContext(), checkResult, progress, result -> {
            if (result.successful()) {
                persistVersion(checkResult.latestVersion());
            }
            completion.onComplete(result);
        }, downloadOptions());
    }

    public boolean isPreDownloadAvailable(ResourceCheckResult checkResult) {
        return checkResult != null && checkResult.isPreDownloadAvailable();
    }

    public long preDownloadSize(ResourceCheckResult checkResult) {
        return checkResult != null ? checkResult.preDownloadSize() : 0L;
    }

    /**
     * 独立执行 PrepareTask，返回本次下载所需磁盘空间（bytes）。
     * 调用方需在后台线程中调用此方法（会下载索引文件，阻塞）。
     *
     * @param checkResult  check 阶段的 ResourceCheckResult
     * @param isPreDownload 预下载用 predownloadUpdateInfo，其余用 updateInfo
     * @return 所需空间字节数；-1 表示 prepare 失败
     */
    public long prepareSize(ResourceCheckResult checkResult, boolean isPreDownload) {
        return resourceCore.prepareSize(resourceContext(), checkResult, isPreDownload);
    }

    /**
     * 取当前安装对应的下载缓存目录可用空间（bytes）。
     * 缓存目录 = customDownloadCacheDir 或 gameDir/launcherDownload。
     */
    public long getCacheAvailableSpace() {
        return resourceContext().cacheDirectory().toFile().getUsableSpace();
    }

    /** 字节格式化：B / KB / MB / GB / TB。供 UI 层展示磁盘空间数据复用。 */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        do {
            value /= 1024;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    /**
     * 将核心错误码映射为用户友好的中文提示；未知码回退 fallback。
     * 优先按语义化 errorType 归类（磁盘满/网络/文件占用/权限/缺失/索引/超时），
     * 再按具体错误码兜底。返回 null 表示无需定制（fallback 原样展示）。
     */
    public static String friendlyError(int errorCode, String fallback) {
        String msg = friendlyError(errorCode);
        return msg != null ? msg : (fallback != null ? fallback : "");
    }

    /** 映射单个错误码；无法识别时返回 null。 */
    public static String friendlyError(int errorCode) {
        return switch (errorCode) {
            // 磁盘空间不足（下载层 DISK_SPACE_CHECK_FAIL / 应用层 HRESULT）
            case GameResourceUpdateService.ERROR_DISK_SPACE_CHECK,
                    GameResourceUpdateService.ERROR_DISK_NOT_ENOUGH_SPACE ->
                    LanguageManager.getString("ui.game_manager.asset.error_disk_space");
            // 网络相关
            case GameResourceUpdateService.ERROR_NETWORK,
                    GameResourceUpdateService.ERROR_STREAM_READ_TIMEOUT ->
                    LanguageManager.getString("ui.game_manager.asset.error_network");
            // 文件占用 / 权限 / 缺失（应用合成阶段）
            case GameResourceUpdateService.ERROR_FILE_OCCUPANCY ->
                    LanguageManager.getString("ui.game_manager.asset.error_file_occupied");
            case GameResourceUpdateService.ERROR_FILE_PERMISSION_DENY ->
                    LanguageManager.getString("ui.game_manager.asset.error_permission");
            case GameResourceUpdateService.ERROR_FILE_MISSING ->
                    LanguageManager.getString("ui.game_manager.asset.error_file_missing");
            // 应用/合成阶段其它
            case GameResourceUpdateService.ERROR_APPLY_MD5_NOT_MATCH ->
                    LanguageManager.getString("ui.game_manager.asset.error_md5");
            case GameResourceUpdateService.ERROR_PATCH_ALREADY_RUNNING ->
                    LanguageManager.getString("ui.game_manager.asset.error_patch_running");
            case GameResourceUpdateService.ERROR_PATCH_START_FAILED ->
                    LanguageManager.getString("ui.game_manager.asset.error_patch_start");
            case GameResourceUpdateService.ERROR_PATCH_UNKNOWN ->
                    LanguageManager.getString("ui.game_manager.asset.error_patch_unknown");
            // 下载层其它
            case GameResourceUpdateService.ERROR_DELETE_FILE ->
                    LanguageManager.getString("ui.game_manager.asset.error_delete_file");
            case GameResourceUpdateService.ERROR_NOT_SUPPORT_RANGE ->
                    LanguageManager.getString("ui.game_manager.asset.error_no_range");
            case GameResourceUpdateService.ERROR_GET_CONTENT_LENGTH ->
                    LanguageManager.getString("ui.game_manager.asset.error_content_length");
            case GameResourceUpdateService.ERROR_BUILD_CHUNK_TASKS,
                    GameResourceUpdateService.ERROR_PARSE_SINGLE_TASK ->
                    LanguageManager.getString("ui.game_manager.asset.error_chunk");
            case GameResourceUpdateService.ERROR_MERGE_CHUNK ->
                    LanguageManager.getString("ui.game_manager.asset.error_merge");
            case GameResourceUpdateService.ERROR_CHECK_MD5 ->
                    LanguageManager.getString("ui.game_manager.asset.error_md5");
            case GameResourceUpdateService.ERROR_CREATE_FILE_STREAM,
                    GameResourceUpdateService.ERROR_MAKE_DIR,
                    GameResourceUpdateService.ERROR_WRITE_FILE ->
                    LanguageManager.getString("ui.game_manager.asset.error_write");
            case GameResourceUpdateService.ERROR_CONTENT_ENCODING ->
                    LanguageManager.getString("ui.game_manager.asset.error_encoding");
            case GameResourceUpdateService.ERROR_DOWNLOAD_UNKNOWN ->
                    LanguageManager.getString("ui.game_manager.asset.error_download_unknown");
            default -> null;
        };
    }

    public void preDownload(ResourceCheckResult checkResult, ResourceProgressListener progress,
            ResourceCompletionListener completion) {
        resourceCore.preDownload(resourceContext(), checkResult, progress, completion, downloadOptions());
    }

    public void repair(ResourceCheckResult checkResult, ResourceProgressListener progress,
            ResourceCompletionListener completion) {
        resourceCore.repair(resourceContext(), checkResult, progress, completion, downloadOptions());
    }

    public void pause() { resourceCore.pauseUpdate(); }
    public void resume() { resourceCore.resumeUpdate(); }
    public void stop() { resourceCore.stopUpdate(); }
    public void pausePreDownload() { resourceCore.pausePreDownload(); }
    public void resumePreDownload() { resourceCore.resumePreDownload(); }
    public void stopPreDownload() { resourceCore.stopPreDownload(); }
    public void pauseRepair() { resourceCore.pauseRepair(); }
    public void resumeRepair() { resourceCore.resumeRepair(); }
    public void stopRepair() { resourceCore.stopRepair(); }

    public String getInstalledVersion() {
        try {
            String installed = resourceCore.installedVersion(resourceContext());
            if (installed != null && !installed.isBlank()) {
                return installed;
            }
        } catch (RuntimeException e) {
            LOG.debug("Failed to read the resource-core installed version", e);
        }
        String cached = Config.setting().getGameInstalledVersion();
        return cached != null ? cached : "";
    }

    /** 读取指定游戏目录已登记的安装版本（launcherDownloadConfig.json 的 version）。 */
    public String getInstalledVersion(Path gameDirectory) {
        try {
            String installed = resourceCore.installedVersion(resourceContextFor(gameDirectory));
            return installed != null ? installed : "";
        } catch (RuntimeException e) {
            LOG.debug("读取已装机版本失败: {}", gameDirectory, e);
            return "";
        }
    }

    private static ResourceContext resourceContext() {
        File gameDirectory = GameResourcesManager.getGameDir();
        if (gameDirectory == null) {
            throw new IllegalStateException("Game directory is not configured");
        }
        return resourceContextFor(gameDirectory.toPath());
    }

    private static ResourceContext resourceContextFor(Path gameDirectory) {
        String customCache = Config.setting().getCustomDownloadCacheDir();
        Path cacheDir = (customCache != null && !customCache.isBlank())
                ? Path.of(customCache) : null;
        SourceType sourceType = Config.setting().getGameRootDirSource();
        GameDownloadSource source = (sourceType != null ? sourceType : SourceType.DEFAULT)
                .toGameDownloadSource();
        return new ResourceContext(gameDirectory, Path.of("."), cacheDir, source);
    }

    private static void persistVersion(String version) {
        if (version == null || version.isBlank()) {
            return;
        }
        try {
            Config.setting().setGameInstalledVersion(version);
            Config.setting().save();
        } catch (Exception e) {
            LOG.warn("Failed to persist installed version: {}", version, e);
        }
    }

    private static DownloadOptions downloadOptions() {
        return new DownloadOptions(Config.setting().getDownloadParallelCount(),
                Config.setting().getDownloadSpeedLimitBytesPerSecond());
    }
}
