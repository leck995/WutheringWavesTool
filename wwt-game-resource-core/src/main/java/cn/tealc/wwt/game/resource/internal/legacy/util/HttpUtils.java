package cn.tealc.wwt.game.resource.internal.legacy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

/**
 * HTTP utilities matching C# KRSharedUtils/HttpUtils.cs.
 *
 * <p>与原实现保持相同公共 API，但内部改用 JDK
 * {@link java.net.http.HttpClient}（模块已依赖），避免引入 Apache HttpClient 5。</p>
 *
 * <p>Supports:
 * - Per-request timeout (connect + read)
 * - Cancellation via a simple CancellationToken
 * - Response validation callback for custom success checks</p>
 */
public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Simple cancellation token (matches C# CancellationTokenSource.IsCancellationRequested). */
    public static class CancellationToken {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        public void cancel() {
            cancelled.set(true);
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    /** HTTP response holder (matches C# KRHttpResponse&lt;T&gt;). */
    public static class KRHttpResponse {
        public String url;
        public int errorCode;
        public String errorMessage;
        public String data;

        public KRHttpResponse(String url, int errorCode, String errorMessage, String data) {
            this.url = url;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.data = data;
        }
    }

    /** Response validation callback (matches C# ResponseValidCheckCallback&lt;T&gt;). */
    @FunctionalInterface
    public interface ResponseValidCheckCallback {
        boolean isValid(KRHttpResponse response);
    }

    /**
     * GET request returning string response.
     * Corresponds to C# HttpUtils.GetString(url, timeout).
     *
     * @param url     URL to fetch
     * @param timeout overall request timeout in ms; null = no timeout
     * @return KRHttpResponse with data on success, error info on failure
     */
    public static KRHttpResponse getString(String url, Long timeout) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(UrlUtils.encodeUrlPath(url)))
                    .header("Accept-Encoding", "gzip")
                    .GET();
            if (timeout != null && timeout > 0) {
                builder.timeout(Duration.ofMillis(timeout));
            }
            HttpRequest request = builder.build();

            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            // C# HttpClient.GetStringAsync accepts any 2xx response.
            if (status >= 200 && status < 300) {
                byte[] bytes = response.body();
                // Check if content is gzip compressed (magic bytes 0x1f 0x8b)
                byte[] data = bytes;
                if (bytes.length >= 2 && bytes[0] == (byte) 0x1f && bytes[1] == (byte) 0x8b) {
                    try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                        data = gzis.readAllBytes();
                    }
                }
                return new KRHttpResponse(url, 0, "", new String(data, "UTF-8"));
            }
            return new KRHttpResponse(url, status, "HTTP " + status, "");
        } catch (Exception e) {
            log.error("HTTP GET failed: {}", url, e);
            return new KRHttpResponse(url, -1, e.getMessage() != null ? e.getMessage() : "", "");
        }
    }

    /** Backward-compatible overload: no timeout. */
    public static String getString(String url) {
        KRHttpResponse resp = getString(url, null);
        if (resp.errorCode == 0 && resp.data != null && !resp.data.isEmpty()) {
            return resp.data;
        }
        throw new RuntimeException(resp.errorMessage != null && !resp.errorMessage.isEmpty()
                ? resp.errorMessage
                : "HTTP error code " + resp.errorCode);
    }

    /**
     * Fetch a string from URL, falling back to backUpUrl on failure.
     * Corresponds to C# HttpUtils.GetStringWithBackUpUrl.
     *
     * @param url                        primary URL
     * @param backUpUrl                  backup URL (may be null)
     * @param maxRetry                   retry count (excluding initial attempt). 0 = single attempt. Negative = infinite.
     * @param timeout                    per-request timeout in ms; null = no timeout
     * @param cancellationToken          cancellation token; null = no cancellation
     * @param responseValidCheckCallback response validation callback; null = errorCode==0 is enough
     * @return KRHttpResponse (last attempt's result)
     */
    public static KRHttpResponse getStringWithBackUpUrl(
            String url, String backUpUrl, int maxRetry,
            Long timeout, CancellationToken cancellationToken,
            ResponseValidCheckCallback responseValidCheckCallback) {

        int num = 0;
        KRHttpResponse response = null;
        while (true) {
            if (cancellationToken != null && cancellationToken.isCancelled()) {
                break;
            }

            String currentUrl = (backUpUrl != null && !backUpUrl.isEmpty() && num % 2 == 1)
                    ? backUpUrl
                    : url;
            log.info("HTTP GET attempt {}, url: {}", num + 1, currentUrl);

            response = getString(currentUrl, timeout);

            if (response.errorCode == 0) {
                if (responseValidCheckCallback == null || responseValidCheckCallback.isValid(response)) {
                    return response;
                }
            }

            if (maxRetry >= 0 && num >= maxRetry) {
                break;
            }
            num++;

            if (maxRetry < 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return response != null ? response : new KRHttpResponse(url, -1, "No response", "");
    }

    /** Backward-compatible overload: no timeout, no cancellation, no validation. */
    public static String getStringWithBackUpUrl(String url, String backUpUrl, int maxRetry) {
        KRHttpResponse resp = getStringWithBackUpUrl(url, backUpUrl, maxRetry,
                null, null, null);
        if (resp.errorCode == 0 && resp.data != null) {
            return resp.data;
        }
        log.error("HTTP GET with backup URL failed: {}", resp.errorMessage);
        return null;
    }

    /**
     * Async GET with backup URL, invoking callback on completion.
     * Corresponds to C# HttpUtils.GetStringAsyncWithBackUpUrl.
     */
    public static void getStringAsyncWithBackUpUrl(
            String url, String backUpUrl,
            int maxRetry, Long timeout,
            CancellationToken cancellationToken,
            ResponseValidCheckCallback responseValidCheckCallback,
            ResponseCallback callback) {
        CompletableFuture.runAsync(() -> {
            KRHttpResponse resp = getStringWithBackUpUrl(url, backUpUrl, maxRetry,
                    timeout, cancellationToken, responseValidCheckCallback);
            callback.onResponse(resp.errorCode == 0, resp);
        });
    }

    @FunctionalInterface
    public interface ResponseCallback {
        void onResponse(boolean success, KRHttpResponse response);
    }
}
