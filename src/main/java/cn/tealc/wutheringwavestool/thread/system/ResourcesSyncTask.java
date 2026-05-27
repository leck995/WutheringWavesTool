package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.netResource.Resource;
import cn.tealc.wutheringwavestool.model.netResource.RootResource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-06 17:10
 */
public class ResourcesSyncTask extends Task<String> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesSyncTask.class);
    private static final String ROOT_RESOURCE_URL_1 = "https://raw.githubusercontent.com/leck995/WutheringWavesToolResources/main-24.10.22/data/Root_%s.json";
    private static final String ROOT_RESOURCE_URL_2 = "https://gitee.com/tealc/WutheringWavesToolResources/raw/main-24.10.22/data/Root_%s.json";
    private static final String RESOURCE_TEMPLATE_1 = "https://raw.githubusercontent.com/leck995/WutheringWavesToolResources/main-24.10.22/%s";
    private static final String RESOURCE_TEMPLATE_2 = "https://gitee.com/tealc/WutheringWavesToolResources/raw/main-24.10.22/%s";


    private static final String LOCAL_ROOT_JSON = "assets/data/Root_%s.json";
    private final String url;
    private final String resource_template;
    private final ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);

    private int filesSize = 0;

    public ResourcesSyncTask() {
        Locale locale = Config.setting.getLanguage();
        String language = null;

        if (locale.getLanguage().equals("zh")) {
            if (locale.getCountry().equals("CN")) {
                language = locale.toString();
            } else {
                language = Locale.SIMPLIFIED_CHINESE.toString();
            }
        } else {
            language = Locale.ENGLISH.toString();
        }

        if (Config.setting.getResourceSource() == 1) {
            url = String.format(ROOT_RESOURCE_URL_2, language);
            resource_template = RESOURCE_TEMPLATE_2;
        } else {
            url = String.format(ROOT_RESOURCE_URL_1, language);
            resource_template = RESOURCE_TEMPLATE_1;
        }
    }


    @Override
    protected String call() {
        String row = readJsonFile(url);
        if (row == null) {
            updateMessage("error");
            return null;
        }
        try {
            RootResource remoteResource = mapper.readValue(row, RootResource.class);
            if (remoteResource != null) {
                File localFile = new File(String.format(LOCAL_ROOT_JSON, Config.setting.getLanguage()));
                if (localFile.exists()) {
                    RootResource localResources = mapper.readValue(localFile, RootResource.class);
                    if (localResources != null) {
                        if (localResources.getVersion().equals(remoteResource.getVersion())) {
                            LOG.debug("资源更新：远程仓库无更新");
                            return null;
                        } else {
                            updateMessage("start");
                            updateDateFile(remoteResource);
                            downloadFile(url, localFile.getPath());
                            updateMessage("success");
                        }
                    }
                } else {
                    updateMessage("start");
                    updateDateFile(remoteResource);
                    downloadFile(url, localFile.getPath());
                    updateMessage("success");
                }
            } else {
                LOG.warn("资源更新：获取资源文件更新失败");
            }
        } catch (IOException e) {
            LOG.error("资源更新：错误", e);
        }
        LOG.debug("资源更新：同步结束");
        return "资源同步完成";
    }







    private void updateDateFile(RootResource remoteResource) {
        filesSize = remoteResource.getResources().size();
        CountDownLatch latch = new CountDownLatch(remoteResource.getResources().size()); // 等待任务完成
        for (Resource resource : remoteResource.getResources().values()) {
            Thread.startVirtualThread(() -> {
                boolean same = checkLocalFile(resource);
                if (!same) {
                    LOG.info("资源更新：{}有新的更新内容，准备下载", resource.getName());
                    try {
                        String url = String.format(resource_template, resource.getFilePath());
                        String row = readJsonFile(url);
                        if (row != null) {
                            Map<String, Resource> map = mapper.readValue(row, new TypeReference<Map<String, Resource>>() {
                            });
                            for (Resource value : map.values()) {
                                boolean checked = checkLocalFile(value);
                                if (!checked) {
                                    LOG.debug("资源更新：{} 下的 {} 的MD5与本地不同", resource.getName(), value.getName());
                                    String fileUrl = String.format(resource_template, value.getFilePath());
                                    downloadFile(fileUrl, value.getAimPath());
                                    Thread.sleep(50);
                                }
                            }
                            String fileUrl = String.format(resource_template, resource.getFilePath());
                            LOG.debug("资源更新：子项更新完毕，开始更新JSON {}", resource.getName());
                            downloadFile(fileUrl, resource.getAimPath());
                        } else {
                            LOG.warn("资源更新：分类JSON {} 无法下载", resource.getName());
                        }
                    } catch (IOException | InterruptedException e) {
                        LOG.error("ERROR", e);
                    }
                }else {
                    LOG.info("资源更新：{}没有新的更新内容跳过",resource.getName());
                }
                latch.countDown();
            });
        }

        try {
            latch.await(); // 等待所有任务完成
        } catch (InterruptedException e) {
            LOG.error("资源更新：更新下载线程执行失败", e);
        }
    }


    /**
     * @return boolean
     * @description: 检查MD5是否一致
     * @param: resource
     * @date: 2024/10/6
     */
    private boolean checkLocalFile(Resource resource) {
        File local = new File(resource.getAimPath());
        if (local.exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(local)){
                String md5Hex = DigestUtils.md5Hex(fileInputStream);
                fileInputStream.close();
                return md5Hex.equals(resource.getMd5());
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }
        return false;
    }


    /**
     * @description: 下载指定url的文件
     * @param:	fileUrl	下载url
     * @param:	savePath	保存位置
     * @return  void
     * @date:   2025/1/3
     */
    public void downloadFile(String fileUrl, String savePath) {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0")
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            File outputFile = new File(savePath);
            File dir = outputFile.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            Files.copy(response.body(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String md5Hex = DigestUtils.md5Hex(new FileInputStream(outputFile));
            LOG.debug("资源更新：文件已下载并保存到:{},当前MD5：{}", savePath, md5Hex);
        } catch (IOException | InterruptedException e) {
            LOG.error("资源更新：", e);
        }
    }

    /**
     * description: 对于保存下载文件具体信息的json进行访问读取，转为String进行处理
     *
     * @param fileUrl 保存下载文件具体信息的json
     * @return
     */
    public String readJsonFile(String fileUrl) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .timeout(Duration.ofSeconds(3))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                return null;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("资源更新：读取网络JSON失败", e);
        }
        return null;
    }
}