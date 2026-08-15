package cn.tealc.wwt.game.resource.internal.legacy.model;

public class ChunkInfo {
    public long start;
    public long end;
    public String md5;

    public ChunkInfo() {}

    public ChunkInfo(long start, long end, String md5) {
        this.start = start;
        this.end = end;
        this.md5 = md5;
    }

    /**
     * Computed length: End - Start + 1.
     * Matches C# KRChunkInfo.Length (computed property, not a field).
     */
    public long length() {
        return end - start + 1;
    }
}
