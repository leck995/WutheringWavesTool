package com.kr.launcher.model;

/**
 * Apply type constants used to select the resource list from IndexFile.
 * Corresponds to KRResources/KRApplyType.cs.
 */
public final class ApplyType {
    public static final String DEFAULT = "";

    public static final String PATCH = "patch";

    public static final String GROUP = "group";

    public static final String ZIP = "zip";

    private ApplyType() {
    }
}
