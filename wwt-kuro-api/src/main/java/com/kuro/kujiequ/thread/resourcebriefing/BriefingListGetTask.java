package com.kuro.kujiequ.thread.resourcebriefing;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.model.resourcebriefing.Briefing;
import com.kuro.kujiequ.model.resourcebriefing.Title;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;

/**
 * @description: 获取资源简报列表
 * @author: Leck
 * @create: 2025-06-14 10:48
 */
public class BriefingListGetTask extends BaseTask<ResponseBody<Briefing>> {
    private static final Logger LOG = LoggerFactory.getLogger(BriefingListGetTask.class);
    private final UserInfo userInfo;

    public BriefingListGetTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<Briefing> call(){
        URI uri = URI.create(ApiConfig.RESOURCE_BRIEFING_LIST);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("token", userInfo.getToken())
                .headers("source", userInfo.getIsWeb() ? "h5" : "android")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; 23116PN5BC Build/PQ3A.190605.02201427; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36 Kuro/2.5.0 KuroGameBox/2.5.0")
                .GET();
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                ResponseBody<Briefing> briefingResponseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<Briefing>>() {
                });

                if (briefingResponseBody.getCode() == 220){
                    throw new AccessTokenException();
                }
                boolean expired = checkResponseTokenExpired(briefingResponseBody, userInfo);
                if(expired){
                    throw new AccessTokenException();
                }


                if (briefingResponseBody.getCode() == 200 && briefingResponseBody.getData() != null) {
                    briefingResponseBody.getData().getMonths().sort(Comparator.comparingInt(Title::getIndex));
                    briefingResponseBody.getData().getVersions().sort(Comparator.comparingInt(Title::getIndex));
                    briefingResponseBody.getData().getWeeks().sort(Comparator.comparingInt(Title::getIndex));
                }
                return briefingResponseBody;
            } else {
                LOG.error("网络错误，状态码：{}", response.statusCode());
                return ResponseBody.create(response.statusCode(), "网络错误", null);
            }
        } catch (IOException | InterruptedException | AccessTokenException e) {
            LOG.error(e.getMessage());
            return ResponseBody.create(-1, e.getMessage(), null);
        }
    }
}