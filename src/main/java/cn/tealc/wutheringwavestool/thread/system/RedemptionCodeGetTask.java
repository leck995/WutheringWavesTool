package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.RedemptionCodeItem;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class RedemptionCodeGetTask extends Task<ResponseBody<Map<String, List<RedemptionCodeItem>>>> {
    private static final Logger LOG = LoggerFactory.getLogger(RedemptionCodeGetTask.class);

    @Override
    protected ResponseBody<Map<String, List<RedemptionCodeItem>>> call() throws Exception {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConstants.URL_REDEMPTION_CODES))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                return mapper.readValue(response.body(),
                        new TypeReference<ResponseBody<Map<String, List<RedemptionCodeItem>>>>() {});
            } else {
                LOG.error("获取兑换码失败，状态码: {}", response.statusCode());
                return new ResponseBody<>(-1, "获取兑换码失败");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("获取兑换码出现异常", e);
            return new ResponseBody<>(-1, "获取兑换码失败，请检查网络状况");
        }
    }
}
