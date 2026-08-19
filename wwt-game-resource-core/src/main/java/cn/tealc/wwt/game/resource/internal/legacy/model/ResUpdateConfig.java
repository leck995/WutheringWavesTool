package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ResUpdateConfig {
    @JsonProperty("cdnList")
    public List<CdnConfig> cdnList;

    @JsonProperty("config")
    public ResConfig config;

    /** 资源清单 URL（resource.json），对应 index.json 中 default.resources。 */
    @JsonProperty("resources")
    public String resources;

    /** 资源下载基路径，对应 index.json 中 default.resourcesBasePath。 */
    @JsonProperty("resourcesBasePath")
    public String resourcesBasePath;
}
