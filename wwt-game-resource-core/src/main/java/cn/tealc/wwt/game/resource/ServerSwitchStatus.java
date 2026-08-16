package cn.tealc.wwt.game.resource;

/** Locally derived server-switch state. A null active source means that it cannot be identified. */
public record ServerSwitchStatus(
        GameDownloadSource activeSource,
        boolean mainlandCacheReady,
        boolean bilibiliCacheReady,
        boolean recoveryPending) {

    public boolean isCacheReady(GameDownloadSource source) {
        return switch (source) {
            case MAINLAND -> mainlandCacheReady;
            case BILIBILI -> bilibiliCacheReady;
            case GLOBAL -> false;
        };
    }
}
