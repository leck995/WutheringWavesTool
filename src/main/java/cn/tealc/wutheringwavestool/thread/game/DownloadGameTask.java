package cn.tealc.wutheringwavestool.thread.game;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.game.model.game.DownloadFile;
import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-14 18:09
 */
public class DownloadGameTask extends Task<ResponseBody<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadGameTask.class);
    private HttpClient httpClient;
    private String dir;
    private String host;
    private DownloadFile downloadFile;
    private boolean pause;
    private boolean stop;


    public DownloadGameTask(HttpClient httpClient, String dir, String host, DownloadFile downloadFile) {
        this.httpClient = httpClient;
        this.dir = dir;
        this.host = host;
        this.downloadFile = downloadFile;
    }

    @Override
    protected ResponseBody<String> call() throws Exception {
        File resource = new File(dir + downloadFile.getDest());
        //第一步，检查目录是否存在，不存在则创建
        File parentFile = resource.getParentFile();
        if (!parentFile.exists()){
            boolean status = parentFile.mkdirs();
            LOG.info("创建父目录状态:{}",status);
        }


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + downloadFile.getDest()))
                .build();
        try (BufferedInputStream in = new BufferedInputStream(httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream()).body());
             FileOutputStream fileOutputStream = new FileOutputStream(resource)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;
            long startTime = System.currentTimeMillis();

            while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                // 检查是否暂停
                while (pause) {
                    Thread.sleep(100); // 等待 100 毫秒
                }
                // 检查是否停止
                if (stop) {
                    LOG.info("下载已停止");
                    return ResponseBody.create(0,"下载已停止",null);
                }
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // 显示下载速度
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime > 1000) { // 每秒更新一次
                    double speed = (totalBytesRead / 1024.0) / (elapsedTime / 1000.0); // KB/s
                    System.out.printf("下载速度: %.2f KB/s%n", speed);
                    startTime = System.currentTimeMillis();
                    totalBytesRead = 0;
                }
            }
            LOG.info("下载完成");
            boolean checked = checkMd5();
            if (checked) {
                LOG.info("MD5验证成功");
                return ResponseBody.create(200,"下载完成",null);
            }else {
                LOG.info("MD5验证失败");
                return ResponseBody.create(1,"MD5验证失败",null);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            return ResponseBody.create(-1,"网络异常，下载失败",null);
        }
    }





    private boolean checkMd5(){
        try (FileInputStream inputStream = new FileInputStream(new File(dir, downloadFile.getDest()))) {
            String md5 = DigestUtils.md5Hex(inputStream);
            return md5.equals(downloadFile.getMd5());
        } catch (IOException e) {
            LOG.error(e.getMessage(),e);
            return false;
        }
    }

}