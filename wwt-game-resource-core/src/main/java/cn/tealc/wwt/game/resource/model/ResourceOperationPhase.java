package cn.tealc.wwt.game.resource.model;

/** User-visible phase derived from a legacy resource operation progress event. */
public enum ResourceOperationPhase {
    VERIFYING,
    DOWNLOADING,
    APPLYING,
    UNKNOWN
}
