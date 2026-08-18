package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.game.FileInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Plans and creates transactional updates for launcher resource manifests.
 * This service has no JavaFX or application configuration dependency.
 */
public final class GameResourceInstallService {
    static final String DOWNLOAD_DIRECTORY = "launcherDownload";
    static final String LOCAL_MANIFEST_FILE = "LocalGameResources.json";
    static final String DOWNLOAD_CONFIG_FILE = "launcherDownloadConfig.json";

    private final GameResourceDownloadService downloadService;
    private final ObjectMapper objectMapper;

    public GameResourceInstallService(GameResourceDownloadService downloadService,
            ObjectMapper objectMapper) {
        this.downloadService = downloadService;
        this.objectMapper = objectMapper;
    }

    public GameResourceRelease check(GameDownloadSource source, Path gameDirectory,
            String fallbackInstalledVersion) {
        var response = downloadService.getLatestUpdate(source);
        if (response == null || !response.isSuccessful() || response.getData() == null) {
            String message = response != null ? response.getMessage() : "No response";
            throw new IllegalStateException("Failed to fetch game resource version: " + message);
        }
        String latest = response.getData().getVersion();
        if (!hasText(latest)) {
            throw new IllegalStateException("The remote game resource version is empty.");
        }
        String installed = readInstalledVersion(gameDirectory);
        if (!hasText(installed)) {
            installed = fallbackInstalledVersion;
        }
        if (!hasText(installed)) {
            throw new IllegalStateException("Unable to determine the installed game resource version.");
        }
        return new GameResourceRelease(source, response.getData(), installed);
    }

    public ResourceUpdateOperation createUpdate(Path gameDirectory, GameResourceRelease release,
            ResourceOperationListener listener) {
        return createUpdate(gameDirectory, release, listener, DownloadOptions.DEFAULT, null);
    }

    public ResourceUpdateOperation createUpdate(Path gameDirectory, GameResourceRelease release,
            ResourceOperationListener listener, DownloadOptions downloadOptions) {
        return createUpdate(gameDirectory, release, listener, downloadOptions, null);
    }

    /**
     * {@code cacheRoot} 可选：下载临时区根目录，默认落在 {@code gameDirectory/launcherDownload}。
     */
    public ResourceUpdateOperation createUpdate(Path gameDirectory, GameResourceRelease release,
            ResourceOperationListener listener, DownloadOptions downloadOptions, Path cacheRoot) {
        return new ResourceUpdateOperation(downloadService, objectMapper, gameDirectory, release, listener,
                downloadOptions, cacheRoot);
    }

    /** 将已完成的全量下载登记为可供后续更新检查使用的本地资源状态。 */
    public void registerInstalledRelease(Path gameDirectory, String version, List<FileInfo> resources)
            throws IOException {
        if (!hasText(version)) {
            throw new IOException("下载配置缺少游戏版本号");
        }
        if (resources == null || resources.isEmpty()) {
            throw new IOException("资源清单为空，无法登记游戏安装状态");
        }
        persistInstalledState(objectMapper, gameDirectory.toAbsolutePath().normalize(), version, resources);
    }

    public String readInstalledVersion(Path gameDirectory) {
        Path config = gameDirectory.toAbsolutePath().normalize().resolve(DOWNLOAD_CONFIG_FILE);
        if (!Files.isRegularFile(config)) {
            return "";
        }
        try {
            DownloadConfig value = objectMapper.readerFor(DownloadConfig.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(Files.readString(config));
            return value != null && value.version != null ? value.version : "";
        } catch (IOException e) {
            return "";
        }
    }

    static ResourcePlan createPlan(ObjectMapper mapper, Path gameDirectory,
            List<FileInfo> remoteResources) throws IOException {
        Path manifest = gameDirectory.resolve(LOCAL_MANIFEST_FILE);
        if (!Files.isRegularFile(manifest)) {
            throw new IOException("The local resource manifest is missing: " + manifest);
        }
        InstalledManifest local = mapper.readerFor(InstalledManifest.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(Files.readString(manifest));
        if (local == null || local.resource == null) {
            throw new IOException("The local resource manifest is invalid: " + manifest);
        }

        Map<String, InstalledFile> localByPath = new HashMap<>();
        for (InstalledFile file : local.resource) {
            if (file != null && hasText(file.path)) {
                localByPath.put(resourceKey(file.path), file);
            }
        }

        List<FileInfo> changedFiles = new ArrayList<>();
        Set<String> remotePaths = new HashSet<>();
        for (FileInfo remote : remoteResources) {
            if (remote == null || !hasText(remote.getDest())) {
                throw new IOException("The remote resource manifest contains an invalid path.");
            }
            String key = resourceKey(remote.getDest());
            remotePaths.add(key);
            if (!sameResource(remote, localByPath.get(key))) {
                changedFiles.add(remote);
            }
        }

        Set<String> obsoleteFiles = new HashSet<>();
        for (InstalledFile localFile : local.resource) {
            if (localFile != null && hasText(localFile.path)
                    && !remotePaths.contains(resourceKey(localFile.path))) {
                obsoleteFiles.add(localFile.path);
            }
        }
        return new ResourcePlan(changedFiles, new ArrayList<>(obsoleteFiles));
    }

    static void persistInstalledState(ObjectMapper mapper, Path gameDirectory, String version,
            List<FileInfo> resources) throws IOException {
        InstalledManifest manifest = new InstalledManifest();
        manifest.resource = resources.stream().map(InstalledFile::fromRemote).toList();
        writeJsonAtomically(mapper, gameDirectory.resolve(LOCAL_MANIFEST_FILE), manifest);

        Path configPath = gameDirectory.resolve(DOWNLOAD_CONFIG_FILE);
        DownloadConfig config = readDownloadConfig(mapper, configPath);
        if (config == null) {
            config = new DownloadConfig();
        }
        config.version = version;
        config.state = "";
        writeJsonAtomically(mapper, configPath, config);
    }

    static void writeJsonAtomically(ObjectMapper mapper, Path path, Object value) throws IOException {
        String content;
        try {
            content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IOException("Unable to serialize resource state: " + path, e);
        }
        Files.createDirectories(path.toAbsolutePath().normalize().getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static DownloadConfig readDownloadConfig(ObjectMapper mapper, Path configPath) {
        if (!Files.isRegularFile(configPath)) {
            return new DownloadConfig();
        }
        try {
            DownloadConfig config = mapper.readerFor(DownloadConfig.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(Files.readString(configPath));
            return config != null ? config : new DownloadConfig();
        } catch (IOException e) {
            return new DownloadConfig();
        }
    }

    static Path resolveResourcePath(Path root, String relativePath) throws IOException {
        if (!hasText(relativePath)) {
            throw new IOException("Resource path is empty.");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath.replace('\\', '/')).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IOException("Resource path escapes its root: " + relativePath);
        }
        return resolved;
    }

    static String resourceKey(String path) {
        return path.replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean sameResource(FileInfo remote, InstalledFile local) {
        if (local == null || !hasText(remote.getMd5()) || !remote.getMd5().equalsIgnoreCase(local.md5)
                || remote.getSize() == null || remote.getSize() != local.size) {
            return false;
        }
        List<cn.tealc.wwt.game.resource.model.game.ChunkInfo> remoteChunks = remote.getChunkInfos();
        if (remoteChunks == null || remoteChunks.isEmpty()) {
            return local.chunkInfos == null || local.chunkInfos.isEmpty();
        }
        if (local.chunkInfos == null || remoteChunks.size() != local.chunkInfos.size()) {
            return false;
        }
        for (int i = 0; i < remoteChunks.size(); i++) {
            var remoteChunk = remoteChunks.get(i);
            InstalledChunk localChunk = local.chunkInfos.get(i);
            if (remoteChunk == null || localChunk == null
                    || remoteChunk.getStart() != localChunk.start
                    || remoteChunk.getEnd() != localChunk.end
                    || !sameText(remoteChunk.getMd5(), localChunk.md5)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameText(String first, String second) {
        return first == null ? second == null : first.equalsIgnoreCase(second);
    }

    static final class ResourcePlan {
        final List<FileInfo> changedFiles;
        final List<String> obsoleteFiles;

        ResourcePlan(List<FileInfo> changedFiles, List<String> obsoleteFiles) {
            this.changedFiles = changedFiles;
            this.obsoleteFiles = obsoleteFiles;
        }
    }

    static final class InstalledManifest {
        public List<InstalledFile> resource;
    }

    static final class InstalledFile {
        public String path;
        public String md5;
        public long size;
        public List<InstalledChunk> chunkInfos;

        static InstalledFile fromRemote(FileInfo remote) {
            InstalledFile file = new InstalledFile();
            file.path = remote.getDest();
            file.md5 = remote.getMd5();
            file.size = remote.getSize() != null ? remote.getSize() : 0L;
            if (remote.getChunkInfos() != null) {
                file.chunkInfos = remote.getChunkInfos().stream().map(InstalledChunk::fromRemote).toList();
            }
            return file;
        }
    }

    static final class InstalledChunk {
        public long start;
        public long end;
        public String md5;

        static InstalledChunk fromRemote(cn.tealc.wwt.game.resource.model.game.ChunkInfo remote) {
            InstalledChunk chunk = new InstalledChunk();
            chunk.start = remote.getStart();
            chunk.end = remote.getEnd();
            chunk.md5 = remote.getMd5();
            return chunk;
        }
    }

    static final class DownloadConfig {
        public String version = "";
        public String reUseVersion = "";
        public String state = "";
        public boolean isPreDownload;
        public String appId = "";
    }
}
