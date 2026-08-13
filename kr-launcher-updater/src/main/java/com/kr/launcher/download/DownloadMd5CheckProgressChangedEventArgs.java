package com.kr.launcher.download;

/**
 * Event args for MD5 check progress changes.
 * Corresponds to KRDownloadMd5CheckProgressChangedEventArgs.cs.
 */
public class DownloadMd5CheckProgressChangedEventArgs {
    public final String taskId;
    public long totalBytes;
    public long completedBytesSize;

    public DownloadMd5CheckProgressChangedEventArgs(String taskId) {
        this.taskId = taskId;
    }

    public double getProgressPercentage() {
        if (totalBytes != 0L) {
            return (double) completedBytesSize * 100.0 / (double) totalBytes;
        }
        return 0.0;
    }
}
