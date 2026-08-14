package cn.tealc.download.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 工具，支持流式计算与进度回调。
 */
public final class MD5Utils {
    private static final Logger LOG = LoggerFactory.getLogger(MD5Utils.class);
    private static final int BUFFER_SIZE = 1 << 20; // 1MB

    private MD5Utils() {}

    public static String md5(Path file) {
        try {
            return md5(file, null);
        } catch (IOException e) {
            LOG.error("MD5 计算失败: {}", file, e);
            return null;
        }
    }

    /** 计算文件 MD5（十六进制小写）。可传入回调获取进度。 */
    public static String md5(Path file, Progress progress) throws IOException {
        MessageDigest digest = newDigest();
        long total = Files.size(file);
        long completed = 0;
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
                completed += read;
                if (progress != null) {
                    progress.onProgress(completed, total);
                }
            }
        }
        return toHex(digest.digest());
    }

    public static String md5(byte[] data) {
        return toHex(newDigest().digest(data));
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 不可用", e);
        }
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @FunctionalInterface
    public interface Progress {
        void onProgress(long completedBytes, long totalBytes);
    }
}