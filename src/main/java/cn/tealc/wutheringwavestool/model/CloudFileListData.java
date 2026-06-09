package cn.tealc.wutheringwavestool.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudFileListData {
    private Map<String, List<CloudFileItem>> folders;
    private int totalFolders;
    private int maxFolders;

    public Map<String, List<CloudFileItem>> getFolders() {
        return folders;
    }

    public void setFolders(Map<String, List<CloudFileItem>> folders) {
        this.folders = folders;
    }

    public int getTotalFolders() {
        return totalFolders;
    }

    public void setTotalFolders(int totalFolders) {
        this.totalFolders = totalFolders;
    }

    public int getMaxFolders() {
        return maxFolders;
    }

    public void setMaxFolders(int maxFolders) {
        this.maxFolders = maxFolders;
    }
}
