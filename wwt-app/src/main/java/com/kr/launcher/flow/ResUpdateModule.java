package com.kr.launcher.flow;

import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.model.CheckUpdateResult;
import com.kr.launcher.model.ResStateInfo;
import com.kr.launcher.model.UpdateInfo;
import com.kr.launcher.model.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main orchestrator for game resource update flows.
 * Corresponds to KRResUpdateModule.cs (resource download/update parts only).
 *
 * Manages the lifecycle of: CheckUpdateFlow, UpdateFlow, PredownloadFlow,
 * RepairFlow, ResCheckFlow. Provides Pause/Resume/Stop for each flow.
 *
 * Note: the existing Java CheckUpdateFlow.exec() is synchronous and returns
 * CheckUpdateResult directly. The UpdateFlow uses setProgressCallback /
 * setCompleteCallback + exec(stateInfo, updateInfo). Pause/Resume/Stop for the
 * update flow are no-ops because UpdateFlow runs synchronously in the current
 * implementation; PredownloadFlow and RepairFlow support pause/resume/stop
 * through their underlying ResourcesDownloadTask.
 */
public class ResUpdateModule {
    private static final Logger log = LoggerFactory.getLogger(ResUpdateModule.class);

    private final ResourceConfigManager configManager;

    private ResStateInfo stateInfo;
    private UpdateInfo updateInfo;
    private UpdateInfo predownloadUpdateInfo;

    private CheckUpdateFlow checkUpdateFlow;
    private UpdateFlow updateFlow;
    private ResCheckFlow checkFlow;
    private PredownloadFlow predownloadFlow;
    private RepairFlow repairFlow;

    private long progressNotifyIntervalMillis = 100L;

    public ResUpdateModule(ResourceConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setProgressNotifyIntervalMillis(long interval) {
        this.progressNotifyIntervalMillis = interval;
    }

    public long getProgressNotifyIntervalMillis() {
        return progressNotifyIntervalMillis;
    }

    public String getUsingVersion() {
        return configManager.getDownloadConfig() != null
                ? configManager.getDownloadConfig().version
                : null;
    }

    // ==================== Check Update ====================

    public interface CheckUpdateCallback {
        void onResult(CheckUpdateResult result);
    }

    /**
     * Check for updates. Runs CheckUpdateFlow.exec() (synchronous) and caches
     * the state/update info before invoking the callback.
     */
    public void checkUpdate(CheckUpdateCallback callback) {
        if (checkUpdateFlow == null) {
            checkUpdateFlow = new CheckUpdateFlow(configManager);
        }
        CheckUpdateResult result = checkUpdateFlow.exec();
        if (result.succ && result.stateInfo != null) {
            stateInfo = result.stateInfo;
            updateInfo = result.updateInfo;
            predownloadUpdateInfo = result.predownloadUpdateInfo;
        }
        callback.onResult(result);
    }

    /**
     * Check local state using the cached gameServerConfig (no network fetch).
     * Corresponds to KRResUpdateModule.CheckLocalState() (line 103-119).
     *
     * Uses CheckUpdateFlow.checkLocalState() which reads the cached config
     * instead of fetching from the network. Returns CHECK_LOCAL_STATE_FAIL
     * if the cached config is null.
     */
    public void checkLocalState(CheckUpdateCallback callback) {
        if (checkUpdateFlow == null) {
            checkUpdateFlow = new CheckUpdateFlow(configManager);
        }
        CheckUpdateResult result = checkUpdateFlow.checkLocalState();
        if (result.succ && result.stateInfo != null) {
            stateInfo = result.stateInfo;
            updateInfo = result.updateInfo;
            predownloadUpdateInfo = result.predownloadUpdateInfo;
        }
        callback.onResult(result);
    }

    // ==================== Update ====================

    public void update(UpdateFlow.ProgressCallback updateProgressCallback,
            UpdateFlow.CompleteCallback updateCompleted) {
        if (updateFlow == null) {
            updateFlow = new UpdateFlow(configManager);
        }
        updateFlow.setProgressCallback(updateProgressCallback);
        updateFlow.setCompleteCallback(updateResult -> {
            updateCompleted.onComplete(updateResult);
            if (updateResult.success) {
                updateFlow = null;
            }
        });
        updateFlow.exec(stateInfo, updateInfo);
    }

    /**
     * 用显式给定的状态执行更新，不依赖内部缓存。
     * 与 {@link #update(UpdateFlow.ProgressCallback, UpdateFlow.CompleteCallback)} 的区别：
     * 直接使用调用方传入的 state/updateInfo，避免因缓存过期而走“无更新直接成功”的空转分支。
     */
    public void update(ResStateInfo stateInfo, UpdateInfo updateInfo,
            UpdateFlow.ProgressCallback updateProgressCallback,
            UpdateFlow.CompleteCallback updateCompleted) {
        if (updateFlow == null) {
            updateFlow = new UpdateFlow(configManager);
        }
        updateFlow.setProgressCallback(updateProgressCallback);
        updateFlow.setCompleteCallback(updateResult -> {
            updateCompleted.onComplete(updateResult);
            if (updateResult.success) {
                updateFlow = null;
            }
        });
        updateFlow.exec(stateInfo, updateInfo);
    }

    public void pause() {
        if (updateFlow != null) {
            updateFlow.pause();
        }
    }

    public void resume() {
        if (updateFlow != null) {
            updateFlow.resume();
        }
    }

    public void stop() {
        if (updateFlow != null) {
            updateFlow.stop();
        }
    }

    // ==================== Pre-download ====================

    public void preDownload(PredownloadFlow.ProgressCallback predownloadProgressCallback,
            PredownloadFlow.CompleteCallback predownloadCompleteCallback) {
        if (predownloadFlow == null) {
            predownloadFlow = new PredownloadFlow(configManager, this);
        }
        predownloadFlow.exec(predownloadUpdateInfo, predownloadProgressCallback, predownloadCompleteCallback);
    }

    public void pausePreDownload() {
        if (predownloadFlow != null)
            predownloadFlow.pause();
    }

    public void resumePreDownload() {
        if (predownloadFlow != null)
            predownloadFlow.resume();
    }

    public void stopPreDownload() {
        if (predownloadFlow != null)
            predownloadFlow.stop();
    }

    // ==================== Repair ====================

    public void repair(RepairFlow.ProgressCallback repairProgressCallback,
            RepairFlow.CompleteCallback repairCompleteCallback) {
        if (predownloadFlow != null && predownloadFlow.predownloadRunning) {
            predownloadFlow.stop(() -> innerRepair(repairProgressCallback, repairCompleteCallback));
        } else {
            innerRepair(repairProgressCallback, repairCompleteCallback);
        }
    }

    private void innerRepair(RepairFlow.ProgressCallback repairProgressCallback,
            RepairFlow.CompleteCallback repairCompleteCallback) {
        if (repairFlow == null) {
            repairFlow = new RepairFlow(configManager, this);
        }
        RepairFlow flow = repairFlow;
        flow.exec(updateInfo, repairProgressCallback, updateResult -> {
            // RepairFlow stores completed CheckFileTask/ResourcesDownloadTask instances;
            // a completed flow cannot perform another full scan.
            if (repairFlow == flow) {
                repairFlow = null;
            }
            repairCompleteCallback.onComplete(updateResult);
        });
    }

    public void pauseRepair() {
        if (repairFlow != null)
            repairFlow.pause();
    }

    public void resumeRepair() {
        if (repairFlow != null)
            repairFlow.resume();
    }

    public void stopRepair() {
        if (repairFlow != null) {
            repairFlow.stop();
            repairFlow = null;
        }
    }

    // ==================== Resource Check ====================

    public void checkResValid(ResCheckFlow.ProgressCallback progressCallback,
            ResCheckFlow.ResultCallback resultCallback) {
        if (checkFlow == null) {
            checkFlow = new ResCheckFlow(configManager, this);
        }
        checkFlow.exec(updateInfo, progressCallback, resultCallback);
    }

    // ==================== Flow Management ====================

    public void resetAllFlow() {
        if (predownloadFlow != null)
            predownloadFlow.stop();
        if (updateFlow != null)
            updateFlow.stop();
        if (repairFlow != null)
            repairFlow.stop();
        predownloadFlow = null;
        updateFlow = null;
        repairFlow = null;
        checkFlow = null;
    }

    public void resetPredownloadFlow() {
        if (predownloadFlow != null)
            predownloadFlow.stop();
        predownloadFlow = null;
    }

    public void resetUpdateFlow() {
        // C# KRResUpdateModule.ResetUpdateFlow: _updateFlow?.Stop(); _updateFlow = null;
        if (updateFlow != null)
            updateFlow.stop();
        updateFlow = null;
    }

    public void resetRepairFlow() {
        if (repairFlow != null)
            repairFlow.stop();
        repairFlow = null;
    }

    public void resetCheckFlow() {
        checkFlow = null;
    }
}
