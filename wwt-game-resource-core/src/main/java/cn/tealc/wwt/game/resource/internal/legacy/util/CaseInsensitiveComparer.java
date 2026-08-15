package cn.tealc.wwt.game.resource.internal.legacy.util;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Case-insensitive string equality comparer.
 * Corresponds to KRResources/CaseInsensitiveComparer.cs which implements
 * IEqualityComparer&lt;string&gt;.
 *
 * Used to build case-insensitive maps (C# Dictionary with this comparer).
 * In Java, use {@link #newCaseInsensitiveMap()} which returns a TreeMap
 * backed by {@link String#CASE_INSENSITIVE_ORDER}.
 */
public final class CaseInsensitiveComparer {

    private CaseInsensitiveComparer() {
    }

    /**
     * Case-insensitive equality. Null-safe (matches C# string.Equals with
     * StringComparison.OrdinalIgnoreCase).
     */
    public static boolean equals(String x, String y) {
        if (x == null)
            return y == null;
        if (y == null)
            return false;
        return x.equalsIgnoreCase(y);
    }

    /**
     * Locale-independent hash code using toUpperCase(Locale.ROOT).
     * Matches C# obj?.ToUpperInvariant().GetHashCode() ?? 0.
     */
    public static int hashCode(String obj) {
        return obj != null ? obj.toUpperCase(Locale.ROOT).hashCode() : 0;
    }

    /**
     * Create a case-insensitive map (C# Dictionary with CaseInsensitiveComparer).
     */
    public static <V> Map<String, V> newCaseInsensitiveMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }
}
