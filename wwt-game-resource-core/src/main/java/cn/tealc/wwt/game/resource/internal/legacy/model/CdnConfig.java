package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CdnConfig {
    @JsonProperty("K1")
    public int k1 = 1;

    @JsonProperty("K2")
    public int k2 = 1;

    @JsonProperty("P")
    public int p;

    @JsonProperty("url")
    public String url = "";

    @Override
    public String toString() {
        return "CdnConfig{url='" + url + "', K1=" + k1 + ", K2=" + k2 + ", P=" + p + "}";
    }
}
