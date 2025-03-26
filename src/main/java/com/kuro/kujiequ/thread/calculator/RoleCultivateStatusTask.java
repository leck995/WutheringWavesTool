package com.kuro.kujiequ.thread.calculator;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.calculator.exist.ExistedRoleDataForCalculator;
import com.kuro.kujiequ.model.sign.SignUserInfo;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: WutheringWavesTool
 * @description: 获取指定玩家指定角色的等级练度
 * @author: Leck
 */
public class RoleCultivateStatusTask extends Task<ResponseBody<List<ExistedRoleDataForCalculator>>> {
    private static final Logger LOG= LoggerFactory.getLogger(RoleCultivateStatusTask.class);
    private SignUserInfo signUserInfo;

    private String ids;
    public RoleCultivateStatusTask(SignUserInfo signUserInfo,Integer... ids) {
        this.signUserInfo = signUserInfo;
        this.ids = Arrays.stream(ids)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Override
    protected ResponseBody<List<ExistedRoleDataForCalculator>> call() throws Exception {
        String url =String.format("%s?userId=%s&serverId=%s&roleId=%s&ids=%s",
                ApiConfig.CALCULATOR_ROLE_CULTIVATE_STATUS,
                signUserInfo.getUserId(),
                ApiConfig.PARAM_SERVER_ID,
                signUserInfo.getRoleId(),
                ids);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,signUserInfo.getToken());
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBody<List<ExistedRoleDataForCalculator>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<ExistedRoleDataForCalculator>>>() {
                });
                return responseBody;
            }else {
                return new ResponseBody<>(1,"无法获取指定玩家指定角色的等级练度");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1,"无法获取指定玩家指定角色的等级练度");
        }
    }

}