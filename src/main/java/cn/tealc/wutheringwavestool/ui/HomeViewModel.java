package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.Config;
import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.model.Message;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;

import java.awt.*;
import java.io.File;
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
 * @create: 2024-07-03 19:57
 */
public class HomeViewModel implements ViewModel {
    public HomeViewModel() {
        //getPoolInfo();
    }

    public void startUpdate(){
        String dir = Config.setting.gameRootDir.get();
        if (dir != null){
            File exe=new File(dir+File.separator + "launcher.exe");
            if (exe.exists()){
                try {
                    Desktop.getDesktop().open(exe);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING,String.format("无法找到%s，请确保游戏目录正确",exe.getPath()),false));
            }
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING,"请在设置在先设置游戏目录"),false);
        }

    }
    public void startGame(){
        String dir = Config.setting.gameRootDir.get();
        if (dir != null){
            File exe=new File(dir+File.separator+"Wuthering Waves Game"+File.separator + "Wuthering Waves.exe");
            if (exe.exists()){
                try {
                    Desktop.getDesktop().open(exe);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING,String.format("无法找到%s，请确保游戏目录正确",exe.getPath()),false));
            }

        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING,"请在设置在先设置游戏目录"),false);
        }
    }


    public void getPoolInfo(){
        String url="https://api.kurobbs.com/wiki/core/homepage/getPage";
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-type","application/json")
                .headers("Wiki_type","9")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            if (response.statusCode() == 200) {
                JsonNode tree = mapper.readTree(response.body());
                JsonNode sideModules = tree.get("data").get("contentJson").get("sideModules");
                JsonNode jsonNode = sideModules.get(0);
                System.out.println(jsonNode.get("title").asText());

                //System.out.println(response.body());
                //return mapper.readValue(response.body(), Message.class);
            }else {
                //return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}