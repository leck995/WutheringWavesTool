package cn.tealc.wwt.game.resource.model;

/** Raw operation progress. Native state is retained for diagnostic logging only. */
public record ResourceProgress(int nativeState, long completedBytes, long totalBytes,
        long completedFiles, long totalFiles) {
    public double fraction() {
        return totalBytes > 0 ? Math.min(1D, (double) completedBytes / totalBytes) : -1D;
    }
}
