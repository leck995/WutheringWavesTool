package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileChunkInfo {
    @JsonProperty("start")
    public long start;

    @JsonProperty("end")
    public long end;

    @JsonProperty("md5")
    public String md5 = "";

    public long length() {
        return end - start + 1;
    }
}
