package cn.tealc.wutheringwavestool.thread.api;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.roleData.user.RoleInfo;
import cn.tealc.wutheringwavestool.util.ApiDecryptException;
import cn.tealc.wutheringwavestool.util.ApiUtil;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

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
        String body=String.format("serverId=%s&type=2&roleId=%s&sizeType=1&gameId=%s"
                ,ApiConfig.PARAM_SERVER_ID,roleId,ApiConfig.PARAM_GAME_ID);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(ApiConfig.BASE_DATA_URL,body,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper=new ObjectMapper();

                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });

                ResponseBody<RoleInfo> responseBody = new ResponseBody<>(responseBodyForApi.getCode(), responseBodyForApi.getMsg(),responseBodyForApi.getSuccess());
                String row = ApiUtil.decrypt(responseBodyForApi.getData());
                RoleInfo roleInfo = mapper.readValue(row, RoleInfo.class);
                responseBody.setData(roleInfo);
                return responseBody;
            }else {
                return new ResponseBody<>(1, "连接出错");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1, "连接出错");
        } catch (ApiDecryptException e){
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}