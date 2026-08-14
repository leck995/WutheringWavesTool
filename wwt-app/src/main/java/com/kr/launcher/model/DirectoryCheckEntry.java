package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Directory check entry config.
 * Corresponds to KRResources/KRDirectoryCheckEntry.cs.
 * Used by KRDirectoryCheckTask to find and delete redundant files.
 */
public class DirectoryCheckEntry {
    @SerializedName("dir")
    public String directory;

    @SerializedName("exts")
    public List<String> extensions = new ArrayList<>();

    @SerializedName("recursive")
    public boolean recursive;

    public DirectoryCheckEntry() {
    }

    public DirectoryCheckEntry(String directory, boolean recursive) {
        this.directory = directory;
        this.recursive = recursive;
    }
}
