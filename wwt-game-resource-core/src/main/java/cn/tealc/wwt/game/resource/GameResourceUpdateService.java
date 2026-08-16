package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.internal.legacy.config.KRAppConfLoader;
import cn.tealc.wwt.game.resource.internal.legacy.config.ResourceConfigManager;
import cn.tealc.wwt.game.resource.internal.legacy.flow.ResUpdateModule;
import cn.tealc.wwt.game.resource.internal.legacy.model.CheckUpdateResult;
import cn.tealc.wwt.game.resource.internal.legacy.model.LauncherConfig;
import cn.tealc.wwt.game.resource.internal.legacy.model.ResStateInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.UpdateProgressInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.UpdateResult;
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
    private static final String KR_APP_CONF_RESOURCE = "/kr/KRApp.conf";

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
            Path configPath = context.workingDirectory().resolve("KRApp.conf");
            LauncherConfig config = KRAppConfLoader.loadFromKRAppConf(configPath.toString());
            ResourceConfigManager configManager = new ResourceConfigManager(
                    config, context.gameDirectory().toString());
            session = new LegacySession(context, configManager, new ResUpdateModule(configManager));
            return session;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize the game resource core", e);
        }
    }

    private static void extractRuntimeFiles(Path workingDirectory) throws IOException {
        Files.createDirectories(workingDirectory);
        extract(HPATCHZ_RESOURCE, workingDirectory.resolve("hpatchz.exe"));
        extract(KR_APP_CONF_RESOURCE, workingDirectory.resolve("KRApp.conf"));
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
