package com.kr.launcher.util;

public class PathUtils {

    /**
     * Combine path parts, normalizing separators to '/'.
     * Matches C# PathUtils.Combine by processing parts pairwise:
     * C# PathUtils.Combine(basePath, path) strips trailing '/' from basePath
     * and leading '/' from path, then joins with '/'. The LAST part's trailing
     * '/' is preserved (C# does not strip trailing '/' from the path argument).
     * Preserves leading '/' on the first part (absolute paths).
     */
    public static String combine(String... parts) {
        if (parts.length == 0)
            return "";
        String result = null;
        for (String part : parts) {
            if (part == null || part.isEmpty())
                continue;
            String normalized = part.replace("\\", "/");
            if (result == null) {
                result = normalized;
            } else {
                result = combineTwo(result, normalized);
            }
        }
        return result != null ? result : "";
    }

    /**
     * Two-argument combine matching C# PathUtils.Combine(basePath, path)
     * exactly: strip trailing '/' from basePath, strip leading '/' from path,
     * join with '/'. The path's trailing '/' is preserved.
     */
    private static String combineTwo(String basePath, String path) {
        // C# PathUtils.Combine: if basePath is null/empty, returns path as-is
        if (basePath == null || basePath.isEmpty()) {
            return path;
        }
        String text = basePath.replace("\\", "/");
        String text2 = path.replace("\\", "/");
        // C# TrimEnd('/') — strip all trailing '/'
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        // C# TrimStart('/') — strip all leading '/'
        while (text2.startsWith("/")) {
            text2 = text2.substring(1);
        }
        return text + "/" + text2;
    }

    public static String normalize(String path) {
        if (path == null)
            return null;
        return path.replace("\\", "/");
    }

    /**
     * Get parent directory. Matches C# PathUtils.GetParentDir: trims trailing
     * separators first, then finds the last separator.
     */
    public static String getParentDir(String path) {
        if (path == null)
            return null;
        // Trim trailing separators (C# does path?.TrimEnd('/', '\\'))
        String trimmed = path;
        while (trimmed.length() > 1 && (trimmed.endsWith("/") || trimmed.endsWith("\\"))) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int lastSep = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        if (lastSep <= 0)
            return "";
        return trimmed.substring(0, lastSep);
    }

    public static boolean isRootDir(String path) {
        String normalized = normalize(path);
        return normalized.equals("/") || normalized.matches("[A-Z]:/?");
    }
}
