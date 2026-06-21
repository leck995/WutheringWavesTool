package com.kuro.kujiequ.thread.resourcebriefing;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.kuro.kujiequ.model.resourcebriefing.Record;

/**
 * @description: 获取资源简报列表
 * @author: Leck
 * @create: 2025-06-14 10:48
 */
public class BriefingDetailGetTask extends BaseTask<ResponseBody<Record>> {
    private static final Logger LOG = LoggerFactory.getLogger(BriefingDetailGetTask.class);
    private final UserInfo userInfo;
    private final Type type;
    private final String period;
    public BriefingDetailGetTask(UserInfo userInfo,String period,Type type) {
        this.userInfo = userInfo;
        this.type = type;
        this.period = period;
    }

    @Override
    protected ResponseBody<Record> call(){
        String url;
        if (type == Type.VERSION){
            url = ApiConfig.RESOURCE_BRIEFING_VERSION;
        }else if (type == Type.MONTH){
            url = ApiConfig.RESOURCE_BRIEFING_MONTH;
        }else {
            url = ApiConfig.RESOURCE_BRIEFING_WEEK;
        }
        String body = String.format("period=%s&roleId=%s&serverId=%s", period, userInfo.getRoleId(), ApiConfig.PARAM_SERVER_ID);
        try {
            HttpRequest.Builder builder = getBuilder(url, body,userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug(response.body());
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                ResponseBody<Record> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<Record>>() {
                });
                checkResponseTokenExpired(responseBody, userInfo);
                return responseBody;
            }else {
                LOG.error("网络错误，状态码：{}",response.statusCode());
                return ResponseBody.create(response.statusCode(),"网络错误",null);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            return ResponseBody.create(-1,e.getMessage(),null);
        } catch (AccessTokenException e) {
            throw new RuntimeException(e);
        }


    }
    
    public enum Type{
        WEEK,MONTH,VERSION
    }
}