package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.model.CardInfo;
import cn.tealc.wutheringwavestool.model.Message;

import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @program: WutheringWavesTool
 * @description: 获取卡池数据
 * @author: Leck
 * @create: 2024-07-03 00:14
 */
public class CardPoolRequestTask extends Task<Map<String, List<CardInfo>>> {
    private static final Logger LOG = LoggerFactory.getLogger(CardPoolRequestTask.class);
    private static final String cn_REQUEST_URL="https://gmserver-api.aki-game2.com/gacha/record/query";
    private static final String oversea_REQUEST_URL="https://gmserver-api.aki-game2.net/gacha/record/query";
    String REQUEST_URL = null;
    String Country = null;
    private static final Map<String,String> typePool= new LinkedHashMap<>();
    private Map<String, String> params;

    public CardPoolRequestTask(Map<String, String> params, String gameRootDir) {
        String[] pools = LanguageManager.getStringArray("ui.analysis.base_pool");
        typePool.put(pools[0],"1");
        typePool.put(pools[1],"2");
        typePool.put(pools[2],"3");
        typePool.put(pools[3],"4");
        typePool.put(pools[4],"5");
        typePool.put(pools[5],"6");
        typePool.put(pools[6],"7");
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
        List<CardInfo> list = new ArrayList<>();
        if (newData.isEmpty()){
            return oldData;
        }else if (oldData.isEmpty()){
            return newData;
        }

        CardInfo last = newData.getLast();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime newTime = LocalDateTime.parse(last.getTime(), formatter);

        for (int i = 0; i <oldData.size(); i++) {
            CardInfo first = oldData.get(i);
            LocalDateTime oldTime = LocalDateTime.parse(first.getTime(), formatter);
            if (oldTime.isBefore(newTime)){
                list.addAll(oldData.subList(i,oldData.size()));
                break;
            }else if (oldTime.isEqual(newTime)){
                for (int j = i; j < oldData.size(); j++) {
                    CardInfo first2 = oldData.get(j);
                    LocalDateTime oldTime2 = LocalDateTime.parse(first2.getTime(), formatter);
                    if (oldTime2.isBefore(newTime)){
                        list.addAll(oldData.subList(j,oldData.size()));
                        break;
                    }
                }
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
        Country_URL(params);
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

    private String Country_URL(Map<String,String> params){
        String playerId = params.get("playerId");
        switch (playerId.charAt(0)) {
            case '1':
                REQUEST_URL = cn_REQUEST_URL;
                Country = "国服";
                break;
            case '6':
                REQUEST_URL = oversea_REQUEST_URL;
                Country = "Eu";
                break;
            case '7':
                REQUEST_URL = oversea_REQUEST_URL;
                Country = "Asia";
                break;
            case '8':
                REQUEST_URL = oversea_REQUEST_URL;
                Country = " HMT (HK, MO, TW)";
                break;
            case '9':
                REQUEST_URL = oversea_REQUEST_URL;
                Country = "SEA";
                break;
            default:

                throw new IllegalArgumentException("Invalid playerId: " + playerId);
        }
        return REQUEST_URL;
    }

}
