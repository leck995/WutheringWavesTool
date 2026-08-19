package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GrayConfig {
    @SerializedName("graySwitch")
    public int graySwitch;

    @SerializedName("grayUrl")
    public String url = "";
}
