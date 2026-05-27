package com.kuro.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MusicData {
    @JsonProperty("Albums")
    private List<Album> albums;

    // 无参构造函数
    public MusicData() {
    }

    // Getter和Setter方法
    public List<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(List<Album> albums) {
        this.albums = albums;
    }

    @Override
    public String toString() {
        return "MusicData{" +
                "albums=" + albums +
                '}';
    }
}