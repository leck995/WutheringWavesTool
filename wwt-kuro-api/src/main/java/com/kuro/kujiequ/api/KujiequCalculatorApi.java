package com.kuro.kujiequ.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.KujiequApiContext;
import com.kuro.kujiequ.model.calculator.exist.ExistedRoleDataForCalculator;
import com.kuro.kujiequ.model.calculator.exist.RoleAim;
import com.kuro.kujiequ.model.calculator.exist.WeaponAim;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.calculator.result.CalculatorResult;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库街区养成计算器 API：刷新、角色列表、武器列表、已拥有角色、练度查询、批量消耗计算。
 * <p>
 * 本类为纯 API 模块，零 JavaFX 依赖。所有方法同步返回 {@link ResponseBody}，
 * 方法体原样搬迁自原 KujiequManager，逻辑无改动。
 *
 * @author Leck
 */
public class KujiequCalculatorApi {
    private static final Logger log = LoggerFactory.getLogger(KujiequCalculatorApi.class);

    private final KujiequApiContext ctx;

    public KujiequCalculatorApi(KujiequApiContext ctx) {
        this.ctx = ctx;
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
}
