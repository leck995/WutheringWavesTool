package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.model.CardInfo;
import cn.tealc.wutheringwavestool.model.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 00:14
 */
public class CardRequestTask extends Task<Map<String, List<CardInfo>>> {
    private static final String REQUEST_URL="https://gmserver-api.aki-game2.com/gacha/record/query";
    private static final Map<String,String> typePool= new LinkedHashMap<>();
    private Map<String, String> params;

    public CardRequestTask(Map<String, String> params,String gameRootDir) {
        typePool.put("角色活动唤取","1");
        typePool.put("武器活动唤取","2");
        typePool.put("角色常驻唤取","3");
        typePool.put("武器常驻唤取","4");
        typePool.put("新手唤取","5");
        typePool.put("新手自选唤取","6");
        typePool.put("新手自选唤取（感恩定向唤取）","7");
        this.params = params;
    }

    @Override
    protected Map<String, List<CardInfo>> call() throws Exception {
        Map<String, List<CardInfo>> map=new LinkedHashMap<String, List<CardInfo>>();
        //先获取保存数据
        String playerId = params.get("playerId");
        File poolJson=new File(String.format("data/%s/pool.json",playerId));
        if(poolJson.exists()){
            ObjectMapper mapper = new ObjectMapper();
            try {
                map= mapper.readValue(poolJson, new TypeReference<Map<String, List<CardInfo>>>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (Map.Entry<String, String> entry : typePool.entrySet()) {
            Message message = query(params, entry.getValue());
            assert message != null;
            if (message.getCode() == 0){
                List<CardInfo> list= new ArrayList<>(message.getData());
                List<CardInfo> oldList = map.get(entry.getKey());
                if (oldList != null){
                    List<CardInfo> update = update(oldList, list);
                    map.replace(entry.getKey(),update);
                }else {
                    map.put(entry.getKey(),list);
                }

            }
        }
        return map;
    }

    private List<CardInfo> update(List<CardInfo> oldData,List<CardInfo> newData){
        CardInfo last = newData.getLast();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime newTime = LocalDateTime.parse(last.getTime(), formatter);
        List<CardInfo> list = new ArrayList<>();
        for (int i = oldData.size()-1; i >= 0; i--) {
            CardInfo first = oldData.get(i);
            LocalDateTime oldTime = LocalDateTime.parse(first.getTime(), formatter);
            if (oldTime.isBefore(newTime)){
                list.addAll(oldData.subList(i,oldData.size()));
                break;
            }else if (oldTime.isEqual(newTime)){
                list.addAll(oldData.subList(i+1,oldData.size()));
                break;
            }
        }
        list.addAll(newData);
        return list;
    }

    private Message query(Map<String,String> params,String cardPoolType) throws IOException, InterruptedException {
        params.replace("cardPoolType",cardPoolType);
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        String s = mapper.writeValueAsString(params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REQUEST_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(s))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), Message.class);
        }else {
            return null;
        }
    }



}