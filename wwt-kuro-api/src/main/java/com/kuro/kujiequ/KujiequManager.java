package com.kuro.kujiequ;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.model.calculator.exist.ExistedRoleDataForCalculator;
import com.kuro.kujiequ.model.calculator.exist.RoleAim;
import com.kuro.kujiequ.model.calculator.exist.WeaponAim;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.calculator.result.CalculatorResult;
import com.kuro.kujiequ.model.newTowerData.NewTowerData;
import com.kuro.kujiequ.model.resourcebriefing.Briefing;
import com.kuro.kujiequ.model.resourcebriefing.Record;
import com.kuro.kujiequ.model.resourcebriefing.Title;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.roleData.RoleDetail;
import com.kuro.kujiequ.model.roleData.user.RoleDailyData;
import com.kuro.kujiequ.model.roleData.user.RoleInfo;
import com.kuro.kujiequ.model.sign.SignGood;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.towerData.DifficultyTotal;
import com.kuro.model.ResponseBody;
import com.kuro.model.ResponseBodyForApi;
import com.kuro.util.HTTPRequestMultipartBody;
import com.kuro.util.HttpRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 库街区 API Facade：将原 19 个 JavaFX Task 的请求逻辑收敛为同步方法。
 * <p>
 * 本类为纯 API 模块，零 JavaFX 依赖。所有方法同步返回 {@link ResponseBody}，
 * 落库（saveToDB）与时间戳转换（convertToHourlyTimestamp）已上移到 app service 层。
 * <p>
 * 原 Task 对照：
 * <ul>
 *   <li>{@link #getGameRoleData}        ← GameRoleDataTask</li>
 *   <li>{@link #getGameRoleDetail}      ← GameRoleDetailTask</li>
 *   <li>{@link #seekGameRole}           ← GameRoleSeekTask</li>
 *   <li>{@link #refreshUserData}        ← UserDataRefreshTask</li>
 *   <li>{@link #getUserDailyData}       ← UserDailyDataTask</li>
 *   <li>{@link #getSignGoods}           ← SignGoodsTask</li>
 *   <li>{@link #getBriefingList}        ← BriefingListGetTask</li>
 *   <li>{@link #getBriefingDetail}      ← BriefingDetailGetTask</li>
 *   <li>{@link #getPlayerBaseData}      ← PlayerBaseDataTask</li>
 *   <li>{@link #getTowerData}           ← TowerDataDetailTask</li>
 *   <li>{@link #getNewTowerData}        ← NewTowerDataDetailTask</li>
 *   <li>{@link #getSlashData}           ← SlashDataDetailTask</li>
 *   <li>{@link #refreshCalculatorData}  ← CalculatorDataRefreshTask</li>
 *   <li>{@link #listCalculatorRole}     ← ListRoleTask</li>
 *   <li>{@link #listCalculatorWeapon}   ← ListWeaponTask</li>
 *   <li>{@link #queryOwnedRole}         ← QueryOwnedRoleTask</li>
 *   <li>{@link #getRoleCultivateStatus} ← RoleCultivateStatusTask</li>
 *   <li>{@link #batchRoleCost}          ← BatchRoleCostTask</li>
 *   <li>{@link #batchWeaponCost}        ← BatchWeaponCostTask</li>
 *   <li>{@link #sendSmsCode}            ← SendSmsTask</li>
 *   <li>{@link #loginUser}              ← LoginUserTask</li>
 * </ul>
 *
 * @author Leck
 */
public class KujiequManager {
    private static final Logger log = LoggerFactory.getLogger(KujiequManager.class);

    private final KujiequApiContext ctx;

    public KujiequManager(KujiequApiContext ctx) {
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

    // ==================== 6. SignGoodsTask：签到商品查询 ====================

    /**
     * 查询签到商品信息。原 SignGoodsTask 不继承 BaseTask，自建请求。
     * 由于本模块零 JavaFX 依赖，使用 {@link SignGoodsResult} 替代原 javafx.util.Pair。
     *
     * @param userInfo 用户信息
     * @return 签到商品响应（包含是否已签到与商品列表）
     */
    public ResponseBody<SignGoodsResult> getSignGoods(UserInfo userInfo) {
        String url = String.format("%s?gameId=%s&serverId=%s&roleId=%s&userId=%s",
                ApiConfig.SIGNIN_INIT_URL, ApiConfig.PARAM_GAME_ID, ApiConfig.PARAM_SERVER_ID,
                userInfo.getRoleId(), userInfo.getUserId());
        HttpClient client = ctx.httpClient();
        ResponseBody<SignGoodsResult> body = new ResponseBody<>();
        try {
            HttpRequest request = HttpRequestUtil.getRequestWithSource(url, userInfo.getToken(), userInfo.getIsWeb(), userInfo.getDevCode());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                if (code == 220) {
                    throw new AccessTokenException();
                } else if (code == 200) {
                    JsonNode data = tree.get("data");
                    JsonNode jsonNode = data.get("signInGoodsConfigs");
                    List<SignGood> signGoods = mapper.readValue(jsonNode.toString(), new TypeReference<List<SignGood>>() {
                    });
                    int num = data.get("sigInNum").asInt();
                    for (int i = 0; i < signGoods.size(); i++) {
                        signGoods.get(i).setSign(i < num);
                    }
                    Boolean isSign = data.get("isSigIn").asBoolean();
                    body.setData(new SignGoodsResult(isSign, signGoods));
                }
                body.setCode(code);
                body.setMsg(tree.get("msg").asText());
                return body;
            } else {
                body.setCode(1);
                body.setMsg("连接失败，错误状态码:" + response.statusCode());
                return body;
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error("错误:{}", e.getMessage());
            body.setCode(1);
            body.setMsg(e.getMessage());
            return body;
        }
    }

    /**
     * 签到商品查询结果，替代原 javafx.util.Pair&lt;Boolean, List&lt;SignGood&gt;&gt;。
     */
    public static final class SignGoodsResult {
        private final Boolean isSign;
        private final List<SignGood> signGoods;

        public SignGoodsResult(Boolean isSign, List<SignGood> signGoods) {
            this.isSign = isSign;
            this.signGoods = signGoods;
        }

        public Boolean getIsSign() {
            return isSign;
        }

        public List<SignGood> getSignGoods() {
            return signGoods;
        }
    }

    // ==================== 7. BriefingListGetTask：资源简报列表 ====================

    /**
     * 获取资源简报列表。原任务自建纯 GET 请求，不走 getBuilder。
     *
     * @param userInfo 用户信息
     * @return 简报列表响应
     */
    public ResponseBody<Briefing> getBriefingList(UserInfo userInfo) {
        URI uri = URI.create(ApiConfig.RESOURCE_BRIEFING_LIST);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("token", userInfo.getToken())
                .headers("source", userInfo.getIsWeb() ? "h5" : "android")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; 23116PN5BC Build/PQ3A.190605.02201427; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36 Kuro/2.5.0 KuroGameBox/2.5.0")
                .GET();
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBody<Briefing> briefingResponseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<Briefing>>() {
                });
                if (briefingResponseBody.getCode() == 220) {
                    throw new AccessTokenException();
                }
                boolean expired = ctx.checkResponseTokenExpired(briefingResponseBody, userInfo);
                if (expired) {
                    throw new AccessTokenException();
                }
                if (briefingResponseBody.getCode() == 200 && briefingResponseBody.getData() != null) {
                    briefingResponseBody.getData().getMonths().sort(Comparator.comparingInt(Title::getIndex));
                    briefingResponseBody.getData().getVersions().sort(Comparator.comparingInt(Title::getIndex));
                    briefingResponseBody.getData().getWeeks().sort(Comparator.comparingInt(Title::getIndex));
                }
                return briefingResponseBody;
            } else {
                log.error("网络错误，状态码：{}", response.statusCode());
                return ResponseBody.create(response.statusCode(), "网络错误", null);
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            log.error(e.getMessage());
            return ResponseBody.create(-1, e.getMessage(), null);
        }
    }

    // ==================== 8. BriefingDetailGetTask：资源简报详情 ====================

    /**
     * 获取资源简报详情。
     *
     * @param userInfo 用户信息
     * @param period   周期标识
     * @param type     简报类型（周/月/版本）
     * @return 简报详情响应
     */
    public ResponseBody<Record> getBriefingDetail(UserInfo userInfo, String period, BriefingType type) {
        String url;
        if (type == BriefingType.VERSION) {
            url = ApiConfig.RESOURCE_BRIEFING_VERSION;
        } else if (type == BriefingType.MONTH) {
            url = ApiConfig.RESOURCE_BRIEFING_MONTH;
        } else {
            url = ApiConfig.RESOURCE_BRIEFING_WEEK;
        }
        String body = String.format("period=%s&roleId=%s&serverId=%s", period, userInfo.getRoleId(), ApiConfig.PARAM_SERVER_ID);
        try {
            HttpRequest.Builder builder = ctx.getBuilder(url, body, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBody<Record> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<Record>>() {
                });
                if (responseBody.getCode() == 220) throw new AccessTokenException();
                ctx.checkResponseTokenExpired(responseBody, userInfo);
                return responseBody;
            } else {
                log.error("网络错误，状态码：{}", response.statusCode());
                return ResponseBody.create(response.statusCode(), "网络错误", null);
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return ResponseBody.create(-1, e.getMessage(), null);
        } catch (AccessTokenException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 资源简报类型枚举，对应原 BriefingDetailGetTask.Type。
     */
    public enum BriefingType {
        WEEK, MONTH, VERSION
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

    // ==================== 13. CalculatorDataRefreshTask：养成计算器刷新 ====================

    /**
     * 刷新养成计算器服务端缓存。
     *
     * @param userInfo 用户信息
     * @return 刷新结果响应
     */
    public ResponseBody<String> refreshCalculatorData(UserInfo userInfo) {
        String url = String.format("%s?userId=%s&serverId=%s&roleId=%s",
                ApiConfig.CALCULATOR_LIST_ROLE, userInfo.getUserId(), ApiConfig.PARAM_SERVER_ID, userInfo.getRoleId());
        try {
            HttpRequest.Builder builder = ctx.getBuilder(url, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return ResponseBody.create(200, "刷新成功", null);
            } else {
                return new ResponseBody<>(1, "养成计算器刷新失败");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            log.error("错误:{}", e.getMessage());
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 14. ListRoleTask：养成计算器角色列表 ====================

    /**
     * 获取养成计算器角色列表，按 priority 降序排序。
     *
     * @param userInfo 用户信息
     * @return 角色列表响应
     */
    public ResponseBody<List<RoleForCalculator>> listCalculatorRole(UserInfo userInfo) {
        String url = ApiConfig.CALCULATOR_LIST_ROLE;
        try {
            HttpRequest request = ctx.getBuilder(url, userInfo).build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBody<List<RoleForCalculator>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<RoleForCalculator>>>() {
                });
                if (responseBody.getCode() == 220) throw new AccessTokenException();
                ctx.checkResponseTokenExpired(responseBody, userInfo);
                if (responseBody.getData() != null) {
                    responseBody.getData().sort(Comparator.comparingInt(RoleForCalculator::getPriority).reversed());
                }
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法获取养成计算器的角色列表");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            log.error("错误:{}", e.getMessage());
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 15. ListWeaponTask：养成计算器武器列表 ====================

    /**
     * 获取养成计算器武器列表，按 priority 降序排序。
     *
     * @param userInfo 用户信息
     * @return 武器列表响应
     */
    public ResponseBody<List<WeaponForCalculator>> listCalculatorWeapon(UserInfo userInfo) {
        String url = ApiConfig.CALCULATOR_LIST_WEAPON;
        try {
            HttpRequest request = ctx.getBuilder(url, userInfo).build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBody<List<WeaponForCalculator>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<WeaponForCalculator>>>() {
                });
                if (responseBody.getCode() == 220) throw new AccessTokenException();
                ctx.checkResponseTokenExpired(responseBody, userInfo);
                if (responseBody.getData() != null) {
                    responseBody.getData().sort(Comparator.comparingInt(WeaponForCalculator::getPriority).reversed());
                }
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法获取养成计算器的武器列表");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            log.error("错误:{}", e.getMessage());
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 16. QueryOwnedRoleTask：查询已拥有角色 ID 列表 ====================

    /**
     * 获取指定玩家已拥有的角色 ID 列表。
     *
     * @param userInfo 用户信息
     * @return 已拥有角色 ID 列表响应
     */
    public ResponseBody<List<Integer>> queryOwnedRole(UserInfo userInfo) {
        String url = String.format("%s?userId=%s&serverId=%s&roleId=%s",
                ApiConfig.CALCULATOR_QUERY_OWNED_ROLE, userInfo.getUserId(), ApiConfig.PARAM_SERVER_ID, userInfo.getRoleId());
        try {
            HttpRequest request = ctx.getBuilder(url, userInfo).build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBody<List<Integer>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<Integer>>>() {
                });
                if (responseBody.getCode() == 220) throw new AccessTokenException();
                ctx.checkResponseTokenExpired(responseBody, userInfo);
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法获取指定玩家以存在的角色列表");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            log.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 17. RoleCultivateStatusTask：角色练度查询 ====================

    /**
     * 获取指定玩家指定角色的等级练度。
     *
     * @param userInfo 用户信息
     * @param ids      角色 ID 列表
     * @return 练度列表响应
     */
    public ResponseBody<List<ExistedRoleDataForCalculator>> getRoleCultivateStatus(UserInfo userInfo, Integer... ids) {
        String idsStr = Arrays.stream(ids)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String url = String.format("%s?userId=%s&serverId=%s&roleId=%s&ids=%s",
                ApiConfig.CALCULATOR_ROLE_CULTIVATE_STATUS,
                userInfo.getUserId(),
                ApiConfig.PARAM_SERVER_ID,
                userInfo.getRoleId(),
                idsStr);
        try {
            HttpRequest request = ctx.getBuilder(url, userInfo).build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                ResponseBody<List<ExistedRoleDataForCalculator>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<ExistedRoleDataForCalculator>>>() {
                });
                if (responseBody.getCode() == 220) throw new AccessTokenException();
                ctx.checkResponseTokenExpired(responseBody, userInfo);
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法获取指定玩家指定角色的等级练度");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            log.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 18. BatchRoleCostTask：批量计算角色练度 ====================

    /**
     * 计算指定角色的养成消耗。
     *
     * @param userInfo 用户信息
     * @param roles    目标角色配置
     * @return 计算结果响应
     */
    public ResponseBody<CalculatorResult> batchRoleCost(UserInfo userInfo, RoleAim... roles) {
        ObjectMapper mapper = ctx.objectMapper();
        try {
            String content = mapper.writeValueAsString(roles);
            content = URLEncoder.encode(content, StandardCharsets.UTF_8);
            String body = String.format("userId=%s&serverId=%s&roleId=%s&content=%s",
                    userInfo.getUserId(),
                    ApiConfig.PARAM_SERVER_ID,
                    userInfo.getRoleId(),
                    content);
            HttpRequest.Builder builder = ctx.getBuilder(ApiConfig.CALCULATOR_BATCH_ROLE_COST, body, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ResponseBody<CalculatorResult> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<CalculatorResult>>() {
                });
                if (responseBody.getCode() == 220) throw new AccessTokenException();
                ctx.checkResponseTokenExpired(responseBody, userInfo);
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法计算指定角色的练度");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            log.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 19. BatchWeaponCostTask：批量计算武器练度 ====================

    /**
     * 计算指定武器的养成消耗。
     *
     * @param userInfo 用户信息
     * @param weapons  目标武器配置
     * @return 计算结果响应
     */
    public ResponseBody<CalculatorResult> batchWeaponCost(UserInfo userInfo, WeaponAim... weapons) {
        ObjectMapper mapper = ctx.objectMapper();
        try {
            String content = mapper.writeValueAsString(weapons);
            content = URLEncoder.encode(content, StandardCharsets.UTF_8);
            String body = String.format("userId=%s&serverId=%s&roleId=%s&content=%s",
                    userInfo.getUserId(),
                    ApiConfig.PARAM_SERVER_ID,
                    userInfo.getRoleId(),
                    content);
            HttpRequest.Builder builder = ctx.getBuilder(ApiConfig.CALCULATOR_BATCH_WEAPON_COST, body, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ResponseBody<CalculatorResult> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<CalculatorResult>>() {
                });
                if (responseBody.getCode() == 220) throw new AccessTokenException();
                ctx.checkResponseTokenExpired(responseBody, userInfo);
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法计算指定武器的练度");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            log.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    // ==================== 20. SendSmsTask：发送短信验证码 ====================

    /**
     * 发送库街区短信验证码（H5 接口 + 极验 geeTestData）。
     * 原 SendSmsTask 自建 static HttpClient 与 ObjectMapper，本实现改用 ctx 提供的实例。
     *
     * @param phone        手机号
     * @param geeTestJson  极验 geeTestData JSON
     * @return 发送结果响应
     */
    public ResponseBody<Boolean> sendSmsCode(String phone, String geeTestJson) {
        String devCode = randomDevCode();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("mobile", phone);
        form.put("geeTestData", geeTestJson == null ? "" : geeTestJson);

        HttpRequest request = HttpRequest.newBuilder(URI.create(ApiConfig.GET_SMS_CODE_H5))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", CONTENT_TYPE)
                .header("User-Agent", UA)
                .header("source", "h5")
                .header("devcode", devCode)
                .header("version", VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(toForm(form)))
                .build();

        try {
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body() == null ? "" : response.body();
            log.debug("getSmsCodeForH5 status={} body={}", response.statusCode(), body);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResponseBody.create(response.statusCode(), "连接失败，响应状态码:" + response.statusCode(), false);
            }
            if (body.isBlank()) {
                return ResponseBody.create(-1, "服务器返回空响应", false);
            }

            ObjectMapper mapper = ctx.objectMapper();
            JsonNode root = mapper.readTree(body);
            boolean success = root.path("success").asBoolean(false)
                    || root.path("code").asInt(-1) == 0
                    || root.path("code").asInt(-1) == 200;
            String msg = root.path("msg").asText(success ? "验证码发送成功" : "请求失败");
            if (success) {
                return ResponseBody.create(200, msg, true);
            }
            int code = root.path("code").asInt(-1);
            return ResponseBody.create(code == 0 ? -1 : code, msg, false);
        } catch (IOException | InterruptedException e) {
            log.error("发送短信验证码错误", e);
            return ResponseBody.create(-1, e.getMessage(), false);
        }
    }

    private static final String CONTENT_TYPE =
            "application/x-www-form-urlencoded; charset=UTF-8";
    private static final String UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) KuroGameBox/3.1.3";
    private static final String VERSION = "3.1.3";

    /**
     * 生成 32 位随机 devCode（原 SendSmsTask.randomDevCode）。
     */
    public static String randomDevCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(32);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 校验中国大陆手机号格式（原 SendSmsTask.isValidCnMobile）。
     */
    public static boolean isValidCnMobile(String mobile) {
        return mobile != null && mobile.matches("^1[3-9]\\d{9}$");
    }

    private static String toForm(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    // ==================== 21. LoginUserTask：登录 ====================

    /**
     * 库街区登录（手机号 + 短信验证码）。
     * 原 LoginUserTask 不走 getBuilder，自建请求。
     *
     * @param phone  手机号
     * @param code   短信验证码
     * @param isWeb  是否为 H5 来源
     * @return 登录用户信息响应
     */
    public ResponseBody<UserInfo> loginUser(String phone, String code, boolean isWeb) {
        String devCode = ctx.getDevCode();
        String did = UUID.randomUUID().toString().replace("-", "");
        log.debug("登录 did: {}", did);
        String url = String.format("%s?code=%s&mobile=%s&devCode=%s", ApiConfig.ACCOUNT_LOGIN, code, phone, did);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("source", isWeb ? "h5" : "android")
                    .header("devcode", devCode)
                    .header("did", did)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = ctx.httpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = ctx.objectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int respCode = tree.get("code").asInt();
                if (respCode == 200) {
                    JsonNode dataNode = tree.get("data");
                    if (!dataNode.isEmpty()) {
                        String token = dataNode.path("token").asText();
                        UserInfo userInfo = new UserInfo();
                        userInfo.setToken(token);
                        userInfo.setDevCode(did);
                        return ResponseBody.create(200, "success", userInfo);
                    } else {
                        return new ResponseBody<>(1, "登录库街区失败");
                    }
                } else {
                    log.error("服务器返回异常，错误代码：{}", respCode);
                    JsonNode node = tree.get("msg");
                    return new ResponseBody<>(respCode, node.asText());
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

    // ==================== token 管理方法（委托给 ctx） ====================

    /**
     * 主动预取 B-At token 并缓存。
     *
     * @param userInfo 用户信息
     * @return true 表示预取成功
     */
    public boolean prefetchAccessToken(UserInfo userInfo) {
        return ctx.prefetchAccessToken(userInfo);
    }

    /**
     * 检查指定用户的 B-At 是否已缓存且未过期。
     *
     * @param userId 用户 ID
     * @return true 表示缓存有效
     */
    public boolean isAccessTokenCached(String userId) {
        return ctx.isAccessTokenCached(userId);
    }

    /**
     * 使指定用户的 B-At 缓存失效。
     *
     * @param userId 用户 ID
     */
    public void invalidateAccessToken(String userId) {
        ctx.invalidateAccessToken(userId);
    }

    /**
     * 使所有用户的 B-At 缓存失效。
     */
    public void invalidateAllAccessTokens() {
        ctx.invalidateAllAccessTokens();
    }

    /**
     * 通知 token 过期（触发上层回调）。
     *
     * @param userInfo 用户信息
     * @param msg      过期提示信息
     */
    public void notifyTokenExpired(UserInfo userInfo, String msg) {
        ctx.notifyTokenExpired(userInfo, msg);
    }
}
