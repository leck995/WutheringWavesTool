package com.kuro.launcher.thread.api;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.SourceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.launcher.model.api.PlayerData;
import com.kuro.launcher.model.api.PlayerInfo;
import javafx.concurrent.Task;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class QueryPlayerInfoTask extends Task<ResponseBody<PlayerInfo>> {
    private static final Logger logger = LoggerFactory.getLogger(QueryPlayerInfoTask.class);
    private static final String API_CN = "https://pc-launcher-sdk-api.kurogame.com/game/queryPlayerInfo";
    private static final String API_GLOBAL = "https://pc-launcher-sdk-api.kurogame.net/game/queryPlayerInfo";

    // 静态常量，避免重复创建对象
    private static final ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String oauthCode;
    private final SourceType sourceType;
    private static final int MAX_RETRIES = 5; // 最大重试次数

    public QueryPlayerInfoTask(String oauthCode, SourceType type) {
        this.oauthCode = oauthCode;
        this.sourceType = type;
    }

    @Override
    protected ResponseBody<PlayerInfo> call() {
        String url= getUrl(sourceType);
        if (url == null) {
            return ResponseBody.create(-1, "无法识别到玩家服务器", null);
        }
        try {
            String jsonPayload = getRequestJson();
            // 开始请求，初始重试计数为 0
            return startRequest(url, jsonPayload, 0);
        } catch (Exception e) {
            logger.error("请求任务异常", e);
            return ResponseBody.create(-1, "异常: " + e.getMessage(), null);
        }
    }

    /**
     * 递归请求核心方法
     */
    private ResponseBody<PlayerInfo> startRequest(String url,String json, int retryCount) throws IOException, InterruptedException {
        if (retryCount >= MAX_RETRIES) {
            logger.error("达到最大重试次数，停止请求");
            return null;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode tree = mapper.readTree(response.body());
            int code = tree.get("code").asInt();
            String message = tree.get("message").asText();

            if (code == 1005) {
                logger.warn("收到1005响应，100ms后进行第{}次重试", retryCount + 1);
                Thread.sleep(500); // 按照需求等待 500ms
                return startRequest(url, json, retryCount + 1); // 必须 return 递归结果

            } else if (code == 200 || code == 0) {
                // 库洛返回的 data 是一个对象，key 是服务器名 (如 "China")
                JsonNode dataNode = tree.get("data");
                if (dataNode != null) {
                    Iterator<String> iterator = dataNode.fieldNames();
                    if (iterator.hasNext()) {
                        String key = iterator.next();
                        String rawPlayerDataJson = dataNode.get(key).asText();
                        PlayerInfo playerInfo = mapper.readValue(rawPlayerDataJson, PlayerInfo.class);
                        playerInfo.setRegion(key);
                        return ResponseBody.create(200,"Success",playerInfo);
                    }
                }
            } else if(code == 1001){
                return ResponseBody.create(-1, "玩家Token失效，无法获取", null);
            }else {
                logger.error("接口返回错误码: {}, 信息: {}", code, message);
                return ResponseBody.create(-1, "获取玩家数据失败", null);
            }
        }
        return ResponseBody.create(-1, "获取玩家数据失败", null);
    }

    private String getRequestJson() throws JsonProcessingException {
        Map<String, String> params = new HashMap<>();
        params.put("oauthCode", oauthCode);
        return mapper.writeValueAsString(params);
    }

    private String getUrl(SourceType sourceType) {
        if (sourceType == null)  return null;
        if (sourceType == SourceType.GLOBAL)
            return API_GLOBAL;
        else
            return API_CN;
    }
}