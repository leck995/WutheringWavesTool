package com.kr.launcher.model;

/**
 * Repair flow state constants.
 * Corresponds to KRResources/KRRepairState.cs.
 */
public final class RepairState {
    public static final int CHECK = 0;

    public static final int DOWNLOADING = 1;

    public static final int CHECK_DOWNLOADED_FILE = 9;

    public static final int MOVE = 5;

    public static final int COMPLETE = 6;

    public static final int FAIL = 7;

    public static final int COMPLETE_CHECK_ALL_VALID = 8;

    private RepairState() {
    }
}
