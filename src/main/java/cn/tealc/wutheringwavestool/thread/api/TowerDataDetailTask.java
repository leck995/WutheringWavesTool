package cn.tealc.wutheringwavestool.thread.api;

import cn.tealc.wutheringwavestool.dao.GameTowerDataDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.RoleDetail;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.user.RoleDailyData;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Difficulty;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.DifficultyTotal;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Floor;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.TowerArea;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import cn.tealc.wutheringwavestool.util.ApiDecryptException;
import cn.tealc.wutheringwavestool.util.ApiUtil;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: WutheringWavesTool
 * @description: 获取深塔数据
 * @author: Leck
 * @create: 2024-10-15 22:19
 */
public class TowerDataDetailTask  extends Task<ResponseBody<DifficultyTotal>> {
    private static final Logger LOG= LoggerFactory.getLogger(TowerDataDetailTask.class);
    private final SignUserInfo signUserInfo;

    public TowerDataDetailTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<DifficultyTotal> call() throws Exception {
        return sign(signUserInfo.getRoleId(), signUserInfo.getToken());
    }

    private ResponseBody<DifficultyTotal> sign(String roleId,String token){
        String url=String.format("%s?roleId=%s&serverId=%s&gameId=%s"
                ,ApiConfig.SELF_TOWER_DATA_URL,roleId,ApiConfig.PARAM_SERVER_ID,ApiConfig.PARAM_GAME_ID);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBodyForApi responseBodyForApi = mapper.readValue(response.body(), new TypeReference<ResponseBodyForApi>() {
                });

                if (responseBodyForApi.getCode() == 200){
                    ResponseBody<DifficultyTotal> responseBody = new ResponseBody<>(responseBodyForApi.getCode(), responseBodyForApi.getMsg(),responseBodyForApi.getSuccess());
                    String row = ApiUtil.decrypt(responseBodyForApi.getData());
                    LOG.debug(row);
                    DifficultyTotal difficultyTotal = mapper.readValue(row, DifficultyTotal.class);
                    difficultyTotal.getDifficultyList().sort((o1, o2) -> {
                        if (o1.getDifficulty() == 3){
                            return -1;
                        }else {
                            return o2.getDifficulty() - o1.getDifficulty();
                        }
                    });
                    responseBody.setData(difficultyTotal);

                    /*把深渊数据保存到数据库中*/
                    GameTowerDataDao dataDao = new GameTowerDataDao();
                    Difficulty first = difficultyTotal.getDifficultyList().getFirst();
                    if (first.getDifficulty() == 3){
                        for (TowerArea towerArea : first.getTowerAreaList()) {
                            for (Floor floor : towerArea.getFloorList()) {
                                TowerData data =new TowerData();
                                data.setAreaId(towerArea.getAreaId());
                                data.setAreaName(towerArea.getAreaName());
                                data.setDifficulty(first.getDifficulty());
                                data.setDifficultyName(first.getDifficultyName());
                                data.setFloor(floor.getFloor());
                                data.setStar(floor.getStar());
                                data.setPicUrl(floor.getPicUrl());
                                if (floor.getRoleList() != null){
                                    String collect = floor.getRoleList().stream().map(role -> String.valueOf(role.getRoleId())).collect(Collectors.joining(","));
                                    data.setRoleList(collect);
                                }
                                long seasonEndTime = difficultyTotal.getSeasonEndTime();
                                long date = System.currentTimeMillis() + seasonEndTime;
                                data.setEndTime(convertToHourlyTimestamp(date));
                                dataDao.add(data);
                            }
                        }
                    }

                    return responseBody;
                }else {
                    return new ResponseBody<>(1,responseBodyForApi.getMsg(),false);
                }
            }else {
                ResponseBody<DifficultyTotal> responseBody = new ResponseBody<>();
                LOG.error("网络请求失败，错误代码：{}",response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("TowerDataDetailTask错误",e);
            return new ResponseBody<>(1,e.getMessage());
        } catch (ApiDecryptException e) {
            LOG.error("TowerDataDetailTask解析JSON错误",e);
            return new ResponseBody<>(1,e.getMessage());
        }
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