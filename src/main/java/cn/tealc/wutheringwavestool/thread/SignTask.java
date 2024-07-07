package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class SignTask extends Task<String> {

    @Override
    protected String call() throws Exception {
        File file=new File("signInfo.json");
        if (file.exists()){
            ObjectMapper mapper = new ObjectMapper();
            List<SignUserInfo> signUserInfos = mapper.readValue(file, new TypeReference<List<SignUserInfo>>() {
            });
            StringBuilder sb=new StringBuilder();
            signUserInfos.forEach(user->{
                String sign = sign(user.roleId(), user.userId(), user.token());
                sb.append(String.format("当前用户ID:%s,签到状态：%s",user.userId(), sign));
                sb.append("\n");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            return sb.toString();
        }else {
            return "无相关配置，不启动签到";
        }
    }

    private String sign(String roleId,String userId,String token){
        LocalDate today = LocalDate.now();
        int monthValue= today.getMonthValue();
        String url=String.format("https://api.kurobbs.com/encourage/signIn/v2?gameId=%s&serverId=%s&roleId=%s&userId=%s&reqMonth=%02d"
                ,"3","76402e5b20be2c39f095a152090afddc",roleId,userId,monthValue);

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
            System.out.println(response.body());
            if (response.statusCode() == 200) {
                if (response.body().contains("请求成功")){
                    return "签到成功";
                }else if (response.body().contains("请勿重复签到")){
                    return "请勿重复签到";
                }else {
                    return "签到失败";
                }
            }else {
                return "签到失败";
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}