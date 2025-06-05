package com.kuro.kujiequ.thread.calculator;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.calculator.exist.RoleAim;
import com.kuro.kujiequ.model.calculator.exist.WeaponAim;
import com.kuro.kujiequ.model.calculator.result.CalculatorResult;
import com.kuro.kujiequ.model.calculator.result.Cost;
import com.kuro.kujiequ.model.sign.SignUserInfo;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;

/**
 * @program: WutheringWavesTool
 * @description: 计算指定武器的练度
 * @author: Leck
 */
public class BatchWeaponCostTask extends Task<ResponseBody<CalculatorResult>> {
    private static final Logger LOG = LoggerFactory.getLogger(BatchWeaponCostTask.class);
    private SignUserInfo signUserInfo;

    private WeaponAim[] weapon;

    public BatchWeaponCostTask(SignUserInfo signUserInfo, WeaponAim... weapon) {
        this.signUserInfo = signUserInfo;
        this.weapon = weapon;
    }

    @Override
    protected ResponseBody<CalculatorResult> call(){
        ObjectMapper mapper = new ObjectMapper();
        HttpClient client = HttpClient.newHttpClient();
        try {
            String content = mapper.writeValueAsString(weapon);
            content = URLEncoder.encode(content, StandardCharsets.UTF_8);
            String body = String.format("userId=%s&serverId=%s&roleId=%s&content=%s",
                    signUserInfo.getUserId(),
                    ApiConfig.PARAM_SERVER_ID,
                    signUserInfo.getRoleId(),
                    content);

            HttpRequest request = HttpRequestUtil.getRequestWithSource(ApiConfig.CALCULATOR_BATCH_WEAPON_COST, body,signUserInfo.getToken(),"h5");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ResponseBody<CalculatorResult> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<CalculatorResult>>() {
                });
                return responseBody;
            } else {
                return new ResponseBody<>(1, "无法计算指定武器的练度");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, "无法计算指定武器的练度");
        }
    }

}