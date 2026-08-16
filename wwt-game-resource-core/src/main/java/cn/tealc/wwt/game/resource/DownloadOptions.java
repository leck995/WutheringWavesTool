package cn.tealc.wwt.game.resource;

/**
 * 下载任务的并发与带宽策略。
 *
 * @param maxParallel 最大并发文件数，范围为 1 至 16
 * @param speedLimitBytesPerSecond 总下载速度上限；0 表示不限速
 */
public record DownloadOptions(int maxParallel, long speedLimitBytesPerSecond) {
    public static final int DEFAULT_MAX_PARALLEL = 4;
    public static final int MIN_MAX_PARALLEL = 1;
    public static final int MAX_MAX_PARALLEL = 16;
    public static final DownloadOptions DEFAULT = new DownloadOptions(DEFAULT_MAX_PARALLEL, 0);

    public DownloadOptions {
        if (maxParallel < MIN_MAX_PARALLEL || maxParallel > MAX_MAX_PARALLEL) {
            throw new IllegalArgumentException("maxParallel 必须在 1 到 16 之间");
        }
        if (speedLimitBytesPerSecond < 0) {
            throw new IllegalArgumentException("speedLimitBytesPerSecond 不能为负数");
        }
    }
}
