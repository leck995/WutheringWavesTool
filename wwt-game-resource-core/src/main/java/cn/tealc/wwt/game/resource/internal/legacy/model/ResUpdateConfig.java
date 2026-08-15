package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ResUpdateConfig {
    @JsonProperty("cdnList")
    public List<CdnConfig> cdnList;

    @JsonProperty("config")
    public ResConfig config;
}
