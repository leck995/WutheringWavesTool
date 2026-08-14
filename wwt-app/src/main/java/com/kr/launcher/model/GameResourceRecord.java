package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import com.kr.launcher.util.FileUtils;
import com.kr.launcher.util.JsonUtils;
import com.kr.launcher.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records the list of game resource files currently installed in the game
 * directory.
 * Stored at {gameDir}/LocalGameResources.json.
 *
 * Corresponds to KRGameResourceRecord.cs. Critical for state tracking:
 * after MoveFileTask moves files into the game dir, it MUST call UpdateResource
 * so that the next CheckUpdateFlow can determine which files are already
 * installed.
 */
public class GameResourceRecord {
    private static final Logger log = LoggerFactory.getLogger(GameResourceRecord.class);

    /**
     * Lazily-built modify map used by UpdateResource/DeleteResource to dedupe by
     * path.
     */
    private transient Map<String, FileInfo> resModifyDict;

    @SerializedName("resource")
    public List<FileInfo> resource;

    public GameResourceRecord() {
    }

    public GameResourceRecord(List<FileInfo> resource) {
        this.resource = resource;
    }

    /** Load record from disk. Returns null if file missing or parse fails. */
    public static GameResourceRecord get(String configPath) {
        if (configPath == null || configPath.isEmpty())
            return null;
        if (!FileUtils.exists(configPath))
            return null;
        try {
            String json = FileUtils.read(configPath);
            if (json == null || json.isEmpty())
                return null;
            return JsonUtils.safeDeserialize(json, GameResourceRecord.class);
        } catch (Exception e) {
            log.warn("Failed to read GameResourceRecord: {}", e.getMessage());
            return null;
        }
    }

    /** Persist record to disk. */
    public static void save(GameResourceRecord instance, String configPath) {
        if (configPath == null || configPath.isEmpty())
            return;
        if (instance == null)
            return;
        GameResourceRecord toSave = instance;
        if (instance.resModifyDict != null) {
            toSave = new GameResourceRecord(new ArrayList<>(instance.resModifyDict.values()));
        }
        try {
            FileUtils.ensureDir(PathUtils.getParentDir(configPath));
            String json = JsonUtils.serialize(toSave);
            if (json != null) {
                FileUtils.write(json, configPath);
            }
        } catch (Exception e) {
            log.warn("Save GameResource Fail, ErrorMessage: {}", e.getMessage());
        }
    }

    /**
     * Merge moved file infos into the record, replacing any existing entry with the
     * same path.
     */
    public void updateResource(List<FileInfo> fileInfos) {
        ensureModifyDict();
        if (fileInfos == null)
            return;
        for (FileInfo fi : fileInfos) {
            if (fi != null && fi.path != null && !fi.path.isEmpty()) {
                resModifyDict.put(fi.path, fi);
            }
        }
    }

    /** Remove the given paths from the record. */
    public void deleteResource(List<String> files) {
        ensureModifyDict();
        if (files == null)
            return;
        for (String file : files) {
            if (file != null && !file.isEmpty()) {
                resModifyDict.remove(file);
            }
        }
    }

    private void ensureModifyDict() {
        if (resModifyDict == null) {
            resModifyDict = new HashMap<>();
            if (resource != null) {
                for (FileInfo fi : resource) {
                    if (fi != null && fi.path != null) {
                        resModifyDict.put(fi.path, fi);
                    }
                }
            }
        }
    }
}
