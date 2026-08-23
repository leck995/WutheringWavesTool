package cn.tealc.wutheringwavestool.thread.game.download;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;

import java.util.Locale;

/**
 * 游戏下载任务公共基类。
 *
 * <p>封装各下载 Task 共享的工具：字节格式化、EMA 速率平滑、取消/暂停/恢复委托钩子。
 * 子类在 {@link #call()} 中执行下载，并可按需调用 {@link #updateProgress} /
 * {@link #updateMessage} / {@link #updateTitle} 上抛进度。</p>
 *
 * <p>公共属性：
 *   <ul>
 *     <li>{@link #speedTextProperty()} — 下载速度文本（Task 无此内置概念）</li>
 *     <li>{@link #pausedProperty()} — 暂停状态（暂停时 Task.state 仍为 RUNNING）</li>
 *   </ul>
 *   其余进度/状态/文案均使用 Task 内置属性：{@link #progressProperty()}、
 *   {@link #messageProperty()}、{@link #titleProperty()}、{@link #stateProperty()}。</p>
 *
 * @param <T> 任务结果类型
 */
public abstract class AbstractGameDownloadTask<T> extends Task<T> {

    protected static final double EMA_ALPHA = 0.3;
    protected static final long SAMPLE_INTERVAL_NANOS = 500_000_000L;

    protected final ReadOnlyStringWrapper speedText = new ReadOnlyStringWrapper("");
    protected final ReadOnlyBooleanWrapper paused = new ReadOnlyBooleanWrapper(false);

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

    /** 计算一次 EMA 平滑后的字节/秒瞬时速率，并返回格式化文本（不足 1 字节/秒返回空串）。
     * 首次调用时仅记录初始基准值，不计算速率，避免续传场景下 completedBytes 远大于 0 导致速率虚高。 */
    protected String updateEmaSpeed(long completedBytes, long[] speedSample, double[] emaSpeed) {
        long now = System.nanoTime();
        // 首次采样：仅记录基准值，不计算速率
        if (speedSample[1] == 0 && completedBytes > 0) {
            speedSample[0] = now;
            speedSample[1] = completedBytes;
            return "";
        }
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

    /** 组合进度消息：已完成 / 总量。 */
    protected static String progressMessage(long completed, long total) {
        return String.format(Locale.ROOT, "%s / %s", formatBytes(completed), formatBytes(total));
    }

    protected static void onFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    // ---------------- 公共只读属性 ----------------

    public ReadOnlyStringProperty speedTextProperty() { return speedText.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty pausedProperty() { return paused.getReadOnlyProperty(); }
    public boolean isPaused() { return paused.get(); }
}