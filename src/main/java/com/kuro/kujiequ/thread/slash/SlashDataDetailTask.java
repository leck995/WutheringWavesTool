package com.kuro.kujiequ.thread.slash;

import cn.tealc.wutheringwavestool.dao.GameSlashDataDao;
import cn.tealc.wutheringwavestool.dao.GameTowerDataDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.ApiDecryptException;
import com.kuro.kujiequ.ApiUtil;
import com.kuro.kujiequ.model.sign.SignUserInfo;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.slash.SlashDifficulty;
import com.kuro.kujiequ.model.towerData.Difficulty;
import com.kuro.kujiequ.model.towerData.DifficultyTotal;
import com.kuro.kujiequ.model.towerData.Floor;
import com.kuro.kujiequ.model.towerData.TowerArea;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: WutheringWavesTool
 * @description: 获取海墟数据
 * @author: Leck
 * @create: 2024-10-15 22:19
 */
public class SlashDataDetailTask extends Task<ResponseBody<SlashData>> {
    private static final Logger LOG= LoggerFactory.getLogger(SlashDataDetailTask.class);
    private final SignUserInfo signUserInfo;

    public SlashDataDetailTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<SlashData> call() throws Exception {
        return get(signUserInfo.getUserId(),signUserInfo.getRoleId(), signUserInfo.getToken());
    }

    private ResponseBody<SlashData> get(String userId,String roleId,String token){
        String url=String.format("%s?roleId=%s&serverId=%s&userId=%s"
                , ApiConfig.SELF_SLASH_DATA_URL,roleId,ApiConfig.PARAM_SERVER_ID,userId);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });

                if (responseBodyForApi.getCode() == 200){
                    ResponseBody<SlashData> responseBody = new ResponseBody<>(responseBodyForApi.getCode(), responseBodyForApi.getMsg(),responseBodyForApi.getSuccess());
                    String row = responseBodyForApi.getData();
                    LOG.debug(row);
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
        } catch (IOException | InterruptedException e) {
            LOG.error("TowerDataDetailTask错误",e);
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