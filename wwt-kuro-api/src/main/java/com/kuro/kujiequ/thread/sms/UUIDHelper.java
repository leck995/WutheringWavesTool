package com.kuro.kujiequ.thread.sms;

import java.util.Random;

public class UUIDHelper {

    private static final Random RANDOM = new Random();

    private static final int COUNTER_MIN = 1000;
    private static final int COUNTER_RANGE = 9000;
    private static final int RANDOM_HEX_PRECISION = 14;
    private static final int SCREEN_HEIGHT = 1280;
    private static final int SCREEN_WIDTH = 1920;
    private static final int SCREEN_FINGERPRINT_MIN_DIGITS = 5;
    private static final int FALLBACK_FINGERPRINT_LENGTH = 8;
    private static final int HASH_BUFFER_SIZE = 4;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";

    /** 生成模拟浏览器指纹的设备 ID，格式：timestamp-随机hex-UA哈希-屏幕指纹-timestamp */
    public static String generateDeviceId() {
        String timestampPrefix = generateTimestampHex();
        String randomHex = generateRandomHex();
        String userAgentHash = computeUserAgentHash();
        String screenFingerprint = computeScreenFingerprint();
        String timestampSuffix = generateTimestampHex();

        return String.join("-", timestampPrefix, randomHex, userAgentHash, screenFingerprint, timestampSuffix);
    }

    /** 生成时间戳（毫秒）+ 4位随机计数器 的十六进制字符串 */
    private static String generateTimestampHex() {
        long timestamp = System.currentTimeMillis();
        int counter = RANDOM.nextInt(COUNTER_RANGE) + COUNTER_MIN;
        return Long.toHexString(timestamp) + Integer.toHexString(counter);
    }

    /** 生成 0 开头的 15 位随机十六进制字符串 */
    private static String generateRandomHex() {
        double fraction = Math.random();
        StringBuilder hexBuilder = new StringBuilder("0");
        for (int i = 0; i < RANDOM_HEX_PRECISION; i++) {
            fraction *= 16;
            int digit = (int) fraction;
            hexBuilder.append(Character.forDigit(digit, 16));
            fraction -= digit;
            if (fraction == 0) break;
        }
        return hexBuilder.toString();
    }

    /** 对 UA 字符串按 4 字节分组做 XOR 哈希 */
    private static String computeUserAgentHash() {
        int hashResult = 0;
        int[] buffer = new int[HASH_BUFFER_SIZE];
        int bufferIndex = 0;

        for (int i = 0; i < USER_AGENT.length(); i++) {
            buffer[bufferIndex++] = USER_AGENT.charAt(i) & 0xFF;
            if (bufferIndex == HASH_BUFFER_SIZE) {
                hashResult ^= packLittleEndianInt(buffer, bufferIndex);
                bufferIndex = 0;
            }
        }

        if (bufferIndex > 0) {
            hashResult ^= packLittleEndianInt(buffer, bufferIndex);
        }

        return Integer.toHexString(hashResult);
    }

    /** 将字节数组按小端序打包为 int */
    private static int packLittleEndianInt(int[] buffer, int length) {
        int packed = 0;
        for (int i = 0; i < length; i++) {
            packed |= (buffer[i] & 0xFF) << (8 * i);
        }
        return packed;
    }

    /** 计算屏幕指纹：宽×高的十六进制，位数不足则使用随机数字回退 */
    private static String computeScreenFingerprint() {
        long screenProduct = (long) SCREEN_HEIGHT * SCREEN_WIDTH;
        String productString = String.valueOf(screenProduct);
        if (productString.matches("\\d{" + SCREEN_FINGERPRINT_MIN_DIGITS + ",}")) {
            return Long.toHexString(screenProduct);
        }
        return String.valueOf(31242 * Math.random())
                .replace(".", "")
                .substring(0, FALLBACK_FINGERPRINT_LENGTH);
    }

    /** 测试入口：打印生成的设备 ID */
    static void main() {
        System.out.println(generateDeviceId());
    }
}
