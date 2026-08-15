package cn.tealc.download.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class GameResourceList {
    private List<FileInfo> resource;

    public List<FileInfo> getResource() {
        return resource;
    }

    public void setResource(List<FileInfo> resource) {
        this.resource = resource;
    }
}
