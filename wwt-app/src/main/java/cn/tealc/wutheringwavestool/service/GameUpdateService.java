package cn.tealc.wutheringwavestool.service;

import cn.tealc.wwt.game.resource.GameResourceUpdateService;
import cn.tealc.wwt.game.resource.DownloadOptions;
import cn.tealc.wwt.game.resource.ResourceCompletionListener;
import cn.tealc.wwt.game.resource.ResourceContext;
import cn.tealc.wwt.game.resource.ResourceProgressListener;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceCheckState;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

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

    private static ResourceContext resourceContext() {
        File gameDirectory = GameResourcesManager.getGameDir();
        if (gameDirectory == null) {
            throw new IllegalStateException("Game directory is not configured");
        }
        return new ResourceContext(gameDirectory.toPath(), Path.of("."));
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
