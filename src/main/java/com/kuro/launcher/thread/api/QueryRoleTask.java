package com.kuro.launcher.thread.api;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.launcher.model.api.PlayerData;
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
import java.util.Map;

public class QueryRoleTask extends Task<ResponseBody<PlayerData>> {
    private static final Logger logger = LoggerFactory.getLogger(QueryRoleTask.class);
    private static final String API_CN = "https://pc-launcher-sdk-api.kurogame.com/game/queryRole";
    private static final String API_GLOBAL = "https://pc-launcher-sdk-api.kurogame.net/game/queryRole";

    // 静态常量，避免重复创建对象
    private static final ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String oauthCode;
    private final String playerId;
    private static final int MAX_RETRIES = 5; // 最大重试次数

    public QueryRoleTask(String playerId, String oauthCode) {
        this.oauthCode = oauthCode;
        this.playerId = playerId;
    }

    @Override
    protected ResponseBody<PlayerData> call() {
        Pair<String, String> region = getRegion(playerId);
        if (region == null) {
            return ResponseBody.create(-1, "无法识别到玩家服务器", null);
        }
        try {
            String jsonPayload = getRequestJson(region);
            // 开始请求，初始重试计数为 0
            PlayerData data = startRequest(region, jsonPayload, 0);

            if (data != null) {
                return ResponseBody.create(200, "success", data);
            } else {
                return ResponseBody.create(-1, "获取玩家数据失败或重试次数过多", null);
            }
        } catch (Exception e) {
            logger.error("请求任务异常", e);
            return ResponseBody.create(-1, "异常: " + e.getMessage(), null);
        }
    }

    /**
     * 递归请求核心方法
     */
    private PlayerData startRequest(Pair<String, String> region, String json, int retryCount) throws IOException, InterruptedException {
        if (retryCount >= MAX_RETRIES) {
            logger.error("达到最大重试次数，停止请求");
            return null;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(region.getValue()))
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
                return startRequest(region, json, retryCount + 1); // 必须 return 递归结果

            } else if (code == 200 || code == 0) {
                // 库洛返回的 data 是一个对象，key 是服务器名 (如 "China")
                JsonNode dataNode = tree.get("data");
                String regionKey = region.getKey();

                if (dataNode != null && dataNode.has(regionKey)) {
                    // 关键：regionKey 对应的值是一个被转义的 JSON 字符串
                    String rawPlayerDataJson = dataNode.get(regionKey).asText();
                    return mapper.readValue(rawPlayerDataJson, PlayerData.class);
                }
            } else {
                logger.error("接口返回错误码: {}, 信息: {}", code, message);
            }
        }
        return null;
    }

    private String getRequestJson(Pair<String, String> region) throws JsonProcessingException {
        Map<String, String> params = new HashMap<>();
        params.put("oauthCode", oauthCode);
        params.put("playerId", playerId);
        params.put("region", region.getKey());
        return mapper.writeValueAsString(params);
    }

    private Pair<String, String> getRegion(String playerId) {
        if (playerId == null || playerId.isEmpty()) return null;
        return switch (playerId.charAt(0)) {
            case '1' -> new Pair<>("China", API_CN);
            case '6' -> new Pair<>("Eu", API_GLOBAL);
            case '7' -> new Pair<>("Asia", API_GLOBAL);
            case '8' -> new Pair<>("HMT", API_GLOBAL);
            case '9' -> new Pair<>("SEA", API_GLOBAL);
            default -> null;
        };
    }
}