package cn.tealc.wutheringwavestool.util;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Base64FileUtil {

    private Base64FileUtil() {
    }

    /**
     * 将文件编码为 Base64 字符串并覆盖写入原文件。
     */
    public static void encodeFile(Path file) throws IOException {
        byte[] sourceBytes = Files.readAllBytes(file);
        byte[] encodedBytes = Base64.encodeBase64(sourceBytes, true);
        Files.write(file, encodedBytes);
    }

    /**
     * 将 Base64 文件解码为原始二进制内容并覆盖写入原文件。
     */
    public static void decodeFile(Path file) throws IOException {
        byte[] encodedBytes = Files.readAllBytes(file);
        byte[] decodedBytes = Base64.decodeBase64(encodedBytes);
        Files.write(file, decodedBytes);
    }
}
