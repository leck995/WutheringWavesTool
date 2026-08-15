package cn.tealc.wwt.game.resource.internal.legacy.download;

/**
 * Event args for download progress changes.
 * Corresponds to KRDownloadProgressChangedEventArgs.cs.
 */
public class DownloadProgressChangedEventArgs {
    public final String taskId;
    public long totalBytesToReceive;
    public long receivedBytesSize;
    public double bytesPerSecondSpeed;
    public long downloadedBytesSizeThisTime;

    public DownloadProgressChangedEventArgs(String taskId) {
        this.taskId = taskId;
    }

    public double getProgressPercentage() {
        if (totalBytesToReceive != 0L) {
            return (double) receivedBytesSize * 100.0 / (double) totalBytesToReceive;
        }
        return 0.0;
    }
}
