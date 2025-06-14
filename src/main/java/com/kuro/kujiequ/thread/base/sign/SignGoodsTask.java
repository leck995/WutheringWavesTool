package com.kuro.kujiequ.thread.base.sign;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.SignGood;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.util.HttpRequestUtil;
import javafx.concurrent.Task;
import javafx.util.Pair;
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
public class SignGoodsTask extends Task<ResponseBody<Pair<Boolean,List<SignGood>>>> {
    private static final Logger LOG= LoggerFactory.getLogger(SignGoodsTask.class);
    private UserInfo userInfo;

    public SignGoodsTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<Pair<Boolean,List<SignGood>>> call() throws Exception {
        String url=String.format("%s?gameId=%s&serverId=%s&roleId=%s&userId=%s"
                , ApiConfig.SIGNIN_INIT_URL,ApiConfig.PARAM_GAME_ID,ApiConfig.PARAM_SERVER_ID,userInfo.getRoleId(),userInfo.getUserId());
        HttpClient client = HttpClient.newHttpClient();
        ResponseBody<Pair<Boolean,List<SignGood>>> body =new ResponseBody<>();
        try {
            HttpRequest request = HttpRequestUtil.getRequestWithSource(url, userInfo.getToken(), userInfo.getIsWeb(), userInfo.getDevCode());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper=new ObjectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                if (code == 200) {
                    JsonNode data = tree.get("data");
                    JsonNode jsonNode = data.get("signInGoodsConfigs");
                    List<SignGood> signGoods = mapper.readValue(jsonNode.toString(), new TypeReference<List<SignGood>>() {
                    });
                    int num = data.get("sigInNum").asInt();
                    for (int i = 0; i < signGoods.size(); i++) {
                        signGoods.get(i).setSign(i < num);
                    }
                    Boolean isSign = data.get("isSigIn").asBoolean();
                    body.setData(new Pair<>(isSign, signGoods));
                }
                body.setCode(code);
                body.setMsg(tree.get("msg").asText());
                return body;
            }else {
                body.setCode(1);
                body.setMsg("连接失败，错误状态码:" + response.statusCode());
                return body;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            body.setCode(1);
            body.setMsg(e.getMessage());
            return body;
        }
    }

}