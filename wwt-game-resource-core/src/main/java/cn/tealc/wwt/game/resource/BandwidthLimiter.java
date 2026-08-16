package cn.tealc.wwt.game.resource;

import java.util.function.BooleanSupplier;

/** 单个下载任务共享的总带宽限制器。 */
public final class BandwidthLimiter {
    private static final long MAX_WAIT_MILLIS = 50;

    private final long bytesPerSecond;
    private long nextAvailableNanos;

    public BandwidthLimiter(long bytesPerSecond) {
        if (bytesPerSecond < 0) {
            throw new IllegalArgumentException("bytesPerSecond 不能为负数");
        }
        this.bytesPerSecond = bytesPerSecond;
    }

    /**
     * 为当前数据块预留发送时间。返回 {@code false} 表示调用方已取消或线程被中断。
     */
    public boolean acquire(int bytes, BooleanSupplier canceled) {
        if (bytesPerSecond == 0 || bytes <= 0) {
            return !canceled.getAsBoolean();
        }

        long waitNanos;
        synchronized (this) {
            long now = System.nanoTime();
            long start = Math.max(now, nextAvailableNanos);
            long duration = Math.max(1, Math.ceilDiv(bytes * 1_000_000_000L, bytesPerSecond));
            nextAvailableNanos = start + duration;
            waitNanos = start - now;
        }
        while (waitNanos > 0) {
            if (canceled.getAsBoolean()) {
                return false;
            }
            long sleepMillis = Math.min(MAX_WAIT_MILLIS,
                    Math.max(1, (waitNanos + 999_999L) / 1_000_000L));
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            waitNanos -= sleepMillis * 1_000_000L;
        }
        return !canceled.getAsBoolean();
    }
}
