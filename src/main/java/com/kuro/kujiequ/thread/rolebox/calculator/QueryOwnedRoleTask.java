package com.kuro.kujiequ.thread.rolebox.calculator;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import com.kuro.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 获取指定玩家以存在的角色列表
 * @author: Leck
 */
public class QueryOwnedRoleTask extends BaseTask<ResponseBody<List<Integer>>> {
    private static final Logger LOG= LoggerFactory.getLogger(QueryOwnedRoleTask.class);
    private UserInfo userInfo;

    public QueryOwnedRoleTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<List<Integer>> call() throws Exception {
        String url =String.format("%s?userId=%s&serverId=%s&roleId=%s",
                ApiConfig.CALCULATOR_QUERY_OWNED_ROLE,userInfo.getUserId(),ApiConfig.PARAM_SERVER_ID,userInfo.getRoleId());
        try {
            HttpRequest request = getBuilder(url,userInfo).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                ResponseBody<List<Integer>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<Integer>>>() {
                });
                return responseBody;
            }else {
                return new ResponseBody<>(1,"无法获取指定玩家以存在的角色列表");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

}