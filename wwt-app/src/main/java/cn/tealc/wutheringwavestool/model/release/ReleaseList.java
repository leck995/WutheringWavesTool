package cn.tealc.wutheringwavestool.model.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: WutheringWavesToolResources
 * @description: 版本更新汇总
 * @author: Leck
 * @create: 2024-12-22 15:21
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseList {
    private Release preRelease; //预览版
    private Release latestRelease; //正式版

    public ReleaseList() {
    }

    public ReleaseList(Release preRelease, Release latestRelease) {
        this.preRelease = preRelease;
        this.latestRelease = latestRelease;
    }

    public Release getPreRelease() {
        return preRelease;
    }

    public void setPreRelease(Release preRelease) {
        this.preRelease = preRelease;
    }

    public Release getLatestRelease() {
        return latestRelease;
    }

    public void setLatestRelease(Release latestRelease) {
        this.latestRelease = latestRelease;
    }
}