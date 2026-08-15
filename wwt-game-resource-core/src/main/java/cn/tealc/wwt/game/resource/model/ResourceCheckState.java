package cn.tealc.wwt.game.resource.model;

/** The availability state reported after checking a game installation. */
public enum ResourceCheckState {
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    PRE_DOWNLOAD_AVAILABLE,
    REPAIR_REQUIRED,
    ROLLBACK_REQUIRED,
    UNKNOWN
}
