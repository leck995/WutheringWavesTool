package cn.tealc.wutheringwavestool.thread.gacha.cloud;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class GachaCloudDownloadTask extends Task<ResponseBody<Void>> {
    private static final Logger LOG = LoggerFactory.getLogger(GachaCloudDownloadTask.class);

    private final String downloadUrl;
    private final String playerId;
    private final String username;
    private final String password;

    public GachaCloudDownloadTask(String downloadUrl, String playerId, String username, String password) {
        this.downloadUrl = downloadUrl;
        this.playerId = playerId;
        this.username = username;
        this.password = password;
        updateTitle("云下载: " + playerId);
    }

    @Override
    protected ResponseBody<Void> call() {
        Path targetDir = Path.of("data", playerId);
        Path poolFile = targetDir.resolve("pool.json");
        Path bakFile = targetDir.resolve("pool.json.bak");

        try {
            Files.createDirectories(targetDir);

            boolean hasBackup = false;
            if (Files.exists(poolFile)) {
                Files.move(poolFile, bakFile, StandardCopyOption.REPLACE_EXISTING);
                hasBackup = true;
            }

            try {
                HttpClient client = AppInjector.getInstance(HttpClient.class);
                String fullUrl = AppConstants.URL_HOST_SERVER + downloadUrl + "?username=" + username + "&password=" + password;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode());
                }

                try (InputStream is = response.body()) {
                    Files.copy(is, poolFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (hasBackup) {
                    Files.deleteIfExists(bakFile);
                }

                LOG.info("下载成功: {} -> {}", downloadUrl, poolFile);
                return new ResponseBody<>(200, "下载成功", true);
            } catch (Exception e) {
                LOG.error("下载失败", e);
                if (hasBackup && Files.exists(bakFile)) {
                    Files.move(bakFile, poolFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return new ResponseBody<>(-1, "下载失败: " + e.getMessage());
            }
        } catch (IOException e) {
            LOG.error("文件操作异常", e);
            return new ResponseBody<>(-1, "文件操作失败: " + e.getMessage());
        }
    }
}
