package cn.tealc.wutheringwavestool.thread.game.download;

import javafx.concurrent.Task;

import java.util.Locale;

/**
 * 游戏下载任务公共基类。
 *
 * <p>封装各下载 Task 共享的工具：字节格式化、EMA 速率平滑、取消/暂停/恢复委托钩子。
 * 子类在 {@link #call()} 中执行下载，并可按需调用 {@link #updateProgress} /
 * {@link #updateMessage} / {@link #updateTitle} 上抛进度。</p>
 *
 * @param <T> 任务结果类型
 */
public abstract class AbstractGameDownloadTask<T> extends Task<T> {

    protected static final double EMA_ALPHA = 0.3;
    protected static final long SAMPLE_INTERVAL_NANOS = 500_000_000L;

    /** 字节格式化：B / KB / MB / GB / TB。 */
    protected static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        do {
            value /= 1024;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    /** 计算一次 EMA 平滑后的字节/秒瞬时速率，并返回格式化文本（不足 1 字节/秒返回空串）。 */
    protected String updateEmaSpeed(long completedBytes, long[] speedSample, double[] emaSpeed) {
        long now = System.nanoTime();
        long elapsed = now - speedSample[0];
        if (elapsed >= SAMPLE_INTERVAL_NANOS) {
            double instant = Math.max(0,
                    (completedBytes - speedSample[1]) / (elapsed / 1_000_000_000.0));
            emaSpeed[0] = (emaSpeed[0] == 0) ? instant
                    : EMA_ALPHA * instant + (1 - EMA_ALPHA) * emaSpeed[0];
            speedSample[0] = now;
            speedSample[1] = completedBytes;
            long v = (long) Math.max(0, emaSpeed[0]);
            return (v > 0) ? formatBytes(v) + "/s" : "";
        }
        return "";
    }

    /** 组合进度消息：已完成 / 总量 · 速率。 */
    protected static String progressMessage(long completed, long total, String speedText) {
        return String.format(Locale.ROOT, "%s / %s" + (speedText.isEmpty() ? "" : "  ·  " + speedText),
                formatBytes(completed), formatBytes(total));
    }
}