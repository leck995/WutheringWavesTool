package cn.tealc.wwt.game.resource.model;

import java.util.Objects;
import java.util.UUID;

/** Immutable check result. The opaque id binds an update request to its checked session. */
public final class ResourceCheckResult {
    private final UUID checkId;
    private final boolean successful;
    private final int errorCode;
    private final String errorMessage;
    private final ResourceCheckState state;
    private final String installedVersion;
    private final String latestVersion;
    private final boolean updatePlanAvailable;
    private final boolean repairAvailable;
    private final boolean preDownloadAvailable;
    private final boolean preDownloadComplete;
    private final long preDownloadSize;

    public ResourceCheckResult(UUID checkId, boolean successful, int errorCode, String errorMessage,
            ResourceCheckState state, String installedVersion, String latestVersion,
            boolean updatePlanAvailable, boolean repairAvailable, boolean preDownloadAvailable,
            boolean preDownloadComplete, long preDownloadSize) {
        this.checkId = Objects.requireNonNull(checkId, "checkId");
        this.successful = successful;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage != null ? errorMessage : "";
        this.state = Objects.requireNonNull(state, "state");
        this.installedVersion = installedVersion != null ? installedVersion : "";
        this.latestVersion = latestVersion != null ? latestVersion : "";
        this.updatePlanAvailable = updatePlanAvailable;
        this.repairAvailable = repairAvailable;
        this.preDownloadAvailable = preDownloadAvailable;
        this.preDownloadComplete = preDownloadComplete;
        this.preDownloadSize = Math.max(0, preDownloadSize);
    }

    public boolean isSuccessful() { return successful; }
    public int errorCode() { return errorCode; }
    public String errorMessage() { return errorMessage; }
    public ResourceCheckState state() { return state; }
    public String installedVersion() { return installedVersion; }
    public String latestVersion() { return latestVersion; }
    public boolean hasUpdatePlan() { return updatePlanAvailable; }
    public boolean isRepairAvailable() { return repairAvailable; }
    public boolean isPreDownloadAvailable() { return preDownloadAvailable; }
    public boolean isPreDownloadComplete() { return preDownloadComplete; }
    public long preDownloadSize() { return preDownloadSize; }

    public boolean isUpdateAvailable() {
        return state == ResourceCheckState.UPDATE_AVAILABLE
                || state == ResourceCheckState.PRE_DOWNLOAD_AVAILABLE;
    }

    /** Opaque identifier used to associate an operation with this check result. */
    public UUID checkId() { return checkId; }
}
