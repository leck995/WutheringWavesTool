package cn.tealc.wutheringwavestool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 卡池日志文件工具类，提供解密 Client.log、提取卡池 URL、解析参数等功能
 * @author: Leck
 * @create: 2026-08-27
 */
public class GachaLogUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GachaLogUtil.class);

    private static final Pattern URL_PATTERN = Pattern.compile("https.*/aki/gacha/index.html#/record[^\"'\\s]+");
    private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");

    /**
     * 解密 Client.log 文件并从中提取卡池 URL
     * 如果 URL 中包含 Unicode 转义序列（如 \u0026），会自动检测并解码
     *
     * @param file Client.log 文件
     * @return 卡池 URL，未找到则返回 null
     */
    public static String getLogFileUrl(File file) {
        String decrypted = decryptLog(file);
        if (decrypted == null) {
            return null;
        }

        Matcher matcher = URL_PATTERN.matcher(decrypted);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(0);
        }

        // 先检测是否包含 Unicode 转义序列，有则解码
        if (lastMatch != null && containsUnicodeEscape(lastMatch)) {
            LOG.debug("检测到 URL 包含 Unicode 转义序列，进行解码");
            lastMatch = decodeUnicodeEscapes(lastMatch);
        }
        return lastMatch;
    }

    /**
     * 解密 Client.log 文件（XOR 解密算法）
     *
     * @param file Client.log 文件
     * @return 解密后的字符串，失败则返回 null
     */
    public static String decryptLog(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {
            long fileSize = channel.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
            channel.read(buffer);
            buffer.flip();
            byte[] bytes = buffer.array();
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i] & 0xFF;
                if (((b & 0x0F) % 2) == 1) {
                    bytes[i] = (byte) (b ^ 0xA5);
                } else {
                    bytes[i] = (byte) (b ^ 0xEF);
                }
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("解密日志文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从卡池 URL 中提取请求参数
     *
     * @param url 卡池 URL
     * @return 参数映射（playerId, recordId, cardPoolId, cardPoolType, serverId, languageCode）
     */
    public static Map<String, String> getParamFromUrl(String url) {
        Map<String, String> parameters = new HashMap<>();
        String paramRow = url.substring(url.indexOf("?") + 1);
        String[] strings = paramRow.split("&");

        for (String param : strings) {
            String[] split = param.split("=");
            if (split.length < 2) continue;
            switch (split[0]) {
                case "player_id" -> parameters.put("playerId", split[1]);
                case "record_id" -> parameters.put("recordId", split[1]);
                case "resources_id" -> parameters.put("cardPoolId", split[1]);
                case "gacha_type" -> parameters.put("cardPoolType", split[1]);
                case "svr_id" -> parameters.put("serverId", split[1]);
                case "lang" -> parameters.put("languageCode", split[1]);
            }
        }
        return parameters;
    }

    /**
     * 检测字符串是否包含 Unicode 转义序列
     *
     * @param s 待检测字符串
     * @return true 如果包含 Unicode 转义序列
     */
    public static boolean containsUnicodeEscape(String s) {
        return UNICODE_ESCAPE_PATTERN.matcher(s).find();
    }

    /**
     * 解码字符串中的 Unicode 转义序列
     *
     * @param s 包含 Unicode 转义序列的字符串
     * @return 解码后的字符串
     */
    public static String decodeUnicodeEscapes(String s) {
        Matcher matcher = UNICODE_ESCAPE_PATTERN.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            char ch = (char) Integer.parseInt(hex, 16);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(ch)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}