package com.kuro.kujiequ.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.KujiequApiContext;
import com.kuro.kujiequ.model.roleData.user.RoleDailyData;
import com.kuro.kujiequ.model.roleData.user.RoleInfo;
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

/**
 * 库街区用户数据 API：刷新用户数据、用户日常数据、玩家基础数据。
 * <p>
 * 本类为纯 API 模块，零 JavaFX 依赖。所有方法同步返回 {@link ResponseBody}，
 * 方法体原样搬迁自原 KujiequManager，逻辑无改动。
 *
 * @author Leck
 */
public class KujiequUserApi {
    private static final Logger log = LoggerFactory.getLogger(KujiequUserApi.class);

    private final KujiequApiContext ctx;

    public KujiequUserApi(KujiequApiContext ctx) {
        this.ctx = ctx;
    }

    // ==================== 4. UserDataRefreshTask：刷新用户数据 ====================

    /**
     * 刷新服务器缓存的用户数据。
     *
     * @param userInfo 用户信息
     * @return 刷新结果响应
     */
    public ResponseBody<String> refreshUserData(UserInfo userInfo) {
        String url1 = ApiConfig.GAME_DATA_URL + "?type=1&sizeType=2";
        try {
            HttpRequest request01 = HttpRequest.newBuilder()
                    .uri(URI.create(url1))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0")
                    .header("source", userInfo.getIsWeb() ? "h5" : "android")
                    .header("token", userInfo.getToken())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response01 = ctx.httpClient().send(request01, HttpResponse.BodyHandlers.ofString());

            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
                    .addPart("serverId", ApiConfig.PARAM_SERVER_ID)
                    .addPart("roleId", userInfo.getRoleId())
                    .addPart("gameId", ApiConfig.PARAM_GAME_ID)
                    .build();
            HttpRequest.Builder builder02 = ctx.getBuilder(ApiConfig.REFRESH_URL, body, userInfo);
            HttpRequest request02 = builder02.build();
            HttpResponse<String> response02 = ctx.httpClient().send(request02, HttpResponse.BodyHandlers.ofString());
            // 检查响应中是否包含 token 过期信息
            if (response01.body() != null && response01.body().contains("登录已过期")) {
                ctx.checkTokenExpired("登录已过期", userInfo);
            } else if (response02.body() != null && response02.body().contains("登录已过期")) {
                ctx.checkTokenExpired("登录已过期", userInfo);
            }
            return new ResponseBody<>(200, "角色数据刷新成功");
        } catch (IOException | InterruptedException | AccessTokenException e) {
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 5. UserDailyDataTask：用户日常数据 ====================

    /**
     * 获取体力、任务完成度等日常数据。
     *
     * @param userInfo 用户信息
     * @return 日常数据响应
     */
    public ResponseBody<RoleDailyData> getUserDailyData(UserInfo userInfo) {
        String url = String.format("%s?type=2&roleId=%s&sizeType=1&gameId=%s&serverId=%s",
                ApiConfig.DAILY_DATA_URL, userInfo.getRoleId(), ApiConfig.PARAM_GAME_ID, ApiConfig.PARAM_SERVER_ID);
        try {
            HttpRequest request = ctx.getBuilder(url, userInfo).build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                if (code == 220) throw new AccessTokenException();
                ResponseBody<RoleDailyData> responseBody = new ResponseBody<>();
                if (code == 200) {
                    RoleDailyData data = mapper.readValue(tree.get("data").toString(), RoleDailyData.class);
                    responseBody.setData(data);
                    responseBody.setSuccess(tree.get("success").asBoolean());
                }
                responseBody.setCode(code);
                responseBody.setMsg(tree.get("msg").asText());
                ctx.checkTokenExpired(responseBody.getMsg(), userInfo);
                return responseBody;
            } else {
                return new ResponseBody<>(1, "网络连接失败,异常状态码: " + response.statusCode());
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 9. PlayerBaseDataTask：玩家基础数据 ====================

    /**
     * 获取用户的游戏基础数据（体力、宝箱数量、周本活动等）。
     *
     * @param userInfo 用户信息
     * @return 基础数据响应
     */
    public ResponseBody<RoleInfo> getPlayerBaseData(UserInfo userInfo) {
        String url = ApiConfig.BASE_DATA_URL;
        try {
            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
                    .addPart("serverId", ApiConfig.PARAM_SERVER_ID)
                    .addPart("roleId", userInfo.getRoleId())
                    .addPart("gameId", ApiConfig.PARAM_GAME_ID)
                    .build();
            HttpRequest.Builder builder = ctx.getBuilder(url, body, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });
                ResponseBody<RoleInfo> responseBody = new ResponseBody<>(responseBodyForApi.getCode(), responseBodyForApi.getMsg(), responseBodyForApi.getSuccess());
                if (responseBody.getCode() == 200) {
                    String row = responseBodyForApi.getData();
                    // 个别用户会返回 null，初步判断可能需要刷新后才能获取
                    if (row == null || row.equals("null")) {
                        return new ResponseBody<>(1, "返回值为空，请先使用库街区APP查看鸣潮数据后再次尝试，后再联系开发者");
                    }
                    RoleInfo roleInfo = mapper.readValue(row, RoleInfo.class);
                    responseBody.setData(roleInfo);
                    return responseBody;
                } else {
                    if (responseBody.getCode() == 220) throw new AccessTokenException();
                    ctx.checkTokenExpired(responseBody.getMsg(), userInfo);
                    return new ResponseBody<>(1, responseBody.getMsg());
                }
            } else {
                return new ResponseBody<>(1, "连接出错");
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}
