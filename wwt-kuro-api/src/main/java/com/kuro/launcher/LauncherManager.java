package com.kuro.launcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.launcher.model.api.PlayerData;
import com.kuro.launcher.model.api.PlayerInfo;
import com.kuro.model.ResponseBody;
import com.kuro.model.SourceType;
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

/**
 * 启动器 API 管理类（Facade）：查询玩家信息 / 角色详数据。
 * 原 QueryPlayerInfoTask + QueryRoleTask 逻辑收敛至此。
 * 纯同步，无 JavaFX 依赖，调用方自行起线程。
 *
 * @author Leck
 */
public final class LauncherManager {
    private static final Logger logger = LoggerFactory.getLogger(LauncherManager.class);

    private static final String API_CN = "https://pc-launcher-sdk-api.kurogame.com/game/queryPlayerInfo";
    private static final String API_GLOBAL = "https://pc-launcher-sdk-api.kurogame.net/game/queryPlayerInfo";
    private static final String API_ROLE_CN = "https://pc-launcher-sdk-api.kurogame.com/game/queryRole";
    private static final String API_ROLE_GLOBAL = "https://pc-launcher-sdk-api.kurogame.net/game/queryRole";

    private static final int MAX_RETRIES = 5;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LauncherManager(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询玩家信息（原 QueryPlayerInfoTask）
     *
     * @param oauthCode  授权码
     * @param sourceType 来源类型，区分 CN / GLOBAL
     * @return 玩家信息
     */
    public ResponseBody<PlayerInfo> queryPlayerInfo(String oauthCode, SourceType sourceType) {
        String url = getUrl(sourceType);
        if (url == null) {
            return ResponseBody.create(-1, "无法识别到玩家服务器", null);
        }
        try {
            String jsonPayload = getRequestJson(oauthCode);
            return startPlayerInfoRequest(url, jsonPayload, 0);
        } catch (Exception e) {
            logger.error("请求任务异常", e);
            return ResponseBody.create(-1, "异常: " + e.getMessage(), null);
        }
    }

    /**
     * 查询角色详数据（原 QueryRoleTask）
     *
     * @param playerId  玩家ID（首字符决定区服）
     * @param oauthCode  授权码
     * @return 角色详数据
     */
    public ResponseBody<PlayerData> queryRole(String playerId, String oauthCode) {
        Region region = getRegion(playerId);
        if (region == null) {
            return ResponseBody.create(-1, "无法识别到玩家服务器", null);
        }
        try {
            String jsonPayload = getRoleRequestJson(oauthCode, playerId, region.key());
            PlayerData data = startRoleRequest(region, jsonPayload, 0);
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

    // ==================== queryPlayerInfo 内部 ====================

    private ResponseBody<PlayerInfo> startPlayerInfoRequest(String url, String json, int retryCount) throws IOException, InterruptedException {
        if (retryCount >= MAX_RETRIES) {
            logger.error("达到最大重试次数，停止请求");
            return null;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode tree = objectMapper.readTree(response.body());
            int code = tree.get("code").asInt();
            String message = tree.get("message").asText();

            if (code == 1005) {
                logger.warn("收到1005响应，500ms后进行第{}次重试", retryCount + 1);
                Thread.sleep(500);
                return startPlayerInfoRequest(url, json, retryCount + 1);

            } else if (code == 200 || code == 0) {
                JsonNode dataNode = tree.get("data");
                if (dataNode != null) {
                    Iterator<String> iterator = dataNode.fieldNames();
                    if (iterator.hasNext()) {
                        String key = iterator.next();
                        String rawPlayerDataJson = dataNode.get(key).asText();
                        PlayerInfo playerInfo = objectMapper.readValue(rawPlayerDataJson, PlayerInfo.class);
                        playerInfo.setRegion(key);
                        return ResponseBody.create(200, "Success", playerInfo);
                    }
                }
            } else if (code == 1001) {
                return ResponseBody.create(-1, "玩家Token失效，无法获取", null);
            } else {
                logger.error("接口返回错误码: {}, 信息: {}", code, message);
                return ResponseBody.create(-1, "获取玩家数据失败", null);
            }
        }
        return ResponseBody.create(-1, "获取玩家数据失败", null);
    }

    private String getRequestJson(String oauthCode) throws JsonProcessingException {
        Map<String, String> params = new HashMap<>();
        params.put("oauthCode", oauthCode);
        return objectMapper.writeValueAsString(params);
    }

    private String getUrl(SourceType sourceType) {
        if (sourceType == null) return null;
        if (sourceType == SourceType.GLOBAL)
            return API_GLOBAL;
        else
            return API_CN;
    }

    // ==================== queryRole 内部 ====================

    private PlayerData startRoleRequest(Region region, String json, int retryCount) throws IOException, InterruptedException {
        if (retryCount >= MAX_RETRIES) {
            logger.error("达到最大重试次数，停止请求");
            return null;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(region.url()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode tree = objectMapper.readTree(response.body());
            int code = tree.get("code").asInt();
            String message = tree.get("message").asText();

            if (code == 1005) {
                logger.warn("收到1005响应，500ms后进行第{}次重试", retryCount + 1);
                Thread.sleep(500);
                return startRoleRequest(region, json, retryCount + 1);

            } else if (code == 200 || code == 0) {
                JsonNode dataNode = tree.get("data");
                if (dataNode != null && dataNode.has(region.key())) {
                    String rawPlayerDataJson = dataNode.get(region.key()).asText();
                    return objectMapper.readValue(rawPlayerDataJson, PlayerData.class);
                }
            } else {
                logger.error("接口返回错误码: {}, 信息: {}", code, message);
            }
        }
        return null;
    }

    private String getRoleRequestJson(String oauthCode, String playerId, String regionKey) throws JsonProcessingException {
        Map<String, String> params = new HashMap<>();
        params.put("oauthCode", oauthCode);
        params.put("playerId", playerId);
        params.put("region", regionKey);
        return objectMapper.writeValueAsString(params);
    }

    /**
     * 按 playerId 首字符路由区服（原 QueryRoleTask.getRegion）
     */
    private Region getRegion(String playerId) {
        if (playerId == null || playerId.isEmpty()) return null;
        return switch (playerId.charAt(0)) {
            case '1' -> new Region("China", API_ROLE_CN);
            case '6' -> new Region("Eu", API_ROLE_GLOBAL);
            case '7' -> new Region("Asia", API_ROLE_GLOBAL);
            case '8' -> new Region("HMT", API_ROLE_GLOBAL);
            case '9' -> new Region("SEA", API_ROLE_GLOBAL);
            default -> null;
        };
    }

    /**
     * 区服路由结果（替代原 javafx.util.Pair）
     */
    private record Region(String key, String url) {}
}
