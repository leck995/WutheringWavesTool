package com.kuro.kujiequ.thread.calculator;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.ResponseBodyForApi;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.result.Cost;
import com.kuro.kujiequ.model.sign.SignUserInfo;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 刷新服务器缓存，获取实时数据
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class ListRoleTask extends Task<ResponseBody<List<RoleForCalculator>>> {
    private static final Logger LOG= LoggerFactory.getLogger(ListRoleTask.class);
    private SignUserInfo signUserInfo;

    public ListRoleTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<List<RoleForCalculator>> call() throws Exception {
        return request(signUserInfo.getToken());
    }

    private ResponseBody<List<RoleForCalculator>> request(String token){
        String url= ApiConfig.CALCULATOR_LIST_ROLE;
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBody<List<RoleForCalculator>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<RoleForCalculator>>>() {
                });
                responseBody.getData().sort(Comparator.comparingInt(RoleForCalculator::getPriority).reversed());
                return responseBody;
            }else {
                return new ResponseBody<>(1,"无法获取养成计算器的角色列表");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1,"无法获取养成计算器的角色列表");
        }
    }
}