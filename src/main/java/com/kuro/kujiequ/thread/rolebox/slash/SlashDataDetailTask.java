package com.kuro.kujiequ.thread.rolebox.slash;

import cn.tealc.wutheringwavestool.dao.GameSlashDataDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.slash.SlashDifficulty;
import com.kuro.kujiequ.thread.BaseTask;
import com.kuro.util.HttpRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Calendar;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 获取海墟数据
 * @author: Leck
 * @create: 2024-10-15 22:19
 */
public class SlashDataDetailTask extends BaseTask<ResponseBody<SlashData>> {
    private static final Logger LOG= LoggerFactory.getLogger(SlashDataDetailTask.class);
    private final UserInfo userInfo;

    public SlashDataDetailTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<SlashData> call() throws Exception {
        String url=String.format("%s?roleId=%s&serverId=%s&userId=%s"
                , ApiConfig.SELF_SLASH_DATA_URL,userInfo.getRoleId(),ApiConfig.PARAM_SERVER_ID,userInfo.getUserId());
        try {
            HttpRequest.Builder builder = getBuilder(url,null,userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug("海墟：{}",response.body());
                ObjectMapper mapper = new ObjectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });
                if (responseBodyForApi.getCode() == 200  || responseBodyForApi.getCode() == 10902){
                    ResponseBody<SlashData> responseBody = new ResponseBody<>(200, responseBodyForApi.getMsg(),responseBodyForApi.getSuccess());
                    String row = responseBodyForApi.getData();
                    SlashData slashData = mapper.readValue(row, SlashData.class);
                    responseBody.setData(slashData);
                    saveToDB(slashData,mapper);
                    return responseBody;
                }else {
                    return new ResponseBody<>(1,responseBodyForApi.getMsg(),false);
                }
            }else {
                ResponseBody<SlashData> responseBody = new ResponseBody<>();
                LOG.error("网络请求失败，错误代码：{}",response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1,e.getMessage());
        }
    }




    /**
     * 保存到数据库
     * @description:
     * @param:	slashData
     * @param:	mapper
     * @return  void
     * @date:   2025/5/29
     */
    private void saveToDB(SlashData slashData,ObjectMapper mapper ) throws JsonProcessingException {
        //过滤一次性的关卡数据
        List<SlashDifficulty> list = slashData.getDifficultyList().stream().filter(slashDifficulty -> slashDifficulty.getDifficulty() != 0).toList();
        if (list.isEmpty())
            return;
        String json = mapper.writeValueAsString(list);
        long seasonEndTime = slashData.getSeasonEndTime();
        long date = convertToHourlyTimestamp(System.currentTimeMillis() + seasonEndTime);

        SlashDataForDB data = new SlashDataForDB();
        data.setData(json);
        data.setEndTime(date);

        GameSlashDataDao dataDao = new GameSlashDataDao();
        dataDao.add(data);
    }

    /**
     * @description: 将给定的时间戳转换成当天4点
     * @param:	timestamp
     * @return  long
     * @date:   2024/10/16
     */
    public long convertToHourlyTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 4); // 这里设置为4表示早上4点
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

}