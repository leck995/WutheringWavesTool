package cn.tealc.wwt.game.resource.internal.legacy.util;

import java.util.Arrays;

public class VersionUtils {

    /**
     * Compare two dot-separated version strings.
     * Matches C# VersionUtils.Compare semantics exactly:
     * 1. null/empty handling: non-empty > empty
     * 2. Compare min(len1, len2) segments as integers
     * 3. If all comparable segments equal, compare array lengths
     *    (so "1.0" < "1.0.0" — longer version wins)
     */
    public static int compare(String v1, String v2) {
        boolean empty1 = v1 == null || v1.isEmpty();
        boolean empty2 = v2 == null || v2.isEmpty();
        if (empty1 || empty2) {
            if (!empty1) return 1;
            if (!empty2) return -1;
            return 0;
        }

        // C# String.Split('.') preserves trailing empty strings (e.g., "1." → ["1", ""]).
        // Java's split("\\.") removes trailing empty strings by default;
        // use limit=-1 to preserve them, matching C# semantics.
        String[] parts1 = v1.split("\\.", -1);
        String[] parts2 = v2.split("\\.", -1);
        int minLen = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < minLen; i++) {
            int num1 = parseInt(parts1[i]);
            int num2 = parseInt(parts2[i]);
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        // All comparable segments equal — longer array wins.
        return Integer.compare(parts1.length, parts2.length);
    }

    public static boolean verify(String versionString) {
        if (versionString == null || versionString.isEmpty()) return false;
        String[] parts = versionString.split("\\.");
        if (parts.length < 2 || parts.length > 4) return false;
        for (String part : parts) {
            try {
                Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parse a version segment to int.
     * Corresponds to C# int.Parse which throws FormatException on non-numeric
     * input. Callers (CheckUpdateFlow) wrap VersionUtils.compare in try/catch,
     * so a malformed version string surfaces as a caught failure — matching C#
     * behavior where the exception propagates to the outer handler.
     */
    private static int parseInt(String s) {
        return Integer.parseInt(s);
    }
}
