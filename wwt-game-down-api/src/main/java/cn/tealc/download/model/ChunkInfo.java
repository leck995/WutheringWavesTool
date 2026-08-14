package cn.tealc.download.model;

/**
 * 服务器在清单中预定义的分块（字节区间 + 该区间的 MD5）。
 * 用于按块下载并逐块校验，对应 KR Launcher 的 FileChunkInfo/ChunkInfo。
 */
public record ChunkInfo(long start, long end, String md5) {

    public long length() {
        return end - start + 1;
    }
}