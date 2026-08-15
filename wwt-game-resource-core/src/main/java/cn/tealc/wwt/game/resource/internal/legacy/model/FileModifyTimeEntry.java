package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * File modification time cache entry.
 * Corresponds to KRResources/KRFileModifyTimeEntry.cs.
 * Used by KRFileChunkCheckTask to skip unchanged files during chunk MD5 verification.
 */
public class FileModifyTimeEntry {
    @JsonProperty("path")
    public String path = "";

    @JsonProperty("modifyTime")
    public long modifyTime;

    public FileModifyTimeEntry() {
    }

    public FileModifyTimeEntry(String path, long modifyTime) {
        this.path = path;
        this.modifyTime = modifyTime;
    }
}
