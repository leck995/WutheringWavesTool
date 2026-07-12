package com.kuro.kujiequ.thread;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.util.HTTPRequestMultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * @program: WutheringWavesTool
 * @description: 刷新服务器缓存，获取实时数据
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class UserDataRefreshTask extends BaseTask<ResponseBody<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(UserDataRefreshTask.class);
    private final UserInfo userInfo;

    public UserDataRefreshTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<String> call() throws Exception {
        return request(userInfo.getRoleId(), userInfo.getToken());
    }

    private ResponseBody<String> request(String roleId, String token) {
        String url1 = ApiConfig.GAME_DATA_URL + "?type=1&sizeType=2";
        try {
            HttpRequest request01 = HttpRequest.newBuilder()
                    .uri(URI.create(url1))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0")
                    .header("source", userInfo.getIsWeb() ? "h5" : "android")
                    .header("token", token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response01 = httpClient.send(request01, HttpResponse.BodyHandlers.ofString());


            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
                    .addPart("serverId", ApiConfig.PARAM_SERVER_ID)
                    .addPart("roleId", userInfo.getRoleId())
                    .addPart("gameId", ApiConfig.PARAM_GAME_ID)
                    .build();
            HttpRequest.Builder builder02 = getBuilder(ApiConfig.REFRESH_URL, body, userInfo);

            HttpRequest request02 = builder02.build();
            HttpResponse<String> response02 = httpClient.send(request02, HttpResponse.BodyHandlers.ofString());
            // 检查响应中是否包含 token 过期信息
            if (response01.body().contains("登录已过期")) {
                checkTokenExpired("登录已过期", userInfo);
            } else if (response02.body() != null && response02.body().contains("登录已过期")) {
                checkTokenExpired("登录已过期", userInfo);
            }
            return new ResponseBody<>(200, "角色数据刷新成功");
        } catch (IOException | InterruptedException | AccessTokenException e) {
            return new ResponseBody<>(1, e.getMessage());
        }
    }


}