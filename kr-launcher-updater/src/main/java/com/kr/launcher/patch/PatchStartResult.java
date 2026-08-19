package com.kr.launcher.patch;

/**
 * Result of starting the HPatchZ patch process.
 * Corresponds to KRPatchStartResult.cs
 */
public class PatchStartResult {
    public int errorCode;
    public String errorMessage = "";

    public PatchStartResult() {}

    public PatchStartResult(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
