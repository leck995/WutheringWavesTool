package com.kuro.game.thread;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.game.ApiConfig;
import com.kuro.game.model.launcher.LauncherResource;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.zip.GZIPInputStream;

/**
 * @description: 获启动器的Json文件数据
 * @author: Leck
 * @create: 2025-02-10 17:22
 */
public class LauncherResourceTask extends Task<ResponseBody<LauncherResource>> {
    private static final Logger LOG = LoggerFactory.getLogger(LauncherResourceTask.class);

    public enum Type{
        BILIBILI,
        CN,
        GLOBAL
    }

    private final Type type;

    public LauncherResourceTask(Type type) {
        this.type = type;
    }

    @Override
    protected ResponseBody<LauncherResource> call() {
        HttpClient client = HttpClient.newHttpClient();
        String url;
        switch (type) {
            case BILIBILI -> url =ApiConfig.INDEX_BILIBILI;
            case GLOBAL -> url =ApiConfig.INDEX_GLOBAL;
            default -> url =ApiConfig.INDEX_CN;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                .header("Accept", "*/*")
                .header("accept-encoding", "gzip")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                try (GZIPInputStream gzipInputStream = new GZIPInputStream(response.body());
                     BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream))) {

                    ObjectMapper mapper = new ObjectMapper();
                    LauncherResource launcherResource = mapper.readValue(reader, LauncherResource.class);
                    System.out.println(response.statusCode());
                    System.out.println(launcherResource.getUpdateData().getVersion());
                    return ResponseBody.create(200, "", launcherResource);
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                    return ResponseBody.create(-1, "获取最新版本信息失败", null);
                }
            }else {
                return ResponseBody.create(-1, "获取最新版本信息失败", null);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            return ResponseBody.create(-1, "获取最新版本信息失败", null);
        }
    }
}