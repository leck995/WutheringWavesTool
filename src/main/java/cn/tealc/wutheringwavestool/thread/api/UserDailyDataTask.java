package cn.tealc.wutheringwavestool.thread.api;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.roleData.user.RoleDailyData;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * @description: 获取体力，任务完成度等数据
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class UserDailyDataTask extends Task<ResponseBody<RoleDailyData>> {
    private static final Logger LOG= LoggerFactory.getLogger(UserDailyDataTask.class);
    private SignUserInfo signUserInfo;

    public UserDailyDataTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<RoleDailyData> call() throws Exception {
        ResponseBody<RoleDailyData> sign = sign(signUserInfo.getRoleId(), signUserInfo.getToken());
        return sign;
    }

    private ResponseBody<RoleDailyData> sign(String roleId,String token){
        String url=String.format("%s?type=2&roleId=%s&sizeType=1&gameId=%s"
                ,ApiConfig.DAILY_DATA_URL,roleId,ApiConfig.PARAM_GAME_ID);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper=new ObjectMapper();

                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                ResponseBody<RoleDailyData> responseBody = new ResponseBody<>();
                if (code == 200) {
                    RoleDailyData data = mapper.readValue(tree.get("data").toString(), RoleDailyData.class);
                    responseBody.setData(data);
                    responseBody.setSuccess(tree.get("success").asBoolean());
                }
                responseBody.setCode(code);
                responseBody.setMsg(tree.get("msg").asText());
                return responseBody;
            }else {
                return new ResponseBody<>(1,"网络连接失败,检查网络");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("UserDailyDataTask错误",e);
            return new ResponseBody<>(1,e.getMessage());
        }
    }
}