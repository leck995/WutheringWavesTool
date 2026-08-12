package com.kuro.kujiequ.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.KujiequApiContext;
import com.kuro.kujiequ.model.resourcebriefing.Briefing;
import com.kuro.kujiequ.model.resourcebriefing.Record;
import com.kuro.kujiequ.model.resourcebriefing.Title;
import com.kuro.kujiequ.model.sign.SignGood;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import com.kuro.util.HttpRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;

/**
 * 库街区签到/资源简报 API：签到商品查询、资源简报列表、资源简报详情。
 * <p>
 * 本类为纯 API 模块，零 JavaFX 依赖。所有方法同步返回 {@link ResponseBody}，
 * 方法体原样搬迁自原 KujiequManager，逻辑无改动。
 * <p>
 * 内部类 {@link SignGoodsResult} 与枚举 {@link BriefingType} 也定义在本类中。
 *
 * @author Leck
 */
public class KujiequSignApi {
    private static final Logger log = LoggerFactory.getLogger(KujiequSignApi.class);

    private final KujiequApiContext ctx;

    public KujiequSignApi(KujiequApiContext ctx) {
        this.ctx = ctx;
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
}
