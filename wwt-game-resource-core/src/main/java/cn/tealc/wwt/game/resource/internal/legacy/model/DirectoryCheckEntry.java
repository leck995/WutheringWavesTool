package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Directory check entry config.
 * Corresponds to KRResources/KRDirectoryCheckEntry.cs.
 * Used by KRDirectoryCheckTask to find and delete redundant files.
 */
public class DirectoryCheckEntry {
    @JsonProperty("dir")
    public String directory;

    @JsonProperty("exts")
    public List<String> extensions = new ArrayList<>();

    @JsonProperty("recursive")
    public boolean recursive;

    public DirectoryCheckEntry() {
    }

    public DirectoryCheckEntry(String directory, boolean recursive) {
        this.directory = directory;
        this.recursive = recursive;
    }
}
