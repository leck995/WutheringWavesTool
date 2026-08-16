package cn.tealc.wwt.game.resource.model;

/** A concrete processing phase for one downloaded file. */
public enum DownloadPhase {
    PREPARING,
    DOWNLOADING,
    VERIFYING,
    MERGING
}
