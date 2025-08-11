package com.kuro.kujiequ.thread.rolebox;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.roleData.user.RoleInfo;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.thread.BaseTask;
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
 * @description: 获取用户的游戏数据，包括体力，宝箱数量，周本活动，潮汐之遗等
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class PlayerBaseDataTask extends BaseTask<ResponseBody<RoleInfo>> {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerBaseDataTask.class);
    private final UserInfo userInfo;

    public PlayerBaseDataTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<RoleInfo> call() throws Exception {
        return sign();
    }

    private ResponseBody<RoleInfo> sign() {
        String url = ApiConfig.BASE_DATA_URL;
        String body = String.format("serverId=%s&type=2&roleId=%s&sizeType=1&gameId=%s"
                , ApiConfig.PARAM_SERVER_ID, userInfo.getRoleId(), ApiConfig.PARAM_GAME_ID);
        try {
            HttpRequest.Builder builder = getBuilder(url,body,userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug(response.body());
                ObjectMapper mapper = new ObjectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });
                ResponseBody<RoleInfo> responseBody = new ResponseBody<>(responseBodyForApi.getCode(), responseBodyForApi.getMsg(), responseBodyForApi.getSuccess());
                if (responseBody.getCode() == 200) {
                    String row = responseBodyForApi.getData();
                    //遇到个用户会返回null,不知道为什么，初步判断可能需要刷新后才能获取，以防万一，做个判断好了
                    if (row == null || row.equals("null")) {
                        return new ResponseBody<>(1, "返回值为空，请先使用库街区APP查看鸣潮数据后再次尝试，后再联系开发者");
                    }
                    RoleInfo roleInfo = mapper.readValue(row, RoleInfo.class);
                    responseBody.setData(roleInfo);
                    return responseBody;
                } else {
                    if (responseBody.getMsg().equals("登录已过期，请重新登录")) {
                        return new ResponseBody<>(1, "检测到Token已过期，请前往账号页面更新Token");
                    } else {
                        return new ResponseBody<>(1, responseBody.getMsg());
                    }
                }
            } else {
                return new ResponseBody<>(1, "连接出错");
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}