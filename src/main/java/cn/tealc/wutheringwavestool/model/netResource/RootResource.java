package cn.tealc.wutheringwavestool.model.netResource;

import java.util.List;
import java.util.Map;

/**
 * @program: WutheringWavesToolResources
 * @description:
 * @author: Leck
 * @create: 2024-10-06 16:46
 */
public class RootResource {
    private String version;
    private Map<String, List<Resource>> resources;


    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, List<Resource>> getResources() {
        return resources;
    }

    public void setResources(Map<String, List<Resource>> resources) {
        this.resources = resources;
    }
}