package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;

/**
 * File modification time cache entry.
 * Corresponds to KRResources/KRFileModifyTimeEntry.cs.
 * Used by KRFileChunkCheckTask to skip unchanged files during chunk MD5 verification.
 */
public class FileModifyTimeEntry {
    @SerializedName("path")
    public String path = "";

    @SerializedName("modifyTime")
    public long modifyTime;

    public FileModifyTimeEntry() {
    }

    public FileModifyTimeEntry(String path, long modifyTime) {
        this.path = path;
        this.modifyTime = modifyTime;
    }
}
