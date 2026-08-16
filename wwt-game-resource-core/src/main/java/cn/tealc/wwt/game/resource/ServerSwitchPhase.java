package cn.tealc.wwt.game.resource;

/** Execution phases for a server-switch operation. */
public enum ServerSwitchPhase {
    PREPARING,
    DOWNLOADING,
    VERIFYING,
    APPLYING,
    ROLLING_BACK
}
