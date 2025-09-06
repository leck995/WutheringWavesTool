package com.kuro.kujiequ.thread;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.UserInfo;
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
        return sign(userInfo.getRoleId(), userInfo.getToken());
    }

    private ResponseBody<String> sign(String roleId, String token) {
        String url1 = ApiConfig.GAME_DATA_URL + "?type=1&sizeType=2";
        String url2 = String.format("%s?gameId=%s&serverId=%s&roleId=%s",
                ApiConfig.REFRESH_URL, ApiConfig.PARAM_GAME_ID, ApiConfig.PARAM_SERVER_ID, roleId);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url1))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0")
                    .header("source", userInfo.getIsWeb() ? "h5" : "android")
                    .header("token", token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            ;
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            HttpRequest.Builder builder = getBuilder(url2, userInfo);
            HttpRequest request2 = builder.build();
            HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
      /*      HttpRequest request2 = HttpRequestUtil.getRequestWithSource(url2,token,"android");
            HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());*/
            LOG.debug("角色刷新,每日数据状态码1: {},游戏进度状态码2: {}", response.statusCode(), response2.statusCode());
            return new ResponseBody<>(200, "角色数据刷新成功");
        } catch (IOException | InterruptedException | AccessTokenException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }


}