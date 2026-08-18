package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.DownloadState;
import cn.tealc.wwt.game.resource.model.ResourceOperationState;
import cn.tealc.wwt.game.resource.model.ResourceProgress;
import cn.tealc.wwt.game.resource.model.game.FileInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

/** A single transactional resource update. The caller executes it on a worker thread. */
public final class ResourceUpdateOperation {
    private static final String DEFAULT_CACHE_FOLDER = GameResourceInstallService.DOWNLOAD_DIRECTORY;
    private final GameResourceDownloadService downloadService;
    private final ObjectMapper objectMapper;
    private final Path gameDirectory;
    private final Path cacheRoot;
    private final GameResourceRelease initialRelease;
    private final ResourceOperationListener listener;
    private final DownloadOptions downloadOptions;

    private volatile DownloadManager activeDownloadManager;
    private volatile boolean canceled;

    ResourceUpdateOperation(GameResourceDownloadService downloadService, ObjectMapper objectMapper,
            Path gameDirectory, GameResourceRelease initialRelease,
            ResourceOperationListener listener) {
        this(downloadService, objectMapper, gameDirectory, initialRelease, listener,
                DownloadOptions.DEFAULT, null);
    }

    ResourceUpdateOperation(GameResourceDownloadService downloadService, ObjectMapper objectMapper,
            Path gameDirectory, GameResourceRelease initialRelease,
            ResourceOperationListener listener, DownloadOptions downloadOptions) {
        this(downloadService, objectMapper, gameDirectory, initialRelease, listener,
                downloadOptions, null);
    }

    ResourceUpdateOperation(GameResourceDownloadService downloadService, ObjectMapper objectMapper,
            Path gameDirectory, GameResourceRelease initialRelease,
            ResourceOperationListener listener, DownloadOptions downloadOptions, Path cacheRoot) {
        this.downloadService = downloadService;
        this.objectMapper = objectMapper;
        this.gameDirectory = gameDirectory.toAbsolutePath().normalize();
        this.cacheRoot = cacheRoot == null
                ? this.gameDirectory.resolve(DEFAULT_CACHE_FOLDER)
                : cacheRoot.toAbsolutePath().normalize();
        this.initialRelease = initialRelease;
        this.listener = listener != null ? listener : new ResourceOperationListener() { };
        this.downloadOptions = downloadOptions != null ? downloadOptions : DownloadOptions.DEFAULT;
    }

    public GameResourceRelease execute() throws IOException {
        try {
            listener.onStateChanged(ResourceOperationState.PREPARING);
            ensureNotCanceled();
            var updateResponse = downloadService.getLatestUpdate(initialRelease.source());
            if (updateResponse == null || !updateResponse.isSuccessful() || updateResponse.getData() == null
                    || !GameResourceInstallService.hasText(updateResponse.getData().getVersion())) {
                throw new IOException("Failed to fetch the latest resource configuration.");
            }
            GameResourceRelease release = new GameResourceRelease(initialRelease.source(),
                    updateResponse.getData(), initialRelease.installedVersion());
            var resourcesResponse = downloadService.getResourceList(release.updateData());
            if (resourcesResponse == null || !resourcesResponse.isSuccessful()
                    || resourcesResponse.getData() == null || resourcesResponse.getData().isEmpty()) {
                throw new IOException("Failed to fetch the game resource manifest.");
            }

            List<FileInfo> resources = resourcesResponse.getData();
            GameResourceInstallService.ResourcePlan plan = GameResourceInstallService.createPlan(
                    objectMapper, gameDirectory, resources);
            Path stageDirectory = stageDirectory(release.latestVersion());
            if (plan.changedFiles.isEmpty() && plan.obsoleteFiles.isEmpty()) {
                cn.tealc.wwt.game.resource.util.FileUtils.deleteRecursively(stageDirectory);
                GameResourceInstallService.persistInstalledState(objectMapper, gameDirectory,
                        release.latestVersion(), resources);
                listener.onStateChanged(ResourceOperationState.COMPLETED);
                return new GameResourceRelease(release.source(), release.updateData(), release.latestVersion());
            }

            download(stageDirectory, release, plan.changedFiles);
            ensureNotCanceled();

            listener.onStateChanged(ResourceOperationState.APPLYING);
            apply(stageDirectory, plan.changedFiles, plan.obsoleteFiles);
            GameResourceInstallService.persistInstalledState(objectMapper, gameDirectory,
                    release.latestVersion(), resources);
            cn.tealc.wwt.game.resource.util.FileUtils.deleteRecursively(stageDirectory);
            listener.onStateChanged(ResourceOperationState.COMPLETED);
            return new GameResourceRelease(release.source(), release.updateData(), release.latestVersion());
        } catch (CancellationException e) {
            listener.onStateChanged(ResourceOperationState.CANCELED);
            throw e;
        } catch (IOException | RuntimeException e) {
            listener.onStateChanged(ResourceOperationState.FAILED);
            throw e;
        }
    }

    public void pause() {
        DownloadManager manager = activeDownloadManager;
        if (manager != null) {
            manager.pause();
            listener.onStateChanged(ResourceOperationState.PAUSED);
        }
    }

    public void resume() {
        DownloadManager manager = activeDownloadManager;
        if (manager != null) {
            manager.resume();
            listener.onStateChanged(ResourceOperationState.DOWNLOADING);
        }
    }

    public void cancel() {
        canceled = true;
        DownloadManager manager = activeDownloadManager;
        if (manager != null) {
            manager.stop();
        }
    }

    private void download(Path stageDirectory, GameResourceRelease release,
            List<FileInfo> changedFiles) throws IOException {
        if (changedFiles.isEmpty()) {
            return;
        }
        Files.createDirectories(stageDirectory);
        DownloadManager manager = downloadService.createDownloadManager(
                stageDirectory, release.updateData(), changedFiles, downloadOptions);
        activeDownloadManager = manager;
        AtomicReference<DownloadState> terminalState = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        manager.setProgressListener((completed, total) -> listener.onProgress(
                new ResourceProgress(0, completed, total, 0, changedFiles.size())));
        manager.setStateListener((state, message) -> {
            if (state == DownloadState.DOWNLOADING) {
                listener.onStateChanged(ResourceOperationState.DOWNLOADING);
            }
            if (state == DownloadState.COMPLETE || state == DownloadState.FAILED
                    || state == DownloadState.CANCELED) {
                terminalState.set(state);
                error.set(message);
            }
        });
        try {
            manager.run();
        } finally {
            activeDownloadManager = null;
        }
        ensureNotCanceled();
        if (terminalState.get() != DownloadState.COMPLETE) {
            throw new IOException(GameResourceInstallService.hasText(error.get())
                    ? error.get() : "The resource download did not complete.");
        }
    }

    private Path stageDirectory(String version) {
        String safeVersion = version.replaceAll("[^A-Za-z0-9._-]", "_");
        return cacheRoot.resolve(safeVersion).resolve("wwt-update").normalize();
    }

    private void apply(Path stageDirectory, List<FileInfo> changedFiles,
            List<String> obsoleteFiles) throws IOException {
        Path backupRoot = stageDirectory.resolve(".backup");
        List<AppliedFile> applied = new ArrayList<>();
        try {
            for (FileInfo resource : changedFiles) {
                ensureNotCanceled();
                Path staged = GameResourceInstallService.resolveResourcePath(stageDirectory, resource.getDest());
                Path target = GameResourceInstallService.resolveResourcePath(gameDirectory, resource.getDest());
                Path backup = GameResourceInstallService.resolveResourcePath(backupRoot, resource.getDest());
                if (!Files.isRegularFile(staged)) {
                    throw new IOException("Staged resource is missing: " + resource.getDest());
                }
                Files.createDirectories(target.getParent());
                boolean hadOriginal = Files.exists(target);
                if (hadOriginal) {
                    Files.createDirectories(backup.getParent());
                    moveReplacing(target, backup);
                }
                try {
                    moveReplacing(staged, target);
                    applied.add(new AppliedFile(staged, target, backup, hadOriginal, true));
                } catch (IOException e) {
                    if (hadOriginal && Files.exists(backup)) {
                        moveReplacing(backup, target);
                    }
                    throw e;
                }
            }
            for (String obsolete : obsoleteFiles) {
                ensureNotCanceled();
                Path target = GameResourceInstallService.resolveResourcePath(gameDirectory, obsolete);
                if (!Files.exists(target)) {
                    continue;
                }
                Path backup = GameResourceInstallService.resolveResourcePath(backupRoot, obsolete);
                Files.createDirectories(backup.getParent());
                moveReplacing(target, backup);
                applied.add(new AppliedFile(null, target, backup, true, false));
            }
        } catch (IOException | RuntimeException applyError) {
            rollback(applied, applyError);
            throw applyError;
        }
        cn.tealc.wwt.game.resource.util.FileUtils.deleteRecursively(backupRoot);
    }

    private static void rollback(List<AppliedFile> applied, Throwable originalError) {
        Collections.reverse(applied);
        for (AppliedFile file : applied) {
            try {
                if (file.installedReplacement && Files.exists(file.target)) {
                    Files.createDirectories(file.staged.getParent());
                    moveReplacing(file.target, file.staged);
                }
                if (file.hadOriginal && Files.exists(file.backup)) {
                    Files.createDirectories(file.target.getParent());
                    moveReplacing(file.backup, file.target);
                }
            } catch (IOException rollbackError) {
                originalError.addSuppressed(rollbackError);
            }
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void ensureNotCanceled() {
        if (canceled || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("The resource update was canceled.");
        }
    }

    private record AppliedFile(Path staged, Path target, Path backup,
            boolean hadOriginal, boolean installedReplacement) {
    }
}
