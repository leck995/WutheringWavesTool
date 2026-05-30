package com.kuro.kujiequ.thread.sms;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 发送验证码
 *
 * @author leck
 * @date 2026/05/30
 */
public class SendSmsTask extends Task<ResponseBody<Boolean>> {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(SendSmsTask.class);
    private static final String GET_SMS_CODE_URL = "https://sdkapi.kurogame.com/sdkcom/v2/login/getPhoneCode.lg";
    private static final String REFERER = "https://usercenter.kurogames.com/";
    private static final String ORIGIN = "https://usercenter.kurogames.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";
    private static final String SEC_CH_UA = "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"";
    private static final String E_PARAM = "1";
    private static final String REDIRECT_URI = "1";
    private static final String PACK_MARK = "1";
    private static final String PROJECT_ID = "G152";
    private static final String PRODUCT_ID = "A1493";
    private static final String PLATFORM = "h5";
    private static final String CHANNEL_ID = "211";
    private static final String VERSION = "2.1.2";
    private static final String SDK_VERSION = "2.1.2";
    private static final String RESPONSE_TYPE = "code";
    private static final String PKG = "com.kurogame.mingchao";
    private static final String CLIENT_ID = "vvkewnskrxxwfo0yi61cy24l";
    private static final String CLIENT_SECRET = "g9ej0i1jf3y68wchb0ncm266";

    private String deviceId;
    private String phone;
    public SendSmsTask(String phone) {
        this.phone = phone;
    }

    @Override
    protected ResponseBody<Boolean> call() throws Exception {
        String response = needCaptcha();
        LOG.debug(response);
        int code = MAPPER.readTree(response).path("code").asInt();
        String msg = MAPPER.readTree(response).path("msg").asText();
        if (code == 0){
            return ResponseBody.create(200,msg,true);
        }else if (code == 41000){
            return ResponseBody.create(41000,msg,true);
        }else{
            return ResponseBody.create(-1,msg,true);
        }
    }




    private String needCaptcha() {
        deviceId = UUIDHelper.generateDeviceId();
        Map<String, String> params = getBaseKuroParams();
        params.putAll(Map.of(
                "response_type", RESPONSE_TYPE,
                "phone", phone,
                "pkg", PKG,
                "client_id", CLIENT_ID,
                "client_secret", CLIENT_SECRET
        ));

        try {
            String response = postKuro(GET_SMS_CODE_URL, buildFormBody(params));
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 将参数 Map 编码为 application/x-www-form-urlencoded 格式 */
    private String buildFormBody(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /** URL 编码辅助方法 */
    private String urlEncode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private String postKuro(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = commonBuilder(url)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .header("kr-ver", "1.9.0")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("Origin", ORIGIN)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    /** 构建通用 HTTP 请求头（UA、Referer、sec-ch-ua 等） */
    private HttpRequest.Builder commonBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "*/*")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("sec-ch-ua", SEC_CH_UA)
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-fetch-site", "cross-site")
                .header("Referer", REFERER)
                .header("User-Agent", USER_AGENT);
    }


    /** 构建 Kuro API 请求的公共参数 */
    private Map<String, String> getBaseKuroParams() {
        return new HashMap<>(Map.of(
                "redirect_uri", REDIRECT_URI,
                "__e__", E_PARAM,
                "pack_mark", PACK_MARK,
                "projectId", PROJECT_ID,
                "productId", PRODUCT_ID,
                "platform", PLATFORM,
                "channelId", CHANNEL_ID,
                "deviceNum", deviceId,
                "version", VERSION,
                "sdkVersion", SDK_VERSION
        ));
    }


}
