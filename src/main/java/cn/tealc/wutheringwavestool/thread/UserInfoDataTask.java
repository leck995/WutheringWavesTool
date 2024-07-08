package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.user.RoleDailyData;
import cn.tealc.wutheringwavestool.model.user.RoleInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;

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
public class UserInfoDataTask extends Task<ResponseBody> {
    private SignUserInfo signUserInfo;

    public UserInfoDataTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody call() throws Exception {
        ResponseBody sign = sign(signUserInfo.getRoleId(), signUserInfo.getToken());
        return sign;
    }

    private ResponseBody sign(String roleId,String token){

        String url=String.format("https://api.kurobbs.com/gamer/roleBox/aki/baseData?serverId=%s&type=2&roleId=%s&sizeType=1&gameId=%s"
                ,"76402e5b20be2c39f095a152090afddc",roleId,"3");
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("sec-ch-ua", "\"Chromium\";v=\"124\", \"Android WebView\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
                    .header("source", "h5")
                    .header("devcode", "111.181.85.154, Mozilla/5.0 (Linux; Android 14; 22081212C Build/UKQ1.230917.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.179 Mobile Safari/537.36 Kuro/2.2.0 KuroGameBox/2.2.0")
                    .header("token", token)
                    .header("sec-ch-ua-platform", "\"Android\"")
                    .header("origin", "https://web-static.kurobbs.com")
                    .header("x-requested-with", "com.kurogame.kjq")
                    .header("sec-fetch-site", "same-site")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-dest", "empty")
                    .header("accept-language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("priority", "u=1, i")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper=new ObjectMapper();

                JsonNode tree = mapper.readTree(response.body());
                RoleInfo data = mapper.readValue(tree.get("data").toString(), RoleInfo.class);

                ResponseBody responseBody = new ResponseBody();
                responseBody.setData(data);
                responseBody.setCode(tree.get("code").asInt());
                responseBody.setMsg(tree.get("msg").asText());
                responseBody.setSuccess(tree.get("success").asBoolean());
                return responseBody;
            }else {
                return null;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}