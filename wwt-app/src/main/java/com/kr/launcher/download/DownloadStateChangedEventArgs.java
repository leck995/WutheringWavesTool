package com.kr.launcher.download;

import java.util.HashMap;
import java.util.Map;

/**
 * Event args for download state changes.
 * Corresponds to KRDownloadStateChangedEventArgs.cs.
 *
 * Carries the new state, error code (matching C# KRDownloadError), the C#
 * HRESULT (for IOException-based failures), an error message, and arbitrary
 * extension data used by callers for tracking.
 */
public class DownloadStateChangedEventArgs {
    public final String taskId;
    public DownloadState state;
    public int errorCode;
    public int cSharpErrorCode;
    public String errorMessage = "";
    public final Map<String, Object> extData = new HashMap<>();

    public DownloadStateChangedEventArgs(String taskId) {
        this.taskId = taskId;
    }
}
