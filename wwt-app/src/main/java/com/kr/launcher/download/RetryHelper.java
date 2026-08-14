package com.kr.launcher.download;

/**
 * Retry helper matching C# KRRetryHelper.
 * Determines whether a download error is retryable.
 */
public final class RetryHelper {
    public static final int ERROR_DISK_FULL = -2147024784;
    public static final int ERROR_PERMISSION_DENY = -2147024891;
    public static final int ERROR_PROXY_EXCEPTION = -2146232800;

    private RetryHelper() {}

    /**
     * Returns true if the error is retryable.
     * Matches C# KRRetryHelper.CanRetry(errorCode, exceptionCode).
     *
     * Non-retryable: disk-full, permission-deny, and proxy-exception when
     * proxy usage is allowed (C# KRDownloader.GetInstance().AllowUseProxy()).
     *
     * @param errorCode      the C# HRESULT (CSharpErrorCode) from the exception
     * @param allowUseProxy  whether proxy usage is allowed (matches C#
     *                       KRDownloader.AllowUseProxy)
     */
    public static boolean canRetry(int errorCode, boolean allowUseProxy) {
        if (errorCode == ERROR_DISK_FULL
                || errorCode == ERROR_PERMISSION_DENY
                || (errorCode == ERROR_PROXY_EXCEPTION && allowUseProxy)) {
            return false;
        }
        return true;
    }

    /**
     * Overload assuming proxy is NOT allowed (proxy exceptions are retryable).
     * Use {@link #canRetry(int, boolean)} when proxy configuration is known.
     */
    public static boolean canRetry(int errorCode) {
        return canRetry(errorCode, false);
    }
}
