package cn.tealc.wwt.game.resource.internal;

import cn.tealc.wwt.game.resource.GameDownloadResponse;
import cn.tealc.wwt.game.resource.GameDownloadSource;
import cn.tealc.wwt.game.resource.model.game.GameResourceList;
import cn.tealc.wwt.game.resource.model.launcher.CdnData;
import cn.tealc.wwt.game.resource.model.launcher.UpdateData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.zip.GZIPInputStream;

/** Internal client for the official game-resource endpoints. */
public final class GameResourceApiClient {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0";
    private static final String INDEX_CN = "https://prod-cn-alicdn-gamestarter.kurogame.com/launcher/game/"
            + "G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json";
    private static final String INDEX_GLOBAL = "https://prod-alicdn-gamestarter.kurogame.com/launcher/game/"
            + "G153/50004_obOHXFrFanqsaIEOmuKroCcbZkQRBC7c/index.json";
    private static final String INDEX_BILIBILI = "https://prod-cn-alicdn-gamestarter.kurogame.com/launcher/game/"
            + "G152/10004_j5GWFuUFlb8N31Wi2uS3ZAVHcb7ZGN7y/index.json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GameResourceApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public GameDownloadResponse<UpdateData> getLatestUpdate(GameDownloadSource source) {
        GameDownloadResponse<LauncherPayload> response = getJson(indexUrl(source), LauncherPayload.class);
        if (!response.isSuccessful() || response.getData().updateData == null) {
            return GameDownloadResponse.failure(response.isSuccessful()
                    ? "游戏更新配置为空" : response.getMessage());
        }
        return GameDownloadResponse.success(response.getData().updateData);
    }

    public GameDownloadResponse<GameResourceList> getResourceList(UpdateData updateData) {
        String url = resourceListUrl(updateData);
        if (url == null) {
            return GameDownloadResponse.failure("下载配置缺少资源清单地址");
        }
        return getJson(url, GameResourceList.class);
    }

    private <T> GameDownloadResponse<T> getJson(String url, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return GameDownloadResponse.failure("请求游戏资源失败，HTTP " + response.statusCode());
            }
            byte[] payload = response.body();
            boolean gzip = response.headers().firstValue("Content-Encoding")
                    .map(value -> value.equalsIgnoreCase("gzip")).orElse(false);
            try {
                return GameDownloadResponse.success(objectMapper.readValue(
                        gzip ? decompress(payload) : payload, responseType));
            } catch (IOException parseError) {
                if (gzip) {
                    throw parseError;
                }
                // Some CDN nodes omit Content-Encoding despite returning gzip data.
                return GameDownloadResponse.success(objectMapper.readValue(
                        decompress(payload), responseType));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return GameDownloadResponse.failure("请求游戏资源被中断");
        } catch (IOException | IllegalArgumentException e) {
            return GameDownloadResponse.failure("请求游戏资源失败: " + e.getMessage());
        }
    }

    private static byte[] decompress(byte[] payload) throws IOException {
        try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(payload))) {
            return input.readAllBytes();
        }
    }

    private static String indexUrl(GameDownloadSource source) {
        return switch (source) {
            case BILIBILI -> INDEX_BILIBILI;
            case GLOBAL -> INDEX_GLOBAL;
            case MAINLAND -> INDEX_CN;
        };
    }

    private static String resourceListUrl(UpdateData updateData) {
        if (updateData == null || !hasText(updateData.getResources())) {
            return null;
        }
        List<CdnData> cdns = updateData.getCdnList();
        if (cdns == null) {
            return null;
        }
        return cdns.stream()
                .map(CdnData::getUrl)
                .filter(GameResourceApiClient::hasText)
                .findFirst()
                .map(cdn -> appendPath(cdn, updateData.getResources()))
                .orElse(null);
    }

    private static String appendPath(String base, String path) {
        String normalizedBase = base.strip();
        String normalizedPath = path.strip();
        while (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return normalizedBase + "/" + normalizedPath;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class LauncherPayload {
        @JsonProperty("default")
        private UpdateData updateData;
    }
}
