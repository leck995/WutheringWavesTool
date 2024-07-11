package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.user.RoleInfo;
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
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class UserDataRefreshTask extends Task<ResponseBody> {
    private static final Logger LOG= LoggerFactory.getLogger(UserDataRefreshTask.class);
    private SignUserInfo signUserInfo;

    public UserDataRefreshTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody call() throws Exception {
        return sign(signUserInfo.getRoleId(),signUserInfo.getToken());
    }

    private ResponseBody sign(String roleId,String token){
        String url1="https://api.kurobbs.com/gamer/widget/game3/refresh?type=1&sizeType=2";
        String url2=String.format(
                "https://api.kurobbs.com/gamer/roleBox/aki/refreshData?gameId=3&serverId=76402e5b20be2c39f095a152090afddc&roleId=%s",roleId);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url1,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            HttpRequest request2 = HttpRequestUtil.getRequest(url2,token);
            HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
            LOG.debug("刷新请求1,状态码:{},刷新请求2,状态码:{}",response.statusCode(),response2.statusCode());
            return null;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}