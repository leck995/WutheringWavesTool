package cn.tealc.wutheringwavestool.thread.role;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.roleData.Role;
import cn.tealc.wutheringwavestool.model.roleData.RoleDetail;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class GameRoleDetailTask extends Task<ResponseBody<RoleDetail>> {
    private static final Logger LOG=LoggerFactory.getLogger(GameRoleDetailTask.class);
    private SignUserInfo signUserInfo;
    private int cardRoleId;

    public GameRoleDetailTask(SignUserInfo signUserInfo,int cardRoleId) {
        this.signUserInfo = signUserInfo;
        this.cardRoleId = cardRoleId;
    }

    @Override
    protected ResponseBody<RoleDetail> call() throws Exception {
        return getRoleDate(signUserInfo.getRoleId(),signUserInfo.getToken(),cardRoleId);
    }

    private ResponseBody<RoleDetail> getRoleDate(String roleId,String token,int cardRoleId){
        String url=String.format(
                "https://api.kurobbs.com/gamer/roleBox/aki/getRoleDetail?gameId=3&serverId=76402e5b20be2c39f095a152090afddc&roleId=%s&id=%d&channelId=19&countryCode=1",roleId,cardRoleId);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBody<RoleDetail> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<RoleDetail>>() {});
                if (responseBody.getCode() != 200) {
                    LOG.error("服务器返回异常，错误代码：{}",responseBody.getCode());
                }
                return responseBody;
            }else {
                ResponseBody<RoleDetail> responseBody = new ResponseBody<>();
                LOG.error("网络请求失败，错误代码：{}",response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，无法获取数据");
                return responseBody;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误：",e);
            ResponseBody<RoleDetail> objectResponseBody = new ResponseBody<>();
            objectResponseBody.setSuccess(false);
            objectResponseBody.setCode(0);
            return objectResponseBody;
        }
    }
}