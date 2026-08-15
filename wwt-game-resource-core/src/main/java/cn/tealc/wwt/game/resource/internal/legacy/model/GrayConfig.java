package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GrayConfig {
    @JsonProperty("graySwitch")
    public int graySwitch;

    @JsonProperty("grayUrl")
    public String url = "";
}
