package com.kuro.kujiequ.thread.base;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.roleData.user.RoleDailyData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import com.kuro.util.HTTPRequestMultipartBody;
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
 * @description: 获取体力，任务完成度等数据(https://api.kurobbs.com/gamer/widget/game3/getData)
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class UserDailyDataTask extends BaseTask<ResponseBody<RoleDailyData>> {
    private static final Logger LOG= LoggerFactory.getLogger(UserDailyDataTask.class);
    private UserInfo userInfo;

    public UserDailyDataTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<RoleDailyData> call() throws Exception {
        return request();
    }

    private ResponseBody<RoleDailyData> request(){
        String url=String.format("%s?type=2&roleId=%s&sizeType=1&gameId=%s&serverId=%s"
                , ApiConfig.DAILY_DATA_URL,userInfo.getRoleId(),ApiConfig.PARAM_GAME_ID,ApiConfig.PARAM_SERVER_ID);
        try {

//            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
//                    .addPart("type", "2")
//                    .addPart("roleId", userInfo.getRoleId())
//                    .addPart("gameId", ApiConfig.PARAM_GAME_ID)
//                    .addPart("sizeType","1")
//                    .addPart("serverId",ApiConfig.PARAM_SERVER_ID)
//                    .build();

            HttpRequest request = getBuilder(url,userInfo).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper=AppInjector.getInstance(ObjectMapper.class);

                LOG.debug(response.body().replace("\\",""));

                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                if (code == 220) throw new AccessTokenException();
                ResponseBody<RoleDailyData> responseBody = new ResponseBody<>();
                if (code == 200) {
                    RoleDailyData data = mapper.readValue(tree.get("data").toString(), RoleDailyData.class);
                    responseBody.setData(data);
                    responseBody.setSuccess(tree.get("success").asBoolean());
                }
                responseBody.setCode(code);
                responseBody.setMsg(tree.get("msg").asText());
                checkTokenExpired(responseBody.getMsg(), userInfo);
                return responseBody;
            }else {
                return new ResponseBody<>(1,"网络连接失败,异常状态码: " + response.statusCode());
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1,e.getMessage());
        }
    }
}