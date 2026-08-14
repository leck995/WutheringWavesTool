package cn.tealc.download.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 文件工具。
 */
public final class FileUtils {
    private FileUtils() {}

    public static void createParents(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /** 递归删除目录（含其子文件）。 */
    public static void deleteRecursively(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 忽略单个文件删除失败，尽可能清理
                }
            });
        }
    }

    /** 返回剩余磁盘可用字节。 */
    public static long freeSpaceBytes(Path path) {
        return path.toFile().getUsableSpace();
    }
}