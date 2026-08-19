package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CdnConfig {
    @SerializedName("K1")
    public int k1 = 1;

    @SerializedName("K2")
    public int k2 = 1;

    @SerializedName("P")
    public int p;

    @SerializedName("url")
    public String url = "";

    @Override
    public String toString() {
        return "CdnConfig{url='" + url + "', K1=" + k1 + ", K2=" + k2 + ", P=" + p + "}";
    }
}
