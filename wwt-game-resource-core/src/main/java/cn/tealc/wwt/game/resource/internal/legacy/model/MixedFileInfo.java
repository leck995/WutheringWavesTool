package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Mixed file info for zip/patch entries.
 * Corresponds to KRMixedFileInfo.cs.
 */
public class MixedFileInfo {
    @JsonProperty("dest")
    public String dest = "";

    @JsonProperty("entries")
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
