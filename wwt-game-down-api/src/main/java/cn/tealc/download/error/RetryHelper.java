package cn.tealc.download.error;

/**
 * 可重试性判定与 HRESULT 分类，对应 KR Launcher 的 RetryHelper。
 */
public final class RetryHelper {
    private RetryHelper() {}

    public static final int ERROR_PROXY_EXCEPTION = 0x80072EFD;
    public static final int ERROR_DISK_FULL = 0x80070070;
    public static final int ERROR_PERMISSION_DENY = 0x80070005;
    public static final int ERROR_WIN32 = 0;

    /**
     * 判断某个异常是否可重试。
     * 磁盘满、权限不足等属于不可重试错误，网络类错误可重试。
     */
    public static boolean canRetry(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("no space") || msg.contains("disk full") || msg.contains("not enough space")
                || msg.contains("permission") || msg.contains("access denied") || msg.contains("access is denied")) {
            return false;
        }
        return true;
    }

    /** 由异常推断 HRESULT 分类（仅用于日志 / 可读性）。 */
    public static int classifyHResult(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        boolean proxyLike = msg.contains("proxy")
                || (e instanceof java.net.ConnectException || e instanceof java.net.SocketException)
                && msg.contains("proxy");
        if (proxyLike) {
            return ERROR_PROXY_EXCEPTION;
        }
        if (msg.contains("no space") || msg.contains("disk full") || msg.contains("not enough space")) {
            return ERROR_DISK_FULL;
        }
        if (msg.contains("permission") || msg.contains("access denied") || msg.contains("access is denied")) {
            return ERROR_PERMISSION_DENY;
        }
        return ERROR_WIN32;
    }
}