package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;

public class FileChunkInfo {
    @SerializedName("start")
    public long start;

    @SerializedName("end")
    public long end;

    @SerializedName("md5")
    public String md5 = "";

    public long length() {
        return end - start + 1;
    }
}
