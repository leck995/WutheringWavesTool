package com.kuro.kujiequ.thread.rolebox.role;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.model.roleData.RoleDetail;
import com.kuro.kujiequ.ApiConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import com.kuro.util.HTTPRequestMultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class GameRoleDetailTask extends BaseTask<ResponseBody<RoleDetail>> {
    private static final Logger LOG = LoggerFactory.getLogger(GameRoleDetailTask.class);
    private final UserInfo userInfo;
    private int cardRoleId;

    public GameRoleDetailTask(UserInfo userInfo, int cardRoleId) {
        this.userInfo = userInfo;
        this.cardRoleId = cardRoleId;
    }

    @Override
    protected ResponseBody<RoleDetail> call() throws Exception {
        return getRoleDate();
    }

    private ResponseBody<RoleDetail> getRoleDate() {
        String url = ApiConfig.ROLE_DETAIL_URL;
        try {
            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
                    .addPart("serverId", ApiConfig.PARAM_SERVER_ID)
                    .addPart("roleId", userInfo.getRoleId())
                    .addPart("gameId", ApiConfig.PARAM_GAME_ID)
                    .addPart("id", String.valueOf(cardRoleId))
                    .addPart("channelId", "19")
                    .addPart("countryCode", "1")
                    .build();
            HttpRequest.Builder builder = getBuilder(url,body,userInfo);

            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug(response.body());
                ObjectMapper mapper = new ObjectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });
                if (responseBodyForApi.getCode() == 200 || responseBodyForApi.getCode() == 10902) {
                    ResponseBody<RoleDetail> responseBody = new ResponseBody<>(200, responseBodyForApi.getMsg(), responseBodyForApi.getSuccess());
                    String row = responseBodyForApi.getData();
                    RoleDetail roleDetail = mapper.readValue(row, RoleDetail.class);
                    responseBody.setData(roleDetail);
                    return responseBody;
                } else {
                    return new ResponseBody<>(1, responseBodyForApi.getMsg(), false);
                }
            } else {
                ResponseBody<RoleDetail> responseBody = new ResponseBody<>();
                LOG.error("网络请求失败，错误代码：{}", response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            LOG.error("错误：", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}