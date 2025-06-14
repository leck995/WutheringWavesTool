package com.kuro.kujiequ.thread.rolebox.role;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * @program: WutheringWavesTool
 * @description: 寻找游戏绑定的玩家信息
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class GameRoleSeekTask extends BaseTask<ResponseBody<UserInfo>> {
    private static final Logger LOG = LoggerFactory.getLogger(GameRoleSeekTask.class);
    private final String token;
    private final boolean isWeb;

    public GameRoleSeekTask(String token, boolean isWeb) {
        this.isWeb = isWeb;
        this.token = token;
    }

    @Override
    protected ResponseBody<UserInfo> call() throws Exception {
        String url = String.format("%s?gameId=3", ApiConfig.ACCOUNT_SEEK_ROLE);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("source", isWeb ? "h5" : "android")
                    .header("token", token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                if (code == 200) {
                    JsonNode dataNode = tree.get("data");
                    if (dataNode.isArray() && !dataNode.isEmpty()) {
                        JsonNode firstItem = dataNode.get(0);
                        long userId = firstItem.path("userId").asLong();
                        String roleId = firstItem.path("roleId").asText();
                        String roleName = firstItem.path("roleName").asText();
                        UserInfo userInfo = new UserInfo();
                        userInfo.setToken(this.token);
                        userInfo.setIsWeb(this.isWeb);
                        userInfo.setRoleId(roleId);
                        userInfo.setRoleName(roleName);
                        userInfo.setUserId(String.valueOf(userId));
                        return ResponseBody.create(200, "success", userInfo);
                    } else {
                        return new ResponseBody<>(1, "找不到游戏账号信息，请检查库街区的游戏绑定");
                    }
                } else {
                    LOG.error("服务器返回异常，错误代码：{}", code);
                    JsonNode node = tree.get("msg");
                    return new ResponseBody<>(code, node.asText());
                }
            } else {
                LOG.error("网络请求失败，错误代码：{}", response.statusCode());
                return ResponseBody.create(response.statusCode(), "连接失败，响应状态码:" + response.statusCode(), null);
            }

        } catch (IOException | InterruptedException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}