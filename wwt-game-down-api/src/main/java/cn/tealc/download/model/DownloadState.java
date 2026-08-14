package cn.tealc.download.model;

/**
 * 下载任务生命周期状态。
 * 对应 KR Launcher 的 DownloadState。
 */
public enum DownloadState {
    IDLE,
    WAITING,
    DOWNLOADING,
    PAUSED,
    FAILED,
    CANCELLING,
    CANCELED,
    COMPLETE
}