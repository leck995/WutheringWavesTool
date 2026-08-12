package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.netResource.Resource;
import cn.tealc.wutheringwavestool.model.netResource.RootResource;
import cn.tealc.wutheringwavestool.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 从远程仓库同步资源文件到本地，支持版本比对、MD5校验及失败自动重试
 */
public class ResourcesSyncTask extends Task<String> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesSyncTask.class);

    private static final String ROOT_URL_1 = "https://raw.githubusercontent.com/leck995/WutheringWavesToolResources/main/data/Root_%s.json";
    private static final String ROOT_URL_2 = "https://gitee.com/tealc/WutheringWavesToolResources/raw/main/data/Root_%s.json";
    private static final String RESOURCE_TPL_1 = "https://raw.githubusercontent.com/leck995/WutheringWavesToolResources/main/%s";
    private static final String RESOURCE_TPL_2 = "https://gitee.com/tealc/WutheringWavesToolResources/raw/main/%s";
    private static final String LOCAL_ROOT = "assets/data/Root_%s.json";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0";
    private static final int MAX_CONCURRENT = 6;
    private static final int DOWNLOAD_DELAY_MS = 200;
    private static final int REQUEST_TIMEOUT_S = 3;
    private static final String KEY_SYNC_SUCCESS = "RESOURCE_SYNC_SUCCESS";

    private final String rootUrl;
    private final String resourceTpl;
    private final ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
    private final ConfigService configService = AppInjector.getInstance(ConfigService.class);
    private final Set<String> failedFiles = ConcurrentHashMap.newKeySet();
    private final AtomicInteger downloadedCount = new AtomicInteger(0);

    private int totalFiles;

    public ResourcesSyncTask() {
        String language = resolveLanguage();
        boolean gitee = Config.setting().getResourceSource() == 1;
        rootUrl = String.format(gitee ? ROOT_URL_2 : ROOT_URL_1, language);
        resourceTpl = gitee ? RESOURCE_TPL_2 : RESOURCE_TPL_1;
    }

    /** 解析当前语言设置，返回 zh_CN / en 等语言标签 */
    private static String resolveLanguage() {
        Locale locale = Config.setting().getLanguage();
        if (!locale.getLanguage().equals("zh")) {
            return Locale.ENGLISH.toString();
        }
        return locale.getCountry().equals("CN")
                ? locale.toString()
                : Locale.SIMPLIFIED_CHINESE.toString();
    }

    @Override
    protected String call() {
        boolean forceVerify = configService.getBoolean(KEY_SYNC_SUCCESS)
                .map(v -> !v)
                .orElse(false);
        if (forceVerify) {
            LOG.info("资源同步 - 上次存在失败文件，强制重新校验");
        }

        String row = readJsonFile(rootUrl);
        if (row == null) {
            updateMessage("error");
            return null;
        }

        try {
            RootResource remote = mapper.readValue(row, RootResource.class);
            if (remote == null || remote.getResources() == null) {
                LOG.warn("资源同步 - 远程资源数据为空");
                return null;
            }

            File localFile = new File(String.format(LOCAL_ROOT, Config.setting().getLanguage()));
            RootResource local = readLocalRoot(localFile);

            if (!forceVerify && local != null && local.getVersion().equals(remote.getVersion())) {
                LOG.debug("资源同步 - 版本一致，无需更新: {}", remote.getVersion());
                return null;
            }

            LOG.info("资源同步 - 开始同步, 远程版本: {}, 本地版本: {}",
                    remote.getVersion(), local != null ? local.getVersion() : "无");
            updateMessage("start");
            updateTitle("资源同步");
            downloadedCount.set(0);

            syncFiles(remote.getResources());

            boolean allSuccess = failedFiles.isEmpty();
            configService.set(KEY_SYNC_SUCCESS, allSuccess);
            if (!allSuccess) {
                LOG.warn("资源同步 - 部分文件失败 ({}/{}): {}", failedFiles.size(), totalFiles, failedFiles);
            }

            downloadFile(rootUrl, localFile.getPath());
            updateMessage("success");
        } catch (IOException e) {
            LOG.error("资源同步 - 解析失败", e);
            updateMessage("资源同步失败: " + e.getMessage());
        }
        LOG.debug("资源同步 - 结束");
        return "资源同步完成";
    }

    /** 并发下载所有资源文件，每个文件下载前校验MD5，下载后再次校验 */
    private void syncFiles(Map<String, List<Resource>> resourcesMap) {
        List<Resource> all = resourcesMap.values().stream()
                .flatMap(List::stream)
                .toList();
        totalFiles = all.size();
        updateProgress(0, totalFiles);

        Semaphore semaphore = new Semaphore(MAX_CONCURRENT);
        CountDownLatch latch = new CountDownLatch(all.size());
        for (Resource resource : all) {
            Thread.startVirtualThread(() -> {
                try {
                    semaphore.acquire();
                    try {
                        downloadResourceIfNeeded(resource);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.error("资源同步 - 下载线程被中断", e);
            Thread.currentThread().interrupt();
        }
    }

    /** 检查并下载单个资源文件 */
    private void downloadResourceIfNeeded(Resource resource) {
        if (checkLocalFile(resource)) {
            return;
        }
        try {
            Thread.sleep(DOWNLOAD_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        LOG.debug("资源同步 - 开始下载: {}", resource.getName());
        String fileUrl = String.format(resourceTpl, resource.getFilePath());
        if (downloadFile(fileUrl, resource.getAimPath())) {
            if (!checkLocalFile(resource)) {
                LOG.warn("资源同步 - MD5校验失败: {}", resource.getName());
                failedFiles.add(resource.getAimPath());
            }
        } else {
            LOG.warn("资源同步 - 下载失败: {}", resource.getName());
            failedFiles.add(resource.getAimPath());
        }
    }

    /** 校验本地文件MD5是否与资源清单一致 */
    private boolean checkLocalFile(Resource resource) {
        File file = new File(resource.getAimPath());
        if (!file.exists()) {
            return false;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fis).equals(resource.getMd5());
        } catch (IOException e) {
            LOG.debug("资源同步 - MD5校验异常: {}", e.getMessage());
            return false;
        }
    }

    /** 构建通用HTTP GET请求 */
    private static HttpRequest buildRequest(String fileUrl) {
        return HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_S))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
    }

    /** 下载文件到本地，JSON文件自动Base64解码 */
    public boolean downloadFile(String fileUrl, String savePath) {
        try {
            HttpResponse<InputStream> response = AppInjector.getInstance(HttpClient.class)
                    .send(buildRequest(fileUrl), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                LOG.warn("资源同步 - HTTP {}: {}", response.statusCode(), fileUrl);
                return false;
            }

            byte[] body;
            try (InputStream is = response.body()) {
                body = is.readAllBytes();
            }

            File outputFile = new File(savePath);
            File parent = outputFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }

            byte[] data = savePath.toLowerCase().endsWith(".json")
                    ? decodeBase64(body)
                    : body;
            Files.write(outputFile.toPath(), data,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            int count = downloadedCount.incrementAndGet();
            updateProgress(count, totalFiles);
            updateMessage("已下载 " + count + " 个文件: " + outputFile.getName());
            return true;
        } catch (IOException | InterruptedException e) {
            LOG.error("资源同步 - 下载失败: {} -> {}", fileUrl, savePath, e);
            return false;
        }
    }

    /** Base64解码字节数组 */
    private static byte[] decodeBase64(byte[] body) {
        return Base64.decodeBase64(new String(body, StandardCharsets.UTF_8).trim());
    }

    /** 读取本地缓存的根资源清单 */
    private RootResource readLocalRoot(File file) {
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        try {
            return mapper.readValue(file, RootResource.class);
        } catch (IOException e) {
            LOG.warn("资源同步 - 本地缓存损坏: {}", e.getMessage());
            return null;
        }
    }

    /** 从远程URL读取并Base64解码JSON内容 */
    public String readJsonFile(String fileUrl) {
        try {
            HttpResponse<String> response = AppInjector.getInstance(HttpClient.class)
                    .send(buildRequest(fileUrl), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body().trim();
                byte[] decoded = Base64.decodeBase64(body);
                return decoded != null ? new String(decoded, StandardCharsets.UTF_8).trim() : body;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("资源同步 - 请求失败: {}", fileUrl, e);
        }
        return null;
    }
}
