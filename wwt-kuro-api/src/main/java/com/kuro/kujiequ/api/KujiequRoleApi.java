package com.kuro.kujiequ.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.KujiequApiContext;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.roleData.RoleDetail;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import com.kuro.model.ResponseBodyForApi;
import com.kuro.util.HTTPRequestMultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 库街区角色数据 API：角色列表、角色详情、查找绑定角色。
 * <p>
 * 本类为纯 API 模块，零 JavaFX 依赖。所有方法同步返回 {@link ResponseBody}，
 * 方法体原样搬迁自原 KujiequManager，逻辑无改动。
 *
 * @author Leck
 */
public class KujiequRoleApi {
    private static final Logger log = LoggerFactory.getLogger(KujiequRoleApi.class);

    private final KujiequApiContext ctx;

    public KujiequRoleApi(KujiequApiContext ctx) {
        this.ctx = ctx;
    }

    // ==================== 1. GameRoleDataTask：获取拥有角色列表 ====================

    /**
     * 获取用户拥有的角色列表，按等级降序排序。
     *
     * @param userInfo 用户信息
     * @return 角色列表响应
     */
    public ResponseBody<List<Role>> getGameRoleData(UserInfo userInfo) {
        String url = ApiConfig.ROLE_DATA_URL;
        try {
            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
                    .addPart("serverId", ApiConfig.PARAM_SERVER_ID)
                    .addPart("roleId", userInfo.getRoleId())
                    .addPart("gameId", ApiConfig.PARAM_GAME_ID)
                    .build();
            HttpRequest.Builder builder = ctx.getBuilder(url, body, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            ResponseBody<List<Role>> responseBody = new ResponseBody<>();
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();

                if (code == 220) throw new AccessTokenException();
                if (code == 200 || code == 10902) {
                    responseBody.setCode(200);
                    String data = tree.get("data").asText();
                    JsonNode dataTree = mapper.readTree(data);
                    JsonNode jsonNode = dataTree.get("roleList");
                    List<Role> roleList = mapper.readValue(jsonNode.toString(), new TypeReference<List<Role>>() {
                    });
                    responseBody.setSuccess(tree.get("success").asBoolean());
                    roleList.sort((o1, o2) -> Integer.compare(o2.getLevel(), o1.getLevel()));
                    responseBody.setData(roleList);
                } else {
                    responseBody.setCode(code);
                    log.error("服务器返回异常，错误代码：{}", code);
                    JsonNode node = tree.get("msg");
                    String msg = node.asText();
                    ctx.checkTokenExpired(msg, userInfo);
                    responseBody.setMsg(msg);
                }
            } else {
                log.error("网络请求失败，错误代码：{}", response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
            }
            return responseBody;
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error("错误:{}", e.getMessage());
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 2. GameRoleDetailTask：获取单个角色详情 ====================

    /**
     * 获取单个角色详情。
     *
     * @param userInfo   用户信息
     * @param cardRoleId 角色卡片 ID
     * @return 角色详情响应
     */
    public ResponseBody<RoleDetail> getGameRoleDetail(UserInfo userInfo, int cardRoleId) {
        String url = ApiConfig.ROLE_DETAIL_URL;
        try {
            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
                    .addPart("serverId", ApiConfig.PARAM_SERVER_ID)
                    .addPart("roleId", userInfo.getRoleId())
                    .addPart("gameId", ApiConfig.PARAM_GAME_ID)
                    .addPart("id", String.valueOf(cardRoleId))
                    .addPart("channelId", "19")
                    .addPart("countryCode", "1")
                    .build();
            HttpRequest.Builder builder = ctx.getBuilder(url, body, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.debug(response.body());
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });
                if (responseBodyForApi.getCode() == 200 || responseBodyForApi.getCode() == 10902) {
                    ResponseBody<RoleDetail> responseBody = new ResponseBody<>(200, responseBodyForApi.getMsg(), responseBodyForApi.getSuccess());
                    String row = responseBodyForApi.getData();
                    RoleDetail roleDetail = mapper.readValue(row, RoleDetail.class);
                    responseBody.setData(roleDetail);
                    return responseBody;
                } else {
                    if (responseBodyForApi.getCode() == 220) throw new AccessTokenException();
                    ctx.checkTokenExpired(responseBodyForApi.getMsg(), userInfo);
                    return new ResponseBody<>(1, responseBodyForApi.getMsg(), false);
                }
            } else {
                ResponseBody<RoleDetail> responseBody = new ResponseBody<>();
                log.error("网络请求失败，错误代码：{}", response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error("错误：", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 3. GameRoleSeekTask：查找角色 ====================

    /**
     * 根据 token 查找游戏绑定的玩家信息列表。
     *
     * @param token 用户登录 token
     * @param isWeb 是否为 H5 来源
     * @return 用户信息列表响应
     */
    public ResponseBody<List<UserInfo>> seekGameRole(String token, boolean isWeb) {
        String url = String.format("%s?gameId=3", ApiConfig.ACCOUNT_SEEK_ROLE);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("source", isWeb ? "h5" : "android")
                    .header("token", token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.debug(response.body());
                ObjectMapper mapper = ctx.objectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                if (code == 200) {
                    JsonNode dataNode = tree.get("data");
                    if (dataNode.isArray() && !dataNode.isEmpty()) {
                        List<UserInfo> list = new ArrayList<>();
                        for (JsonNode jsonNode : dataNode) {
                            long userId = jsonNode.path("userId").asLong();
                            String roleId = jsonNode.path("roleId").asText();
                            String roleName = jsonNode.path("roleName").asText();
                            UserInfo userInfo = new UserInfo();
                            userInfo.setToken(token);
                            userInfo.setIsWeb(isWeb);
                            userInfo.setRoleId(roleId);
                            userInfo.setRoleName(roleName);
                            userInfo.setUserId(String.valueOf(userId));
                            list.add(userInfo);
                        }
                        return ResponseBody.create(200, "success", list);
                    } else {
                        return new ResponseBody<>(1, "找不到游戏账号信息，请检查库街区的游戏绑定");
                    }
                } else {
                    log.error("服务器返回异常，错误代码：{}", code);
                    JsonNode node = tree.get("msg");
                    return new ResponseBody<>(code, node.asText());
                }
            } else {
                log.error("网络请求失败，错误代码：{}", response.statusCode());
                return ResponseBody.create(response.statusCode(), "连接失败，响应状态码:" + response.statusCode(), null);
            }
        } catch (IOException | InterruptedException e) {
            log.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}
