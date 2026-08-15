package cn.tealc.wwt.game.resource.internal.legacy.util;

import java.util.Locale;

public class ByteUtils {

    /**
     * Convert bytes to human-readable string.
     * Corresponds to C# KRByteUtils.ByteConvert.
     * - 0 → "0KB" (no space)
     * - >= 1GB → "N.N GB"
     * - >= 1MB → "N.N MB"
     * - else → "N.N KB" (always KB, even for < 1024 bytes)
     * Uses 1 decimal place (F1) and Locale.ROOT for decimal separator.
     */
    public static String byteConvert(long bytes) {
        if (bytes == 0L) {
            return "0KB";
        }
        if ((double) bytes >= 1073741824.0) {
            return String.format(Locale.ROOT, "%.1f GB", (double) bytes / 1073741824.0);
        }
        if ((double) bytes >= 1048576.0) {
            return String.format(Locale.ROOT, "%.1f MB", (double) bytes / 1048576.0);
        }
        return String.format(Locale.ROOT, "%.1f KB", (double) bytes / 1024.0);
    }

    public static String formatSpeed(long bytesPerSecond) {
        return byteConvert(bytesPerSecond) + "/s";
    }

    public static String formatTime(long seconds) {
        if (seconds < 0 || seconds > 86400) return "--:--:--";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
