package com.kuro.game.thread;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.game.ApiConfig;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.LauncherResource;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * @description:
 * @author: Leck
 * @create: 2025-08-28 17:38
 */
public class GameResourceListGetTask extends Task<ResponseBody<GameResourceList>> {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourceListGetTask.class);
    private String url;

    public GameResourceListGetTask(String url) {
        this.url = url;
    }

    @Override
    protected ResponseBody<GameResourceList> call() {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                //System.out.println(response.body());
                GameResourceList resourceList = mapper.readValue(response.body(), GameResourceList.class);
                return ResponseBody.create(200, "", resourceList);

            }else {
                return ResponseBody.create(-1, "获取下载文件清单失败", null);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            return ResponseBody.create(-1, "获取下载文件清单失败", null);
        }
    }
}