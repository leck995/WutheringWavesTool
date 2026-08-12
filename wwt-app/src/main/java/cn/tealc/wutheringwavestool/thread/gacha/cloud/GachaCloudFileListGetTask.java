package cn.tealc.wutheringwavestool.thread.gacha.cloud;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.CloudFileListData;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GachaCloudFileListGetTask extends Task<ResponseBody<CloudFileListData>> {
    private static final Logger LOG = LoggerFactory.getLogger(GachaCloudFileListGetTask.class);

    private final String username;
    private final String password;

    public GachaCloudFileListGetTask(String username, String password) {
        this.username = username;
        this.password = password;
        updateTitle("获取云文件列表");
    }

    @Override
    protected ResponseBody<CloudFileListData> call() throws Exception {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);

        String query = "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConstants.URL_GACHA_FILE_LIST + query))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        if (responseBody == null || responseBody.isEmpty()) {
            LOG.error("获取云文件列表响应为空, 状态码: {}", response.statusCode());
            return new ResponseBody<>(-1, "获取文件列表失败，服务器无响应");
        }
        return mapper.readValue(responseBody,
                new TypeReference<ResponseBody<CloudFileListData>>() {});
    }
}
