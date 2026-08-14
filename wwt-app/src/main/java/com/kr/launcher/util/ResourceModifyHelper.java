package com.kr.launcher.util;

import com.kr.launcher.model.FileModifyTimeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for loading/saving file modification time cache.
 * Corresponds to KRResources/KRResourceModifyHelper.cs.
 * Used by FileChunkCheckTask to skip unchanged files.
 */
public final class ResourceModifyHelper {
    private static final Logger log = LoggerFactory.getLogger(ResourceModifyHelper.class);

    /**
     * Load modify time entries from cache file.
     * 
     * @param modifyCacheFilePath path to the JSON cache file
     * @return list of entries (empty if file doesn't exist or fails to parse)
     */
    @SuppressWarnings("unchecked")
    public static List<FileModifyTimeEntry> loadModifyTimeEntries(String modifyCacheFilePath) {
        List<FileModifyTimeEntry> result = new ArrayList<>();
        try {
            File file = new File(modifyCacheFilePath);
            if (!file.exists()) {
                log.info("Modify time cache file: {} does not exist", modifyCacheFilePath);
                return result;
            }
            String json = FileUtils.read(modifyCacheFilePath);
            if (json != null && !json.isEmpty()) {
                List<FileModifyTimeEntry> list = JsonUtils.safeDeserializeList(json, FileModifyTimeEntry.class);
                if (list != null) {
                    result = list;
                }
            }
        } catch (Exception e) {
            log.error("Failed to read data from file: {}", modifyCacheFilePath, e);
        }
        return result;
    }

    /**
     * Save modify time entries to cache file.
     * 
     * @param modifyCacheFilePath path to the JSON cache file
     * @param entries             list of entries to save
     */
    public static void saveModifyTimeEntries(String modifyCacheFilePath, List<FileModifyTimeEntry> entries) {
        try {
            String json = JsonUtils.serialize(entries);
            FileUtils.write(json, modifyCacheFilePath);
        } catch (Exception e) {
            log.error("Failed to write data to file: {}", modifyCacheFilePath, e);
        }
    }

    private ResourceModifyHelper() {
    }
}
