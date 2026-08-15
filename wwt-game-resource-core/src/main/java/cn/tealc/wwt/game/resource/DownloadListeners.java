package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.DownloadState;

/**
 * 下载事件监听器。回调在引擎 worker 线程触发，调用方需自行收敛到目标线程（如 FX Application Thread）。
 */
public final class DownloadListeners {

    /** 下载进度回调：(已下载字节, 总字节)。 */
    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(long downloadedBytes, long totalBytes);
    }

    /** 状态变化回调：(状态, 错误信息)。 */
    @FunctionalInterface
    public interface StateListener {
        void onStateChanged(DownloadState state, String error);
    }

    /** MD5 校验进度回调：(已完成字节, 总字节)。 */
    @FunctionalInterface
    public interface Md5CheckListener {
        void onMd5Check(long completedBytes, long totalBytes);
    }

    private DownloadListeners() {}
}