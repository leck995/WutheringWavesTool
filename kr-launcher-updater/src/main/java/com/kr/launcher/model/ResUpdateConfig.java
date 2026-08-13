package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResUpdateConfig {
    @SerializedName("cdnList")
    public List<CdnConfig> cdnList;

    @SerializedName("config")
    public ResConfig config;
}
