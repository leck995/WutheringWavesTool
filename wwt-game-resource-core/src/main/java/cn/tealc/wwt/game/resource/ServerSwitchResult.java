package cn.tealc.wwt.game.resource;

/** Result of caching, repairing, or switching the reduced server component set. */
public record ServerSwitchResult(
        GameDownloadSource source,
        boolean appliedToGame,
        boolean cacheReady,
        String resourceVersion) {
}
