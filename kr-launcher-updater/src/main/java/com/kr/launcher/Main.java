package com.kr.launcher;

import com.kr.launcher.config.KRAppConfLoader;
import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.flow.ResUpdateModule;
import com.kr.launcher.flow.UpdateFlow;
import com.kr.launcher.model.*;
import com.kr.launcher.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;

/**
 * Main entry point for the KR Launcher Updater.
 *
 * <p>
 * This program replicates the Wuthering Waves (鸣潮) official launcher's
 * game resource update flow, ported from C# to Java. It handles the complete
 * update lifecycle: checking for updates, downloading game files (with CDN
 * rotation and chunk-based parallel download), applying binary patches via
 * HPatchZ, moving files to the game directory, and cleaning up redundant files.
 *
 * <h2>Overall Update Flow</h2>
 * 
 * <pre>
 *   1. Check Update (CheckUpdateFlow)
 *      - Fetch server config from configUrl
 *      - Compare local version vs server version
 *      - Determine update state: UP_TO_DATE, NEED_DOWNLOAD, DOWNLOADING,
 *        PRE_DOWNLOAD, REPAIRING, ROLLBACK
 *
 *   2. Execute Update (UpdateFlow)
 *      a. Prepare (PrepareTask)
 *         - Download index file from CDN
 *         - Check existing files (size + MD5) to build download list
 *         - Detect interrupted downloads (chunk dir exists) for resume
 *
 *      b. Download (CDNDownloadTask -> DownloadTask)
 *         - Parallel download (default 4 workers) from primary/backup CDNs
 *         - Large files split into chunks (each chunk is a Range request)
 *         - Per-chunk MD5 verification + merged file MD5 verification
 *         - 3-tier retry: per-chunk retry -> outer retry (re-download all
 *           chunks) -> CDN rotation (backup CDNs)
 *
 *      c. Apply (PatchApplyTask / GroupApplyTask / ZipApplyTask / NopApply)
 *         - If patchInfos present: launch HPatchZ.exe to apply .krdiff patches
 *         - HPatchZ writes patched files to krdiff_temp/ directory
 *         - Post-patch MD5 verification of all patched files
 *         - Failed files trigger re-download of those specific files
 *
 *      d. Move (MoveFileTask)
 *         - Move downloaded/patched files from cache to game directory
 *         - Update GameResourceRecord (local index of installed files)
 *
 *      e. Delete (DeleteRedundantFiles)
 *         - Delete files listed in indexFile.deleteFiles
 *         - Update GameResourceRecord to remove deleted entries
 *
 *      f. Cleanup
 *         - Delete the download cache directory
 *         - Clear the downloading/moving state config files
 * </pre>
 *
 * <h2>Progress Reporting</h2>
 * <p>
 * The progress callback uses state numbers to indicate the current phase:
 * <ul>
 * <li>state=1: DOWNLOAD - overall download progress (bytes + speed + ETA)</li>
 * <li>state=2: APPLY - patch apply progress (bytes + file count)</li>
 * <li>state=3: VERIFY - post-patch MD5 verification (bytes only)</li>
 * <li>state=4: RE_DOWNLOAD - re-download of failed patch files (bytes + speed +
 * ETA)</li>
 * <li>state=5: MOVE - file moving progress (file count)</li>
 * <li>state=6: DELETE - redundant file deletion progress (file count)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * 
 * <pre>
 *   java -jar kr-launcher-updater.jar [KRApp.conf|config.json] [game-dir]
 * </pre>
 * <ul>
 * <li>If no config path is provided, looks for KRApp.conf next to the JAR.</li>
 * <li>If no game directory is provided, creates "Wuthering Waves Game" next
 * to the JAR.</li>
 * </ul>
 *
 * <h2>Process Interruption Recovery</h2>
 * <p>
 * If the process is killed during update, the next launch detects the
 * interrupted state via config files:
 * <ul>
 * <li>Downloading config exists (state=DOWNLOADING) - resume download,
 * reuse existing chunks</li>
 * <li>Moving config exists (state=MOVING) - re-run MoveFileTask from
 * download cache</li>
 * <li>Repairing config exists (state=REPAIRING) - full MD5 verification
 * and re-download of corrupted files</li>
 * </ul>
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * Program entry point. Resolves configuration and game directory, then
     * dispatches to the appropriate update flow based on the current state.
     *
     * @param args optional [config-path] [game-dir]
     */
    public static void main(String[] args) {
        log.info("=== KR Launcher Updater ===");
        log.info("Replicating Wuthering Waves game resource update flow");

        try {
            LauncherConfig config = null;
            String gameDir = null;

            // Resolve the JAR's directory. All relative paths (KRApp.conf,
            // game directory, hpatchz.exe) are resolved relative to this.
            String jarDir = getJarDir();

            if (args.length > 0) {
                String firstArg = args[0];

                // Two config formats are supported:
                // 1. KRApp.conf - official launcher format (Base64 + XOR 99 encoded)
                // 2. config.json - plain JSON with LauncherConfig fields
                if (firstArg.endsWith(".conf") || firstArg.contains("KRApp")) {
                    config = KRAppConfLoader.loadFromKRAppConf(firstArg);
                    if (config == null) {
                        log.error("Failed to load KRApp.conf from: {}", firstArg);
                        return;
                    }
                    gameDir = (args.length > 1) ? args[1] : null;
                } else {
                    config = loadJsonConfig(firstArg);
                    if (config == null) {
                        log.error("Failed to load config from: {}", firstArg);
                        return;
                    }
                    gameDir = (args.length > 1) ? args[1] : null;
                }
            } else {
                // No arguments: auto-discover KRApp.conf next to the JAR.
                // This allows the user to simply double-click or run the JAR
                // without any arguments if KRApp.conf is in the same directory.
                log.info("No config specified, looking for KRApp.conf next to JAR...");
                String defaultPath = PathUtils.combine(jarDir, "KRApp.conf");
                if (Files.exists(Path.of(defaultPath))) {
                    log.info("Found KRApp.conf at: {}", defaultPath);
                    config = KRAppConfLoader.loadFromKRAppConf(defaultPath);
                }
                if (config == null) {
                    log.error(
                            "KRApp.conf not found next to JAR. Usage: java -jar kr-launcher-updater.jar <KRApp.conf|config.json> [game-dir]");
                    return;
                }
            }

            // Resolve game directory:
            // - If not provided: default to "Wuthering Waves Game" next to JAR
            // - If relative: resolve relative to JAR directory (not working dir)
            // - If absolute: use as-is
            // The game directory is where files are ultimately moved to after
            // download and patch are complete.
            if (gameDir == null || gameDir.isEmpty()) {
                gameDir = PathUtils.combine(jarDir, "Wuthering Waves Game");
            } else if (!Paths.get(gameDir).isAbsolute()) {
                gameDir = PathUtils.combine(jarDir, gameDir);
            }

            // Ensure the game directory exists before proceeding.
            Files.createDirectories(Path.of(gameDir));

            // ResourceConfigManager holds the launcher config and game directory
            // path. It provides paths for:
            // - gameDirPath: the game installation directory
            // - gameCacheDirPath: {gameDir}/launcherDownload (download cache)
            // - launcherConfig: the parsed LauncherConfig (URLs, appId, etc.)
            ResourceConfigManager configManager = new ResourceConfigManager(config, gameDir);

            log.info("Game ID: {}", config.gameId);
            log.info("App ID: {}", config.appId);
            log.info("Game directory: {}", configManager.gameDirPath);
            log.info("Config URL: {}", config.configUrl);
            log.info("Backup Config URL: {}", config.backUpConfigUrl);

            // Execute the update flow.
            runUpdateFlow(configManager);

        } catch (Exception e) {
            log.error("Fatal error", e);
        }
    }

    /**
     * Step 1: Check for updates, then dispatch to the appropriate handler
     * based on the current update state.
     *
     * <p>
     * The check update flow:
     * <ol>
     * <li>Fetch server config from configUrl (contains CDN list, version
     * info, index file URL)</li>
     * <li>Compare local version (from GameResourceRecord) with server version</li>
     * <li>Determine the update state and build UpdateInfo (download size,
     * CDN URLs, patch info)</li>
     * </ol>
     *
     * <p>
     * State dispatch:
     * <ul>
     * <li>UP_TO_DATE: optionally handle pre-download</li>
     * <li>NEED_DOWNLOAD / DOWNLOADING: normal update (download + apply + move)</li>
     * <li>PRE_DOWNLOAD: apply a previously completed pre-download</li>
     * <li>REPAIRING: full file verification and re-download of corrupted files</li>
     * <li>ROLLBACK: local version is newer than server (not implemented)</li>
     * </ul>
     */
    private static void runUpdateFlow(ResourceConfigManager configManager) {
        log.info("\n=== Step 1: Check Update ===");
        ResUpdateModule updateModule = new ResUpdateModule(configManager);

        // ResUpdateModule.checkUpdate() is synchronous internally (calls
        // CheckUpdateFlow.exec()) and caches
        // stateInfo/updateInfo/predownloadUpdateInfo.
        // The callback is invoked once with the result.
        final CheckUpdateResult[] resultHolder = new CheckUpdateResult[1];
        updateModule.checkUpdate(result -> resultHolder[0] = result);

        CheckUpdateResult checkResult = resultHolder[0];
        if (!checkResult.succ) {
            log.error("Check update failed: {} ({})", checkResult.errorMessage, checkResult.errorCode);
            return;
        }

        ResStateInfo stateInfo = checkResult.stateInfo;
        log.info("Update state: {} ({})", getStateName(stateInfo.state), stateInfo.state);
        log.info("Local version: '{}'", stateInfo.usingVersion);
        log.info("Server version: '{}'", stateInfo.newVersion);

        if (checkResult.updateInfo != null) {
            log.info("Update type: {}", checkResult.updateInfo.updateType);
            log.info("Update size: {}", ByteUtils.byteConvert(checkResult.updateInfo.size));
        }

        if (stateInfo.enablePreDownload) {
            log.info("Pre-download available: version={}, size={}",
                    checkResult.predownloadUpdateInfo != null ? checkResult.predownloadUpdateInfo.version : "N/A",
                    checkResult.predownloadUpdateInfo != null
                            ? ByteUtils.byteConvert(checkResult.predownloadUpdateInfo.size)
                            : "N/A");
            log.info("Pre-download status: {}",
                    stateInfo.preDownloadComplete ? "COMPLETE (ready to apply)" : "NOT STARTED");
        }

        // Dispatch based on the update state. Each state corresponds to a
        // different entry point in the update lifecycle.
        switch (stateInfo.state) {
            case ResStateInfo.STATE_UP_TO_DATE:
                // Game is already at the latest version. If pre-download is
                // available, offer to start or apply it.
                if (stateInfo.enablePreDownload && checkResult.predownloadUpdateInfo != null) {
                    handlePreDownload(configManager, updateModule, stateInfo, checkResult.predownloadUpdateInfo);
                } else {
                    log.info("\nGame is already up to date!");
                }
                break;

            case ResStateInfo.STATE_NEED_DOWNLOAD:
                // Normal update: local version is older, need to download
                // the diff/full package and apply it.
            case ResStateInfo.STATE_DOWNLOADING:
                // Interrupted download: a previous download was in progress.
                // PrepareTask will detect existing chunk files and resume.
            case ResStateInfo.STATE_PRE_DOWNLOAD:
                // C# KRCheckUpdateFlow.cs:467-484 sets State=3 in two scenarios:
                // 1. usingVersion < serverVersion AND downloadingVersion ==
                // serverVersion AND downloadingConfig.isPreDownload — a
                // pre-download for the now-current server version was
                // completed. PrepareTask detects cached files and skips
                // re-downloading.
                // 2. usingVersion < serverVersion AND downloadingVersion !=
                // serverVersion — no matching download in progress. This
                // is effectively a normal update (State=3 is used as a
                // catch-all in C#). PrepareTask finds no cached files and
                // downloads everything.
                // In both cases, checkResult.updateInfo (built from
                // defaultConfig, NOT preDownloadConfig) is used.
                log.info("\n=== Step 2: Execute Update ===");
                executeUpdate(configManager, stateInfo, checkResult.updateInfo);
                break;

            case ResStateInfo.STATE_REPAIRING:
                // A previous move operation failed (FILE_MISSING). Run full
                // MD5 verification of all game files and re-download any
                // corrupted or missing files.
                log.info("\n=== Repair mode ===");
                executeUpdate(configManager, stateInfo, checkResult.updateInfo);
                break;

            case ResStateInfo.STATE_ROLLBACK:
                // Local version is newer than server version. This shouldn't
                // happen in normal operation. Rollback is not implemented.
                log.info("\nLocal version is newer than server. Rollback not implemented.");
                break;

            default:
                log.warn("Unknown state: {}", stateInfo.state);
        }
    }

    /**
     * Handle pre-download flow when game is UP_TO_DATE and pre-download is
     * available.
     *
     * <p>
     * Two scenarios:
     * <ul>
     * <li>Pre-download COMPLETE: ask user whether to apply it. Applying
     * runs UpdateFlow with STATE_PRE_DOWNLOAD, which triggers
     * PrepareTask to detect pre-downloaded files and skip download,
     * then MoveFileTask moves them to the game directory.</li>
     * <li>Pre-download NOT STARTED: ask user whether to start it.
     * Pre-download downloads files to cache without applying them.</li>
     * </ul>
     */
    private static void handlePreDownload(ResourceConfigManager configManager, ResUpdateModule updateModule,
            ResStateInfo stateInfo, UpdateInfo predownloadUpdateInfo) {
        if (stateInfo.preDownloadComplete) {
            log.info("\nPre-download version {} is complete and ready to apply.", predownloadUpdateInfo.version);
            if (askUser("Apply pre-download now? (y/n)")) {
                // Apply pre-download: run update flow with predownloadUpdateInfo.
                // PrepareTask will detect pre-downloaded files in cache and skip
                // re-downloading, then MoveFileTask moves them to the game directory.
                ResStateInfo applyStateInfo = new ResStateInfo();
                applyStateInfo.usingVersion = stateInfo.usingVersion;
                applyStateInfo.newVersion = predownloadUpdateInfo.version;
                applyStateInfo.state = ResStateInfo.STATE_PRE_DOWNLOAD;
                log.info("\n=== Applying Pre-download (v{}) ===", predownloadUpdateInfo.version);
                executeUpdate(configManager, applyStateInfo, predownloadUpdateInfo);
            } else {
                log.info("Pre-download apply skipped by user.");
            }
        } else {
            log.info("\nPre-download version {} is available but not yet downloaded.", predownloadUpdateInfo.version);
            if (askUser("Start pre-download now? (y/n)")) {
                log.info("\n=== Starting Pre-download (v{}) ===", predownloadUpdateInfo.version);
                executePreDownload(updateModule);
            } else {
                log.info("Pre-download skipped by user.");
            }
        }
    }

    /**
     * Execute pre-download flow using ResUpdateModule.preDownload().
     * Pre-download downloads files to the cache directory without applying
     * them. The files are applied later when the user chooses to update.
     */
    private static void executePreDownload(ResUpdateModule updateModule) {
        updateModule.preDownload(
                (state, progressInfo) -> {
                    int percent = progressInfo.progressPercentage;
                    String stateName = getUpdateStateName(state);
                    log.info("[PreDownload:{}] {}/{} ({}%)", stateName,
                            ByteUtils.byteConvert(progressInfo.completedSize),
                            ByteUtils.byteConvert(progressInfo.totalSize), percent);
                },
                result -> {
                    if (result.success) {
                        log.info("\n=== Pre-download completed successfully! ===");
                    } else {
                        log.error("\n=== Pre-download failed ===");
                        log.error("Error: {} ({})", result.errorMessage, result.errorCode);
                    }
                });
    }

    /**
     * Prompt the user with a yes/no question on stdin.
     */
    private static boolean askUser(String prompt) {
        System.out.print(prompt + " ");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line = reader.readLine();
            return line != null && (line.trim().equalsIgnoreCase("y") || line.trim().equalsIgnoreCase("yes"));
        } catch (Exception e) {
            log.warn("Failed to read user input: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Execute the full update pipeline: prepare -> download -> apply -> move
     * -> delete -> cleanup.
     *
     * <p>
     * This method sets up the progress callback (which formats and logs
     * progress events from UpdateFlow) and the completion callback, then
     * starts UpdateFlow.exec() which runs synchronously.
     *
     * <p>
     * The progress callback receives (state, completed, total,
     * completedCount, totalCount) where:
     * <ul>
     * <li>state 1 (DOWNLOAD): completed/total are bytes; speed and ETA are
     * calculated using a 1-second windowed speed monitor (matching
     * C# KRSpeedMonitor). completedCount/totalCount are unused.</li>
     * <li>state 2 (APPLY): completed/total are bytes (patch bytes from
     * HPatchZ); completedCount/totalCount are file counts. Both are
     * displayed.</li>
     * <li>state 3 (VERIFY): completed/total are bytes (MD5 check progress);
     * completedCount/totalCount are unused (C# passes 0, 0).</li>
     * <li>state 4 (RE_DOWNLOAD): same format as state 1, for re-downloading
     * failed patch files.</li>
     * <li>state 5 (MOVE): completed/total are file counts (not bytes).</li>
     * <li>state 6 (DELETE): completed/total are file counts (not bytes).</li>
     * </ul>
     *
     * <h3>Speed Monitor Algorithm (matches C# KRSpeedMonitor)</h3>
     * <p>
     * The speed is calculated using a 1-second window with caching:
     * <ul>
     * <li>On the first callback: initialize baseline time/bytes, return 0.</li>
     * <li>If &gt; 1000ms since last calculation: compute speed =
     * (bytes_delta) / (elapsed_seconds). Update baseline. If speed > 0,
     * cache it; otherwise return 0 (don't cache zero speed).</li>
     * <li>If &le; 1000ms since last calculation: return the cached speed
     * from the last calculation. This prevents noisy fluctuations when
     * callbacks fire more frequently than once per second.</li>
     * </ul>
     *
     * <h3>ETA Calculation (matches C# KRUpdateFlow.OnDownloadProgressChanged)</h3>
     * <p>
     * ETA = remaining_bytes / speed (in seconds). If speed &le; 0 or ETA
     * exceeds MAX_REMAINING_TIME (359999 seconds ~ 100 hours), ETA is capped
     * at 359999.
     */
    private static void executeUpdate(ResourceConfigManager configManager, ResStateInfo stateInfo,
            UpdateInfo updateInfo) {
        UpdateFlow updateFlow = new UpdateFlow(configManager);
        updateFlow.setSkipMoveFile(false);

        // Speed monitor state: tracks the last speed calculation time, bytes
        // at that time, and the cached speed value. Matches C# KRSpeedMonitor
        // which uses _speedNotifyIntervalMillis = 1000 (1 second window).
        final long[] lastSpeedNotifyTime = { 0L };
        final long[] lastSpeedNotifyBytes = { 0L };
        final double[] lastSpeed = { 0.0 };

        updateFlow.setProgressCallback((state, completed, total, completedCount, totalCount) -> {
            int percent = (total > 0) ? (int) ((double) completed / total * 100) : 0;
            String stateName = getUpdateStateName(state);

            if (state == 1 || state == 4) {
                // ---- Download / Re-download progress ----
                // Displays: [DOWNLOAD] 1.2 GB/104.5 GB (1%) | 30.5 MB/s, ETA 57m14s
                //
                // Speed calculation uses a 1-second windowed algorithm matching
                // C# KRSpeedMonitor. See method javadoc for details.
                long now = System.currentTimeMillis();
                double speedBps;
                if (lastSpeedNotifyTime[0] == 0L) {
                    // First callback: initialize baseline, speed = 0.
                    lastSpeedNotifyTime[0] = now;
                    lastSpeedNotifyBytes[0] = completed;
                    speedBps = 0.0;
                } else {
                    long elapsedMs = now - lastSpeedNotifyTime[0];
                    if (elapsedMs > 1000) {
                        // More than 1 second since last calculation: recompute.
                        long deltaBytes = completed - lastSpeedNotifyBytes[0];
                        speedBps = deltaBytes * 1000.0 / elapsedMs;
                        lastSpeedNotifyTime[0] = now;
                        lastSpeedNotifyBytes[0] = completed;
                        if (speedBps > 0) {
                            lastSpeed[0] = speedBps;
                        } else {
                            // Speed <= 0: return 0, don't update cache.
                            // Matches C# KRSpeedMonitor.cs:37-42.
                            speedBps = 0.0;
                        }
                    } else {
                        // Within 1 second: return cached speed.
                        // Matches C# KRSpeedMonitor.cs:44.
                        speedBps = lastSpeed[0];
                    }
                }

                // ETA = remaining_bytes / speed (seconds).
                // Capped at MAX_REMAINING_TIME = 359999s (~100 hours).
                // Matches C# KRUpdateFlow.cs:247-251 and KRUpdateProgressInfo.cs:29-36.
                long remainingBytes = total - completed;
                long etaSeconds;
                if (speedBps > 0) {
                    etaSeconds = (long) (remainingBytes / speedBps);
                } else {
                    etaSeconds = 359999L;
                }
                if (etaSeconds > 359999L || etaSeconds < 0) {
                    etaSeconds = 359999L;
                }

                log.info("[{}] {}/{} ({}%) | {}/s, ETA {}",
                        stateName,
                        ByteUtils.byteConvert(completed),
                        ByteUtils.byteConvert(total),
                        percent,
                        ByteUtils.byteConvert((long) speedBps),
                        formatEta(etaSeconds * 1000));
            } else if (state == 2) {
                // ---- Apply (patch) progress ----
                // Displays: [APPLY] 1.2GB/27.8GB (4%) | 12/285 files
                //
                // Progress comes from HPatchZ via shared memory. HPatchZ reports:
                // - patchedCurrBytes / patchTotalBytes (bytes being patched)
                // - patchedFileCount / fileTotalCount (files being patched)
                // C# KRPatchApplyTask.cs:65-71 forwards both as state=2.
                log.info("[{}] {}/{} ({}%) | {}/{} files",
                        stateName,
                        ByteUtils.byteConvert(completed),
                        ByteUtils.byteConvert(total),
                        percent,
                        completedCount,
                        totalCount);
            } else if (state == 3) {
                // ---- MD5 verification progress ----
                // Displays: [VERIFY] 1.2GB/27.8GB (4%)
                //
                // Post-patch MD5 check of all patched files. Byte-based only
                // (no file count). C# KRPatchApplyTask.cs:94 reports state=3
                // with (completedSize, totalSize, 0L, 0L).
                log.info("[{}] {}/{} ({}%)", stateName,
                        ByteUtils.byteConvert(completed), ByteUtils.byteConvert(total), percent);
            } else if (state == 5 || state == 6) {
                // ---- Move / Delete progress ----
                // Displays: [MOVE] 12/285 files (4%)
                // Displays: [DELETE] 3/10 files (30%)
                //
                // Both are file-count based (not byte-based). Move reports
                // after each file is moved; delete reports after each file is
                // deleted. No throttling (every file is logged).
                // C# KRMoveFileTask.cs:99 reports after each file move.
                // C# DeleteRedundantFiles has no progress reporting (Java adds it).
                log.info("[{}] {}/{} files ({}%)", stateName, completed, total, percent);
            } else {
                // Fallback for any other state.
                log.info("[{}] {}/{} ({}%)", stateName,
                        ByteUtils.byteConvert(completed), ByteUtils.byteConvert(total), percent);
            }
        });

        // Completion callback: invoked once when UpdateFlow finishes (success
        // or failure). The result contains success flag, error code, error
        // message, and state (0=normal, 2=apply/disk error, 4=re-download
        // error, 5=move/delete error).
        updateFlow.setCompleteCallback(result -> {
            if (result.success) {
                log.info("\n=== Update completed successfully! ===");
            } else {
                log.error("\n=== Update failed ===");
                log.error("Error: {} ({})", result.errorMessage, result.errorCode);
            }
        });

        // Start the update flow. This call blocks until the entire pipeline
        // (prepare -> download -> apply -> move -> delete -> cleanup) completes.
        updateFlow.exec(stateInfo, updateInfo);
    }

    /**
     * Load a plain JSON config file (alternative to KRApp.conf).
     * The JSON should contain LauncherConfig fields: configUrl, backUpConfigUrl,
     * gameId, appId, appKey, gameExeName, gameDirName.
     */
    private static LauncherConfig loadJsonConfig(String configPath) {
        try {
            String json = Files.readString(Path.of(configPath));
            return JsonUtils.safeDeserialize(json, LauncherConfig.class);
        } catch (Exception e) {
            log.error("Failed to load config from: {}", configPath, e);
            return null;
        }
    }

    /**
     * Get the directory containing the running JAR (or classes directory when
     * running from IDE). Falls back to user.dir if the location cannot be
     * resolved.
     *
     * <p>
     * This is used to resolve relative paths for:
     * <ul>
     * <li>KRApp.conf (when no config path is provided)</li>
     * <li>Game directory (when not provided, defaults to "Wuthering Waves
     * Game")</li>
     * <li>hpatchz.exe (found via PathUtils search starting from game dir)</li>
     * </ul>
     */
    private static String getJarDir() {
        try {
            return Paths.get(
                    Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().toString();
        } catch (Exception e) {
            log.warn("Failed to resolve JAR directory, falling back to user.dir", e);
            return System.getProperty("user.dir");
        }
    }

    /**
     * Get the human-readable name for a check-update state.
     * These states are from ResStateInfo and represent the high-level
     * update lifecycle state (different from the progress callback states).
     *
     * @param state ResStateInfo.STATE_* constant
     * @return human-readable name
     */
    private static String getStateName(int state) {
        return switch (state) {
            case 0 -> "NEED_DOWNLOAD";
            case 1 -> "DOWNLOADING";
            case 2 -> "UP_TO_DATE";
            case 3 -> "PRE_DOWNLOAD";
            case 4 -> "REPAIRING";
            case 5 -> "ROLLBACK";
            default -> "UNKNOWN(" + state + ")";
        };
    }

    /**
     * Format a duration in milliseconds as a human-readable string.
     * Examples: "45s", "5m30s", "2h15m".
     * Returns "--" for invalid or extremely large values.
     */
    private static String formatEta(long millis) {
        if (millis < 0 || millis > 999_999_999L)
            return "--";
        long seconds = millis / 1000;
        if (seconds < 60)
            return seconds + "s";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60)
            return minutes + "m" + remainingSeconds + "s";
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h" + remainingMinutes + "m";
    }

    /**
     * Get the human-readable name for a progress callback state.
     * These states are reported by UpdateFlow during the update pipeline
     * (different from the check-update states in getStateName).
     *
     * <p>
     * State mapping:
     * <ul>
     * <li>1 = DOWNLOAD (overall download progress)</li>
     * <li>2 = APPLY (HPatchZ patch apply progress)</li>
     * <li>3 = VERIFY (post-patch MD5 verification)</li>
     * <li>4 = RE_DOWNLOAD (re-download of failed patch files)</li>
     * <li>5 = MOVE (moving files to game directory)</li>
     * <li>6 = DELETE (deleting redundant files)</li>
     * </ul>
     */
    private static String getUpdateStateName(int state) {
        return switch (state) {
            case 1 -> "DOWNLOAD";
            case 2 -> "APPLY";
            case 3 -> "VERIFY";
            case 4 -> "RE_DOWNLOAD";
            case 5 -> "MOVE";
            case 6 -> "DELETE";
            default -> "STATE_" + state;
        };
    }
}
