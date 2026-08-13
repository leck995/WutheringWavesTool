package com.kr.launcher.util;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP utilities matching C# KRSharedUtils/HttpUtils.cs.
 *
 * Supports:
 * - Per-request timeout (connect + read)
 * - Cancellation via a simple CancellationToken
 * - Response validation callback for custom success checks
 */
public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
    private static final CloseableHttpClient httpClient = HttpClients.custom()
            .setUserAgent("KR-Launcher/1.0")
            .disableContentCompression()
            .build();

    /**
     * Simple cancellation token (matches C#
     * CancellationTokenSource.IsCancellationRequested).
     */
    public static class CancellationToken {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        public void cancel() {
            cancelled.set(true);
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    /** HTTP response holder (matches C# KRHttpResponse<T>). */
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

    /** Response validation callback (matches C# ResponseValidCheckCallback<T>). */
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
            RequestConfig.Builder configBuilder = RequestConfig.custom();
            if (timeout != null && timeout > 0) {
                configBuilder
                        .setConnectionRequestTimeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .setResponseTimeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            HttpGet request = new HttpGet(URI.create(UrlUtils.encodeUrlPath(url)));
            // C# HttpUtils.cs:136 uses DecompressionMethods.GZip only.
            request.setHeader("Accept-Encoding", "gzip");
            request.setConfig(configBuilder.build());

            return httpClient.execute(request, response -> {
                int status = response.getCode();
                // C# HttpClient.GetStringAsync accepts any 2xx response.
                if (status >= 200 && status < 300) {
                    org.apache.hc.core5.http.HttpEntity entity = response.getEntity();
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    // Check if content is gzip compressed (magic bytes 0x1f 0x8b)
                    if (bytes.length >= 2 && bytes[0] == (byte) 0x1f && bytes[1] == (byte) 0x8b) {
                        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
                                java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(bais)) {
                            return new KRHttpResponse(url, 0, "", new String(gzis.readAllBytes(), "UTF-8"));
                        }
                    }
                    return new KRHttpResponse(url, 0, "", new String(bytes, "UTF-8"));
                }
                return new KRHttpResponse(url, status, "HTTP " + status, "");
            });
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
     * @param maxRetry                   retry count (excluding initial attempt).
     *                                   0 = single attempt. Negative = infinite.
     * @param timeout                    per-request timeout in ms; null = no
     *                                   timeout
     * @param cancellationToken          cancellation token; null = no cancellation
     * @param responseValidCheckCallback response validation callback; null =
     *                                   errorCode==0 is enough
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
