package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.release.Release;
import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @program: WutheringWavesTool
 * @description: 下载新版本助手压缩包，且解压
 * @author: Leck
 * @create: 2024-12-22 20:27
 */
public class DownloadUpdateTask extends Task<ResponseBody<Boolean>> {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadUpdateTask.class);

    private final File saveFile;
    private final Release release;
    private String url;
    public DownloadUpdateTask(Release release) {
        this.release = release;
        saveFile = new File("update.zip");
        if (release.getUrls().length == 1){
            url = release.getUrls()[0];
        }else if (release.getUrls().length > 1){
            if (Config.setting.getResourceSource() == 0){
                url = release.getUrls()[0];
            }else {
                url = release.getUrls()[1];
            }
        }
    }

    @Override
    protected ResponseBody<Boolean> call() throws Exception {
        updateProgress(0,1);
        if (saveFile.exists()) {
            String md5 = DigestUtils.md5Hex(new FileInputStream(saveFile));
            if (md5.equals(release.getMd5())){
                unzip();
                return ResponseBody.create(200,"准备安装",true);
            }else {
                deleteZip();
            }
        }
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5)).build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                long contentLength = response.headers().firstValueAsLong("Content-Length")
                        .orElse(-1L); // 获取文件大小
                if (contentLength > 0) {
                    updateTitle(String.format("%d M",contentLength / 1000 / 1000));
                    InputStream inputStream = response.body();
                    try (FileOutputStream outputStream = new FileOutputStream(saveFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            // 更新进度
                            updateProgress(totalBytesRead, contentLength);
                        }
                    }
                    inputStream.close();
                }
                boolean checked = checkMd5();
                if (checked){
                    unzip();
                    return ResponseBody.create(200,"准备安装",true);
                }else {
                    deleteZip();
                    return ResponseBody.create(201,"校验安装包失败，将重新下载",false);
                }
            } else {
                // 处理错误响应
                LOG.error("下载安装包失败,错误代码: {}",response.statusCode());
                return ResponseBody.create(-1,"下载安装包失败",false);
            }
        }catch (HttpTimeoutException e) {
            LOG.error("请求超时: {}", e.getMessage());
            return ResponseBody.create(408, "请求超时", false); // 408 是请求超时的 HTTP 状态码
        } catch (Exception e) {
            LOG.error("发生错误: {}", e.getMessage());
            return ResponseBody.create(-1, "发生错误: " + e.getMessage(), false);
        }
    }

    private boolean checkMd5(){
        try (FileInputStream inputStream = new FileInputStream(saveFile)){
            String md5 = DigestUtils.md5Hex(inputStream);
            return md5.equals(release.getMd5());
        } catch (IOException e) {
            LOG.error(e.getMessage(),e);
            return false;
        }
    }


    public void unzip() {
        String zipFilePath = "update.zip"; // ZIP 文件路径
        String destDir = "update"; // 解压目标目录
        // 确保目标目录存在
        File dir = new File(destDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 创建ZIP输入流
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                // 创建目录结构
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // 确保父目录存在
                    new File(newFile.getParent()).mkdirs();

                    // 写入文件
                    try (FileOutputStream outputStream = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(),e);
        }
    }



    private void deleteZip(){
        if (saveFile.exists()) {
            saveFile.delete();
        }
    }
}