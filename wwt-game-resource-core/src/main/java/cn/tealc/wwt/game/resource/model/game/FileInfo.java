package cn.tealc.wwt.game.resource.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class FileInfo {
    private String dest;
    private String md5;
    private Long size;
    private List<ChunkInfo> chunkInfos;

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public List<ChunkInfo> getChunkInfos() {
        return chunkInfos;
    }

    public void setChunkInfos(List<ChunkInfo> chunkInfos) {
        this.chunkInfos = chunkInfos;
    }
}
