package com.kuro.kujiequ.thread.rolebox.calculator;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import com.kuro.util.HttpRequestUtil;
import com.kuro.kujiequ.ApiConfig;
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
public class CalculatorDataRefreshTask extends BaseTask<ResponseBody<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(CalculatorDataRefreshTask.class);
    private UserInfo userInfo;

    public CalculatorDataRefreshTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<String> call() throws Exception {
        String url =String.format("%s?userId=%s&serverId=%s&roleId=%s",
                ApiConfig.CALCULATOR_LIST_ROLE,userInfo.getUserId(),ApiConfig.PARAM_SERVER_ID,userInfo.getRoleId());
        try {
            HttpRequest.Builder builder = getBuilder(url, null, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return ResponseBody.create(200,"刷新成功",null);
            } else {
                return new ResponseBody<>(1, "养成计算器刷新失败");
            }
        }catch (AccessTokenException | IOException | InterruptedException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}