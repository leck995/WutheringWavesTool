package com.kuro.game.thread;

import com.kuro.game.model.game.DownloadTaskInfo;
import com.kuro.game.model.game.FileInfo;
import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

/**
 * @description:
 * @author: Leck
 * @create: 2025-08-28 17:43
 */
public class GameFileDownloadTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GameFileDownloadTask.class);
    private HttpClient client;
    private DownloadTaskInfo taskInfo;
    private int recoveryTime = 3;

    public GameFileDownloadTask(HttpClient client, DownloadTaskInfo taskInfo) {
        this.client = client;
        this.taskInfo = taskInfo;
    }

    @Override
    public void run() {
        boolean success = false;
        taskInfo.setStatus(DownloadTaskInfo.Status.IN_PROGRESS);
        for (int i = 0; i < recoveryTime && !success && recoveryTime > 0; i++) {
            success = download();
            if (!success) {
                recoveryTime--; // 只有下载失败时才减少恢复次数
            }
        }

        if (success) {
            taskInfo.setStatus(DownloadTaskInfo.Status.FINISHED);
        }else {
            taskInfo.setStatus(DownloadTaskInfo.Status.FAILED);
        }
    }




    private boolean download() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(taskInfo.getDownloadUrl()))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<InputStream> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            return false;
        }
        if (response.statusCode() != 200) {
            return false;
        }

        Path aimFile = Path.of(taskInfo.getAimPath());
        if (Files.exists(aimFile)) {
            try (FileInputStream fileInputStream = new FileInputStream(aimFile.toFile())) {
                String md5Hex = DigestUtils.md5Hex(fileInputStream);
                fileInputStream.close();
                if (md5Hex.equals(taskInfo.getMd5())) {
                    return true;
                } else {
                    Files.delete(aimFile);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }



        try (BufferedInputStream in = new BufferedInputStream(response.body());
             FileOutputStream fileOutputStream = new FileOutputStream(aimFile.toFile())) {
            byte[] dataBuffer = new byte[8192];
            int bytesRead;
            long downloaded = 0;

            while ((bytesRead = in.read(dataBuffer, 0, 8192)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                downloaded += bytesRead;
                taskInfo.addSpeedDownload(bytesRead);
            }

            //下载完成后继续验证md5
            try (FileInputStream fileInputStream = new FileInputStream(aimFile.toFile())) {
                String md5Hex = DigestUtils.md5Hex(fileInputStream);
                fileInputStream.close();
                if (md5Hex.equals(taskInfo.getMd5())) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }

        }catch (IOException e){
            LOG.error(e.getMessage());
        }


        return false;



    }


}