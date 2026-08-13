package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.SignHistoryDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.SignRecord;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.util.HttpRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Singleton
public class SignService {
    private static final Logger LOG = LoggerFactory.getLogger(SignService.class);
    private final UserInfoDao userInfoDao;
    private final SignHistoryDao signHistoryDao;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public SignService(UserInfoDao userInfoDao, SignHistoryDao signHistoryDao,
                       HttpClient httpClient, ObjectMapper objectMapper) {
        this.userInfoDao = userInfoDao;
        this.signHistoryDao = signHistoryDao;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /** 为所有用户执行签到，返回结果摘要 */
    public String signAll() {
        List<UserInfo> users = userInfoDao.getAll();
        StringBuilder sb = new StringBuilder();
        int maxUsers = Math.min(users.size(), 9);
        for (int i = 0; i < maxUsers; i++) {
            UserInfo user = users.get(i);
            if (isSignedToday(user.getLastSignTime())) {
                sb.append(String.format("当前用户ID:%s,签到状态：已完成签到", user.getUserId()));
            } else {
                String result = sign(user);
                sb.append(String.format("当前用户ID:%s,签到状态：%s", user.getUserId(), result));
                sb.append("\n");
            }
        }
        sb.append("===签到完成===");
        return sb.toString();
    }

    private String sign(UserInfo userInfo) {
        LocalDate today = LocalDate.now();
        String url = String.format("%s?gameId=%s&serverId=%s&roleId=%s&userId=%s&reqMonth=%02d",
                ApiConfig.SIGNIN_URL, ApiConfig.PARAM_GAME_ID, ApiConfig.PARAM_SERVER_ID,
                userInfo.getRoleId(), userInfo.getUserId(), today.getMonthValue());
        try {
            HttpRequest request = HttpRequestUtil.getRequestWithSource(
                    url, userInfo.getToken(), userInfo.getIsWeb(), userInfo.getDevCode());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (response.body().contains("请求成功")) {
                    userInfoDao.updateLastSignTime(System.currentTimeMillis(), userInfo.getId());
                    updateSignHistory(userInfo);
                    return "签到成功";
                } else if (response.body().contains("请勿重复签到")) {
                    userInfoDao.updateLastSignTime(System.currentTimeMillis(), userInfo.getId());
                    updateSignHistory(userInfo);
                    return "请勿重复签到";
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
        return "签到失败";
    }

    /** 从 API 拉取签到历史并保存 */
    public void updateSignHistory(UserInfo userInfo) {
        String url = String.format("%s?gameId=%s&serverId=%s&roleId=%s&userId=%s",
                ApiConfig.SIGNIN_QUERY_URL, ApiConfig.PARAM_GAME_ID, ApiConfig.PARAM_SERVER_ID,
                userInfo.getRoleId(), userInfo.getUserId());
        try {
            HttpRequest request = HttpRequestUtil.getRequestWithSource(
                    url, userInfo.getToken(), userInfo.getIsWeb(), userInfo.getDevCode());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ResponseBody<List<SignRecord>> responseBody = objectMapper.readValue(
                        response.body(), new TypeReference<ResponseBody<List<SignRecord>>>() {});
                if (responseBody.getSuccess()) {
                    for (SignRecord record : responseBody.getData()) {
                        record.setRoleId(userInfo.getRoleId());
                        record.setUserId(userInfo.getUserId());
                        signHistoryDao.addHistory(record);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("获取签到历史失败", e);
        }
    }

    /** 获取指定角色的签到历史记录 */
    public List<SignRecord> getHistoriesByRoleId(String roleId) {
        return signHistoryDao.getHistoriesByRoleId(roleId);
    }

    public static boolean isSignedToday(Long lastSignTimeMillis) {
        if (lastSignTimeMillis == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate signDate = Instant.ofEpochMilli(lastSignTimeMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return today.isEqual(signDate);
    }
}
