package com.kuro.kujiequ.thread.base.sign;

import cn.tealc.wutheringwavestool.dao.SignHistoryDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.SignRecord;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
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

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class SignTask extends Task<String> {
    private static final Logger LOG = LoggerFactory.getLogger(SignTask.class);
    private final SignHistoryDao historyDao = new SignHistoryDao();

    @Override
    protected String call() throws Exception {
        UserInfoDao dao = new UserInfoDao();
        List<UserInfo> userInfos = dao.getAll();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userInfos.size(); i++) {
            UserInfo user = userInfos.get(i);
            if (i < 9) {
                Long lastSignTime = user.getLastSignTime();
                boolean sameDay = lastSignTime != null && isSameDay(lastSignTime);
                if (sameDay) {
                    LOG.debug("与上次签到同一天，跳过签到");
                    sb.append(String.format("当前用户ID:%s,签到状态：已完成签到", user.getUserId()));
                } else {
                    long lastTime = System.currentTimeMillis();
                    String sign = sign(user);
                    if (!sign.equals("签到失败")) {
                        dao.updateLastSignTime(lastTime,user.getId());
                    }
                    sb.append(String.format("当前用户ID:%s,签到状态：%s", user.getUserId(), sign));
                    sb.append("\n");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                sb.append(String.format("当前用户ID:%s,签到状态：签到账号数量超过最大限制", user.getUserId()));
            }
        }
        sb.append("===签到完成===");
        return sb.toString();
    }

    private String sign(UserInfo userInfo) {
        LocalDate today = LocalDate.now();
        int monthValue = today.getMonthValue();
        String url = String.format("%s?gameId=%s&serverId=%s&roleId=%s&userId=%s&reqMonth=%02d"
                , ApiConfig.SIGNIN_URL, ApiConfig.PARAM_GAME_ID, ApiConfig.PARAM_SERVER_ID, userInfo.getRoleId(), userInfo.getUserId(), monthValue);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequestWithSource(url, userInfo.getToken(), userInfo.getIsWeb(), userInfo.getDevCode());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug("签到完成：{}", response.body());
                if (response.body().contains("请求成功")) {
                    updateHistory(userInfo);//获取更新签到记录
                    return "签到成功";
                } else if (response.body().contains("请勿重复签到")) {
                    updateHistory(userInfo);//获取更新签到记录
                    return "请勿重复签到";
                } else {
                    return "签到失败";
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage(),e);
        }
        return "签到失败";
    }

    private void updateHistory(UserInfo userInfo) {
        String url = String.format("%s?gameId=%s&serverId=%s&roleId=%s&userId=%s"
                , ApiConfig.SIGNIN_QUERY_URL, ApiConfig.PARAM_GAME_ID, ApiConfig.PARAM_SERVER_ID, userInfo.getRoleId(), userInfo.getUserId());
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequestWithSource(url, userInfo.getToken(), userInfo.getIsWeb(), userInfo.getDevCode());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug("连接成功，添加签到历史记录");
                ObjectMapper mapper = new ObjectMapper();
                ResponseBody<List<SignRecord>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<SignRecord>>>() {
                });
                if (responseBody.getSuccess()) {
                    for (SignRecord record : responseBody.getData()) {
                        record.setRoleId(userInfo.getRoleId());
                        record.setUserId(userInfo.getUserId());
                        historyDao.addHistory(record);
                    }
                }
            } else {
                LOG.info("连接失败");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("JSON异常", e);
        }
    }

    /**
     * 检查给定的时间戳（毫秒）是否与当前时间在同一天（使用系统默认时区）。
     *
     * @param timestampMillis 要检查的时间戳（毫秒）
     * @return 如果在同一天则返回 true，否则返回 false
     */
    public static boolean isSameDay(long timestampMillis) {
        // 1. 获取当前日期（使用系统默认时区）
        LocalDate today = LocalDate.now();
        // 2. 将时间戳转换为 Instant
        Instant instant = Instant.ofEpochMilli(timestampMillis);
        // 3. 将 Instant 转换为带有系统默认时区的 LocalDate
        // 这一步是关键，它将时间戳转换成了特定时区下的“日历日期”
        LocalDate dateFromTimestamp = instant.atZone(ZoneId.systemDefault())
                .toLocalDate();
        // 4. 比较两个 LocalDate 对象
        return today.isEqual(dateFromTimestamp);
    }

}