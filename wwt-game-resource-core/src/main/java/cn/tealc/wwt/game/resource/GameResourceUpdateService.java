package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.internal.legacy.config.ResourceConfigManager;
import cn.tealc.wwt.game.resource.internal.legacy.flow.ResUpdateModule;
import cn.tealc.wwt.game.resource.internal.legacy.model.CheckUpdateResult;
import cn.tealc.wwt.game.resource.internal.legacy.model.DownloadInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.LauncherConfig;
import cn.tealc.wwt.game.resource.internal.legacy.model.ResStateInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.UpdateInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.UpdateProgressInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.UpdateResult;
import cn.tealc.wwt.game.resource.internal.legacy.task.PrepareTask;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceCheckState;
import cn.tealc.wwt.game.resource.model.ResourceOperationResult;
import cn.tealc.wwt.game.resource.model.ResourceProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * JavaFX-free facade over the official resource update pipeline.
 *
 * <p>Callers own threading. Progress callbacks run on the worker executing the
 * operation and must be marshalled to a UI thread by UI applications.</p>
 */
public final class GameResourceUpdateService {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourceUpdateService.class);
    private static final String HPATCHZ_RESOURCE = "/kr/hpatchz.exe";

    // ==================== 公开错误码 ====================
    // 下载阶段错误（CDN / 分块下载），对应 internal/legacy/download/DownloadError。
    public static final int ERROR_NETWORK = 7001001;
    public static final int ERROR_DOWNLOAD_UNKNOWN = 7001002;
    public static final int ERROR_DELETE_FILE = 7001003;
    public static final int ERROR_NOT_SUPPORT_RANGE = 7001004;
    public static final int ERROR_GET_CONTENT_LENGTH = 7001005;
    public static final int ERROR_BUILD_CHUNK_TASKS = 7001006;
    public static final int ERROR_MERGE_CHUNK = 7001007;
    public static final int ERROR_PARSE_SINGLE_TASK = 7001008;
    public static final int ERROR_CHECK_MD5 = 7001009;
    public static final int ERROR_CREATE_FILE_STREAM = 7001011;
    public static final int ERROR_MAKE_DIR = 7001012;
    public static final int ERROR_WRITE_FILE = 7001013;
    public static final int ERROR_CONTENT_ENCODING = 7001014;
    public static final int ERROR_DISK_SPACE_CHECK = 7001015;
    public static final int ERROR_STREAM_READ_TIMEOUT = 700106;

    // 应用/合成阶段错误（HRESULT 文件错误 + Patch），对应 internal/legacy/model/UpdateResult 与 PatchExecutor。
    public static final int ERROR_FILE_OCCUPANCY = -2147024864;
    public static final int ERROR_DISK_NOT_ENOUGH_SPACE = -2147024784;
    public static final int ERROR_FILE_PERMISSION_DENY = -2147024891;
    public static final int ERROR_FILE_MISSING = -2147024894;
    public static final int ERROR_APPLY_MD5_NOT_MATCH = 7002017;
    public static final int ERROR_PATCH_UNKNOWN = 7002015;
    public static final int ERROR_PATCH_ALREADY_RUNNING = 7002016;
    public static final int ERROR_PATCH_START_FAILED = 7002019;

    private LegacySession session;

    public synchronized void initialize(ResourceContext context) {
        sessionFor(context);
    }

    public synchronized ResourceCheckResult check(ResourceContext context) {
        LegacySession active = sessionFor(context);
        final CheckUpdateResult[] holder = new CheckUpdateResult[1];
        active.updateModule.checkUpdate(result -> holder[0] = result);
        CheckUpdateResult legacy = holder[0];
        if (legacy == null) {
            return failure("The update check did not return a result.");
        }

        ResourceCheckResult result = toPublicResult(legacy);
        if (result.isSuccessful()) {
            active.checks.put(result.checkId(), legacy);
        }
        return result;
    }

    public synchronized String installedVersion(ResourceContext context) {
        var config = sessionFor(context).configManager.getDownloadConfig();
        return config != null && config.version != null ? config.version : "";
    }

    /**
     * 独立执行 PrepareTask，返回本次下载所需磁盘空间（bytes）。
     * 调用方需在后台线程中调用此方法（会下载索引文件，阻塞）。
     *
     * @param context      资源上下文（与 check/update 同源）
     * @param checked      check 阶段的 ResourceCheckResult
     * @param isPreDownload true=用 predownloadUpdateInfo（预下载），false=用 updateInfo（更新/修复/全量）
     * @return 所需空间字节数；-1 表示 prepare 失败或无可用清单
     */
    public long prepareSize(ResourceContext context, ResourceCheckResult checked, boolean isPreDownload) {
        LegacySession active;
        CheckUpdateResult legacy;
        synchronized (this) {
            active = sessionFor(context);
            legacy = active.checks.get(checked.checkId());
        }
        if (legacy == null) {
            return -1;
        }
        UpdateInfo updateInfo = isPreDownload ? legacy.predownloadUpdateInfo : legacy.updateInfo;
        if (updateInfo == null) {
            return -1;
        }
        PrepareTask prepareTask = new PrepareTask(active.configManager, updateInfo, isPreDownload);
        PrepareTask.PrepareResult result = prepareTask.run();
        if (!result.success || result.downloadInfoList == null) {
            return -1;
        }
        long total = 0;
        for (DownloadInfo info : result.downloadInfoList) {
            total += info.fileSize;
        }
        return total;
    }

    public void update(ResourceContext context, ResourceCheckResult checked,
            ResourceProgressListener progress, ResourceCompletionListener completion) {
        update(context, checked, progress, completion, DownloadOptions.DEFAULT);
    }

    public void update(ResourceContext context, ResourceCheckResult checked,
            ResourceProgressListener progress, ResourceCompletionListener completion,
            DownloadOptions downloadOptions) {
        LegacySession active;
        CheckUpdateResult legacy;
        synchronized (this) {
            active = sessionFor(context);
            legacy = active.checks.get(checked.checkId());
        }
        if (legacy == null || legacy.stateInfo == null || legacy.updateInfo == null) {
            completion.onComplete(ResourceOperationResult.failure(
                    "The update operation requires a current successful check result."));
            return;
        }
        active.updateModule.setDownloadOptions(downloadOptions);
        active.updateModule.update(legacy.stateInfo, legacy.updateInfo,
                (state, completedBytes, totalBytes, completedFiles, totalFiles) ->
                        progress.onProgress(new ResourceProgress(state, completedBytes, totalBytes,
                                completedFiles, totalFiles)),
                result -> completion.onComplete(toPublicResult(result)));
    }

    public void preDownload(ResourceContext context, ResourceCheckResult checked,
            ResourceProgressListener progress, ResourceCompletionListener completion) {
        preDownload(context, checked, progress, completion, DownloadOptions.DEFAULT);
    }

    public void preDownload(ResourceContext context, ResourceCheckResult checked,
            ResourceProgressListener progress, ResourceCompletionListener completion,
            DownloadOptions downloadOptions) {
        LegacySession active;
        boolean checkKnown;
        synchronized (this) {
            active = sessionFor(context);
            checkKnown = active.checks.containsKey(checked.checkId());
        }
        if (!checkKnown || !checked.isPreDownloadAvailable()) {
            completion.onComplete(ResourceOperationResult.failure(
                    "The pre-download operation requires an available pre-download check result."));
            return;
        }
        active.updateModule.setDownloadOptions(downloadOptions);
        active.updateModule.preDownload(
                (state, info) -> progress.onProgress(toPublicProgress(state, info)),
                result -> completion.onComplete(toPublicResult(result)));
    }

    public void repair(ResourceContext context, ResourceCheckResult checked,
            ResourceProgressListener progress, ResourceCompletionListener completion) {
        repair(context, checked, progress, completion, DownloadOptions.DEFAULT);
    }

    public void repair(ResourceContext context, ResourceCheckResult checked,
            ResourceProgressListener progress, ResourceCompletionListener completion,
            DownloadOptions downloadOptions) {
        LegacySession active;
        boolean checkKnown;
        synchronized (this) {
            active = sessionFor(context);
            checkKnown = active.checks.containsKey(checked.checkId());
        }
        if (!checkKnown || !checked.isRepairAvailable()) {
            completion.onComplete(ResourceOperationResult.failure(
                    "The repair operation requires a current successful check result."));
            return;
        }
        active.updateModule.setDownloadOptions(downloadOptions);
        active.updateModule.repair(
                (state, info) -> progress.onProgress(toPublicProgress(state, info)),
                result -> completion.onComplete(toPublicResult(result)));
    }

    public synchronized void pauseUpdate() {
        if (session != null) {
            session.updateModule.pause();
        }
    }

    public synchronized void resumeUpdate() {
        if (session != null) {
            session.updateModule.resume();
        }
    }

    public synchronized void stopUpdate() {
        if (session != null) {
            session.updateModule.stop();
        }
    }

    public synchronized void pausePreDownload() {
        if (session != null) {
            session.updateModule.pausePreDownload();
        }
    }

    public synchronized void resumePreDownload() {
        if (session != null) {
            session.updateModule.resumePreDownload();
        }
    }

    public synchronized void stopPreDownload() {
        if (session != null) {
            session.updateModule.stopPreDownload();
        }
    }

    public synchronized void pauseRepair() {
        if (session != null) {
            session.updateModule.pauseRepair();
        }
    }

    public synchronized void resumeRepair() {
        if (session != null) {
            session.updateModule.resumeRepair();
        }
    }

    public synchronized void stopRepair() {
        if (session != null) {
            session.updateModule.stopRepair();
        }
    }

    private LegacySession sessionFor(ResourceContext context) {
        if (session != null && session.context.equals(context)) {
            return session;
        }
        try {
            extractRuntimeFiles(context.workingDirectory());
            LauncherConfig config = LauncherConfigs.forSource(context.source());
            ResourceConfigManager configManager = new ResourceConfigManager(
                    config, context.gameDirectory().toString());
            configManager.setCustomCacheDir(context.cacheDirectory().toString());
            session = new LegacySession(context, configManager, new ResUpdateModule(configManager));
            return session;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize the game resource core", e);
        }
    }

    private static void extractRuntimeFiles(Path workingDirectory) throws IOException {
        Files.createDirectories(workingDirectory);
        extract(HPATCHZ_RESOURCE, workingDirectory.resolve("hpatchz.exe"));
    }

    private static void extract(String resource, Path target) throws IOException {
        if (Files.exists(target)) {
            return;
        }
        try (InputStream stream = GameResourceUpdateService.class.getResourceAsStream(resource)) {
            Objects.requireNonNull(stream, "Missing resource: " + resource);
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static ResourceCheckResult toPublicResult(CheckUpdateResult result) {
        ResStateInfo state = result.stateInfo;
        ResourceCheckState checkState = toPublicState(state);
        boolean successful = result.succ && state != null;
        boolean preDownloadAvailable = successful && state.enablePreDownload
                && !state.preDownloadComplete && result.predownloadUpdateInfo != null;
        return new ResourceCheckResult(UUID.randomUUID(), successful, result.errorCode,
                result.errorMessage, checkState,
                state != null ? state.usingVersion : "",
                state != null ? state.newVersion : "",
                result.updateInfo != null, result.updateInfo != null, preDownloadAvailable,
                state != null && state.preDownloadComplete,
                state != null ? state.preDownloadSize : 0L);
    }

    private static ResourceCheckResult failure(String message) {
        return new ResourceCheckResult(UUID.randomUUID(), false, 0, message,
                ResourceCheckState.UNKNOWN, "", "", false, false, false, false, 0L);
    }

    private static ResourceCheckState toPublicState(ResStateInfo state) {
        if (state == null) {
            return ResourceCheckState.UNKNOWN;
        }
        return switch (state.state) {
            case ResStateInfo.STATE_UP_TO_DATE -> ResourceCheckState.UP_TO_DATE;
            case ResStateInfo.STATE_NEED_DOWNLOAD, ResStateInfo.STATE_DOWNLOADING ->
                    ResourceCheckState.UPDATE_AVAILABLE;
            case ResStateInfo.STATE_PRE_DOWNLOAD -> ResourceCheckState.PRE_DOWNLOAD_AVAILABLE;
            case ResStateInfo.STATE_REPAIRING -> ResourceCheckState.REPAIR_REQUIRED;
            case ResStateInfo.STATE_ROLLBACK -> ResourceCheckState.ROLLBACK_REQUIRED;
            default -> ResourceCheckState.UNKNOWN;
        };
    }

    private static ResourceProgress toPublicProgress(int state, UpdateProgressInfo info) {
        if (info == null) {
            return new ResourceProgress(state, 0, 0, 0, 0);
        }
        return new ResourceProgress(state, info.completedSize, info.totalSize,
                info.completedCount, info.totalCount);
    }

    private static ResourceOperationResult toPublicResult(UpdateResult result) {
        if (result == null) {
            return ResourceOperationResult.failure("The operation did not return a result.");
        }
        return new ResourceOperationResult(result.success, result.errorCode,
                result.errorMessage, result.state);
    }

    private record LegacySession(ResourceContext context, ResourceConfigManager configManager,
            ResUpdateModule updateModule, Map<UUID, CheckUpdateResult> checks) {
        private LegacySession(ResourceContext context, ResourceConfigManager configManager,
                ResUpdateModule updateModule) {
            this(context, configManager, updateModule, new HashMap<>());
        }
    }
}
