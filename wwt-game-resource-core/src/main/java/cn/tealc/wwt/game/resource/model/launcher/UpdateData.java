package cn.tealc.wwt.game.resource.model.launcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Comparator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class UpdateData {
    private List<CdnData> cdnList;
    private String resources;
    private String resourcesBasePath;
    private String version;

    public List<CdnData> getCdnList() {
        return cdnList;
    }

    public void setCdnList(List<CdnData> cdnList) {
        this.cdnList = cdnList;
        if (cdnList != null) {
            cdnList.sort(Comparator.comparingInt(CdnData::getPing));
        }
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public String getResourcesBasePath() {
        return resourcesBasePath;
    }

    public void setResourcesBasePath(String resourcesBasePath) {
        this.resourcesBasePath = resourcesBasePath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
