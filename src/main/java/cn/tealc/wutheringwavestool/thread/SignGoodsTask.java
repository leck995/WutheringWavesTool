package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.model.sign.SignGood;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class SignGoodsTask extends Task<Pair<Boolean,List<SignGood>>> {
    private static final Logger LOG= LoggerFactory.getLogger(SignGoodsTask.class);
    private SignUserInfo signUserInfo;

    public SignGoodsTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected Pair<Boolean,List<SignGood>> call() throws Exception {
        Pair<Boolean,List<SignGood>> sign = sign(signUserInfo.getRoleId(), signUserInfo.getUserId(), signUserInfo.getToken());
        return sign;

    }

    private Pair<Boolean,List<SignGood>> sign(String roleId,String userId,String token){
        String url=String.format("https://api.kurobbs.com/encourage/signIn/initSignInV2?gameId=%s&serverId=%s&roleId=%s&userId=%s"
                ,"3","76402e5b20be2c39f095a152090afddc",roleId,userId);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper=new ObjectMapper();
                JsonNode tree = mapper.readTree(response.body());
                JsonNode jsonNode = tree.get("data").get("signInGoodsConfigs");
                List<SignGood> signGoods = mapper.readValue(jsonNode.toString(), new TypeReference<List<SignGood>>() {
                });
                int num = tree.get("data").get("sigInNum").asInt();
                for (int i = 0; i < signGoods.size(); i++) {
                    signGoods.get(i).setSign(i < num);
                }
                Boolean isSign = tree.get("data").get("isSigIn").asBoolean();
                return new Pair<>(isSign, signGoods);
            }else {
                return null;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return null;
        }
    }
}