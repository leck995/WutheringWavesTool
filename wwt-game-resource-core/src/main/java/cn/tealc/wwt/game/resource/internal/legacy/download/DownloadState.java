package cn.tealc.wwt.game.resource.internal.legacy.download;

/**
 * Download state enum.
 * Corresponds to C# KRDownloader.KRDownloadState.
 *
 * NOTE: WAITTING is intentionally misspelled (double-T) to match the upstream
 * C# enum constant KRDownloadState.WAITTING. Do NOT "fix" the spelling —
 * callers and serializers depend on the exact name.
 */
public enum DownloadState {
    IDLE,
    WAITTING,
    DOWNLOADING,
    PAUSED,
    FAILED,
    CANCELLING,
    CANCELED,
    COMPLETE
}
