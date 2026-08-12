package com.kuro.kujiequ;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.util.HTTPRequestMultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 库街区 API 基础设施：替代原 BaseTask。
 * 承载 HttpClient / ObjectMapper / token 缓存 / 请求头构建 / token 过期回调。
 * 由调用方注入，单例使用。
 *
 * @author Leck
 */
public final class KujiequApiContext {
    private static final Logger log = LoggerFactory.getLogger(KujiequApiContext.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TokenExpiredCallback tokenExpiredCallback;

    // B-At token 缓存（原 BaseTask 的 static 字段，改为实例字段，单例时行为等价）
    private final Map<String, CachedAccessToken> accessTokenWithExpiry = new HashMap<>();
    private final Map<String, String> accessTokenMap = new HashMap<>();
    private final Map<String, Object> userTokenLocks = new ConcurrentHashMap<>();

    private static final long TOKEN_CACHE_DURATION = 3600; // B-At token 缓存有效期，单位：秒
    private static final long TOKEN_REFRESH_ADVANCE = 300; // 提前5分钟（300秒）主动刷新

    // 公网 IP 缓存
    private String cachedIP = null;
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION = 86400; // 缓存有效期，单位：秒

    private volatile String devCode = null;
    private final Object devCodeLock = new Object();

    public KujiequApiContext(HttpClient httpClient,
                              ObjectMapper objectMapper,
                              TokenExpiredCallback tokenExpiredCallback) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.tokenExpiredCallback = tokenExpiredCallback;
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    /**
     * 通知 token 过期（原 BaseTask.checkTokenExpired 里的 NotificationManager.publish）
     */
    public void notifyTokenExpired(UserInfo userInfo, String msg) {
        if (tokenExpiredCallback != null) {
            tokenExpiredCallback.onTokenExpired(userInfo, msg);
        }
    }

    // ==================== token 过期检查（原 BaseTask.checkTokenExpired） ====================

    public boolean checkTokenExpired(String responseMsg, UserInfo userInfo) {
        if (responseMsg != null && (responseMsg.contains("登录已过期") || responseMsg.contains("Token"))) {
            log.warn("Token expired for user {}: {}", userInfo.getRoleName(), responseMsg);
            invalidateAccessToken(userInfo.getUserId());
            notifyTokenExpired(userInfo, responseMsg);
            return true;
        }
        return false;
    }

    public <T> boolean checkResponseTokenExpired(com.kuro.model.ResponseBody<T> response, UserInfo userInfo) {
        if (response != null && response.getCode() != null && response.getCode() != 200) {
            return checkTokenExpired(response.getMsg(), userInfo);
        }
        return false;
    }

    // ==================== B-At token 获取与缓存（原 BaseTask.getAccessToken / requestToken） ====================

    public String getAccessToken(UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        String userId = userInfo.getUserId();
        long now = System.currentTimeMillis() / 1000;

        // 快路径：缓存命中且未过期
        synchronized (accessTokenWithExpiry) {
            CachedAccessToken cached = accessTokenWithExpiry.get(userId);
            if (cached != null && (now - cached.timestamp) < (TOKEN_CACHE_DURATION - TOKEN_REFRESH_ADVANCE)) {
                return cached.token;
            }
        }

        // 慢路径：逐用户锁，防并发重复请求
        Object lock = userTokenLocks.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            // 二次检查：可能其他线程刚刷完
            synchronized (accessTokenWithExpiry) {
                CachedAccessToken cached = accessTokenWithExpiry.get(userId);
                if (cached != null && (now - cached.timestamp) < (TOKEN_CACHE_DURATION - TOKEN_REFRESH_ADVANCE)) {
                    return cached.token;
                }
            }

            String freshToken = requestToken(userInfo);
            long fetchTime = System.currentTimeMillis() / 1000;
            synchronized (accessTokenWithExpiry) {
                accessTokenWithExpiry.put(userId, new CachedAccessToken(freshToken, fetchTime));
            }
            synchronized (accessTokenMap) {
                accessTokenMap.put(userId, freshToken);
            }
            return freshToken;
        }
    }

    private String requestToken(UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        String url = String.format("%s?serverId=%s&roleId=%s&userId=%s", ApiConfig.ROLE_ACCESS_TOKEN, ApiConfig.PARAM_SERVER_ID, userInfo.getRoleId(), userInfo.getUserId());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("source", "android")
                .header("B-At", "")
                .header("token", userInfo.getToken())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            log.debug(response.body());
            JsonNode tree = objectMapper.readTree(response.body());
            int code = tree.get("code").asInt();
            if (code == 200 || code == 10902) {
                String dataString = tree.get("data").asText();
                JsonNode dataNode = objectMapper.readTree(dataString);
                String accessToken = dataNode.path("accessToken").asText();
                return accessToken;
            } else {
                throw new AccessTokenException();
            }
        } else {
            throw new AccessTokenException();
        }
    }

    public boolean prefetchAccessToken(UserInfo userInfo) {
        if (userInfo == null || userInfo.getUserId() == null || userInfo.getToken() == null) {
            return false;
        }
        String userId = userInfo.getUserId();
        long now = System.currentTimeMillis() / 1000;

        // 已有有效缓存，跳过
        synchronized (accessTokenWithExpiry) {
            CachedAccessToken cached = accessTokenWithExpiry.get(userId);
            if (cached != null && (now - cached.timestamp) < (TOKEN_CACHE_DURATION - TOKEN_REFRESH_ADVANCE)) {
                return true;
            }
        }

        try {
            String token = requestToken(userInfo);
            if (token != null && !token.isEmpty()) {
                synchronized (accessTokenWithExpiry) {
                    accessTokenWithExpiry.put(userId, new CachedAccessToken(token, System.currentTimeMillis() / 1000));
                }
                synchronized (accessTokenMap) {
                    accessTokenMap.put(userId, token);
                }
                log.info("B-At 预取成功: userId={}", userId);
                return true;
            }
        } catch (Exception e) {
            log.warn("B-At 预取失败 (将在首次请求时重试): userId={}, error={}", userId, e.getMessage());
        }
        return false;
    }

    public boolean isAccessTokenCached(String userId) {
        synchronized (accessTokenWithExpiry) {
            CachedAccessToken cached = accessTokenWithExpiry.get(userId);
            if (cached != null) {
                long now = System.currentTimeMillis() / 1000;
                return (now - cached.timestamp) < TOKEN_CACHE_DURATION;
            }
        }
        return false;
    }

    public void invalidateAccessToken(String userId) {
        synchronized (accessTokenWithExpiry) {
            accessTokenWithExpiry.remove(userId);
        }
        synchronized (accessTokenMap) {
            accessTokenMap.remove(userId);
        }
        log.debug("B-At cache invalidated for userId={}", userId);
    }

    public void invalidateAllAccessTokens() {
        synchronized (accessTokenWithExpiry) {
            accessTokenWithExpiry.clear();
        }
        synchronized (accessTokenMap) {
            accessTokenMap.clear();
        }
        log.debug("All B-At cache invalidated");
    }

    // ==================== 请求头构建（原 BaseTask.getBuilder 系列） ====================

    public HttpRequest.Builder getBuilder(String url, HTTPRequestMultipartBody body, UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        HttpRequest.Builder builder = getBaseBuilder();
        builder.uri(URI.create(url))
                .header("Content-Type", body.getContentType())
                .header("Source",  "android")
                .header("Devcode", getDevCode())
                .headers("B-At", getAccessToken(userInfo))
                .header("Did", userInfo.getDevCode());

        builder.POST(HttpRequest.BodyPublishers.ofByteArray(body.getBody()));
        return builder;
    }

    public HttpRequest.Builder getBuilderWithToken(String url, HTTPRequestMultipartBody body, UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        HttpRequest.Builder builder = getBaseBuilder();
        builder.uri(URI.create(url))
                .header("Content-Type", body.getContentType())
                .header("Source", userInfo.getIsWeb() ? "h5" : "android")
                .header("Devcode", getDevCode())
                .headers("B-At", getAccessToken(userInfo))
                .header("Did", userInfo.getDevCode());
        if (userInfo.getToken() != null) {
            builder.header("Token", userInfo.getToken());
        }
        builder.POST(HttpRequest.BodyPublishers.ofByteArray(body.getBody()));
        return builder;
    }

    public HttpRequest.Builder getBuilder(String url, UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        HttpRequest.Builder builder = getBaseBuilder();
        builder.uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Source", userInfo.getIsWeb() ? "h5" : "android")
                .header("Devcode", getDevCode())
                .headers("B-At", getAccessToken(userInfo))
                .header("Did", userInfo.getDevCode());
        if (userInfo.getToken() != null) {
            builder.header("token", userInfo.getToken());
        }
        builder.POST(HttpRequest.BodyPublishers.noBody());
        return builder;
    }

    public HttpRequest.Builder getBuilderWithoutToken(String url, UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        HttpRequest.Builder builder = getBaseBuilder();
        builder.uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Source", userInfo.getIsWeb() ? "h5" : "android")
                .header("Devcode", getDevCode())
                .headers("B-At", getAccessToken(userInfo))
                .header("Did", userInfo.getDevCode());
        builder.POST(HttpRequest.BodyPublishers.noBody());
        return builder;
    }

    public HttpRequest.Builder getBuilder(String url, String body, UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        HttpRequest.Builder builder = getBaseBuilder();
        builder.uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Source", "android")
                .header("Devcode", getDevCode())
                .headers("B-At", getAccessToken(userInfo))
                .header("Did", userInfo.getDevCode());
        if (userInfo.getToken() != null) {
            builder.header("token", userInfo.getToken());
        }
        if (body != null) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        return builder;
    }

    public HttpRequest.Builder getBaseBuilder() {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.timeout(Duration.ofSeconds(5))
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; 23116PN5BC Build/PQ3A.190605.02201427; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36 Kuro/2.5.0 KuroGameBox/2.5.0")
                .header("Accept", "application/json, text/plain, */*");
        return builder;
    }

    // ==================== DevCode / 公网IP（原 BaseTask.getDevCode / getPublicIP） ====================

    public String getDevCode() {
        if (devCode != null) {
            return devCode;
        }
        synchronized (devCodeLock) {
            if (devCode == null) {
                String publicIP = getPublicIP();
                devCode = String.format("%s, Mozilla/5.0 (Linux; Android 9; 23116PN5BC Build/PQ3A.190605.02201427; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36 Kuro/2.5.0 Kuro/2.5.0 KuroGameBox/2.5.0", publicIP);
            }
        }
        return devCode;
    }

    public String getPublicIP() {
        long currentTime = System.currentTimeMillis() / 1000;
        if (cachedIP == null || (currentTime - lastFetchTime) > CACHE_DURATION) {
            cachedIP = fetchIPFromServices();
            lastFetchTime = currentTime;
        }
        return cachedIP;
    }

    private String fetchIPFromServices() {
        String[] services = {
                "https://event.kurobbs.com/event/ip",
                "https://api.ipify.org/?format=json",
                "https://httpbin.org/ip"
        };

        for (String service : services) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(service))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return response.body();
                }
            } catch (IOException | InterruptedException e) {
                return "127.127.127.127";
            }
        }
        return "127.127.127.127";
    }

    // ==================== B-At token 缓存条目（原 BaseTask.CachedAccessToken） ====================

    private static class CachedAccessToken {
        final String token;
        final long timestamp;

        CachedAccessToken(String token, long timestamp) {
            this.token = token;
            this.timestamp = timestamp;
        }
    }
}
