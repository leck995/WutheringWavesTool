package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Group patch file info for multi-group sequential patching.
 * Corresponds to KRGroupFileInfo.cs.
 *
 * Each group has:
 *  - dest: the .krpdiff patch file path
 *  - srcFiles: source files in the game dir that will be consumed (deleted) by this patch
 *  - dstFiles: destination files produced by this patch (written to krpdiff_temp)
 */
public class GroupFileInfo {
    @JsonProperty("dest")
    public String dest;

    @JsonProperty("srcFiles")
    public List<FileInfo> srcFiles;

    @JsonProperty("dstFiles")
    public List<FileInfo> dstFiles;

    public long getSourceFilesSize() {
        long total = 0;
        if (srcFiles != null) {
            for (FileInfo fi : srcFiles) {
                total += fi.size;
            }
        }
        return total;
    }

    public long getDestinationFilesSize() {
        long total = 0;
        if (dstFiles != null) {
            for (FileInfo fi : dstFiles) {
                total += fi.size;
            }
        }
        return total;
    }
}
