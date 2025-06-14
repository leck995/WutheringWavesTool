package com.kuro.kujiequ.thread.rolebox.calculator;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import com.kuro.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.calculator.exist.RoleAim;
import com.kuro.kujiequ.model.calculator.result.CalculatorResult;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * @program: WutheringWavesTool
 * @description: 计算指定角色的练度
 * @author: Leck
 */
public class BatchRoleCostTask extends BaseTask<ResponseBody<CalculatorResult>> {
    private static final Logger LOG = LoggerFactory.getLogger(BatchRoleCostTask.class);
    private UserInfo userInfo;
    private RoleAim[] roles;

    public BatchRoleCostTask(UserInfo userInfo, RoleAim... roles) {
        this.userInfo = userInfo;
        this.roles = roles;
    }

    @Override
    protected ResponseBody<CalculatorResult> call() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = mapper.writeValueAsString(roles);
            content = URLEncoder.encode(content, StandardCharsets.UTF_8);
            String body = String.format("userId=%s&serverId=%s&roleId=%s&content=%s",
                    userInfo.getUserId(),
                    ApiConfig.PARAM_SERVER_ID,
                    userInfo.getRoleId(),
                    content);
            HttpRequest.Builder builder = getBuilder(ApiConfig.CALCULATOR_BATCH_ROLE_COST, body, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ResponseBody<CalculatorResult> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<CalculatorResult>>() {
                });
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法计算指定角色的练度");
            }
        }catch (AccessTokenException | IOException | InterruptedException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}
