package com.kuro.kujiequ.thread.base.user;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-11 20:51
 */
public class LoginUserTask extends BaseTask<ResponseBody<UserInfo>> {
    private static final Logger log = LoggerFactory.getLogger(LoginUserTask.class);
    private final String phone;
    private final String code;
    private final boolean isWeb;

    public LoginUserTask(String phone, String code,boolean isWeb) {
        this.phone = phone;
        this.code = code;
        this.isWeb = isWeb;
    }

    @Override
    protected ResponseBody<UserInfo> call() throws Exception {
    /*    String devCode = UUID.nameUUIDFromBytes(phone.getBytes(StandardCharsets.UTF_8)).toString().replaceAll("-", "");
        System.out.println(devCode);*/
        String devCode = getDevCode();
        String did = UUID.randomUUID().toString().replace("-", "");
        System.out.println(did);
        String url = String.format("%s?code=%s&mobile=%s&devCode=%s", ApiConfig.ACCOUNT_LOGIN,code, phone,did);
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
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                if (code == 200) {
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
                    log.error("服务器返回异常，错误代码：{}", code);
                    JsonNode node = tree.get("msg");
                    return new ResponseBody<>(code, node.asText());
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
}