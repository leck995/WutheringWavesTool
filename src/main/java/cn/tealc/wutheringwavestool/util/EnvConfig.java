package cn.tealc.wutheringwavestool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 环境配置工具类，用于读取.env文件
 */
public class EnvConfig {
    private static final Logger LOG = LoggerFactory.getLogger(EnvConfig.class);
    private static Map<String, String> envConfig = new HashMap<>();
    private static boolean loaded = false;

    /**
     * 加载.env文件
     */
    public static void load() {
        if (loaded) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int index = line.indexOf('=');
                if (index > 0) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    envConfig.put(key, value);
                }
            }
            loaded = true;
            LOG.info("Environment config loaded successfully");
        } catch (IOException e) {
            LOG.error("Failed to load .env file: {}", e.getMessage());
        }
    }

    /**
     * 获取配置值
     */
    public static String get(String key) {
        if (!loaded) {
            load();
        }
        return envConfig.get(key);
    }

    /**
     * 获取配置值，如果不存在则返回默认值
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数配置值
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid integer value for key {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    // 具体配置项的便捷方法
    public static String getApiUrl() {
        return get("HOST_API_URL");
    }

    public static String getAuthCode() {
        return get("HOST_AUTH_CODE");
    }

    public static String getApiToken() {
        return get("HOST_API_TOKEN");
    }

    public static String getUploadFolder() {
        return get("HOST_UPLOAD_FOLDER", "wuwa-records");
    }

    public static int getTimeout() {
        return getInt("HOST_TIMEOUT", 120000);
    }
}