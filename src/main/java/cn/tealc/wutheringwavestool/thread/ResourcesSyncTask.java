package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.netResource.Resource;
import cn.tealc.wutheringwavestool.model.netResource.RootResource;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-06 17:10
 */
public class ResourcesSyncTask extends Task<String> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesSyncTask.class);
    private static final String ROOT_RESOURCE_URL_1="https://raw.githubusercontent.com/leck995/WutheringWavesToolResources/main/data/Root.json";
    private static final String ROOT_RESOURCE_URL_2="https://gitee.com/tealc/WutheringWavesToolResources/raw/main/data/Root.json";
    private static final String RESOURCE_TEMPLATE_1="https://raw.githubusercontent.com/leck995/WutheringWavesToolResources/main/%s";
    private static final String RESOURCE_TEMPLATE_2="https://gitee.com/tealc/WutheringWavesToolResources/raw/main/%s";

    private final String url;
    private final String resource_template;
    private ObjectMapper mapper = new ObjectMapper();

    private  int filesSize = 0;

    public ResourcesSyncTask() {
        switch (Config.setting.getResourceSource()){
            case 1 -> {
                url =  ROOT_RESOURCE_URL_2;
                resource_template = RESOURCE_TEMPLATE_2;
            }
            default ->  {
                url =  ROOT_RESOURCE_URL_1;
                resource_template = RESOURCE_TEMPLATE_1;
            }
        }

    }


    /**
     * @description:
     * @param:
     * @return  void
     * @date:   2024/10/6
     */
    private synchronized void minusFileSize() {
        filesSize--;
        if (filesSize == 0) {
            updateMessage("更新资源完成");
        }
    }

    @Override
    protected String call(){
        try {
            String row = readJsonFile(url);
            if (row == null){
                updateMessage("无法访问远程资源仓库，检查网络并正确设置资源仓库源");
                return null;
            }
            RootResource remoteResource = mapper.readValue(row, RootResource.class);
            if (remoteResource != null) {
                File localFile= new File("assets/data/Root.json");
                if (localFile.exists()){
                    RootResource localResources = mapper.readValue(localFile, RootResource.class);
                    if (localResources != null) {
                        if (localResources.getVersion().equals(remoteResource.getVersion())){
                            LOG.debug("远程仓库无更新");
                            return null;
                        }else {
                            updateMessage("检测到有新的资源，开始更新");
                            updateDateFile(remoteResource);
                            downloadFile(url,localFile.getPath());
                        }
                    }
                }else {
                    updateMessage("检测到有新的资源，开始更新");
                    updateDateFile(remoteResource);
                    downloadFile(url,localFile.getPath());
                }
            }else {
                LOG.warn("获取资源文件更新失败");
            }
        } catch (IOException e) {
            LOG.error("错误",e);
        }

        LOG.debug("同步结束");
        //updateMessage("更新资源完成");
        return "";
    }


    private void updateDateFile(RootResource remoteResource){
        filesSize = remoteResource.getResources().size();
        for (Resource resource : remoteResource.getResources().values()) {
            Thread.startVirtualThread(()->{
                boolean same = checkLocalFile(resource);
                if (!same) {
                    LOG.info("================{} 的MD5与本地不同================",resource.getName());
                    try {
                        String url = String.format(resource_template, resource.getFilePath());
                        String row = readJsonFile(url);
                        if (row != null){
                            Map<String, Resource> map = mapper.readValue(row, new TypeReference<Map<String, Resource>>() {
                            });
                            for (Resource value : map.values()) {
                                boolean checked = checkLocalFile(value);
                                if (!checked) {
                                    LOG.info("{} 下的 {} 的MD5与本地不同",resource.getName(),value.getName());
                                    String fileUrl = String.format(resource_template, value.getFilePath());
                                    downloadFile(fileUrl,value.getAimPath());
                                    Thread.sleep(50);
                                }
                            }
                            String fileUrl = String.format(resource_template, resource.getFilePath());
                            LOG.debug("子项更新完毕，开始更新JSON {}",resource.getName());
                            downloadFile(fileUrl,resource.getAimPath());
                        }else {
                            LOG.warn("分类JSON {} 无法下载",resource.getName());
                        }
                    } catch (IOException | InterruptedException e) {
                        LOG.error("ERROR",e);
                    }
                }
                minusFileSize();
            });
        }

    }


    /**
     * @description: 检查MD5是否一致
     * @param:	resource
     * @return  boolean
     * @date:   2024/10/6
     */
    private boolean checkLocalFile(Resource resource){
        File local = new File(resource.getAimPath());
        if (local.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(local);
                String md5Hex = DigestUtils.md5Hex(fileInputStream);
                LOG.debug("远程MD5:{},本地{}",resource.getMd5(),md5Hex);
                fileInputStream.close();
                return md5Hex.equals(resource.getMd5());
            }catch (Exception e){
                LOG.error(e.getMessage());
                return false;
            }
        }
        return false;
    }


    public void downloadFile(String fileUrl, String savePath) {
        System.out.println(fileUrl);
        HttpClient client = HttpClient.newHttpClient();
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
            LOG.info("文件已下载并保存到:{},当前MD5：{}" ,savePath,md5Hex);
        } catch (IOException | InterruptedException e) {
            LOG.error("downloadFile ERROR",e);
        }
    }
    public String readJsonFile(String fileUrl) {
        System.out.println(fileUrl);
        HttpClient client = HttpClient.newHttpClient();
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
            }else {
                return null;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("readJsonFile ERROR",e);
        }
        return null;
    }
}