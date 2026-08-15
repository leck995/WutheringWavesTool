package cn.tealc.wwt.game.resource.internal.legacy.util;

import cn.tealc.wwt.game.resource.internal.legacy.model.DownloadRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Tracks cumulative download time across sessions.
 * Corresponds to KRResources/UpdateWatch.cs.
 * Caches to KRDownloadRecordCache.json in the game cache dir.
 */
public final class UpdateWatch {
    private static final Logger log = LoggerFactory.getLogger(UpdateWatch.class);
    private static final String CACHE_FILE = "KRDownloadRecordCache.json";

    /**
     * Update the cumulative download time cache.
     * @param cacheDir game cache directory
     * @param type download type (e.g. "download", "preDownload", "fix")
     * @param usingVersion source version
     * @param targetVersion target version
     * @param costTime additional time in ms
     */
    public static void updateDownloadingConsumingCache(
            String cacheDir, String type, String usingVersion, String targetVersion, long costTime) {
        try {
            String path = PathUtils.combine(cacheDir, CACHE_FILE);
            long total = 0;
            if (new File(path).exists()) {
                String json = FileUtils.read(path);
                DownloadRecord record = JsonUtils.safeDeserialize(json, DownloadRecord.class);
                if (record != null
                        && usingVersion.equals(record.usingVersion)
                        && targetVersion.equals(record.targetVersion)
                        && type.equals(record.type)) {
                    total += record.costTime;
                }
            }
            total += costTime;

            DownloadRecord newRecord = new DownloadRecord();
            newRecord.usingVersion = usingVersion;
            newRecord.targetVersion = targetVersion;
            newRecord.costTime = total;
            newRecord.type = type;

            FileUtils.ensureDir(cacheDir);
            FileUtils.write(JsonUtils.serialize(newRecord), path);
        } catch (Exception e) {
            log.warn("UpdateDownloadingConsumingCache Fail: {}", e.getMessage());
        }
    }

    /**
     * Get cumulative download time for a given version pair.
     */
    public static long getDownloadingConsumingTime(
            String cacheDir, String type, String usingVersion, String targetVersion) {
        long total = 0;
        try {
            String path = PathUtils.combine(cacheDir, CACHE_FILE);
            if (new File(path).exists()) {
                String json = FileUtils.read(path);
                DownloadRecord record = JsonUtils.safeDeserialize(json, DownloadRecord.class);
                if (record != null
                        && usingVersion.equals(record.usingVersion)
                        && targetVersion.equals(record.targetVersion)
                        && type.equals(record.type)) {
                    total += record.costTime;
                }
            }
        } catch (Exception e) {
            log.warn("GetDownloadingConsumingTime Fail: {}", e.getMessage());
        }
        return total;
    }

    private UpdateWatch() {
    }
}
