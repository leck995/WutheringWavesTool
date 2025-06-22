package com.kuro.game.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 16:16
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameResource {
    private List<FileInfo> resource;

    public List<FileInfo> getResource() {
        return resource;
    }

    public void setResource(List<FileInfo> resource) {
        this.resource = resource;
    }
}