package cn.tealc.wutheringwavestool.thread.api.role;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import cn.tealc.wutheringwavestool.model.roleData.RoleDetail;
import cn.tealc.wutheringwavestool.model.roleData.user.RoleInfo;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.thread.api.ApiConfig;
import cn.tealc.wutheringwavestool.util.ApiDecryptException;
import cn.tealc.wutheringwavestool.util.ApiUtil;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
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
                "%s?gameId=3&serverId=76402e5b20be2c39f095a152090afddc&roleId=%s&id=%d&channelId=19&countryCode=1", ApiConfig.ROLE_DETAIL_URL,roleId,cardRoleId);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });

                if (responseBodyForApi.getCode() == 200){
                    ResponseBody<RoleDetail> responseBody = new ResponseBody<>(responseBodyForApi.getCode(), responseBodyForApi.getMsg(),responseBodyForApi.getSuccess());
                    String row = ApiUtil.decrypt(responseBodyForApi.getData());
                    RoleDetail roleDetail = mapper.readValue(row, RoleDetail.class);
                    responseBody.setData(roleDetail);
                    return responseBody;
                }else {
                    return new ResponseBody<>(1,responseBodyForApi.getMsg(),false);
                }
            }else {
                ResponseBody<RoleDetail> responseBody = new ResponseBody<>();
                LOG.error("网络请求失败，错误代码：{}",response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException | ApiDecryptException e) {
            LOG.error("错误：",e);
            return new ResponseBody<>(1,e.getMessage());
        }
    }
}