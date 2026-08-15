package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.launcher.UpdateData;

/** A checked resource release. Its manifest data remains owned by the core. */
public final class GameResourceRelease {
    private final GameDownloadSource source;
    private final UpdateData updateData;
    private final String installedVersion;

    GameResourceRelease(GameDownloadSource source, UpdateData updateData, String installedVersion) {
        this.source = source;
        this.updateData = updateData;
        this.installedVersion = installedVersion != null ? installedVersion : "";
    }

    public GameDownloadSource source() { return source; }
    public String installedVersion() { return installedVersion; }
    public String latestVersion() { return updateData.getVersion(); }

    UpdateData updateData() { return updateData; }
}
