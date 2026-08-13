package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Mixed file info for zip/patch entries.
 * Corresponds to KRMixedFileInfo.cs.
 */
public class MixedFileInfo {
    @SerializedName("dest")
    public String dest = "";

    @SerializedName("entries")
    public List<FileInfo> entries;

    public long getEntriesSize() {
        long total = 0;
        if (entries != null) {
            for (FileInfo fi : entries) {
                total += fi.size;
            }
        }
        return total;
    }
}
