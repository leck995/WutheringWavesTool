package com.kuro.kujiequ.thread.rolebox.tower;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.dao.GameNewTowerDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.newTowerData.NewTowerData;
import com.kuro.kujiequ.model.newTowerData.NewTowerModeDetail;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import com.kuro.util.HTTPRequestMultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Calendar;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 获取矩阵数据
 * @author: Leck
 * @create: 2024-10-15 22:19
 */
public class NewTowerDataDetailTask extends BaseTask<ResponseBody<NewTowerData>> {
    private static final Logger LOG = LoggerFactory.getLogger(NewTowerDataDetailTask.class);
    private final UserInfo userInfo;

    public NewTowerDataDetailTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<NewTowerData> call() throws Exception {
        return request();
    }

    private ResponseBody<NewTowerData> request() {
        String url = ApiConfig.SELF_NEW_TOWER_DATA_URL;
        try {
            HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
                    .addPart("serverId", ApiConfig.PARAM_SERVER_ID)
                    .addPart("roleId", userInfo.getRoleId())
                    .build();
            HttpRequest.Builder builder = getBuilder(url,body,userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug("矩阵：{}", response.body());
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });

                if (responseBodyForApi.getCode() == 200 || responseBodyForApi.getCode() == 10902) {
                    ResponseBody<NewTowerData> responseBody = new ResponseBody<>(200, responseBodyForApi.getMsg(), responseBodyForApi.getSuccess());
                    String row = responseBodyForApi.getData();
                    LOG.debug(row);
                    NewTowerData newTowerData = mapper.readValue(row, NewTowerData.class);
                    responseBody.setData(newTowerData);
                    if (newTowerData.isUnlock()){ //解锁才保存
                        saveToDB(newTowerData, mapper);
                    }
                    return responseBody;
                } else {
                    checkTokenExpired(responseBodyForApi.getMsg(), userInfo);
                    return new ResponseBody<>(1, responseBodyForApi.getMsg(), false);
                }
            } else {
                ResponseBody<NewTowerData> responseBody = new ResponseBody<>();
                LOG.error("网络请求失败，错误代码：{}", response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

    private void saveToDB(NewTowerData newTowerData, ObjectMapper mapper) throws JsonProcessingException {
        List<NewTowerModeDetail> list = newTowerData.getModeDetails().stream()
                .filter(d -> d.isHasRecord() && d.getScore() > 0)
                .toList();
        if (list.isEmpty())
            return;

        String json = mapper.writeValueAsString(list);
        long seasonEndTime = newTowerData.getEndTime();
        long date = convertToHourlyTimestamp(System.currentTimeMillis() + seasonEndTime);

        SlashDataForDB data = new SlashDataForDB();
        data.setRoleId(userInfo.getRoleId());
        data.setData(json);
        data.setEndTime(date);

        GameNewTowerDao dataDao = AppInjector.getInstance(GameNewTowerDao.class);
        dataDao.add(data);
    }

    /**
     * 将给定的时间戳转换成当天4点
     */
    private long convertToHourlyTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

}