package com.kuro.kujiequ.thread.sms;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.model.sms.SmsCodeResponse;
import com.kuro.kujiequ.thread.BaseTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 发送验证码 — APP 端 API
 * <p>
 * 调用 api.kurobbs.com/user/getSmsCode 发送短信验证码。
 * 当触发风控时，API 返回 geeTest=true，需要用户完成极验人机验证后重试。
 */
public class SendSmsTask extends BaseTask<ResponseBody<SmsCodeResponse>> {
    private static final Logger LOG = LoggerFactory.getLogger(SendSmsTask.class);
    private static final String GET_SMS_CODE_URL = "https://api.kurobbs.com/user/getSmsCode";
    private static final String ANDROID_UA = "Mozilla/5.0 (Linux; Android 9; 23116PN5BC Build/PQ3A.190605.02201427; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36 Kuro/2.5.0 KuroGameBox/2.5.0";

    private final String phone;
    private final String geeTestData;

    /**
     * @param phone       手机号
     * @param geeTestData 极验验证通过后的 tokens（JSON 字符串，首次传空串）
     */
    public SendSmsTask(String phone, String geeTestData) {
        this.phone = phone;
        this.geeTestData = geeTestData != null ? geeTestData : "";
    }

    public SendSmsTask(String phone) {
        this(phone, "");
    }


    @Override
    protected ResponseBody<SmsCodeResponse> call() {
        String devCode = getDevCode();
        String distinctId = UUID.randomUUID().toString().replace("-", "");
        String ip = getPublicIP();

        // 构建 form body
        String body = buildFormBody(devCode, distinctId, ip);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GET_SMS_CODE_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", ANDROID_UA)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .header("source", "android")
                    .header("devcode", devCode)
                    .header("distinct_id", distinctId)
                    .header("countrycode", "CN")
                    .header("ip", ip)
                    .header("model", "23116PN5BC")
                    .header("lang", "zh-Hans")
                    .header("version", "2.5.0")
                    .header("versioncode", "2500")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response.body());

        } catch (IOException | InterruptedException e) {
            LOG.error("发送验证码请求失败", e);
            return new ResponseBody<>(-1, "网络请求失败: " + e.getMessage());
        }
    }


    /**
     * 构建 application/x-www-form-urlencoded 请求体
     */
    private String buildFormBody(String devCode, String distinctId, String ip) {
        StringBuilder sb = new StringBuilder();
        appendParam(sb, "mobile", phone);
        appendParam(sb, "devCode", devCode);
        appendParam(sb, "distinct_id", distinctId);
        appendParam(sb, "countryCode", "CN");
        appendParam(sb, "ip", ip);
        appendParam(sb, "model", "23116PN5BC");
        appendParam(sb, "source", "android");
        appendParam(sb, "geeTestData", geeTestData);
        return sb.toString();
    }

    private void appendParam(StringBuilder sb, String key, String value) {
        if (!sb.isEmpty()) {
            sb.append('&');
        }
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        sb.append('=');
        sb.append(URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8));
    }


    /**
     * 解析 API 响应，提取 geeTest 状态
     * <ul>
     *   <li>code=200, geeTest=false → 验证码发送成功，返回 {@code ResponseBody(200, ...)}</li>
     *   <li>code=200, geeTest=true  → 需要极验，返回 {@code ResponseBody(41000, ...)}，data 含 gt/challenge</li>
     *   <li>code=242              → 发送过于频繁</li>
     *   <li>其他                  → 透传 API 错误</li>
     * </ul>
     */
    private ResponseBody<SmsCodeResponse> parseResponse(String responseBody) {
        try {
            ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
            JsonNode root = mapper.readTree(responseBody);
            int code = root.path("code").asInt();
            String msg = root.path("msg").asText();

            if (code != 200) {
                LOG.warn("getSmsCode 返回非 200: code={}, msg={}", code, msg);
                return new ResponseBody<>(code, msg);
            }

            JsonNode dataNode = root.path("data");
            SmsCodeResponse smsData = mapper.treeToValue(dataNode, SmsCodeResponse.class);

            if (smsData != null && smsData.isGeeTest()) {
                // 需要极验人机验证
                LOG.info("getSmsCode 需要极验: gt={}, challenge={}", smsData.getGt(), smsData.getChallenge());
                return ResponseBody.create(41000, "需要完成人机验证", smsData);
            }

            // 验证码发送成功
            return ResponseBody.create(200, msg, smsData);

        } catch (Exception e) {
            LOG.error("解析 getSmsCode 响应失败", e);
            return new ResponseBody<>(-1, "解析响应失败: " + e.getMessage());
        }
    }
}
