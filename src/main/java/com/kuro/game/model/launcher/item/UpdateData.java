package com.kuro.game.model.launcher.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Comparator;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-27 22:53
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateData {
    private List<CdnData> cdnList; //cdn
    private String resources; //resource.json地址
    private String resourcesBasePath; //下载地址前缀
    public ResourceChunk resourceChunk;
    public ResourcesDiff resourcesDiff;
    private String version; //资源版本
    private Config config;

    /**
     * @description:
     * @param:		获取resource.json的下载地址
     * @return  java.lang.String
     * @date:   2024/10/27
     */
    public String getResourceJsonUrl(){
        CdnData cdnData = cdnList.getFirst();
        return cdnData.getUrl() + resources;
    }


    public List<CdnData> getCdnList() {
        return cdnList;
    }

    public void setCdnList(List<CdnData> cdnList) {
        this.cdnList = cdnList;
        //依据ping进行排序
        cdnList.sort(Comparator.comparingInt(CdnData::getPing));
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