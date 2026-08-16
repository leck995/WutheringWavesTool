package cn.tealc.wwt.game.resource.model;

/** Raw operation progress. Native state is retained for diagnostic logging only. */
public record ResourceProgress(int nativeState, long completedBytes, long totalBytes,
        long completedFiles, long totalFiles) {
    public double fraction() {
        return totalBytes > 0 ? Math.min(1D, (double) completedBytes / totalBytes) : -1D;
    }

    /** Maps stable legacy state codes to the public operation phases. */
    public ResourceOperationPhase operationPhase() {
        return switch (nativeState) {
            case 0, 9 -> ResourceOperationPhase.VERIFYING;
            case 1 -> ResourceOperationPhase.DOWNLOADING;
            case 5 -> ResourceOperationPhase.APPLYING;
            default -> ResourceOperationPhase.UNKNOWN;
        };
    }
}
