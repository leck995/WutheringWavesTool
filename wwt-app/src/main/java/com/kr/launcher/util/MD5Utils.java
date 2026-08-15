package com.kr.launcher.util;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * MD5 utilities.
 * Corresponds to KRSharedUtils/MD5Util.cs.
 *
 * IMPORTANT: case sensitivity matches C# exactly:
 * - getFileMD5 / safeGetFileMd5 → lowercase (C# uses ToLowerInvariant())
 * - getFileMD5WithProgress → UPPERCASE (C#
 * GetFileStreamMd5/CheckFileMd5WithProgress
 * use BitConverter.ToString without ToLowerInvariant)
 * - getFileChunkMd5 → UPPERCASE + boundary check (C# GetFileChunkMd5 returns ""
 * when fileStream.Length <= end)
 *
 * Buffer size is 1MB (1048576) to match C#.
 */
public class MD5Utils {

    private static final int BUFFER_SIZE = 1048576; // 1MB, matches C#

    /**
     * Get file MD5 hash (lowercase).
     * Corresponds to C# MD5Util.GetFileMd5 (uses ToLowerInvariant).
     */
    public static String getFileMD5(String filePath) {
        try (InputStream is = new FileInputStream(filePath)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            return bytesToHexLower(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get file MD5 hash (lowercase), throwing on I/O errors.
     * Corresponds to C# MD5Util.GetFileStreamMd5 which does NOT catch exceptions.
     * Used by post-download MD5 verification where the caller needs to check
     * CanRetry on the exception's HRESULT (C#
     * KRDownloadTask.DownloadStateChangedWrapper
     * and KRChunkDownloadTask.MergeChunk).
     */
    public static String getFileMD5OrThrow(String filePath) throws IOException {
        try (InputStream is = new FileInputStream(filePath)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            return bytesToHexUpper(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 algorithm not available", e);
        }
    }

    /**
     * Get file MD5 hash with progress callback (UPPERCASE).
     * Corresponds to C# MD5Util.GetFileStreamMd5 / CheckFileMd5WithProgress
     * (uses BitConverter.ToString without ToLowerInvariant → uppercase).
     */
    public static String getFileMD5WithProgress(String filePath, ProgressCallback callback) {
        return getFileMD5WithProgress(filePath, callback, () -> false);
    }

    /**
     * Get file MD5 hash with progress reporting unless cancellation is requested.
     * Returns {@code null} when cancelled or when the hash cannot be calculated.
     */
    public static String getFileMD5WithProgress(
            String filePath, ProgressCallback callback, BooleanSupplier cancellationRequested) {
        File file = new File(filePath);
        long totalSize = file.length();
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            long bytesRead = 0;
            int read;
            while ((read = is.read(buffer)) != -1) {
                if (cancellationRequested.getAsBoolean()) {
                    return null;
                }
                md.update(buffer, 0, read);
                bytesRead += read;
                if (callback != null) {
                    callback.onProgress(bytesRead, totalSize);
                }
            }
            if (cancellationRequested.getAsBoolean()) {
                return null;
            }
            return bytesToHexUpper(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get file MD5 hash with progress callback (UPPERCASE), throwing on I/O errors.
     * Corresponds to C# MD5Util.GetFileStreamMd5 which does NOT catch exceptions.
     */
    public static String getFileMD5WithProgressOrThrow(String filePath, ProgressCallback callback) throws IOException {
        File file = new File(filePath);
        long totalSize = file.length();
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            long bytesRead = 0;
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
                bytesRead += read;
                if (callback != null) {
                    callback.onProgress(bytesRead, totalSize);
                }
            }
            return bytesToHexUpper(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 algorithm not available", e);
        }
    }

    public static String getStringMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // C# uses Encoding.UTF8.GetBytes(input) — always UTF-8 regardless of
            // platform default charset. Non-ASCII strings would otherwise hash
            // differently on non-UTF-8 default-charset JVMs.
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHexLower(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Get MD5 of a file chunk (byte range [start, end] inclusive).
     * Corresponds to C# MD5Util.GetFileChunkMd5.
     *
     * Returns "" (empty string) when fileStream.Length <= end, matching C#
     * behavior.
     * Returns UPPERCASE hex (C# uses BitConverter.ToString without
     * ToLowerInvariant).
     */
    public static String getFileChunkMd5(String filePath, long start, long end) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            // C# boundary check: if (fileStream.Length <= end) return ""
            if (raf.length() <= end) {
                return "";
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            long bytesToRead = end - start + 1;
            raf.seek(start);
            byte[] buffer = new byte[8192];
            long read = 0;
            while (read < bytesToRead) {
                int toRead = (int) Math.min(buffer.length, bytesToRead - read);
                int n = raf.read(buffer, 0, toRead);
                if (n <= 0)
                    break;
                md.update(buffer, 0, n);
                read += n;
            }
            return bytesToHexUpper(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safe file MD5 - returns empty string on failure (never null).
     * Corresponds to C# MD5Util.SafeGetFileMd5 (uses ToLowerInvariant → lowercase).
     */
    public static String safeGetFileMd5(String filePath) {
        String md5 = getFileMD5(filePath);
        return md5 != null ? md5 : "";
    }

    /**
     * Convert bytes to lowercase hex string.
     * Matches C# GetFileMd5/SafeGetFileMd5 which use ToLowerInvariant().
     */
    private static String bytesToHexLower(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }

    /**
     * Convert bytes to UPPERCASE hex string.
     * Matches C# GetFileStreamMd5/GetFileChunkMd5/CheckFileMd5WithProgress which
     * use
     * BitConverter.ToString(...).Replace("-", "") (uppercase, no ToLowerInvariant).
     */
    private static String bytesToHexUpper(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02X", b));
        }
        return sb.toString();
    }

    public interface ProgressCallback {
        void onProgress(long current, long total);
    }
}
