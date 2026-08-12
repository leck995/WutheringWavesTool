package com.kuro.kujiequ.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.KujiequApiContext;
import com.kuro.kujiequ.model.newTowerData.NewTowerData;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.towerData.DifficultyTotal;
import com.kuro.model.ResponseBody;
import com.kuro.model.ResponseBodyForApi;
import com.kuro.util.HTTPRequestMultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 库街区塔/矩阵/海墟数据 API：深塔、矩阵（新塔）、海墟。
 * <p>
 * 本类为纯 API 模块，零 JavaFX 依赖。所有方法同步返回 {@link ResponseBody}，
 * 方法体原样搬迁自原 KujiequManager，逻辑无改动。
 *
 * @author Leck
 */
public class KujiequTowerApi {
    private static final Logger log = LoggerFactory.getLogger(KujiequTowerApi.class);

    private final KujiequApiContext ctx;

    public KujiequTowerApi(KujiequApiContext ctx) {
        this.ctx = ctx;
    }

    // ==================== 10. TowerDataDetailTask：获取深塔数据 ====================

    /**
     * 获取深塔数据。原 saveToDB 与 convertToHourlyTimestamp 已删除（上移到 app service）。
     * 难度列表排序：difficulty==3 在前，其余按降序。
     *
     * @param userInfo 用户信息
     * @return 深塔数据响应
     */
    public ResponseBody<DifficultyTotal> getTowerData(UserInfo userInfo) {
        String url = ApiConfig.SELF_TOWER_DATA_URL;
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
                if (responseBodyForApi.getCode() == 200 || responseBodyForApi.getCode() == 10902) {
                    ResponseBody<DifficultyTotal> responseBody = new ResponseBody<>(200, responseBodyForApi.getMsg(), responseBodyForApi.getSuccess());
                    String row = responseBodyForApi.getData();
                    DifficultyTotal difficultyTotal = mapper.readValue(row, DifficultyTotal.class);
                    difficultyTotal.getDifficultyList().sort((o1, o2) -> {
                        if (o1.getDifficulty() == 3) {
                            return -1;
                        } else {
                            return o2.getDifficulty() - o1.getDifficulty();
                        }
                    });
                    responseBody.setData(difficultyTotal);
                    // saveToDB 已删除，落库上移到 app service
                    return responseBody;
                } else {
                    if (responseBodyForApi.getCode() == 220) throw new AccessTokenException();
                    ctx.checkTokenExpired(responseBodyForApi.getMsg(), userInfo);
                    return new ResponseBody<>(1, responseBodyForApi.getMsg(), false);
                }
            } else {
                ResponseBody<DifficultyTotal> responseBody = new ResponseBody<>();
                log.error("网络请求失败，错误代码：{}", response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error("错误:{}", e.getMessage());
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 11. NewTowerDataDetailTask：获取矩阵数据 ====================

    /**
     * 获取矩阵（新塔）数据。原 saveToDB 与 convertToHourlyTimestamp 已删除。
     *
     * @param userInfo 用户信息
     * @return 矩阵数据响应
     */
    public ResponseBody<NewTowerData> getNewTowerData(UserInfo userInfo) {
        String url = ApiConfig.SELF_NEW_TOWER_DATA_URL;
        try {
            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
                    .addPart("serverId", ApiConfig.PARAM_SERVER_ID)
                    .addPart("roleId", userInfo.getRoleId())
                    .build();
            HttpRequest.Builder builder = ctx.getBuilder(url, body, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.debug("矩阵：{}", response.body());
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });
                if (responseBodyForApi.getCode() == 200 || responseBodyForApi.getCode() == 10902) {
                    ResponseBody<NewTowerData> responseBody = new ResponseBody<>(200, responseBodyForApi.getMsg(), responseBodyForApi.getSuccess());
                    String row = responseBodyForApi.getData();
                    log.debug(row);
                    NewTowerData newTowerData = mapper.readValue(row, NewTowerData.class);
                    responseBody.setData(newTowerData);
                    // 原 saveToDB 已删除，落库上移到 app service
                    return responseBody;
                } else {
                    if (responseBodyForApi.getCode() == 220) throw new AccessTokenException();
                    ctx.checkTokenExpired(responseBodyForApi.getMsg(), userInfo);
                    return new ResponseBody<>(1, responseBodyForApi.getMsg(), false);
                }
            } else {
                ResponseBody<NewTowerData> responseBody = new ResponseBody<>();
                log.error("网络请求失败，错误代码：{}", response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error("错误:{}", e.getMessage());
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 12. SlashDataDetailTask：获取海墟数据 ====================

    /**
     * 获取海墟数据。原 saveToDB 与 convertToHourlyTimestamp 已删除。
     * URL 带 query 参数，走 getBuilderWithoutToken。
     *
     * @param userInfo 用户信息
     * @return 海墟数据响应
     */
    public ResponseBody<SlashData> getSlashData(UserInfo userInfo) {
        String url = String.format("%s?gameId=3&serverId=76402e5b20be2c39f095a152090afddc&roleId=%s",
                ApiConfig.SELF_SLASH_DATA_URL, userInfo.getRoleId());
        try {
            HttpRequest.Builder builder = ctx.getBuilderWithoutToken(url, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });
                if (responseBodyForApi.getCode() == 200 || responseBodyForApi.getCode() == 10902) {
                    ResponseBody<SlashData> responseBody = new ResponseBody<>(200, responseBodyForApi.getMsg(), responseBodyForApi.getSuccess());
                    String row = responseBodyForApi.getData();
                    SlashData slashData = mapper.readValue(row, SlashData.class);
                    responseBody.setData(slashData);
                    // saveToDB 已删除，落库上移到 app service
                    return responseBody;
                } else {
                    if (responseBodyForApi.getCode() == 220) throw new AccessTokenException();
                    ctx.checkTokenExpired(responseBodyForApi.getMsg(), userInfo);
                    return new ResponseBody<>(1, responseBodyForApi.getMsg(), false);
                }
            } else {
                ResponseBody<SlashData> responseBody = new ResponseBody<>();
                log.error("网络请求失败，错误代码：{}", response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error("错误:{}", e.getMessage());
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}
