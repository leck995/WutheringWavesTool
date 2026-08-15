package cn.tealc.wutheringwavestool.model;

import cn.tealc.download.GameDownloadSource;

public enum SourceType {
    DEFAULT,
    WE_GAME,
    BILIBILI,
    GLOBAL;

    public GameDownloadSource toGameDownloadSource() {
        return switch (this) {
            case BILIBILI -> GameDownloadSource.BILIBILI;
            case GLOBAL -> GameDownloadSource.GLOBAL;
            case DEFAULT, WE_GAME -> GameDownloadSource.MAINLAND;
        };
    }
}
