package com.kr.launcher.model;

/**
 * Update flow state constants.
 * Corresponds to KRResources/KRUpdateState.cs.
 */
public final class UpdateState {
    public static final int CHECK = 0;

    public static final int DOWNLOADING = 1;

    public static final int CHECK_DOWNLOADED_FILE = 9;

    public static final int APPLY = 2;

    public static final int CHECK_MD5 = 3;

    public static final int RE_DOWNLOAD = 4;

    public static final int MOVE = 5;

    public static final int COMPLETE = 6;

    public static final int FAIL = 7;

    private UpdateState() {
    }
}
