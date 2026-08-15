package cn.tealc.wwt.game.resource.internal.legacy.patch;

import cn.tealc.wwt.game.resource.internal.legacy.model.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;

/**
 * Manages the HPatchZ external process for applying binary patches.
 * Launches HPatchZ.exe, captures stdout/stderr for progress, handles exit
 * codes.
 *
 * HPatchZ command line:
 * HPatchZ.exe {oldPath} {diffPath} {newPath} -f -d -k-{sharedMemoryKey}
 *
 * Exit codes:
 * 0 = success
 * 24 = disk not enough space
 * 25 = permission denied
 * 27 = file occupied
 * 28 = permission denied (alt)
 * 30 = file occupied (alt)
 *
 * Corresponds to KRResourcePatchExecutor.cs
 */
public class PatchExecutor {
    private static final Logger log = LoggerFactory.getLogger(PatchExecutor.class);

    // Exit codes from C# KRUpdateResult
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_DISK_NOT_ENOUGH_SPACE = 24;
    public static final int EXIT_PERMISSION_DENIED_1 = 25;
    public static final int EXIT_FILE_OCCUPANCY_1 = 27;
    public static final int EXIT_PERMISSION_DENIED_2 = 28;
    public static final int EXIT_FILE_OCCUPANCY_2 = 30;

    // Error codes matching C# KRResourceError
    public static final int ERROR_CODE_FILE_OCCUPANCY = UpdateResult.ERROR_CODE_FILE_OCCUPANCY;
    public static final int ERROR_CODE_DISK_NOT_ENOUGH_SPACE = UpdateResult.ERROR_CODE_DISK_NOT_ENOUGH_SPACE;
    public static final int ERROR_CODE_FILE_PERMISSION_DENY = UpdateResult.ERROR_CODE_FILE_PERMISSION_DENY;
    public static final int ERROR_CODE_UNKNOWN = 7002015;
    public static final int ERROR_CODE_PATCH_PROCESS_IS_RUNNING = 7002016;
    public static final int ERROR_CODE_START_PATCH_FAILED = 7002019;

    private Process process;
    private SharedMemory sharedMemory;
    private String sharedMemoryKey;
    private ScheduledExecutorService progressTimer;
    private volatile boolean running = false;

    public PatchProgressCallback progressCallback;
    public PatchFinishedCallback finishedCallback;

    // Executor for marshaling callbacks to a specific thread.
    // Corresponds to C# KRResourcePatchExecutor._synchronizationContext.Post.
    // C# always Post (async). Default to a single-thread executor so callbacks
    // are async and serialized, matching C# behavior. Callers can override via
    // setCallbackExecutor() to use a UI thread executor.
    private java.util.concurrent.Executor callbackExecutor = java.util.concurrent.Executors
            .newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "PatchExecutor-callback");
                t.setDaemon(true);
                return t;
            });

    /**
     * Set the executor used to marshal callbacks to a specific thread.
     * Corresponds to C# capturing SynchronizationContext at StartAsyncEx time.
     * Pass a single-threaded executor (e.g., Executors.newSingleThreadExecutor())
     * or the UI thread executor to ensure callbacks fire on the right thread.
     */
    public void setCallbackExecutor(java.util.concurrent.Executor executor) {
        this.callbackExecutor = executor;
    }

    /**
     * Start the HPatchZ patch process asynchronously with wrapper logic.
     * Corresponds to C# KRResourcePatchExecutor.StartAsyncEx (line 55-69).
     *
     * If StartAsync fails (and the error is not PATCH_PROCESS_IS_RUNNING),
     * invokes finishedCallback with START_PATCH_PROCESS_FAILED so the caller
     * does not hang waiting for a callback that will never fire.
     */
    public void startAsyncEx(String hpatchzExePath, String oldPath, String diffPath,
            String newPath, long reportProgressIntervalMs) {
        PatchStartResult result = startAsync(hpatchzExePath, oldPath, diffPath, newPath, reportProgressIntervalMs);
        if (result.errorCode != 0 && result.errorCode != ERROR_CODE_PATCH_PROCESS_IS_RUNNING) {
            log.error("StartAsyncEx: startAsync failed with code {}, invoking finishedCallback", result.errorCode);
            if (finishedCallback != null) {
                if (callbackExecutor != null) {
                    callbackExecutor.execute(() -> {
                        if (finishedCallback != null) {
                            finishedCallback.onFinished(ERROR_CODE_START_PATCH_FAILED);
                        }
                    });
                } else {
                    finishedCallback.onFinished(ERROR_CODE_START_PATCH_FAILED);
                }
            }
        }
    }

    /**
     * Start the HPatchZ patch process asynchronously.
     *
     * @param hpatchzExePath           path to HPatchZ.exe
     * @param oldPath                  path to the old file/directory (current game
     *                                 files)
     * @param diffPath                 path to the .krdiff file (downloaded patch)
     * @param newPath                  path to output directory (temp krdiff_temp/)
     * @param reportProgressIntervalMs how often to read progress (ms)
     * @return PatchStartResult with error code (0 = success)
     */
    public PatchStartResult startAsync(String hpatchzExePath, String oldPath, String diffPath,
            String newPath, long reportProgressIntervalMs) {
        // C# KRResourcePatchExecutor.cs:74: checks _process != null &&
        // !_process.HasExited. Using process.isAlive() is more accurate than
        // the running flag, which may be stale if the exit handler hasn't run
        // yet.
        if (process != null && process.isAlive()) {
            log.warn("PatchExecutor is already running");
            return new PatchStartResult(ERROR_CODE_PATCH_PROCESS_IS_RUNNING, "PatchExecutor is already running");
        }

        // Create shared memory key
        long pid = ProcessHandle.current().pid();
        sharedMemoryKey = "launcher_shared_memory_" + pid + "_"
                + java.util.UUID.randomUUID().toString().replace("-", "");

        // Initialize shared memory (file-based for cross-platform)
        sharedMemory = new SharedMemory();
        int shmResult = sharedMemory.create(sharedMemoryKey, 4096);
        if (shmResult != 0) {
            return new PatchStartResult(shmResult, "Failed to create shared memory");
        }

        // Build HPatchZ command
        // HPatchZ.exe {oldPath} {diffPath} {newPath} -f -d -k-{sharedMemoryKey}
        String[] command = {
                hpatchzExePath,
                oldPath,
                diffPath,
                newPath,
                "-f",
                "-d",
                "-k-" + sharedMemoryKey
        };

        log.info("Starting HPatchZ: oldPath={}, diffPath={}, newPath={}", oldPath, diffPath, newPath);
        log.info("HPatchZ command: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            process = pb.start();
            running = true;

            // Read stdout in background
            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("HPatchZ output: {}", line);
                    }
                } catch (Exception e) {
                    log.warn("Error reading HPatchZ stdout", e);
                }
            }, "hpatchz-stdout");
            stdoutReader.setDaemon(true);
            stdoutReader.start();

            // Read stderr in background
            Thread stderrReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("HPatchZ error: {}", line);
                    }
                } catch (Exception e) {
                    log.warn("Error reading HPatchZ stderr", e);
                }
            }, "hpatchz-stderr");
            stderrReader.setDaemon(true);
            stderrReader.start();

            // Start periodic progress reporting
            startProgressReporting(reportProgressIntervalMs);

            // Start exit handler thread
            Thread exitHandler = new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    running = false;
                    stopProgressReporting();

                    // Read final progress
                    reportProgress();

                    log.info("HPatchZ exited with code: {}", exitCode);

                    // Cleanup shared memory
                    if (sharedMemory != null) {
                        sharedMemory.close();
                        sharedMemory = null;
                    }

                    // Notify finished (marshal via callbackExecutor if set,
                    // matching C# SynchronizationContext.Post).
                    if (finishedCallback != null) {
                        if (callbackExecutor != null) {
                            callbackExecutor.execute(() -> finishedCallback.onFinished(exitCode));
                        } else {
                            finishedCallback.onFinished(exitCode);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("HPatchZ exit handler interrupted");
                }
            }, "hpatchz-exit");
            exitHandler.setDaemon(true);
            exitHandler.start();

            return new PatchStartResult(0, "OK");
        } catch (Exception e) {
            log.error("Failed to start HPatchZ process", e);
            if (sharedMemory != null) {
                sharedMemory.close();
                sharedMemory = null;
            }
            // C# KRResourcePatchExecutor.cs:137-145 returns ex.HResult on exception.
            return new PatchStartResult(cn.tealc.wwt.game.resource.internal.legacy.util.ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    /**
     * Kill the HPatchZ process.
     * Corresponds to C# KRResourcePatchExecutor.Kill() which ONLY kills the
     * process — it does not stop the timer, reset state, or close shared
     * memory. All cleanup happens via the Exited event handler or Dispose().
     */
    public void kill() {
        if (process == null || !process.isAlive()) {
            return;
        }
        try {
            process.destroyForcibly();
        } catch (Exception e) {
            log.warn("Failed to kill process", e);
        }
    }

    /**
     * Read progress from shared memory and notify callback.
     * Fields: patchedFileCount, fileTotalCount, patchingFileCurrBytes,
     * patchingFileTotalBytes, patchedCurrBytes, patchTotalBytes
     */
    private void reportProgress() {
        if (sharedMemory == null || progressCallback == null)
            return;

        // C# KRResourcePatchExecutor.cs:234 uses TimeSpan.FromMicroseconds(500.0)
        // = 0.5ms. Java's minimum practical timeout is 1ms.
        long[] data = sharedMemory.readUlong(0, 6, 1);
        if (data != null) {
            // Marshal via callbackExecutor if set (matches C#
            // SynchronizationContext.Post in OnPatchProgressChanged).
            if (callbackExecutor != null) {
                callbackExecutor.execute(() -> {
                    if (progressCallback != null) {
                        progressCallback.onProgressChanged(data[0], data[1], data[2], data[3], data[4], data[5]);
                    }
                });
            } else {
                progressCallback.onProgressChanged(data[0], data[1], data[2], data[3], data[4], data[5]);
            }
        }
    }

    private void startProgressReporting(long intervalMs) {
        progressTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hpatchz-progress");
            t.setDaemon(true);
            return t;
        });
        progressTimer.scheduleAtFixedRate(this::reportProgress, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void stopProgressReporting() {
        if (progressTimer != null) {
            progressTimer.shutdownNow();
            progressTimer = null;
        }
    }

    /**
     * Close resources.
     * Corresponds to C# KRResourcePatchExecutor.Dispose() which stops the
     * timer, disposes the process, and disposes shared memory.
     */
    public void close() {
        stopProgressReporting();
        kill();
        if (sharedMemory != null) {
            sharedMemory.close();
            sharedMemory = null;
        }
        running = false;
    }

    /**
     * Map HPatchZ exit code to error code.
     * Matches C# KRPatchApplyTask switch logic.
     */
    public static int mapExitCodeToError(int exitCode) {
        return switch (exitCode) {
            case EXIT_FILE_OCCUPANCY_1, EXIT_FILE_OCCUPANCY_2 -> ERROR_CODE_FILE_OCCUPANCY;
            case EXIT_DISK_NOT_ENOUGH_SPACE -> ERROR_CODE_DISK_NOT_ENOUGH_SPACE;
            case EXIT_PERMISSION_DENIED_1, EXIT_PERMISSION_DENIED_2 -> ERROR_CODE_FILE_PERMISSION_DENY;
            default -> 0; // success
        };
    }

    /**
     * Get human-readable error message for exit code.
     */
    public static String getExitCodeMessage(int exitCode) {
        return switch (exitCode) {
            case EXIT_FILE_OCCUPANCY_1, EXIT_FILE_OCCUPANCY_2 -> "DIR_PATCH FAIL, Because File OCCUPANCY";
            case EXIT_DISK_NOT_ENOUGH_SPACE -> "DIR_PATCH FAIL, Because Disk Not Enough Space";
            case EXIT_PERMISSION_DENIED_1, EXIT_PERMISSION_DENIED_2 -> "DIR_PATCH FAIL, Because File Permission Deny";
            default -> "Unknown exit code: " + exitCode;
        };
    }

    public interface PatchProgressCallback {
        void onProgressChanged(long patchedFileCount, long fileTotalCount,
                long patchingFileCurrBytes, long patchingFileTotalBytes,
                long patchedCurrBytes, long patchTotalBytes);
    }

    public interface PatchFinishedCallback {
        void onFinished(int exitCode);
    }
}
