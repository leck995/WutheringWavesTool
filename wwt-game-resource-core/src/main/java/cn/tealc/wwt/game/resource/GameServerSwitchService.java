package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.internal.legacy.download.CDNDownloadTask;
import cn.tealc.wwt.game.resource.internal.legacy.download.CDNDownloadTaskBuilder;
import cn.tealc.wwt.game.resource.internal.legacy.download.DownloadState;
import cn.tealc.wwt.game.resource.internal.legacy.model.CdnConfig;
import cn.tealc.wwt.game.resource.internal.legacy.model.DownloadInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.FileInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.GameServerConfig;
import cn.tealc.wwt.game.resource.internal.legacy.model.IndexFile;
import cn.tealc.wwt.game.resource.internal.legacy.util.HttpUtils;
import cn.tealc.wwt.game.resource.internal.legacy.util.JsonUtils;
import cn.tealc.wwt.game.resource.internal.legacy.util.ResourceHelper;
import cn.tealc.wwt.game.resource.internal.legacy.util.UrlUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Caches and swaps the reduced mainland/Bilibili server component set.
 *
 * <p>The service deliberately does not compare game versions before a switch. A user can refresh a
 * cache manually when a stale component set no longer works.</p>
 */
public final class GameServerSwitchService {
    private static final int CACHE_SCHEMA_VERSION = 1;
    private static final String CACHE_DIRECTORY = "WwtBackup/server-switch";
    private static final String PAYLOAD_DIRECTORY = "payload";
    private static final String METADATA_FILE = "metadata.json";
    private static final String TRANSACTION_FILE = "transaction.json";

    private static final Path GAME_EXECUTABLE = Path.of("Client", "Binaries", "Win64",
            "Client-Win64-Shipping.exe");
    private static final Path SDK_DIRECTORY = Path.of("Client", "Binaries", "Win64", "ThirdParty",
            "KrPcSdk_Mainland");
    private static final Path GLOBAL_SDK_DIRECTORY = Path.of("Client", "Binaries", "Win64", "ThirdParty",
            "KrPcSdk_Global");
    private static final Path ANTI_CHEAT_DIRECTORY = Path.of("Client", "Binaries", "Win64",
            "AntiCheatExpert");
    private static final String SDK_PREFIX = toResourcePath(SDK_DIRECTORY) + "/";
    private static final String ANTI_CHEAT_PREFIX = toResourcePath(ANTI_CHEAT_DIRECTORY) + "/";
    private static final Path BILIBILI_MARKER = SDK_DIRECTORY.resolve("KRSDKRes").resolve("Bilibili");
    private static final Path LAUNCHER_DOWNLOAD_CONFIG = Path.of("launcherDownloadConfig.json");
    private static final String MAINLAND_APP_ID = "10003";
    private static final String BILIBILI_APP_ID = "10004";
    private static final String GLOBAL_APP_ID = "50004";

    private final ObjectMapper objectMapper;

    public GameServerSwitchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Returns local state without downloading or mutating files. */
    public ServerSwitchStatus inspect(Path gameDirectory) throws IOException {
        Path gameRoot = normalizeGameDirectory(gameDirectory);
        CacheMetadata metadata = readMetadata(gameRoot);
        return new ServerSwitchStatus(
                detectActiveSource(gameRoot).orElse(null),
                isCacheReady(gameRoot, metadata, GameDownloadSource.MAINLAND),
                isCacheReady(gameRoot, metadata, GameDownloadSource.BILIBILI),
                Files.exists(transactionPath(gameRoot)));
    }

    /**
     * Identifies the active installation from the launcher appId and SDK tree.
     *
     * <p>The launcher configuration is preferred when it is available, while the SDK tree is used
     * as a fallback for older or incomplete installations. A conflict between both signals is
     * reported as unknown instead of silently selecting one of them.</p>
     */
    public Optional<GameDownloadSource> detectActiveSource(Path gameDirectory) {
        Path gameRoot = normalizeGameDirectory(gameDirectory);
        Optional<GameDownloadSource> configuredSource = readConfiguredSource(gameRoot);
        Optional<GameDownloadSource> directorySource = detectSourceFromDirectories(gameRoot);
        if (configuredSource.isPresent() && directorySource.isPresent()
                && configuredSource.get() != directorySource.get()) {
            return Optional.empty();
        }
        return configuredSource.isPresent() ? configuredSource : directorySource;
    }

    private Optional<GameDownloadSource> detectSourceFromDirectories(Path gameRoot) {
        if (Files.isDirectory(gameRoot.resolve(BILIBILI_MARKER))) {
            return Optional.of(GameDownloadSource.BILIBILI);
        }
        if (Files.isDirectory(gameRoot.resolve(GLOBAL_SDK_DIRECTORY))) {
            return Optional.of(GameDownloadSource.GLOBAL);
        }
        if (Files.isDirectory(gameRoot.resolve(SDK_DIRECTORY))) {
            return Optional.of(GameDownloadSource.MAINLAND);
        }
        return Optional.empty();
    }

    private Optional<GameDownloadSource> readConfiguredSource(Path gameRoot) {
        Path configPath = gameRoot.resolve(LAUNCHER_DOWNLOAD_CONFIG);
        if (!Files.isRegularFile(configPath)) {
            return Optional.empty();
        }
        try {
            String appId = objectMapper.readTree(configPath.toFile()).path("appId").asText("").trim();
            return switch (appId) {
                case MAINLAND_APP_ID -> Optional.of(GameDownloadSource.MAINLAND);
                case BILIBILI_APP_ID -> Optional.of(GameDownloadSource.BILIBILI);
                case GLOBAL_APP_ID -> Optional.of(GameDownloadSource.GLOBAL);
                default -> Optional.empty();
            };
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Downloads the selected server's required files. If {@code applyToGame} is true, the verified
     * files replace the active files and repair the selected server directly; otherwise they refresh
     * only the corresponding cache payload.
     */
    public ServerSwitchResult redownloadRequiredFiles(Path gameDirectory, GameDownloadSource source,
            boolean applyToGame, ServerSwitchProgressListener listener) throws IOException {
        validateSupportedSource(source);
        Path gameRoot = normalizeGameDirectory(gameDirectory);
        ServerSwitchProgressListener progressListener = listener != null ? listener
                : ServerSwitchProgressListener.noop();
        recoverIfNeeded(gameRoot, progressListener);

        progressListener.onProgress(ServerSwitchPhase.PREPARING, 0, 0, "正在获取资源清单");
        DownloadSelection selection = loadRequiredFiles(gameRoot, source);
        String operationId = UUID.randomUUID().toString();
        Path stagePayload = switchRoot(gameRoot).resolve(".staging").resolve(operationId)
                .resolve(PAYLOAD_DIRECTORY);
        Files.createDirectories(stagePayload);

        try {
            downloadSelection(stagePayload, selection, progressListener);
            materializeUnchangedFiles(gameRoot, stagePayload, selection);
            progressListener.onProgress(ServerSwitchPhase.VERIFYING, 0, selection.totalBytes(), "正在校验下载文件");
            verifyFiles(stagePayload, toCachedFiles(selection.files()));

            if (applyToGame) {
                applyRepair(gameRoot, source, stagePayload, selection, operationId, progressListener);
            } else {
                replaceCache(gameRoot, source, stagePayload, selection, operationId, progressListener);
            }
            return new ServerSwitchResult(source, applyToGame, !applyToGame, selection.version());
        } finally {
            deleteTreeQuietly(stagePayload.getParent());
        }
    }

    /**
     * Deletes only the selected server's switch cache.
     *
     * <p>The active game files are never touched. The cache metadata is cleared after the
     * payload has been removed successfully.</p>
     */
    public void deleteCache(Path gameDirectory, GameDownloadSource source,
            ServerSwitchProgressListener listener) throws IOException {
        validateSupportedSource(source);
        Path gameRoot = normalizeGameDirectory(gameDirectory);
        ServerSwitchProgressListener progressListener = listener != null ? listener
                : ServerSwitchProgressListener.noop();
        recoverIfNeeded(gameRoot, progressListener);

        progressListener.onProgress(ServerSwitchPhase.PREPARING, 0, 0, "正在检查切换缓存");
        Path payload = payloadPath(gameRoot, source);
        progressListener.onProgress(ServerSwitchPhase.APPLYING, 0, 0, "正在删除切换缓存");
        deleteTree(payload);

        CacheMetadata metadata = readMetadata(gameRoot);
        writeMetadata(gameRoot, metadata.withCache(sourceKey(source), CacheEntry.empty(source)));
        progressListener.onProgress(ServerSwitchPhase.APPLYING, 1, 1, "切换缓存已删除");
    }

    /** Switches to a previously downloaded mainland or Bilibili cache payload. */
    public ServerSwitchResult switchTo(Path gameDirectory, GameDownloadSource target,
            ServerSwitchProgressListener listener) throws IOException {
        validateSupportedSource(target);
        Path gameRoot = normalizeGameDirectory(gameDirectory);
        ServerSwitchProgressListener progressListener = listener != null ? listener
                : ServerSwitchProgressListener.noop();
        recoverIfNeeded(gameRoot, progressListener);

        GameDownloadSource source = detectActiveSource(gameRoot)
                .orElseThrow(() -> new IOException("无法识别当前服务器，请先重新下载必要文件进行修复"));
        if (source == target) {
            return new ServerSwitchResult(target, true, isReady(readMetadata(gameRoot), target), "");
        }

        CacheMetadata metadata = readMetadata(gameRoot);
        CacheEntry targetCache = metadata.caches().get(sourceKey(target));
        if (targetCache == null || !targetCache.ready() || !hasRequiredCachedComponents(targetCache.files())) {
            throw new IOException("目标服务器切换缓存不存在或不完整，请先重新下载必要文件");
        }
        Path targetPayload = payloadPath(gameRoot, target);
        progressListener.onProgress(ServerSwitchPhase.VERIFYING, 0, targetCache.totalBytes(), "正在检查目标切换缓存");
        verifyCachedFiles(targetPayload, targetCache.files());

        String operationId = UUID.randomUUID().toString();
        Transaction transaction = beginTransaction(gameRoot, operationId, "SWITCH", source, target);
        try {
            progressListener.onProgress(ServerSwitchPhase.APPLYING, 0, 0, "正在切换服务器文件");
            Path transactionRoot = transactionRoot(gameRoot, operationId);
            moveExistingComponents(gameRoot, payloadPath(gameRoot, source), transactionRoot.resolve("previous-cache"),
                    transaction, gameRoot);
            moveComponents(gameRoot, transactionRoot.resolve("active"), transaction, gameRoot);
            moveComponents(targetPayload, gameRoot, transaction, gameRoot);
            moveComponents(transactionRoot.resolve("active"), payloadPath(gameRoot, source), transaction, gameRoot);

            updateLauncherAppId(gameRoot, target);
            CacheEntry sourceCache = capturePayload(source, payloadPath(gameRoot, source),
                    metadata.caches().get(sourceKey(source)));
            CacheMetadata updatedMetadata = metadata.withCache(sourceKey(source), sourceCache)
                    .withCache(sourceKey(target), targetCache.withReady(false));
            writeMetadata(gameRoot, updatedMetadata);
            completeTransaction(gameRoot, transaction);
            deleteTreeQuietly(transactionRoot);
            return new ServerSwitchResult(target, true, isReady(readMetadata(gameRoot), target), targetCache.version());
        } catch (IOException | RuntimeException e) {
            rollback(gameRoot, transaction, progressListener);
            throw e;
        }
    }

    /** Recovers an interrupted file transaction before a new operation starts. */
    public void recoverIfNeeded(Path gameDirectory, ServerSwitchProgressListener listener) throws IOException {
        Path gameRoot = normalizeGameDirectory(gameDirectory);
        Path path = transactionPath(gameRoot);
        if (!Files.exists(path)) {
            return;
        }
        Transaction transaction = objectMapper.readValue(path.toFile(), Transaction.class);
        if (transaction.completed()) {
            Files.deleteIfExists(path);
            deleteTreeQuietly(transactionRoot(gameRoot, transaction.operationId()));
            return;
        }
        rollback(gameRoot, transaction, listener != null ? listener : ServerSwitchProgressListener.noop());
    }

    private DownloadSelection loadRequiredFiles(Path gameRoot, GameDownloadSource source) throws IOException {
        cn.tealc.wwt.game.resource.internal.legacy.model.LauncherConfig launcher =
                LauncherConfigs.forSource(source);
        GameServerConfig serverConfig = fetchServerConfig(launcher);
        if (serverConfig == null || serverConfig.defaultConfig == null
                || serverConfig.defaultConfig.config == null) {
            throw new IOException("获取服务器更新配置失败");
        }
        List<FileInfo> allFiles = fetchResourceList(serverConfig);
        List<FileInfo> files = allFiles.stream()
                .filter(GameServerSwitchService::isRequiredComponent)
                .sorted(Comparator.comparing(file -> file.path))
                .toList();
        if (files.stream().noneMatch(file -> GAME_EXECUTABLE.toString().replace('\\', '/')
                .equals(normalizeResourcePath(file.path)))) {
            throw new IOException("服务器资源清单缺少 Client-Win64-Shipping.exe");
        }
        if (files.stream().noneMatch(file -> normalizeResourcePath(file.path).startsWith(SDK_PREFIX))) {
            throw new IOException("服务器资源清单缺少 KrPcSdk_Mainland 资源");
        }
        if (files.stream().noneMatch(file -> normalizeResourcePath(file.path).startsWith(ANTI_CHEAT_PREFIX))) {
            throw new IOException("服务器资源清单缺少 AntiCheatExpert 资源");
        }
        List<FileInfo> downloadFiles = selectFilesToDownload(gameRoot, files);
        long totalBytes = downloadFiles.stream().mapToLong(file -> file.size).sum();
        String version = serverConfig.defaultConfig.config.version;
        return new DownloadSelection(serverConfig, files, downloadFiles,
                version != null ? version : "", totalBytes);
    }

    private GameServerConfig fetchServerConfig(
            cn.tealc.wwt.game.resource.internal.legacy.model.LauncherConfig launcher) {
        String json = HttpUtils.getStringWithBackUpUrl(launcher.configUrl, launcher.backUpConfigUrl,
                10, 10_000L, null, null).data;
        if (json == null || json.isBlank()) {
            return null;
        }
        return JsonUtils.safeDeserialize(json, GameServerConfig.class);
    }

    private List<FileInfo> fetchResourceList(GameServerConfig serverConfig) throws IOException {
        String resources = serverConfig.defaultConfig.resources;
        if (resources == null || resources.isBlank()) {
            throw new IOException("服务器配置缺少资源清单地址");
        }
        List<CdnConfig> cdns = serverConfig.defaultConfig.cdnList;
        if (cdns == null || cdns.isEmpty()) {
            throw new IOException("服务器配置缺少 CDN 列表");
        }
        String url = UrlUtils.appendPath(cdns.get(0).url, resources);
        String json = HttpUtils.getString(url, 15_000L).data;
        if (json == null || json.isBlank()) {
            throw new IOException("资源清单为空");
        }
        IndexFile indexFile = JsonUtils.safeDeserialize(json, IndexFile.class);
        if (indexFile == null || indexFile.getResource() == null) {
            throw new IOException("资源清单解析失败");
        }
        return indexFile.getResource();
    }

    private void downloadSelection(Path stagePayload, DownloadSelection selection,
            ServerSwitchProgressListener listener) throws IOException {
        List<DownloadInfo> downloadInfos = ResourceHelper
                .transformFileInfoListToDownloadInfoList(selection.downloadFiles());
        String basePath = selection.serverConfig().defaultConfig.resourcesBasePath;
        CDNDownloadTask task = new CDNDownloadTaskBuilder(downloadInfos,
                selection.serverConfig().defaultConfig.cdnList)
                .withBasePath(basePath)
                .withBaseDestPath(stagePayload.toString())
                .withMaxRetryCount(5)
                .build();
        AtomicReference<DownloadState> finalState = new AtomicReference<>();
        AtomicReference<String> failure = new AtomicReference<>();
        task.setStateCallback((state, message) -> {
            finalState.set(state);
            if (message != null && !message.isBlank()) {
                failure.set(message);
            }
        });
        task.setProgressCallback((completed, total) -> listener.onProgress(ServerSwitchPhase.DOWNLOADING,
                completed, total, "正在下载必要文件"));
        task.run();
        if (finalState.get() != DownloadState.COMPLETE) {
            throw new IOException(failure.get() != null ? failure.get() : "必要文件下载失败");
        }
    }

    private void replaceCache(Path gameRoot, GameDownloadSource source, Path stagePayload,
            DownloadSelection selection, String operationId, ServerSwitchProgressListener listener) throws IOException {
        CacheMetadata metadata = readMetadata(gameRoot);
        Transaction transaction = beginTransaction(gameRoot, operationId, "CACHE", null, source);
        try {
            progressListener(listener, ServerSwitchPhase.APPLYING, "正在更新切换缓存");
            Path transactionRoot = transactionRoot(gameRoot, operationId);
            moveExistingComponents(gameRoot, payloadPath(gameRoot, source), transactionRoot.resolve("previous-cache"),
                    transaction, gameRoot);
            moveComponents(stagePayload, payloadPath(gameRoot, source), transaction, gameRoot);
            CacheEntry entry = CacheEntry.ready(source, selection.version(), appId(source), Instant.now().toString(),
                    toCachedFiles(selection.files()));
            writeMetadata(gameRoot, metadata.withCache(sourceKey(source), entry));
            completeTransaction(gameRoot, transaction);
            deleteTreeQuietly(transactionRoot);
        } catch (IOException | RuntimeException e) {
            rollback(gameRoot, transaction, listener);
            throw e;
        }
    }

    private void applyRepair(Path gameRoot, GameDownloadSource source, Path stagePayload,
            DownloadSelection selection, String operationId, ServerSwitchProgressListener listener) throws IOException {
        CacheMetadata metadata = readMetadata(gameRoot);
        Transaction transaction = beginTransaction(gameRoot, operationId, "REPAIR", source, source);
        try {
            progressListener(listener, ServerSwitchPhase.APPLYING, "正在修复当前服务器文件");
            Path transactionRoot = transactionRoot(gameRoot, operationId);
            moveExistingComponents(gameRoot, payloadPath(gameRoot, source), transactionRoot.resolve("previous-cache"),
                    transaction, gameRoot);
            moveComponents(gameRoot, transactionRoot.resolve("old-active"), transaction, gameRoot);
            moveComponents(stagePayload, gameRoot, transaction, gameRoot);
            updateLauncherAppId(gameRoot, source);
            writeMetadata(gameRoot, metadata.withCache(sourceKey(source), CacheEntry.empty(source)));
            completeTransaction(gameRoot, transaction);
            deleteTreeQuietly(transactionRoot);
        } catch (IOException | RuntimeException e) {
            rollback(gameRoot, transaction, listener);
            throw e;
        }
    }

    private void moveExistingComponents(Path gameRoot, Path fromRoot, Path destinationRoot,
            Transaction transaction, Path journalRoot) throws IOException {
        if (Files.exists(fromRoot.resolve(GAME_EXECUTABLE)) || Files.exists(fromRoot.resolve(SDK_DIRECTORY))
                || Files.exists(fromRoot.resolve(ANTI_CHEAT_DIRECTORY))) {
            moveComponents(fromRoot, destinationRoot, transaction, journalRoot);
        }
    }

    private void moveComponents(Path fromRoot, Path destinationRoot, Transaction transaction,
            Path journalRoot) throws IOException {
        moveIfExists(fromRoot.resolve(GAME_EXECUTABLE), destinationRoot.resolve(GAME_EXECUTABLE), transaction,
                journalRoot);
        moveIfExists(fromRoot.resolve(SDK_DIRECTORY), destinationRoot.resolve(SDK_DIRECTORY), transaction,
                journalRoot);
        moveIfExists(fromRoot.resolve(ANTI_CHEAT_DIRECTORY), destinationRoot.resolve(ANTI_CHEAT_DIRECTORY),
                transaction, journalRoot);
    }

    private void moveIfExists(Path from, Path to, Transaction transaction, Path journalRoot) throws IOException {
        if (!Files.exists(from)) {
            return;
        }
        if (Files.exists(to)) {
            throw new IOException("移动目标已存在：" + to);
        }
        Files.createDirectories(to.getParent());
        moveAtomically(from, to);
        transaction.moves().add(new MoveRecord(journalRoot.relativize(from).toString(),
                journalRoot.relativize(to).toString()));
        writeTransaction(journalRoot, transaction);
    }

    private Transaction beginTransaction(Path gameRoot, String operationId, String action,
            GameDownloadSource source, GameDownloadSource target) throws IOException {
        Files.createDirectories(switchRoot(gameRoot));
        Path metadata = metadataPath(gameRoot);
        Path launcherConfig = gameRoot.resolve("launcherDownloadConfig.json");
        Transaction transaction = new Transaction(operationId, action, source != null ? source.name() : "",
                target != null ? target.name() : "", false, new ArrayList<>(),
                readTextIfExists(metadata), Files.exists(metadata),
                readTextIfExists(launcherConfig), Files.exists(launcherConfig));
        writeTransaction(gameRoot, transaction);
        return transaction;
    }

    private void completeTransaction(Path gameRoot, Transaction transaction) throws IOException {
        transaction.completed = true;
        writeTransaction(gameRoot, transaction);
        Files.deleteIfExists(transactionPath(gameRoot));
    }

    private void rollback(Path gameRoot, Transaction transaction, ServerSwitchProgressListener listener) throws IOException {
        progressListener(listener, ServerSwitchPhase.ROLLING_BACK, "正在回滚服务器文件");
        IOException failure = null;
        for (int index = transaction.moves().size() - 1; index >= 0; index--) {
            MoveRecord move = transaction.moves().get(index);
            Path from = gameRoot.resolve(move.from());
            Path to = gameRoot.resolve(move.to());
            try {
                if (Files.exists(to) && !Files.exists(from)) {
                    Files.createDirectories(from.getParent());
                    moveAtomically(to, from);
                }
            } catch (IOException e) {
                failure = failure == null ? e : failure;
            }
        }
        try {
            restoreText(metadataPath(gameRoot), transaction.metadataBefore(), transaction.metadataExisted());
            restoreText(gameRoot.resolve("launcherDownloadConfig.json"), transaction.launcherConfigBefore(),
                    transaction.launcherConfigExisted());
        } catch (IOException e) {
            failure = failure == null ? e : failure;
        }
        if (failure == null) {
            Files.deleteIfExists(transactionPath(gameRoot));
            deleteTreeQuietly(transactionRoot(gameRoot, transaction.operationId()));
            return;
        }
        throw new IOException("服务器文件回滚失败，请关闭游戏和启动器后重试", failure);
    }

    static List<FileInfo> selectFilesToDownload(Path gameRoot, List<FileInfo> files) throws IOException {
        List<FileInfo> changed = new ArrayList<>();
        for (FileInfo file : files) {
            if (!matchesLocalFile(gameRoot, file)) {
                changed.add(file);
            }
        }
        return List.copyOf(changed);
    }

    private static void materializeUnchangedFiles(Path gameRoot, Path stagePayload,
            DownloadSelection selection) throws IOException {
        java.util.Set<String> downloadedPaths = new java.util.HashSet<>();
        for (FileInfo file : selection.downloadFiles()) {
            downloadedPaths.add(normalizeResourcePath(file.path));
        }
        for (FileInfo file : selection.files()) {
            String resourcePath = normalizeResourcePath(file.path);
            if (downloadedPaths.contains(resourcePath)) {
                continue;
            }
            Path source = resolveResourcePath(gameRoot, resourcePath);
            if (!matchesLocalFile(gameRoot, file)) {
                throw new IOException("公共文件无法从当前游戏目录复用：" + resourcePath);
            }
            Path target = resolveResourcePath(stagePayload, resourcePath);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private static boolean matchesLocalFile(Path root, FileInfo file) throws IOException {
        if (file == null || file.path == null || file.size < 0) {
            return false;
        }
        String resourcePath = normalizeResourcePath(file.path);
        if (!isSafeResourcePath(resourcePath)) {
            return false;
        }
        Path target = resolveResourcePath(root, resourcePath);
        if (!Files.isRegularFile(target) || Files.size(target) != file.size) {
            return false;
        }
        return file.md5 == null || file.md5.isBlank()
                || file.md5.equalsIgnoreCase(md5(target));
    }


    private static CacheEntry capturePayload(GameDownloadSource source, Path payload, CacheEntry previous)
            throws IOException {
        // A source payload is moved, not downloaded, during a switch. Reuse its verified manifest
        // when the file set still matches; a size mismatch falls back to a full MD5 capture.
        if (previous != null && hasRequiredCachedComponents(previous.files())
                && matchesCachedFiles(payload, previous.files())) {
            return previous.withReady(true);
        }
        if (!Files.isRegularFile(payload.resolve(GAME_EXECUTABLE)) || !Files.isDirectory(payload.resolve(SDK_DIRECTORY))
                || !Files.isDirectory(payload.resolve(ANTI_CHEAT_DIRECTORY))) {
            throw new IOException("当前服务器必要文件不完整，无法建立切换缓存");
        }
        List<CachedFile> files = new ArrayList<>();
        try (var paths = Files.walk(payload)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String relativePath = normalizeResourcePath(payload.relativize(path).toString());
                if (relativePath.equals(toResourcePath(GAME_EXECUTABLE)) || relativePath.startsWith(SDK_PREFIX)
                        || relativePath.startsWith(ANTI_CHEAT_PREFIX)) {
                    files.add(new CachedFile(relativePath, Files.size(path), md5(path)));
                }
            }
        }
        if (files.stream().noneMatch(file -> file.path().equals(toResourcePath(GAME_EXECUTABLE)))
                || files.stream().noneMatch(file -> file.path().startsWith(SDK_PREFIX))
                || files.stream().noneMatch(file -> file.path().startsWith(ANTI_CHEAT_PREFIX))) {
            throw new IOException("当前服务器必要文件不完整，无法建立切换缓存");
        }
        return CacheEntry.ready(source, "", appId(source), Instant.now().toString(), files);
    }

    private void updateLauncherAppId(Path gameRoot, GameDownloadSource source) throws IOException {
        Path configPath = gameRoot.resolve("launcherDownloadConfig.json");
        ObjectNode node;
        if (Files.exists(configPath)) {
            var tree = objectMapper.readTree(configPath.toFile());
            node = tree instanceof ObjectNode objectNode ? objectNode : objectMapper.createObjectNode();
        } else {
            node = objectMapper.createObjectNode();
        }
        node.put("appId", appId(source));
        writeJsonAtomically(configPath, node);
    }

    private CacheMetadata readMetadata(Path gameRoot) throws IOException {
        Path metadataPath = metadataPath(gameRoot);
        if (!Files.exists(metadataPath)) {
            return CacheMetadata.empty();
        }
        CacheMetadata metadata = objectMapper.readValue(metadataPath.toFile(), CacheMetadata.class);
        return metadata != null ? metadata.normalize() : CacheMetadata.empty();
    }

    private void writeMetadata(Path gameRoot, CacheMetadata metadata) throws IOException {
        Files.createDirectories(switchRoot(gameRoot));
        writeJsonAtomically(metadataPath(gameRoot), metadata.normalize());
    }

    private void writeTransaction(Path gameRoot, Transaction transaction) throws IOException {
        writeJsonAtomically(transactionPath(gameRoot), transaction);
    }

    private void writeJsonAtomically(Path target, Object value) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), value);
        moveReplacing(temp, target);
    }

    private static void moveAtomically(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to);
        }
    }

    private static void moveReplacing(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Fast switch check; full MD5 verification is reserved for newly downloaded cache files. */
    private static void verifyCachedFiles(Path root, List<CachedFile> files) throws IOException {
        for (CachedFile file : files) {
            Path target = resolveResourcePath(root, file.path());
            if (!Files.isRegularFile(target) || Files.size(target) != file.size()) {
                throw new IOException("缓存文件缺失或大小错误：" + file.path());
            }
        }
    }

    private static void verifyFiles(Path root, List<CachedFile> files) throws IOException {
        verifyCachedFiles(root, files);
        for (CachedFile file : files) {
            Path target = resolveResourcePath(root, file.path());
            if (file.md5() != null && !file.md5().isBlank() && !file.md5().equalsIgnoreCase(md5(target))) {
                throw new IOException("缓存文件校验失败：" + file.path());
            }
        }
    }

    private static boolean matchesCachedFiles(Path root, List<CachedFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return false;
        }
        for (CachedFile file : files) {
            if (file == null || file.path() == null || !isSafeResourcePath(file.path())) {
                return false;
            }
            Path target = resolveResourcePath(root, file.path());
            if (!Files.isRegularFile(target) || Files.size(target) != file.size()) {
                return false;
            }
        }
        return true;
    }

    private static String md5(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前运行环境不支持 MD5", e);
        }
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(file)) {
            int count;
            while ((count = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static boolean isRequiredComponent(FileInfo file) {
        if (file == null || file.path == null || file.size < 0) {
            return false;
        }
        String path = normalizeResourcePath(file.path);
        if (!isSafeResourcePath(path)) {
            return false;
        }
        return path.equals(toResourcePath(GAME_EXECUTABLE)) || path.startsWith(SDK_PREFIX)
                || path.startsWith(ANTI_CHEAT_PREFIX);
    }

    private static List<CachedFile> toCachedFiles(List<FileInfo> files) {
        return files.stream().map(file -> new CachedFile(normalizeResourcePath(file.path),
                file.size, file.md5)).toList();
    }

    private static Path resolveResourcePath(Path root, String path) throws IOException {
        if (!isSafeResourcePath(path)) {
            throw new IOException("资源清单包含非法路径：" + path);
        }
        Path resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root.normalize())) {
            throw new IOException("资源路径越界：" + path);
        }
        return resolved;
    }

    private static boolean isSafeResourcePath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.contains(":")) {
            return false;
        }
        Path normalized = Path.of(path).normalize();
        return !normalized.isAbsolute() && !normalized.startsWith("..")
                && normalized.toString().replace('\\', '/').equals(path);
    }

    private static String normalizeResourcePath(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }

    private static Path normalizeGameDirectory(Path gameDirectory) {
        if (gameDirectory == null) {
            throw new IllegalArgumentException("游戏目录不能为空");
        }
        return gameDirectory.toAbsolutePath().normalize();
    }

    private static void validateSupportedSource(GameDownloadSource source) {
        if (source != GameDownloadSource.MAINLAND && source != GameDownloadSource.BILIBILI) {
            throw new IllegalArgumentException("仅支持官服与 Bilibili 服务器切换");
        }
    }

    private static boolean isReady(CacheMetadata metadata, GameDownloadSource source) {
        CacheEntry entry = metadata.caches().get(sourceKey(source));
        return entry != null && entry.ready() && hasRequiredCachedComponents(entry.files());
    }

    private static boolean isCacheReady(Path gameRoot, CacheMetadata metadata, GameDownloadSource source) {
        if (!isReady(metadata, source)) {
            return false;
        }
        CacheEntry entry = metadata.caches().get(sourceKey(source));
        Path payload = payloadPath(gameRoot, source);
        for (CachedFile file : entry.files()) {
            if (file == null || file.path() == null) {
                return false;
            }
            try {
                Path target = resolveResourcePath(payload, file.path());
                if (!Files.isRegularFile(target) || Files.size(target) != file.size()) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasRequiredCachedComponents(List<CachedFile> files) {
        if (files == null || files.isEmpty()) {
            return false;
        }
        return files.stream().filter(file -> file != null && file.path() != null)
                .anyMatch(file -> file.path().equals(toResourcePath(GAME_EXECUTABLE)))
                && files.stream().filter(file -> file != null && file.path() != null)
                        .anyMatch(file -> file.path().startsWith(SDK_PREFIX))
                && files.stream().filter(file -> file != null && file.path() != null)
                        .anyMatch(file -> file.path().startsWith(ANTI_CHEAT_PREFIX));
    }

    private static Path switchRoot(Path gameRoot) {
        return gameRoot.resolve(CACHE_DIRECTORY);
    }

    private static Path metadataPath(Path gameRoot) {
        return switchRoot(gameRoot).resolve(METADATA_FILE);
    }

    private static Path transactionPath(Path gameRoot) {
        return switchRoot(gameRoot).resolve(TRANSACTION_FILE);
    }

    private static Path transactionRoot(Path gameRoot, String operationId) {
        return switchRoot(gameRoot).resolve(".discard").resolve(operationId);
    }

    private static Path payloadPath(Path gameRoot, GameDownloadSource source) {
        return switchRoot(gameRoot).resolve(sourceKey(source)).resolve(PAYLOAD_DIRECTORY);
    }

    private static String sourceKey(GameDownloadSource source) {
        return switch (source) {
            case MAINLAND -> "cn";
            case BILIBILI -> "bilibili";
            case GLOBAL -> throw new IllegalArgumentException("国际服不支持快速切换");
        };
    }

    private static String appId(GameDownloadSource source) {
        return switch (source) {
            case MAINLAND -> MAINLAND_APP_ID;
            case BILIBILI -> BILIBILI_APP_ID;
            case GLOBAL -> GLOBAL_APP_ID;
        };
    }

    private static String toResourcePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String readTextIfExists(Path path) throws IOException {
        return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
    }

    private static void restoreText(Path path, String value, boolean existed) throws IOException {
        if (existed) {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".rollback");
            Files.writeString(temp, value != null ? value : "", StandardCharsets.UTF_8);
            moveReplacing(temp, path);
        } else {
            Files.deleteIfExists(path);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void deleteTreeQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // The operation journal preserves recovery information when a locked file remains.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup only; stale staging files are harmless.
        }
    }

    private static void progressListener(ServerSwitchProgressListener listener, ServerSwitchPhase phase,
            String detail) {
        (listener != null ? listener : ServerSwitchProgressListener.noop()).onProgress(phase, 0, 0, detail);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CacheMetadata(int schemaVersion, Map<String, CacheEntry> caches) {
        static CacheMetadata empty() {
            return new CacheMetadata(CACHE_SCHEMA_VERSION, Map.of());
        }

        CacheMetadata normalize() {
            return new CacheMetadata(CACHE_SCHEMA_VERSION, caches != null ? Map.copyOf(caches) : Map.of());
        }

        CacheMetadata withCache(String key, CacheEntry entry) {
            Map<String, CacheEntry> values = new HashMap<>(caches != null ? caches : Map.of());
            values.put(key, entry);
            return new CacheMetadata(CACHE_SCHEMA_VERSION, values);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CacheEntry(boolean ready, String source, String version, String appId, String preparedAt,
            List<CachedFile> files) {
        static CacheEntry ready(GameDownloadSource source, String version, String appId, String preparedAt,
                List<CachedFile> files) {
            return new CacheEntry(true, source.name(), version, appId, preparedAt, List.copyOf(files));
        }

        static CacheEntry empty(GameDownloadSource source) {
            return new CacheEntry(false, source.name(), "", "", "", List.of());
        }

        CacheEntry withReady(boolean value) {
            return new CacheEntry(value, source, version, appId, preparedAt, files != null ? List.copyOf(files) : List.of());
        }

        long totalBytes() {
            return files == null ? 0 : files.stream().mapToLong(CachedFile::size).sum();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CachedFile(String path, long size, String md5) {
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Transaction {
        private String operationId;
        private String action;
        private String source;
        private String target;
        private boolean completed;
        private List<MoveRecord> moves;
        private String metadataBefore;
        private boolean metadataExisted;
        private String launcherConfigBefore;
        private boolean launcherConfigExisted;

        @SuppressWarnings("unused")
        private Transaction() {
        }

        private Transaction(String operationId, String action, String source, String target, boolean completed,
                List<MoveRecord> moves, String metadataBefore, boolean metadataExisted,
                String launcherConfigBefore, boolean launcherConfigExisted) {
            this.operationId = operationId;
            this.action = action;
            this.source = source;
            this.target = target;
            this.completed = completed;
            this.moves = moves;
            this.metadataBefore = metadataBefore;
            this.metadataExisted = metadataExisted;
            this.launcherConfigBefore = launcherConfigBefore;
            this.launcherConfigExisted = launcherConfigExisted;
        }

        String operationId() { return operationId; }
        boolean completed() { return completed; }
        List<MoveRecord> moves() { return moves != null ? moves : List.of(); }
        String metadataBefore() { return metadataBefore; }
        boolean metadataExisted() { return metadataExisted; }
        String launcherConfigBefore() { return launcherConfigBefore; }
        boolean launcherConfigExisted() { return launcherConfigExisted; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MoveRecord(String from, String to) {
    }

    private record DownloadSelection(GameServerConfig serverConfig, List<FileInfo> files, List<FileInfo> downloadFiles,
            String version, long totalBytes) {
    }
}
