package com.kr.launcher.model;

/**
 * Resource state constants for CheckUpdateFlow state machine.
 * Corresponds to KRResources/KRResState.cs.
 */
public final class ResState {
    public static final int DOWNLOAD = 0;

    public static final int CONTINUE_DOWNLOAD = 1;

    public static final int ENTER_GAME = 2;

    public static final int UPDATE = 3;

    public static final int REPAIR = 4;

    public static final int ONLY_ENTER_GAME = 5;

    private ResState() {
    }
}
