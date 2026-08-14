package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * File info entry in index file.
 * Corresponds to KRFileInfo.cs.
 */
public class FileInfo implements Comparable<FileInfo> {
    @SerializedName("dest")
    public String path = "";

    @SerializedName("md5")
    public String md5 = "";

    @SerializedName("size")
    public long size;

    @SerializedName("fromFolder")
    public String fromFolder;

    @SerializedName("chunkInfos")
    public List<FileChunkInfo> chunkInfos;

    public transient boolean isDownloading;

    @Override
    public int compareTo(FileInfo other) {
        if (other == null) return 1;
        if (this.isDownloading && !other.isDownloading) return 1;
        if (other.isDownloading && !this.isDownloading) return -1;
        return Long.compare(this.size, other.size);
    }

    /**
     * Check if two FileInfo represent the same file (same md5 only).
     * Corresponds to KRFileInfo.IsSameFile().
     * C# compares only Md5 (case-sensitive); path is not checked.
     * C# Md5 defaults to "" (never null), so null-coalesce to "".
     */
    public static boolean isSameFile(FileInfo a, FileInfo b) {
        if (a == null || b == null) return false;
        String amd5 = a.md5 != null ? a.md5 : "";
        String bmd5 = b.md5 != null ? b.md5 : "";
        return amd5.equals(bmd5);
    }

    /**
     * Check if all chunks match between two FileInfo (md5 only, case-sensitive).
     * Corresponds to KRFileInfo.IsAllChunkSame().
     * C# compares only chunk Md5 (not Start/End).
     */
    public static boolean isAllChunkSame(FileInfo a, FileInfo b) {
        if (a == null || b == null) return false;
        if (a.chunkInfos != null && b.chunkInfos == null) return false;
        if (b.chunkInfos != null && a.chunkInfos == null) return false;
        if (a.chunkInfos != null && b.chunkInfos != null) {
            if (a.chunkInfos.size() != b.chunkInfos.size()) return false;
            for (int i = 0; i < a.chunkInfos.size(); i++) {
                String ma = a.chunkInfos.get(i).md5;
                String mb = b.chunkInfos.get(i).md5;
                if (ma == null) ma = "";
                if (mb == null) mb = "";
                if (!ma.equals(mb)) return false;
            }
        }
        return true;
    }
}
