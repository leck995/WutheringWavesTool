package cn.tealc.wutheringwavestool.model;

import java.io.File;

public class CloudUploadItem {
    private final String playerId;
    private final File file;

    public CloudUploadItem(String playerId, File file) {
        this.playerId = playerId;
        this.file = file;
    }

    public String getPlayerId() {
        return playerId;
    }

    public File getFile() {
        return file;
    }
}
