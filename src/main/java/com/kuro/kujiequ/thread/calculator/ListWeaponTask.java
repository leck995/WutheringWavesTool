package com.kuro.kujiequ.thread.calculator;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.sign.SignUserInfo;
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
public class ListWeaponTask extends Task<ResponseBody<List<WeaponForCalculator>>> {
    private static final Logger LOG= LoggerFactory.getLogger(ListWeaponTask.class);
    private SignUserInfo signUserInfo;

    public ListWeaponTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<List<WeaponForCalculator>> call() throws Exception {
        return request(signUserInfo.getToken());
    }

    private ResponseBody<List<WeaponForCalculator>> request(String token){
        String url= ApiConfig.CALCULATOR_LIST_WEAPON;
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequestWithSource(url,token,"h5");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBody<List<WeaponForCalculator>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<WeaponForCalculator>>>() {
                });
                responseBody.getData().sort(Comparator.comparingInt(WeaponForCalculator::getPriority).reversed());
                return responseBody;
            }else {
                return new ResponseBody<>(1,"无法获取养成计算器的武器列表");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1,"无法获取养成计算器的武器列表");
        }
    }
}