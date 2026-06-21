package com.kuro.kujiequ.thread.rolebox.calculator;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.calculator.exist.ExistedRoleDataForCalculator;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import com.kuro.util.HttpRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: WutheringWavesTool
 * @description: 获取指定玩家指定角色的等级练度
 * @author: Leck
 */
public class RoleCultivateStatusTask extends BaseTask<ResponseBody<List<ExistedRoleDataForCalculator>>> {
    private static final Logger LOG = LoggerFactory.getLogger(RoleCultivateStatusTask.class);
    private UserInfo userInfo;

    private String ids;

    public RoleCultivateStatusTask(UserInfo userInfo, Integer... ids) {
        this.userInfo = userInfo;
        this.ids = Arrays.stream(ids)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Override
    protected ResponseBody<List<ExistedRoleDataForCalculator>> call() throws Exception {
        String url = String.format("%s?userId=%s&serverId=%s&roleId=%s&ids=%s",
                ApiConfig.CALCULATOR_ROLE_CULTIVATE_STATUS,
                userInfo.getUserId(),
                ApiConfig.PARAM_SERVER_ID,
                userInfo.getRoleId(),
                ids);
        try {
            HttpRequest request = getBuilder(url,userInfo).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                ResponseBody<List<ExistedRoleDataForCalculator>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<ExistedRoleDataForCalculator>>>() {
                });
                checkResponseTokenExpired(responseBody, userInfo);
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法获取指定玩家指定角色的等级练度");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }

}