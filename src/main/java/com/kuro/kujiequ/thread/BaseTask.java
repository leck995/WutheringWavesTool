package com.kuro.kujiequ.thread;

import cn.tealc.wutheringwavestool.base.AppInjector;
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
    private static volatile String token;
    private static final Object tokenLock = new Object();

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
     *
     * @param userInfo
     * @return {@link String }
     * @throws AccessTokenException
     * @throws IOException
     * @throws InterruptedException
     */
    protected String getAccessToken(UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        if (accessTokenMap.containsKey(userInfo.getUserId())) {
            return accessTokenMap.get(userInfo.getUserId());
        } else {
            String s = requestToken(userInfo);
            accessTokenMap.put(userInfo.getUserId(), s);
            return s;
        }
    }


    /**
     * 请求，获取B-AT令牌
     *
     * @param userInfo
     * @return {@link String }
     * @throws AccessTokenException
     * @throws IOException
     * @throws InterruptedException
     */
    private String requestToken(UserInfo userInfo) throws AccessTokenException, IOException, InterruptedException {
        if (token != null) {
            return token;
        }
        synchronized (tokenLock) {
            if (token != null) {
                return token;
            }
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
                    // 获取data字段并解析为JsonNode
                    String dataString = tree.get("data").asText();
                    JsonNode dataNode = mapper.readTree(dataString); // 将data字符串解析为JsonNode
                    token = dataNode.path("accessToken").asText();
                    return token;
                } else {
                    throw new AccessTokenException();
                }
            } else {
                throw new AccessTokenException();
            }
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
                .header("Source", userInfo.getIsWeb() ? "h5" : "android")
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