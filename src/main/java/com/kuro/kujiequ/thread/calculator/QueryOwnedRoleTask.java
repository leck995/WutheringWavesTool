package com.kuro.kujiequ.thread.calculator;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.sign.SignUserInfo;
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
public class QueryOwnedRoleTask extends Task<ResponseBody<List<Integer>>> {
    private static final Logger LOG= LoggerFactory.getLogger(QueryOwnedRoleTask.class);
    private SignUserInfo signUserInfo;

    public QueryOwnedRoleTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<List<Integer>> call() throws Exception {
        String url =String.format("%s?userId=%s&serverId=%s&roleId=%s",
                ApiConfig.CALCULATOR_QUERY_OWNED_ROLE,signUserInfo.getUserId(),ApiConfig.PARAM_SERVER_ID,signUserInfo.getRoleId());
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,signUserInfo.getToken());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBody<List<Integer>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<Integer>>>() {
                });
                return responseBody;
            }else {
                return new ResponseBody<>(1,"无法获取指定玩家以存在的角色列表");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1,"无法获取指定玩家以存在的角色列表");
        }
    }

}