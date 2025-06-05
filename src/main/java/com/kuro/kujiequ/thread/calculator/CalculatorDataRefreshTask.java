package com.kuro.kujiequ.thread.calculator;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.sign.SignUserInfo;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @program: WutheringWavesTool
 * @description: 刷新服务器缓存，获取实时数据
 * @author: Leck
 */
public class CalculatorDataRefreshTask extends Task<ResponseBody<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(CalculatorDataRefreshTask.class);
    private SignUserInfo signUserInfo;

    public CalculatorDataRefreshTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<String> call() throws Exception {
        String url =String.format("%s?userId=%s&serverId=%s&roleId=%s",
                ApiConfig.CALCULATOR_LIST_ROLE,signUserInfo.getUserId(),ApiConfig.PARAM_SERVER_ID,signUserInfo.getRoleId());
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url, signUserInfo.getToken(),"h5");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return ResponseBody.create(200,"刷新成功",null);
            } else {
                return new ResponseBody<>(1, "养成计算器刷新失败");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, "养成计算器刷新失败");
        }
    }


}