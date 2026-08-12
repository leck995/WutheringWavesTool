package com.kuro.kujiequ.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.KujiequApiContext;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 库街区认证/短信/token API：发送短信验证码、登录、token 缓存管理。
 * <p>
 * 本类为纯 API 模块，零 JavaFX 依赖。所有方法同步返回 {@link ResponseBody}，
 * 方法体原样搬迁自原 KujiequManager，逻辑无改动。
 * <p>
 * 静态辅助方法 {@link #randomDevCode()}、{@link #isValidCnMobile(String)}、
 * {@link #toForm(Map)} 也定义在本类中。
 *
 * @author Leck
 */
public class KujiequAuthApi {
    private static final Logger log = LoggerFactory.getLogger(KujiequAuthApi.class);

    private final KujiequApiContext ctx;

    public KujiequAuthApi(KujiequApiContext ctx) {
        this.ctx = ctx;
    }

    // ==================== 20. SendSmsTask：发送短信验证码 ====================

    /**
     * 发送库街区短信验证码（H5 接口 + 极验 geeTestData）。
     * 原 SendSmsTask 自建 static HttpClient 与 ObjectMapper，本实现改用 ctx 提供的实例。
     *
     * @param phone        手机号
     * @param geeTestJson  极验 geeTestData JSON
     * @return 发送结果响应
     */
    public ResponseBody<Boolean> sendSmsCode(String phone, String geeTestJson) {
        String devCode = randomDevCode();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("mobile", phone);
        form.put("geeTestData", geeTestJson == null ? "" : geeTestJson);

        HttpRequest request = HttpRequest.newBuilder(URI.create(ApiConfig.GET_SMS_CODE_H5))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", CONTENT_TYPE)
                .header("User-Agent", UA)
                .header("source", "h5")
                .header("devcode", devCode)
                .header("version", VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(toForm(form)))
                .build();

        try {
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body() == null ? "" : response.body();
            log.debug("getSmsCodeForH5 status={} body={}", response.statusCode(), body);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResponseBody.create(response.statusCode(), "连接失败，响应状态码:" + response.statusCode(), false);
            }
            if (body.isBlank()) {
                return ResponseBody.create(-1, "服务器返回空响应", false);
            }

            ObjectMapper mapper = ctx.objectMapper();
            JsonNode root = mapper.readTree(body);
            boolean success = root.path("success").asBoolean(false)
                    || root.path("code").asInt(-1) == 0
                    || root.path("code").asInt(-1) == 200;
            String msg = root.path("msg").asText(success ? "验证码发送成功" : "请求失败");
            if (success) {
                return ResponseBody.create(200, msg, true);
            }
            int code = root.path("code").asInt(-1);
            return ResponseBody.create(code == 0 ? -1 : code, msg, false);
        } catch (IOException | InterruptedException e) {
            log.error("发送短信验证码错误", e);
            return ResponseBody.create(-1, e.getMessage(), false);
        }
    }

    private static final String CONTENT_TYPE =
            "application/x-www-form-urlencoded; charset=UTF-8";
    private static final String UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) KuroGameBox/3.1.3";
    private static final String VERSION = "3.1.3";

    /**
     * 生成 32 位随机 devCode（原 SendSmsTask.randomDevCode）。
     */
    public static String randomDevCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(32);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 校验中国大陆手机号格式（原 SendSmsTask.isValidCnMobile）。
     */
    public static boolean isValidCnMobile(String mobile) {
        return mobile != null && mobile.matches("^1[3-9]\\d{9}$");
    }

    private static String toForm(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    // ==================== 21. LoginUserTask：登录 ====================

    /**
     * 库街区登录（手机号 + 短信验证码）。
     * 原 LoginUserTask 不走 getBuilder，自建请求。
     *
     * @param phone  手机号
     * @param code   短信验证码
     * @param isWeb  是否为 H5 来源
     * @return 登录用户信息响应
     */
    public ResponseBody<UserInfo> loginUser(String phone, String code, boolean isWeb) {
        String devCode = ctx.getDevCode();
        String did = UUID.randomUUID().toString().replace("-", "");
        log.debug("登录 did: {}", did);
        String url = String.format("%s?code=%s&mobile=%s&devCode=%s", ApiConfig.ACCOUNT_LOGIN, code, phone, did);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("source", isWeb ? "h5" : "android")
                    .header("devcode", devCode)
                    .header("did", did)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int respCode = tree.get("code").asInt();
                if (respCode == 200) {
                    JsonNode dataNode = tree.get("data");
                    if (!dataNode.isEmpty()) {
                        String token = dataNode.path("token").asText();
                        UserInfo userInfo = new UserInfo();
                        userInfo.setToken(token);
                        userInfo.setDevCode(did);
                        return ResponseBody.create(200, "success", userInfo);
                    } else {
                        return new ResponseBody<>(1, "登录库街区失败");
                    }
                } else {
                    log.error("服务器返回异常，错误代码：{}", respCode);
                    JsonNode node = tree.get("msg");
                    return new ResponseBody<>(respCode, node.asText());
                }
            } else {
                log.error("网络请求失败，错误代码：{}", response.statusCode());
                return ResponseBody.create(response.statusCode(), "连接失败，响应状态码:" + response.statusCode(), null);
            }
        } catch (IOException | InterruptedException e) {
            log.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== token 管理方法（委托给 ctx） ====================

    /**
     * 主动预取 B-At token 并缓存。
     *
     * @param userInfo 用户信息
     * @return true 表示预取成功
     */
    public boolean prefetchAccessToken(UserInfo userInfo) {
        return ctx.prefetchAccessToken(userInfo);
    }

    /**
     * 检查指定用户的 B-At 是否已缓存且未过期。
     *
     * @param userId 用户 ID
     * @return true 表示缓存有效
     */
    public boolean isAccessTokenCached(String userId) {
        return ctx.isAccessTokenCached(userId);
    }

    /**
     * 使指定用户的 B-At 缓存失效。
     *
     * @param userId 用户 ID
     */
    public void invalidateAccessToken(String userId) {
        ctx.invalidateAccessToken(userId);
    }

    /**
     * 使所有用户的 B-At 缓存失效。
     */
    public void invalidateAllAccessTokens() {
        ctx.invalidateAllAccessTokens();
    }

    /**
     * 通知 token 过期（触发上层回调）。
     *
     * @param userInfo 用户信息
     * @param msg      过期提示信息
     */
    public void notifyTokenExpired(UserInfo userInfo, String msg) {
        ctx.notifyTokenExpired(userInfo, msg);
    }
}
