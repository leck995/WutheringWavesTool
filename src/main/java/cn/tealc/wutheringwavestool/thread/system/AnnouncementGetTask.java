package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.AnnouncementItem;
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

public class AnnouncementGetTask extends Task<ResponseBody<List<AnnouncementItem>>> {
    private static final Logger LOG = LoggerFactory.getLogger(AnnouncementGetTask.class);

    private final String gameId;

    public AnnouncementGetTask(String gameId) {
        this.gameId = gameId;
    }

    @Override
    protected ResponseBody<List<AnnouncementItem>> call() throws Exception {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        try {
            String url = AppConstants.URL_ANNOUNCEMENTS + "/" + gameId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                return mapper.readValue(response.body(),
                        new TypeReference<ResponseBody<List<AnnouncementItem>>>() {});
            } else {
                LOG.error("获取公告失败，状态码: {}", response.statusCode());
                return new ResponseBody<>(-1, "获取公告失败");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("获取公告出现异常", e);
            return new ResponseBody<>(-1, "获取公告失败");
        }
    }
}
