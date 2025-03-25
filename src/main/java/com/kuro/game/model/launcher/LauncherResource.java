package com.kuro.game.model.launcher;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kuro.game.model.game.DownloadResource;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 16:39
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LauncherResource {
    @JsonAlias("default")
    public UpdateData updateData;
    private List<String> keyFileCheckList;
    private DownloadResource downloadResource;
    private UpdateData predownload;

    public UpdateData getUpdateData() {
        return updateData;
    }

    public void setUpdateData(UpdateData updateData) {
        this.updateData = updateData;
    }

    public List<String> getKeyFileCheckList() {
        return keyFileCheckList;
    }

    public void setKeyFileCheckList(List<String> keyFileCheckList) {
        this.keyFileCheckList = keyFileCheckList;
    }

    public DownloadResource getDownloadResource() {
        return downloadResource;
    }

    public void setDownloadResource(DownloadResource downloadResource) {
        this.downloadResource = downloadResource;
    }

    public UpdateData getPredownload() {
        return predownload;
    }

    public void setPredownload(UpdateData predownload) {
        this.predownload = predownload;
    }
}