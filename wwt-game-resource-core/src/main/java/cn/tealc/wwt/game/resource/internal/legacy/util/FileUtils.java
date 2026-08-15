package cn.tealc.wwt.game.resource.internal.legacy.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileUtils {

    /**
     * Reset readonly attribute before deleting.
     * C# FileUtils.cs:27,60 calls File.SetAttributes(path, FileAttributes.Normal)
     * before delete to handle readonly files (common in game directories).
     */
    private static void deleteWithReadOnlyReset(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try {
            // On Windows, reset readonly attribute via DOS attribute view.
            Files.setAttribute(path, "dos:readonly", false);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-Windows or attribute not supported; proceed with delete.
        }
        Files.delete(path);
    }

    /**
     * Delete a file if it exists. Propagates exceptions on failure.
     * Corresponds to C# FileUtils.DeleteFile (no try/catch) which throws IOException.
     * Uses UncheckedIOException to preserve the IOException type for ExceptionUtils
     * classification while remaining compatible with existing callers.
     */
    public static void deleteFile(String path) {
        try {
            deleteWithReadOnlyReset(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete file: " + path, e);
        }
    }

    /**
     * Recursively delete a directory. Propagates exceptions on failure.
     * Corresponds to C# FileUtils.DeleteDirectory (no try/catch) which throws IOException.
     * Uses UncheckedIOException to preserve the IOException type for ExceptionUtils
     * classification while remaining compatible with existing callers.
     */
    public static void deleteDirectory(String dirPath) {
        Path dir = Path.of(dirPath);
        if (!Files.exists(dir)) return;
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        deleteWithReadOnlyReset(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to delete: " + path, e);
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + dirPath, e);
        }
    }

    /**
     * Try to delete a directory, collecting per-file failures instead of throwing.
     * Returns a list of (filePath, errorMessage) pairs for files that could not be deleted.
     * Corresponds to C# FileUtils.TryDeleteDirectory.
     */
    public static List<Map.Entry<String, String>> tryDeleteDirectory(String targetDir) {
        List<Map.Entry<String, String>> failures = new ArrayList<>();
        Path dir = Path.of(targetDir);
        if (!Files.exists(dir)) {
            return failures;
        }
        try {
            // Delete subdirectories first (recursively)
            try (var stream = Files.list(dir)) {
                for (Path child : stream.toList()) {
                    if (Files.isDirectory(child)) {
                        List<Map.Entry<String, String>> subFailures = tryDeleteDirectory(child.toString());
                        failures.addAll(subFailures);
                    }
                }
            }
            // Delete files
            try (var stream = Files.list(dir)) {
                for (Path file : stream.toList()) {
                    if (!Files.isDirectory(file)) {
                        try {
                            deleteWithReadOnlyReset(file);
                        } catch (IOException e) {
                            failures.add(new java.util.AbstractMap.SimpleEntry<>(file.toString(), e.getMessage()));
                        }
                    }
                }
            }
            // Delete the directory itself only if no failures
            if (failures.isEmpty()) {
                deleteWithReadOnlyReset(dir);
            }
        } catch (IOException e) {
            failures.add(new java.util.AbstractMap.SimpleEntry<>(targetDir, e.getMessage()));
        }
        return failures;
    }

    public static void ensureDir(String dirPath) {
        try {
            Files.createDirectories(Path.of(dirPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + dirPath, e);
        }
    }

    public static void write(String content, String filePath) {
        try {
            Files.createDirectories(Path.of(filePath).getParent());
            Files.writeString(Path.of(filePath), content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    public static String read(String filePath) {
        try {
            return Files.readString(Path.of(filePath));
        } catch (IOException e) {
            return null;
        }
    }

    public static void move(String src, String dest) {
        try {
            Files.createDirectories(Path.of(dest).getParent());
            Files.move(Path.of(src), Path.of(dest), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to move file: " + src + " -> " + dest, e);
        }
    }

    public static boolean exists(String path) {
        return Files.exists(Path.of(path));
    }

    public static long fileSize(String path) {
        try {
            return Files.size(Path.of(path));
        } catch (IOException e) {
            return -1;
        }
    }
}
