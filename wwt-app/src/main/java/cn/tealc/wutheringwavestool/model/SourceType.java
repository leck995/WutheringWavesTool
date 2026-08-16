package cn.tealc.wutheringwavestool.model;

import cn.tealc.wwt.game.resource.GameDownloadSource;

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

    public static SourceType fromGameDownloadSource(GameDownloadSource source) {
        return switch (source) {
            case BILIBILI -> BILIBILI;
            case GLOBAL -> GLOBAL;
            case MAINLAND -> DEFAULT;
        };
    }
}
