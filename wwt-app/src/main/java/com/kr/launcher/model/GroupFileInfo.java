package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
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
    @SerializedName("dest")
    public String dest;

    @SerializedName("srcFiles")
    public List<FileInfo> srcFiles;

    @SerializedName("dstFiles")
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
