package cn.tealc.wutheringwavestool.thread.api;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @program: WutheringWavesTool
 * @description: 刷新服务器缓存，获取实时数据
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class UserDataRefreshTask extends Task<ResponseBody<String>> {
    private static final Logger LOG= LoggerFactory.getLogger(UserDataRefreshTask.class);
    private SignUserInfo signUserInfo;

    public UserDataRefreshTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<String> call() throws Exception {
        return sign(signUserInfo.getRoleId(),signUserInfo.getToken());
    }

    private ResponseBody<String> sign(String roleId,String token){
        String url1=ApiConfig.GAME_DATA_URL +"?type=1&sizeType=2";
        String url2=String.format("%s?gameId=%s&serverId=%s&roleId=%s",
                ApiConfig.REFRESH_URL,ApiConfig.PARAM_GAME_ID,ApiConfig.PARAM_SERVER_ID,roleId);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url1,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            HttpRequest request2 = HttpRequestUtil.getRequest(url2,token);
            HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
            LOG.debug("角色数刷新,状态码1: {},状态码2: {}",response.statusCode(),response2.statusCode());
            return new ResponseBody<>(200,"角色数据刷新成功");
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1,"无法刷新角色数据，连接超时或解析错误");
        }
    }
}