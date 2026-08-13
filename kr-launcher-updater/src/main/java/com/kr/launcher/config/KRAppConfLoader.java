package com.kr.launcher.config;

import com.kr.launcher.model.LauncherConfig;
import com.kr.launcher.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Loads and decodes the launcher configuration from KRApp.conf.
 * Corresponds to KRLauncherConfig.Init() (line 472-507)
 *
 * KRApp.conf is a Base64-encoded, XOR-obfuscated (key=99) JSON file
 * containing all launcher URLs, game IDs, and settings.
 */
public class KRAppConfLoader {
    private static final Logger log = LoggerFactory.getLogger(KRAppConfLoader.class);
    private static final byte XOR_KEY = 99;

    /**
     * Load config from KRApp.conf file path.
     * 1. Read raw bytes from file
     * 2. Base64 decode
     * 3. XOR each byte with key 99
     * 4. Parse as JSON
     */
    public static LauncherConfig loadFromKRAppConf(String krAppConfPath) {
        try {
            log.info("Loading KRApp.conf from: {}", krAppConfPath);

            // Read raw file content
            byte[] rawBytes = Files.readAllBytes(Path.of(krAppConfPath));
            String rawContent = new String(rawBytes).trim();

            // Base64 decode
            byte[] decoded = Base64.getDecoder().decode(rawContent);

            // XOR decode with key 99
            byte[] decrypted = new byte[decoded.length];
            for (int i = 0; i < decoded.length; i++) {
                decrypted[i] = (byte)(decoded[i] ^ XOR_KEY);
            }

            String json = new String(decrypted, "UTF-8");
            log.debug("Decrypted config: {}", json);

            // Parse JSON
            LauncherConfig config = JsonUtils.safeDeserialize(json, LauncherConfig.class);
            if (config == null) {
                throw new RuntimeException("Failed to parse decrypted KRApp.conf");
            }

            log.info("Loaded config: gameId={}, appId={}, configUrl={}",
                    config.gameId, config.appId, config.configUrl);
            return config;
        } catch (IOException e) {
            log.error("Failed to read KRApp.conf: {}", krAppConfPath, e);
            return null;
        } catch (Exception e) {
            log.error("Failed to decode/parse KRApp.conf", e);
            return null;
        }
    }

    /**
     * Decode KRApp.conf content (Base64 + XOR 99) and print the JSON.
     * Useful for inspecting the config.
     */
    public static String decodeKRAppConf(byte[] rawBytes) throws Exception {
        String rawContent = new String(rawBytes).trim();
        byte[] decoded = Base64.getDecoder().decode(rawContent);
        byte[] decrypted = new byte[decoded.length];
        for (int i = 0; i < decoded.length; i++) {
            decrypted[i] = (byte)(decoded[i] ^ XOR_KEY);
        }
        return new String(decrypted, "UTF-8");
    }
}
