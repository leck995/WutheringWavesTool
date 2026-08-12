package com.kuro.kujiequ.thread.sms;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 发送库街区短信验证码（H5 接口 + 极验 geeTestData）。
 *
 * @author leck
 * @date 2026/05/30
 */
public class SendSmsTask extends Task<ResponseBody<Boolean>> {
    private static final Logger LOG = LoggerFactory.getLogger(SendSmsTask.class);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CONTENT_TYPE =
            "application/x-www-form-urlencoded; charset=UTF-8";
    private static final String UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) KuroGameBox/3.1.3";
    private static final String VERSION = "3.1.3";

    private final String phone;
    private final String geeTestJson;

    public SendSmsTask(String phone, String geeTestJson) {
        this.phone = phone;
        this.geeTestJson = geeTestJson;
    }

    @Override
    protected ResponseBody<Boolean> call() throws Exception {
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

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = response.body() == null ? "" : response.body();
        LOG.debug("getSmsCodeForH5 status={} body={}", response.statusCode(), body);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return ResponseBody.create(response.statusCode(), "连接失败，响应状态码:" + response.statusCode(), false);
        }
        if (body.isBlank()) {
            return ResponseBody.create(-1, "服务器返回空响应", false);
        }

        JsonNode root = MAPPER.readTree(body);
        boolean success = root.path("success").asBoolean(false)
                || root.path("code").asInt(-1) == 0
                || root.path("code").asInt(-1) == 200;
        String msg = root.path("msg").asText(success ? "验证码发送成功" : "请求失败");
        if (success) {
            return ResponseBody.create(200, msg, true);
        }
        int code = root.path("code").asInt(-1);
        return ResponseBody.create(code == 0 ? -1 : code, msg, false);
    }

    public static String randomDevCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(32);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

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
}
