package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.CardInfo;
import cn.tealc.wutheringwavestool.model.Message;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.util.FileIO;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.util.LogFileUtil;
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
public class CardPoolRequestTask extends Task<ResponseBody<Map<String, List<CardInfo>>>> {
    private static final Logger LOG = LoggerFactory.getLogger(CardPoolRequestTask.class);
    private static final String CN_REQUEST_URL = "https://gmserver-api.aki-game2.com/gacha/record/query";
    private static final String GLOBAL_REQUEST_URL = "https://gmserver-api.aki-game2.net/gacha/record/query";

    private final Map<String,String> typePool= new LinkedHashMap<>();

    private String requestUrl;
    private Map<String, String> params;


    /**
     * description: 用于刷新
     * @param params
     */
    public CardPoolRequestTask(Map<String, String> params) {
        this();
        this.params = params;
    }

    /**
     * description: 用于获取
     */
    public CardPoolRequestTask() {
        String[] pools = LanguageManager.getStringArray("ui.analysis.base_pool");
        typePool.put(pools[0],"1");
        typePool.put(pools[1],"2");
        typePool.put(pools[2],"3");
        typePool.put(pools[3],"4");
        typePool.put(pools[4],"5");
        typePool.put(pools[5],"6");
        typePool.put(pools[6],"7");
        typePool.put(pools[7],"8");
        typePool.put(pools[8],"9");
    }

    @Override
    protected ResponseBody<Map<String, List<CardInfo>>> call() throws Exception {
        if (params == null){
            String dir = Config.setting.getGameRootDir();
            if (dir != null) {
                File gameLogFile = GameResourcesManager.getGameLogFile();
                if (gameLogFile.exists()) {
                    String url = LogFileUtil.getLogFileUrl(gameLogFile);
                    if (url != null){
                        params = LogFileUtil.getParamFromUrl(url);
                        Map<String, List<CardInfo>> data = getData(params);
                        ResponseBody<Map<String, List<CardInfo>>> responseBody = new ResponseBody<>();
                        responseBody.setData(data);
                        responseBody.setCode(200);
                        responseBody.setMsg(params.get("playerId"));
                        savePoolData(params,data); //保存数据到本地
                        return responseBody;
                    }else { //找不到卡池链接时
                        return new ResponseBody<>(101,LanguageManager.getString("ui.analysis.message.type04"));
                    }
                }else { //日志文件不存在时
                    return new ResponseBody<>(102,String.format(LanguageManager.getString("ui.analysis.message.type05"),gameLogFile.getAbsolutePath()));
                }
            }else { //游戏根目录未设置
                return new ResponseBody<>(103,LanguageManager.getString("ui.analysis.message.type06"));
            }
        }else {
            Map<String, List<CardInfo>> data = getData(params);
            ResponseBody<Map<String, List<CardInfo>>> responseBody = new ResponseBody<>();
            responseBody.setData(data);
            responseBody.setCode(200);
            responseBody.setMsg(params.get("playerId"));
            savePoolData(params,data); //保存数据到本地
            return responseBody;
        }
    }


    /**
     * @description: 获取数据
     * @param:	params
     * @return  java.util.Map<java.lang.String,java.util.List<cn.tealc.wutheringwavestool.model.CardInfo>>
     * @date:   2024/11/21
     */
    private Map<String, List<CardInfo>> getData(Map<String, String> params){
        Map<String, List<CardInfo>> map=new LinkedHashMap<String, List<CardInfo>>();
        //先获取保存数据
        LOG.debug("获取本地抽卡数据");
        String playerId = params.get("playerId");
        File poolJson=new File(String.format("data/%s/pool.json",playerId));
        if(poolJson.exists()){
            ObjectMapper mapper = new ObjectMapper();
            try {
                map = mapper.readValue(poolJson, new TypeReference<Map<String, List<CardInfo>>>() {});
            } catch (IOException e) {
                LOG.error("解析本地用户 {} 抽卡文件失败:", playerId, e);
            }
        }

        requestUrl = getRequestUrl(playerId);
        LOG.debug("开始获取最新数据且匹配本地数据");
        for (Map.Entry<String, String> entry : typePool.entrySet()) {
            Message message = query(params, entry.getValue());
            if (message != null) {
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
            }else {
                LOG.info("请求出现问题");
            }
        }
        LOG.debug("获取结束");
        return map;
    }






    /**
     * @description: 将本地旧数据与最新数据进行整合
     * @param:	oldData
     * @param:	newData
     * @return  java.util.List<cn.tealc.wutheringwavestool.model.CardInfo>
     * @date:   2024/11/21
     */
    private List<CardInfo> update(List<CardInfo> oldData,List<CardInfo> newData){
        if (newData.isEmpty()){
            return oldData;
        }else if (oldData.isEmpty()){
            return newData;
        }

        CardInfo last = newData.getLast();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime newTime = LocalDateTime.parse(last.getTime(), formatter);

        List<CardInfo> list = new ArrayList<>(newData);
        for (int i = 0; i <oldData.size(); i++) {
            CardInfo first = oldData.get(i);
            LocalDateTime oldTime = LocalDateTime.parse(first.getTime(), formatter);
            if (oldTime.isBefore(newTime)){
                list.addAll(oldData.subList(i,oldData.size()));
                break;
            }else if (oldTime.isEqual(newTime)){ //当存在相等时间时，找到同一时间最早的数据
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
        return list;
    }


    /**
     * @description: 查询指定卡池数据
     * @param:	params
     * @param:	cardPoolType
     * @return  cn.tealc.wutheringwavestool.model.Message
     * @date:   2024/11/21
     */
    private Message query(Map<String,String> params,String cardPoolType){
        params.replace("cardPoolType",cardPoolType);
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        String s = null;
        try {
            s = mapper.writeValueAsString(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(s))
                    .build();

            LOG.debug("正在获取抽卡连接:{}",request.uri().toString());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOG.debug("获取到抽卡Json:{}",response.body());
                return mapper.readValue(response.body(), Message.class);
            }else {
                LOG.info("无法获取到抽卡连接，错误代码:{}",response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("请求最新抽卡数据失败", e);
        }
        return null;
    }


    /**
     * @description: 判断是国服还是国际服
     * @param:	playerId
     * @return  java.lang.String
     * @date:   2024/11/21
     */
    private String getRequestUrl(String playerId){
        if (playerId.startsWith("1")){
            return CN_REQUEST_URL;
        }else {
            return GLOBAL_REQUEST_URL;
        }
    }


    /**
     * @description: 保存角色及卡池信息至本地
     * @param:	params
     * @return  void
     * @date:   2024/7/4
     */
    private void savePoolData(Map<String, String> params,Map<String, List<CardInfo>> data){
        String playerId = params.get("playerId");
        ObjectMapper mapper = new ObjectMapper();
        File poolJson=new File(String.format("data/%s/pool.json",playerId));
        File dateJson=new File(String.format("data/%s/data.json",playerId));


        if (poolJson.exists()){ //存在则备份
            long time = poolJson.lastModified();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long todayStart = calendar.getTimeInMillis();
            if (time < todayStart) {// 判断文件修改时间是否不是今天
                LOG.info("文件的最后修改时间不是今天,进行备份");
                FileIO.rename(poolJson,"pool.json.bak",true);
            } else {
                LOG.info("文件的最后修改时间是今天,跳过备份");
            }
        }

        File parentDir = poolJson.getParentFile();
        File dataDir = parentDir.getParentFile();
        if (!dataDir.exists()){
            dataDir.mkdirs();
        }
        if (!parentDir.exists()){
            parentDir.mkdirs();
        }
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(poolJson,data);
            mapper.writerWithDefaultPrettyPrinter().writeValue(dateJson,params);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
