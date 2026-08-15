package cn.tealc.wutheringwavestool.service;

import cn.tealc.download.DownloadManager;
import cn.tealc.download.GameDownloadSource;
import cn.tealc.download.GameResourceDownloadService;
import cn.tealc.download.model.DownloadState;
import cn.tealc.download.model.game.FileInfo;
import cn.tealc.download.model.launcher.UpdateData;
import cn.tealc.download.util.FileUtils;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kr.launcher.config.LauncherDownloadConfigHelper;
import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.model.FileChunkInfo;
import com.kr.launcher.model.GameResourceRecord;
import com.kr.launcher.model.LauncherDownloadConfig;
import com.kr.launcher.util.JsonUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 全局游戏资源更新协调器。首页和资源管理页共享同一检查结果、后台任务和下载控制状态。
 */
@Singleton
public class GameResourceUpdateCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourceUpdateCoordinator.class);

    public enum UpdateState {
        IDLE,
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        PREPARING,
        DOWNLOADING,
        PAUSED,
        APPLYING,
        COMPLETED,
        FAILED,
        CANCELED
    }

    private record RemoteUpdate(GameDownloadSource source, UpdateData updateData,
            String currentVersion, String latestVersion) {
    }

    private record AppliedFile(Path stagedFile, Path targetFile, Path backupFile,
            boolean hadOriginal, boolean installedReplacement) {
    }

    private record ResourcePlan(List<FileInfo> changedFiles, List<String> obsoleteFiles) {
    }

    private final GameResourceDownloadService downloadService;
    private final TaskManageService taskManageService;

    private final ReadOnlyObjectWrapper<UpdateState> state =
            new ReadOnlyObjectWrapper<>(UpdateState.IDLE);
    private final ReadOnlyStringWrapper statusText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper detailText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper currentVersion = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper latestVersion = new ReadOnlyStringWrapper("-");
    private final ReadOnlyStringWrapper actionText = new ReadOnlyStringWrapper("");
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0);
    private final ReadOnlyStringWrapper progressText = new ReadOnlyStringWrapper("0%");
    private final ReadOnlyBooleanWrapper statusVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper retryVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper updateActionVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper progressVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper pauseVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper resumeVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper cancelVisible = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper operating = new ReadOnlyBooleanWrapper(false);

    private volatile Task<?> activeTask;
    private volatile DownloadManager activeDownloadManager;
    private RemoteUpdate remoteUpdate;
    private boolean startAfterCheck;

    @Inject
    public GameResourceUpdateCoordinator(GameResourceDownloadService downloadService,
            TaskManageService taskManageService) {
        this.downloadService = downloadService;
        this.taskManageService = taskManageService;
        actionText.set(LanguageManager.getString("ui.home.button.start_update"));
    }

    public void checkForUpdates() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::checkForUpdates);
            return;
        }
        if (activeTask != null) {
            return;
        }
        if (GameResourcesManager.getGameExeBase() == null) {
            remoteUpdate = null;
            setState(UpdateState.IDLE, "", "");
            return;
        }

        SourceType configuredSource = Config.setting().getGameRootDirSource();
        SourceType source = configuredSource != null ? configuredSource : SourceType.DEFAULT;
        setState(UpdateState.CHECKING,
                LanguageManager.getString("ui.home.resource.checking"),
                LanguageManager.getString("ui.home.resource.checking_detail"));

        Task<RemoteUpdate> task = new Task<>() {
            @Override
            protected RemoteUpdate call() {
                updateTitle(LanguageManager.getString("ui.home.resource.task"));
                var response = downloadService.getLatestUpdate(source.toGameDownloadSource());
                if (response == null || !response.isSuccessful()) {
                    String reason = response != null ? response.getMessage() : "无响应";
                    throw new IllegalStateException("获取游戏资源版本失败: " + reason);
                }
                UpdateData updateData = response.getData();
                String latest = updateData.getVersion();
                String current = readInstalledVersion();
                if (!hasText(latest)) {
                    throw new IllegalStateException("远端游戏资源版本为空");
                }
                if (!hasText(current)) {
                    throw new IllegalStateException("无法读取本地游戏资源版本");
                }
                return new RemoteUpdate(source.toGameDownloadSource(), updateData, current, latest);
            }
        };
        activeTask = task;
        task.setOnSucceeded(event -> {
            if (!finishTask(task)) {
                return;
            }
            remoteUpdate = task.getValue();
            currentVersion.set(remoteUpdate.currentVersion());
            latestVersion.set(remoteUpdate.latestVersion());
            if (remoteUpdate.currentVersion().equalsIgnoreCase(remoteUpdate.latestVersion())) {
                startAfterCheck = false;
                setState(UpdateState.UP_TO_DATE,
                        LanguageManager.getString("ui.home.resource.up_to_date"),
                        String.format(LanguageManager.getString("ui.home.resource.current_version"),
                                remoteUpdate.currentVersion()));
            } else {
                setState(UpdateState.UPDATE_AVAILABLE,
                        LanguageManager.getString("ui.home.resource.update_available"),
                        String.format(LanguageManager.getString("ui.home.resource.version_diff"),
                                remoteUpdate.currentVersion(), remoteUpdate.latestVersion()));
                if (startAfterCheck) {
                    startAfterCheck = false;
                    startUpdate();
                }
            }
        });
        task.setOnFailed(event -> {
            if (!finishTask(task)) {
                return;
            }
            startAfterCheck = false;
            LOG.warn("检查游戏资源更新失败", task.getException());
            setState(UpdateState.FAILED,
                    LanguageManager.getString("ui.home.resource.check_failed"),
                    LanguageManager.getString("ui.home.resource.check_failed_detail"));
        });
        task.setOnCancelled(event -> {
            if (finishTask(task)) {
                startAfterCheck = false;
                setState(UpdateState.CANCELED,
                        LanguageManager.getString("ui.game_manager.asset.stopped"), "");
            }
        });
        taskManageService.execute(task);
    }

    public void startUpdate() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::startUpdate);
            return;
        }
        if (activeTask != null) {
            return;
        }
        if (remoteUpdate == null) {
            startAfterCheck = true;
            checkForUpdates();
            return;
        }
        if (remoteUpdate.currentVersion().equalsIgnoreCase(remoteUpdate.latestVersion())) {
            setState(UpdateState.UP_TO_DATE,
                    LanguageManager.getString("ui.home.resource.up_to_date"),
                    String.format(LanguageManager.getString("ui.home.resource.current_version"),
                            remoteUpdate.currentVersion()));
            return;
        }
        if (GameAppListener.getInstance().isRunning()) {
            setState(UpdateState.FAILED,
                    LanguageManager.getString("ui.home.resource.update_failed"),
                    LanguageManager.getString("ui.home.resource.close_game"));
            return;
        }

        ResourceUpdateTask task = new ResourceUpdateTask(remoteUpdate);
        activeTask = task;
        progress.unbind();
        progress.bind(task.progressProperty());
        task.messageProperty().addListener((observable, oldValue, newValue) -> detailText.set(newValue));
        task.progressProperty().addListener((observable, oldValue, newValue) -> {
            double value = newValue != null ? newValue.doubleValue() : 0;
            progressText.set(value < 0 ? "--" : String.format(Locale.ROOT, "%.1f%%", value * 100));
        });
        setState(UpdateState.PREPARING,
                LanguageManager.getString("ui.home.resource.preparing"),
                LanguageManager.getString("ui.home.resource.preparing_detail"));

        task.setOnSucceeded(event -> {
            if (!finishTask(task)) {
                return;
            }
            progress.unbind();
            progress.set(1);
            progressText.set("100%");
            RemoteUpdate completedUpdate = task.getValue();
            String installedVersion = completedUpdate.latestVersion();
            currentVersion.set(installedVersion);
            latestVersion.set(installedVersion);
            cacheInstalledVersion(installedVersion);
            remoteUpdate = completedUpdate;
            setState(UpdateState.COMPLETED,
                    LanguageManager.getString("ui.home.resource.update_complete"),
                    String.format(LanguageManager.getString("ui.home.resource.current_version"),
                            currentVersion.get()));
        });
        task.setOnFailed(event -> {
            if (!finishTask(task)) {
                return;
            }
            progress.unbind();
            Throwable exception = task.getException();
            String message = exception != null && hasText(exception.getMessage())
                    ? exception.getMessage()
                    : LanguageManager.getString("ui.home.resource.update_failed_detail");
            LOG.error("游戏资源更新失败", exception);
            setState(UpdateState.FAILED,
                    LanguageManager.getString("ui.home.resource.update_failed"), message);
        });
        task.setOnCancelled(event -> {
            if (!finishTask(task)) {
                return;
            }
            progress.unbind();
            progress.set(0);
            progressText.set("0%");
            setState(UpdateState.CANCELED,
                    LanguageManager.getString("ui.game_manager.asset.stopped"),
                    LanguageManager.getString("ui.home.resource.update_canceled_detail"));
        });
        taskManageService.execute(task);
    }

    public void retry() {
        if (remoteUpdate != null
                && !remoteUpdate.currentVersion().equalsIgnoreCase(remoteUpdate.latestVersion())) {
            startUpdate();
        } else {
            checkForUpdates();
        }
    }

    public void pause() {
        if (state.get() != UpdateState.DOWNLOADING || activeDownloadManager == null) {
            return;
        }
        activeDownloadManager.pause();
        setState(UpdateState.PAUSED,
                LanguageManager.getString("ui.home.resource.update_paused"), detailText.get());
    }

    public void resume() {
        if (state.get() != UpdateState.PAUSED || activeDownloadManager == null) {
            return;
        }
        activeDownloadManager.resume();
        setState(UpdateState.DOWNLOADING,
                LanguageManager.getString("ui.home.resource.downloading"), detailText.get());
    }

    public void cancel() {
        UpdateState currentState = state.get();
        if (activeTask == null || currentState == UpdateState.APPLYING) {
            return;
        }
        DownloadManager manager = activeDownloadManager;
        if (manager != null) {
            manager.stop();
        }
        activeTask.cancel(true);
    }

    private boolean finishTask(Task<?> task) {
        if (activeTask != task) {
            return false;
        }
        activeTask = null;
        activeDownloadManager = null;
        return true;
    }

    private void setState(UpdateState newState, String status, String detail) {
        Runnable update = () -> {
            statusText.set(status != null ? status : "");
            detailText.set(detail != null ? detail : "");
            statusVisible.set(newState != UpdateState.IDLE);
            retryVisible.set(newState == UpdateState.FAILED || newState == UpdateState.CANCELED);
            updateActionVisible.set(newState == UpdateState.UPDATE_AVAILABLE);
            progressVisible.set(isProgressState(newState));
            pauseVisible.set(newState == UpdateState.DOWNLOADING);
            resumeVisible.set(newState == UpdateState.PAUSED);
            cancelVisible.set(newState == UpdateState.PREPARING
                    || newState == UpdateState.DOWNLOADING
                    || newState == UpdateState.PAUSED);
            operating.set(isOperatingState(newState));
            if (newState == UpdateState.UPDATE_AVAILABLE) {
                actionText.set(String.format(LanguageManager.getString("ui.home.button.update_to"),
                        latestVersion.get()));
            } else {
                actionText.set(LanguageManager.getString("ui.home.button.start_update"));
            }
            if (!isProgressState(newState) && newState != UpdateState.COMPLETED) {
                progressText.set("0%");
            }
            state.set(newState);
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    private static boolean isProgressState(UpdateState state) {
        return state == UpdateState.PREPARING || state == UpdateState.DOWNLOADING
                || state == UpdateState.PAUSED || state == UpdateState.APPLYING;
    }

    private static boolean isOperatingState(UpdateState state) {
        return state == UpdateState.CHECKING || isProgressState(state);
    }

    private String readInstalledVersion() {
        Path gameDir = getGameDir();
        LauncherDownloadConfig localConfig = LauncherDownloadConfigHelper.get(
                gameDir.resolve(ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG).toString());
        if (localConfig != null && hasText(localConfig.version)) {
            return localConfig.version;
        }
        String cachedVersion = Config.setting().getGameInstalledVersion();
        return hasText(cachedVersion) ? cachedVersion : "";
    }

    private static Path getGameDir() {
        var gameDir = GameResourcesManager.getGameDir();
        if (gameDir == null) {
            throw new IllegalStateException("游戏目录未设置");
        }
        return gameDir.toPath().toAbsolutePath().normalize();
    }

    private final class ResourceUpdateTask extends Task<RemoteUpdate> {
        private final RemoteUpdate initialUpdate;

        private ResourceUpdateTask(RemoteUpdate initialUpdate) {
            this.initialUpdate = initialUpdate;
        }

        @Override
        protected RemoteUpdate call() throws Exception {
            updateTitle(LanguageManager.getString("ui.home.resource.update_task"));
            Path gameDir = getGameDir();

            var launcherResponse = downloadService.getLatestUpdate(initialUpdate.source());
            if (launcherResponse == null || launcherResponse.getCode() != 200
                    || launcherResponse.getData() == null) {
                throw new IllegalStateException("获取最新资源配置失败");
            }
            UpdateData updateData = launcherResponse.getData();
            if (!hasText(updateData.getVersion())) {
                throw new IllegalStateException("最新资源版本为空");
            }
            var resourceResponse = downloadService.getResourceList(updateData);
            if (resourceResponse == null || resourceResponse.getCode() != 200
                    || resourceResponse.getData() == null) {
                throw new IllegalStateException("获取游戏资源清单失败");
            }
            List<FileInfo> resources = resourceResponse.getData();
            if (resources.isEmpty()) {
                throw new IllegalStateException("游戏资源清单为空");
            }

            phase(UpdateState.PREPARING,
                    LanguageManager.getString("ui.home.resource.preparing"),
                    LanguageManager.getString("ui.home.resource.preparing_detail"));
            ResourcePlan plan = createResourcePlan(gameDir, resources);
            List<FileInfo> changedFiles = plan.changedFiles();
            List<String> obsoleteFiles = plan.obsoleteFiles();
            ensureNotCancelled();

            Path stageDir = stageDirectory(gameDir, updateData.getVersion());
            if (changedFiles.isEmpty() && obsoleteFiles.isEmpty()) {
                FileUtils.deleteRecursively(stageDir);
                persistInstalledState(gameDir, updateData.getVersion(), resources);
                return completedUpdate(updateData);
            }

            if (!changedFiles.isEmpty()) {
                Files.createDirectories(stageDir);
                DownloadManager manager = downloadService.createDownloadManager(
                        stageDir, updateData, changedFiles);
                activeDownloadManager = manager;
                AtomicReference<DownloadState> terminalState = new AtomicReference<>();
                AtomicReference<String> downloadError = new AtomicReference<>();
                long[] speedSample = {System.nanoTime(), 0L};
                double[] speed = {0};
                manager.setProgressListener((done, total) -> {
                    updateProgress(done, total);
                    long now = System.nanoTime();
                    long elapsed = now - speedSample[0];
                    if (elapsed >= 500_000_000L) {
                        speed[0] = (done - speedSample[1]) / (elapsed / 1_000_000_000.0);
                        speedSample[0] = now;
                        speedSample[1] = done;
                    }
                    updateMessage(String.format(Locale.ROOT, "%s / %s  ·  %s/s",
                            formatBytes(done), formatBytes(total),
                            formatBytes((long) Math.max(0, speed[0]))));
                });
                manager.setStateListener((downloadState, error) -> {
                    if (downloadState == DownloadState.DOWNLOADING) {
                        phase(UpdateState.DOWNLOADING,
                                LanguageManager.getString("ui.home.resource.downloading"),
                                LanguageManager.getString("ui.home.resource.download_start"));
                    }
                    if (downloadState == DownloadState.COMPLETE
                            || downloadState == DownloadState.FAILED
                            || downloadState == DownloadState.CANCELED) {
                        terminalState.set(downloadState);
                        downloadError.set(error);
                    }
                });
                try {
                    manager.run();
                } finally {
                    activeDownloadManager = null;
                }
                ensureNotCancelled();
                if (terminalState.get() != DownloadState.COMPLETE) {
                    throw new IllegalStateException(hasText(downloadError.get())
                            ? downloadError.get() : "资源下载未完成");
                }

            }

            if (GameAppListener.getInstance().isRunning()) {
                throw new IllegalStateException(LanguageManager.getString("ui.home.resource.close_game"));
            }
            phase(UpdateState.APPLYING,
                    LanguageManager.getString("ui.home.resource.applying"),
                    LanguageManager.getString("ui.home.resource.applying_detail"));
            applyStagedFiles(gameDir, stageDir, changedFiles, obsoleteFiles);
            persistInstalledState(gameDir, updateData.getVersion(), resources);
            FileUtils.deleteRecursively(stageDir);
            return completedUpdate(updateData);
        }

        private RemoteUpdate completedUpdate(UpdateData updateData) {
            return new RemoteUpdate(initialUpdate.source(), updateData,
                    updateData.getVersion(), updateData.getVersion());
        }

        private ResourcePlan createResourcePlan(Path gameDir, List<FileInfo> resources) {
            Path recordPath = gameDir.resolve(ResourceConfigManager.LOCAL_INDEX_FILE_NAME);
            GameResourceRecord localRecord = GameResourceRecord.get(recordPath.toString());
            if (localRecord == null || localRecord.resource == null) {
                throw new IllegalStateException(
                        LanguageManager.getString("ui.home.resource.local_manifest_missing"));
            }

            Map<String, com.kr.launcher.model.FileInfo> localResources = new HashMap<>();
            for (com.kr.launcher.model.FileInfo localResource : localRecord.resource) {
                if (localResource != null && hasText(localResource.path)) {
                    localResources.put(resourceKey(localResource.path), localResource);
                }
            }

            List<FileInfo> changedFiles = new ArrayList<>();
            Set<String> latestPaths = new HashSet<>();
            for (FileInfo resource : resources) {
                if (resource == null || !hasText(resource.getDest())) {
                    throw new IllegalStateException("远端资源清单包含无效路径");
                }
                String key = resourceKey(resource.getDest());
                latestPaths.add(key);
                com.kr.launcher.model.FileInfo localResource = localResources.get(key);
                if (!sameResource(resource, localResource)) {
                    changedFiles.add(resource);
                }
            }
            Set<String> obsoletePaths = new HashSet<>();
            for (com.kr.launcher.model.FileInfo localResource : localRecord.resource) {
                if (localResource != null && hasText(localResource.path)
                        && !latestPaths.contains(resourceKey(localResource.path))) {
                    obsoletePaths.add(localResource.path);
                }
            }
            return new ResourcePlan(changedFiles, new ArrayList<>(obsoletePaths));
        }

        private void ensureNotCancelled() {
            if (isCancelled() || Thread.currentThread().isInterrupted()) {
                throw new CancellationException("资源更新已取消");
            }
        }

        private void phase(UpdateState updateState, String status, String detail) {
            if (updateState == UpdateState.APPLYING) {
                updateProgress(-1, -1);
            }
            updateMessage(detail);
            setState(updateState, status, detail);
        }
    }

    private static Path stageDirectory(Path gameDir, String version) {
        String safeVersion = version.replaceAll("[^A-Za-z0-9._-]", "_");
        return gameDir.resolve(ResourceConfigManager.LAUNCHER_DOWNLOAD)
                .resolve(safeVersion)
                .resolve("wwt-update")
                .normalize();
    }

    private static void applyStagedFiles(Path gameDir, Path stageDir,
            List<FileInfo> changedFiles, List<String> obsoleteFiles) throws IOException {
        Path backupRoot = stageDir.resolve(".backup");
        List<AppliedFile> appliedFiles = new ArrayList<>();
        try {
            for (FileInfo resource : changedFiles) {
                Path stagedFile = resolveResourcePath(stageDir, resource.getDest());
                Path targetFile = resolveResourcePath(gameDir, resource.getDest());
                Path backupFile = resolveResourcePath(backupRoot, resource.getDest());
                if (!Files.isRegularFile(stagedFile)) {
                    throw new IOException("暂存文件不存在: " + resource.getDest());
                }
                Files.createDirectories(targetFile.getParent());
                boolean hadOriginal = Files.exists(targetFile);
                if (hadOriginal) {
                    Files.createDirectories(backupFile.getParent());
                    moveReplacing(targetFile, backupFile);
                }
                try {
                    moveReplacing(stagedFile, targetFile);
                    appliedFiles.add(new AppliedFile(
                            stagedFile, targetFile, backupFile, hadOriginal, true));
                } catch (IOException e) {
                    if (hadOriginal && Files.exists(backupFile)) {
                        moveReplacing(backupFile, targetFile);
                    }
                    throw e;
                }
            }
            for (String obsoleteFile : obsoleteFiles) {
                Path targetFile = resolveResourcePath(gameDir, obsoleteFile);
                if (!Files.exists(targetFile)) {
                    continue;
                }
                Path backupFile = resolveResourcePath(backupRoot, obsoleteFile);
                Files.createDirectories(backupFile.getParent());
                moveReplacing(targetFile, backupFile);
                appliedFiles.add(new AppliedFile(
                        null, targetFile, backupFile, true, false));
            }
        } catch (IOException applyError) {
            rollbackAppliedFiles(appliedFiles, applyError);
            throw applyError;
        }
        FileUtils.deleteRecursively(backupRoot);
    }

    private static void rollbackAppliedFiles(List<AppliedFile> appliedFiles, IOException applyError) {
        Collections.reverse(appliedFiles);
        for (AppliedFile applied : appliedFiles) {
            try {
                if (applied.installedReplacement() && Files.exists(applied.targetFile())) {
                    Files.createDirectories(applied.stagedFile().getParent());
                    moveReplacing(applied.targetFile(), applied.stagedFile());
                }
                if (applied.hadOriginal() && Files.exists(applied.backupFile())) {
                    Files.createDirectories(applied.targetFile().getParent());
                    moveReplacing(applied.backupFile(), applied.targetFile());
                }
            } catch (IOException rollbackError) {
                applyError.addSuppressed(rollbackError);
            }
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void persistInstalledState(Path gameDir, String version,
            List<FileInfo> resources) throws IOException {
        List<com.kr.launcher.model.FileInfo> recordFiles = resources.stream()
                .map(GameResourceUpdateCoordinator::toResourceRecordFile)
                .toList();
        Path recordPath = gameDir.resolve(ResourceConfigManager.LOCAL_INDEX_FILE_NAME);
        writeJsonAtomically(recordPath, new GameResourceRecord(recordFiles));

        Path configPath = gameDir.resolve(ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG);
        LauncherDownloadConfig localConfig = LauncherDownloadConfigHelper.get(configPath.toString());
        if (localConfig == null) {
            localConfig = new LauncherDownloadConfig();
        }
        localConfig.version = version;
        localConfig.state = "";
        writeJsonAtomically(configPath, localConfig);
    }

    private static void cacheInstalledVersion(String version) {
        try {
            Config.setting().setGameInstalledVersion(version);
            Config.setting().save();
        } catch (Exception e) {
            LOG.warn("保存游戏资源版本失败: {}", version, e);
        }
    }

    private static com.kr.launcher.model.FileInfo toResourceRecordFile(FileInfo source) {
        com.kr.launcher.model.FileInfo target = new com.kr.launcher.model.FileInfo();
        target.path = source.getDest();
        target.md5 = source.getMd5();
        target.size = fileSize(source);
        if (source.getChunkInfos() != null) {
            target.chunkInfos = source.getChunkInfos().stream().map(chunk -> {
                FileChunkInfo targetChunk = new FileChunkInfo();
                targetChunk.start = chunk.getStart();
                targetChunk.end = chunk.getEnd();
                targetChunk.md5 = chunk.getMd5();
                return targetChunk;
            }).toList();
        }
        return target;
    }

    private static void writeJsonAtomically(Path path, Object value) throws IOException {
        String json = JsonUtils.serialize(value);
        if (json == null) {
            throw new IOException("序列化资源状态失败: " + path.getFileName());
        }
        Files.createDirectories(path.toAbsolutePath().normalize().getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".wwt.tmp");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            moveReplacing(temporary, path);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path resolveResourcePath(Path root, String relativePath) {
        if (!hasText(relativePath)) {
            throw new IllegalArgumentException("资源路径为空");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath.replace('\\', '/')).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("资源路径超出游戏目录: " + relativePath);
        }
        return resolved;
    }

    private static long fileSize(FileInfo fileInfo) {
        return fileInfo.getSize() != null ? Math.max(0, fileInfo.getSize()) : 0;
    }

    private static String resourceKey(String path) {
        return path.replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static boolean sameResource(FileInfo remote, com.kr.launcher.model.FileInfo local) {
        if (local == null || fileSize(remote) != Math.max(0, local.size)) {
            return false;
        }
        String remoteMd5 = remote.getMd5() != null ? remote.getMd5() : "";
        String localMd5 = local.md5 != null ? local.md5 : "";
        return remoteMd5.equalsIgnoreCase(localMd5);
    }

    private static String formatBytes(long bytes) {
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public ReadOnlyObjectProperty<UpdateState> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty statusTextProperty() {
        return statusText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty detailTextProperty() {
        return detailText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty currentVersionProperty() {
        return currentVersion.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty latestVersionProperty() {
        return latestVersion.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty actionTextProperty() {
        return actionText.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty progressTextProperty() {
        return progressText.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty statusVisibleProperty() {
        return statusVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty retryVisibleProperty() {
        return retryVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty updateActionVisibleProperty() {
        return updateActionVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty progressVisibleProperty() {
        return progressVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty pauseVisibleProperty() {
        return pauseVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty resumeVisibleProperty() {
        return resumeVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty cancelVisibleProperty() {
        return cancelVisible.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty operatingProperty() {
        return operating.getReadOnlyProperty();
    }
}
