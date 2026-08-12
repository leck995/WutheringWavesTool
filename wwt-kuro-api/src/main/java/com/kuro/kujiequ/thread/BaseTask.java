package com.kuro.kujiequ.thread;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.util.HTTPRequestMultipartBody;
import javafx.concurrent.Task;
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
 * @description: 多线程请求的基本类
 * @author: Leck
 * @create: 2025-06-11 21:20
 */
public abstract class BaseTask<V> extends Task<V> {
    private static final Logger log = LoggerFactory.getLogger(BaseTask.class);
    protected static final Map<String, String> accessTokenMap = new HashMap<>();
    private static final long CACHE_DURATION = 86400; // 缓存有效期，单位：秒
    private static String cachedIP = null;
    private static long lastFetchTime = 0;
    protected static final HttpClient httpClient = AppInjector.getInstance(HttpClient.class);
    private static volatile String devCode;
    private static final Object devCodeLock = new Object();
    private static final long TOKEN_CACHE_DURATION = 3600; // B-At token 缓存有效期，单位：秒
    private static final long TOKEN_REFRESH_ADVANCE = 300; // 提前5分钟（300秒）主动刷新
    private static final Map<String, CachedAccessToken> accessTokenWithExpiry = new HashMap<>();
    private static final Map<String, Object> userTokenLocks = new ConcurrentHashMap<>();

    public String getDevCode() {
        if (devCode != null) {
            return devCode;
        }
        synchronized (devCodeLock) {
            if (devCode == null) {
                String publicIP = getPublicIP();
                devCode = String.format("%s, Mozilla/5.0 (Linux; Android 9; 23116PN5BC Build/PQ3A.190605.02201427; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36 Kuro/2.5.0 KuroGameBox/2.5.0", publicIP);
            }
        }
        return devCode;
    }

    /**
     * 获取公网IP并缓存
     *
     * @return {@link String }
     */
    public String getPublicIP() {
        long currentTime = System.currentTimeMillis() / 1000; // 当前时间（秒）
        // 检查缓存是否过期
        if (cachedIP == null || (currentTime - lastFetchTime) > CACHE_DURATION) {
            cachedIP = fetchIPFromServices();
            lastFetchTime = currentTime;
        }
        return cachedIP;
    }


    /**
     * 向服务器查询公网IP
     *
     * @return {@link String }
     */
    private String fetchIPFromServices() {
        // 尝试从第一个服务获取 IP 地址
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
        // 如果所有服务都失败，返回默认值
        return "127.127.127.127";
    }


    /**
     * 获取B-AT令牌，2025.6后需要该令牌方可查询部分数据
     * 缓存 keyed by userId，带有效期；自动续期，带 per-user 锁防并发。
     */
    protected String getAccessToken(UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
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


    /**
     * 主动预取 B-At token 并缓存（供 TokenRefreshService 启动时调用，不抛异常）
     * @return true 如果获取成功
     */
    public static boolean prefetchAccessToken(UserInfo userInfo) {
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
            // 临时构造一个 BaseTask 子类来调用 requestToken
            PrefetchTask task = new PrefetchTask(userInfo);
            String token = task.call();
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


    /**
     * 检查指定用户的 B-At 是否已缓存且未过期
     */
    public static boolean isAccessTokenCached(String userId) {
        synchronized (accessTokenWithExpiry) {
            CachedAccessToken cached = accessTokenWithExpiry.get(userId);
            if (cached != null) {
                long now = System.currentTimeMillis() / 1000;
                return (now - cached.timestamp) < TOKEN_CACHE_DURATION;
            }
        }
        return false;
    }


    /**
     * 供 prefetchAccessToken 使用的临时子类
     */
    private static class PrefetchTask extends BaseTask<String> {
        private final UserInfo userInfo;
        PrefetchTask(UserInfo userInfo) {
            this.userInfo = userInfo;
        }
        @Override
        protected String call() throws Exception {
            return requestToken(userInfo);
        }
    }


    /**
     * 使指定用户的 B-At 缓存失效（当主 token 更新时调用）
     */
    public static void invalidateAccessToken(String userId) {
        synchronized (accessTokenWithExpiry) {
            accessTokenWithExpiry.remove(userId);
        }
        synchronized (accessTokenMap) {
            accessTokenMap.remove(userId);
        }
        log.debug("B-At cache invalidated for userId={}", userId);
    }

    /**
     * 使所有用户的 B-At 缓存失效
     */
    public static void invalidateAllAccessTokens() {
        synchronized (accessTokenWithExpiry) {
            accessTokenWithExpiry.clear();
        }
        synchronized (accessTokenMap) {
            accessTokenMap.clear();
        }
        log.debug("All B-At cache invalidated");
    }


    /**
     * 请求，获取B-AT令牌
     */
    protected String requestToken(UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
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
            ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
            JsonNode tree = mapper.readTree(response.body());
            int code = tree.get("code").asInt();
            if (code == 200 || code == 10902) {
                String dataString = tree.get("data").asText();
                JsonNode dataNode = mapper.readTree(dataString);
                String accessToken = dataNode.path("accessToken").asText();
                return accessToken;
            } else {
                throw new AccessTokenException();
            }
        } else {
            throw new AccessTokenException();
        }
    }


    /**
     * 检查 Kuro API 响应是否为"token 已过期"，并发布通知。
     * 所有子任务应在解析 response JSON 后调用此方法。
     *
     * @param responseMsg  API 返回的 msg 字段
     * @param userInfo     当前用户
     * @return true 如果 token 已过期
     */
    protected boolean checkTokenExpired(String responseMsg, UserInfo userInfo) {
        if (responseMsg != null && (responseMsg.contains("登录已过期") || responseMsg.contains("Token"))) {
            log.warn("Token expired for user {}: {}", userInfo.getRoleName(), responseMsg);
            invalidateAccessToken(userInfo.getUserId());
            NotificationManager.publish(NotificationKey.TOKEN_EXPIRED, userInfo, responseMsg);
            return true;
        }
        return false;
    }


    /**
     * 检查已解析的 ResponseBody 是否包含 token 过期错误，便捷方法。
     */
    protected <T> boolean checkResponseTokenExpired(ResponseBody<T> response, UserInfo userInfo) {
        if (response != null && response.getCode() != null && response.getCode() != 200) {
            return checkTokenExpired(response.getMsg(), userInfo);
        }
        return false;
    }


    /**
     * B-At token 缓存条目，带时间戳
     */
    private static class CachedAccessToken {
        final String token;
        final long timestamp;

        CachedAccessToken(String token, long timestamp) {
            this.token = token;
            this.timestamp = timestamp;
        }
    }


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


    /**
     * 生成请求头
     *
     * @param url
     * @param body
     * @param userInfo
     * @return {@link HttpRequest.Builder }
     * @throws AccessTokenException
     * @throws IOException
     * @throws InterruptedException
     */
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
}