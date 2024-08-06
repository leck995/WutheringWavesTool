package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.roleData.user.RoleInfo;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
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
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class UserInfoDataTask extends Task<ResponseBody<RoleInfo>> {
    private static final Logger LOG= LoggerFactory.getLogger(UserInfoDataTask.class);
    private SignUserInfo signUserInfo;

    public UserInfoDataTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<RoleInfo> call() throws Exception {
        ResponseBody<RoleInfo> sign = sign(signUserInfo.getRoleId(), signUserInfo.getToken());
        return sign;
    }

    private ResponseBody<RoleInfo> sign(String roleId,String token){
        String url=String.format("https://api.kurobbs.com/gamer/roleBox/aki/baseData?serverId=%s&type=2&roleId=%s&sizeType=1&gameId=%s"
                ,"76402e5b20be2c39f095a152090afddc",roleId,"3");
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper=new ObjectMapper();

                JsonNode tree = mapper.readTree(response.body());
                //RoleInfo data = mapper.readValue(tree.get("data").toString(), RoleInfo.class);

                return mapper.readValue(response.body(), new TypeReference<ResponseBody<RoleInfo>>() {
                });
            }else {
                return new ResponseBody<>(1, "连接出错");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1, "连接出错");
        }
    }
}