package cn.tealc.wwt.game.resource.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;

/**
 * 轻量 HTTP 工具，基于 JDK HttpURLConnection。
 */
public final class HttpUtils {
    private HttpUtils() {}

    /** 建立连接：设超时、User-Agent。disableProxy 为 true 时使用 NO_PROXY。 */
    public static HttpURLConnection openConnection(String url, int connectTimeoutMs, int readTimeoutMs,
            boolean disableProxy) throws IOException {
        URI uri = URI.create(url);
        Proxy proxy = disableProxy ? Proxy.NO_PROXY : null;
        HttpURLConnection conn;
        if (proxy != null) {
            conn = (HttpURLConnection) uri.toURL().openConnection(proxy);
        } else {
            conn = (HttpURLConnection) uri.toURL().openConnection();
        }
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setRequestProperty("Accept-Encoding", "identity");
        return conn;
    }
}