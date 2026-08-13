package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import com.kr.launcher.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Version cache model loaded from version.txt.
 * Corresponds to KRResources/KRVersion.cs.
 *
 * Holds the history_game_list used for incremental update / old-file diff.
 */
public class KRVersion {
    private static final Logger log = LoggerFactory.getLogger(KRVersion.class);

    @SerializedName("history_game_list")
    public List<OldFileInfo> historyGameList = new ArrayList<>();

    /**
     * Load a KRVersion from the given cache file path.
     * Corresponds to C# KRVersion.Get(string cacheFilePath).
     *
     * Returns null if the file does not exist or cannot be parsed.
     */
    public static KRVersion get(String cacheFilePath) {
        if (cacheFilePath == null || cacheFilePath.isEmpty()) {
            return null;
        }
        File file = new File(cacheFilePath);
        if (!file.exists()) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            return JsonUtils.safeDeserialize(content, KRVersion.class);
        } catch (Exception e) {
            log.error("Read version.txt Fail, ErrorMessage: {}", e.getMessage(), e);
            return null;
        }
    }
}
