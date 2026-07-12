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
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 刷新服务器缓存，获取实时数据
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class ListWeaponTask extends BaseTask<ResponseBody<List<WeaponForCalculator>>> {
    private static final Logger LOG= LoggerFactory.getLogger(ListWeaponTask.class);
    private UserInfo userInfo;

    public ListWeaponTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<List<WeaponForCalculator>> call() throws Exception {
        return request();
    }

    private ResponseBody<List<WeaponForCalculator>> request(){
        String url= ApiConfig.CALCULATOR_LIST_WEAPON;
        try {
            HttpRequest request = getBuilder(url,userInfo).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                ResponseBody<List<WeaponForCalculator>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<WeaponForCalculator>>>() {
                });
                if (responseBody.getCode() == 220) throw new AccessTokenException();
                checkResponseTokenExpired(responseBody, userInfo);
                if (responseBody.getData() != null) {
                    responseBody.getData().sort(Comparator.comparingInt(WeaponForCalculator::getPriority).reversed());
                }
                return responseBody;
            }else {
                return new ResponseBody<>(1,"无法获取养成计算器的武器列表");
            }
        } catch (AccessTokenException | IOException | InterruptedException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}