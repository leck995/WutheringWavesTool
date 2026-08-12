package com.kuro.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.game.model.Type;
import com.kuro.game.model.game.DownloadTaskInfo;
import com.kuro.game.model.game.FileInfo;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.UpdateData;
import com.kuro.model.ResponseBody;
import com.kuro.model.SourceType;
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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * 游戏下载更新 API 管理类（Facade）。
 * 原 LauncherResourceTask / GameResourceListGetTask / GameFileDownloadTask / ServerUniqueFileDownload 逻辑收敛至此。
 * 纯同步，无 JavaFX 依赖，调用方自行起线程。
 *
 * @author Leck
 */
public final class GameManager {
    private static final Logger LOG = LoggerFactory.getLogger(GameManager.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GameManager(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取启动器资源 index.json（原 LauncherResourceTask）
     */
    public ResponseBody<LauncherResource> getLauncherResource(Type type) {
        String url;
        switch (type) {
            case BILIBILI -> url = ApiConfig.INDEX_BILIBILI;
            case GLOBAL -> url = ApiConfig.INDEX_GLOBAL;
            default -> url = ApiConfig.INDEX_CN;
        }

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
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                try (GZIPInputStream gzipInputStream = new GZIPInputStream(response.body());
                     BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream))) {
                    LauncherResource launcherResource = objectMapper.readValue(reader, LauncherResource.class);
                    return ResponseBody.create(200, "", launcherResource);
                } catch (IOException e) {
                    // gzip 解压失败，尝试直接解析（服务器可能未压缩）
                    LOG.error(e.getMessage());
                    LauncherResource launcherResource = objectMapper.readValue(response.body(), LauncherResource.class);
                    return ResponseBody.create(200, "", launcherResource);
                }
            } else {
                return ResponseBody.create(-1, "获取最新版本信息失败", null);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            return ResponseBody.create(-1, "获取最新版本信息失败", null);
        }
    }

    /**
     * 获取游戏资源文件清单 resource.json（原 GameResourceListGetTask）
     */
    public ResponseBody<GameResourceList> getGameResourceList(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                GameResourceList resourceList = objectMapper.readValue(response.body(), GameResourceList.class);
                return ResponseBody.create(200, "", resourceList);
            } else {
                return ResponseBody.create(-1, "获取下载文件清单失败", null);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage());
            return ResponseBody.create(-1, "获取下载文件清单失败", null);
        }
    }

    /**
     * 下载单个游戏文件 + MD5 校验（原 GameFileDownloadTask，已零依赖）
     */
    public boolean downloadFile(DownloadTaskInfo taskInfo) {
        boolean success = false;
        taskInfo.setStatus(DownloadTaskInfo.Status.IN_PROGRESS);
        int recoveryTime = 3;
        for (int i = 0; i < recoveryTime && !success && recoveryTime > 0; i++) {
            success = download(taskInfo);
            if (!success) {
                recoveryTime--;
            }
        }
        if (success) {
            taskInfo.setStatus(DownloadTaskInfo.Status.FINISHED);
        } else {
            taskInfo.setStatus(DownloadTaskInfo.Status.FAILED);
        }
        return success;
    }

    private boolean download(DownloadTaskInfo taskInfo) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(taskInfo.getDownloadUrl()))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            return false;
        }
        if (response.statusCode() != 200) {
            return false;
        }

        Path aimFile = Path.of(taskInfo.getAimPath());

        // 已存在文件 MD5 校验跳过
        if (Files.exists(aimFile)) {
            try (FileInputStream fileInputStream = new FileInputStream(aimFile.toFile())) {
                String md5Hex = DigestUtils.md5Hex(fileInputStream);
                if (md5Hex.equals(taskInfo.getMd5())) {
                    LOG.debug("文件存在，跳过: {}", aimFile);
                    return true;
                } else {
                    Files.delete(aimFile);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }

        File parentDir = aimFile.toFile().getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();
            if (!dirsCreated) {
                LOG.error("Failed to create directories: " + parentDir.getAbsolutePath());
                return false;
            }
        }
        try (BufferedInputStream in = new BufferedInputStream(response.body());
             FileOutputStream fileOutputStream = new FileOutputStream(aimFile.toFile())) {
            byte[] dataBuffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 8192)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                taskInfo.addSpeedDownload(bytesRead);
            }

            // 下载完成后继续验证 MD5
            try (FileInputStream fileInputStream = new FileInputStream(aimFile.toFile())) {
                String md5Hex = DigestUtils.md5Hex(fileInputStream);
                if (md5Hex.equals(taskInfo.getMd5())) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 编排下载白名单关键文件（原 ServerUniqueFileDownload）。
     * gameRootDir / sourceType 改为参数传入，切断对 app Config 的依赖。
     *
     * @param gameRootDir 游戏根目录
     * @param sourceType  来源类型
     * @return 下载整体成功标志
     */
    public ResponseBody<Boolean> downloadServerFiles(String gameRootDir, SourceType sourceType) {
        if (sourceType == null) {
            return ResponseBody.create(-1, "游戏目录不存在", false);
        }

        try {
            // 1. 获取启动器资源
            ResponseBody<LauncherResource> launcherRes = getLauncherResource(Type.CN);
            if (launcherRes.getCode() != 200 || launcherRes.getData() == null) {
                return ResponseBody.create(-1, "获取文件网址失败", false);
            }
            LauncherResource launcherResource = launcherRes.getData();

            // 2. 获取游戏资源列表
            ResponseBody<GameResourceList> resourceRes = getGameResourceList(launcherResource.getUpdateData().getResourceJsonUrl());
            if (resourceRes.getCode() != 200 || resourceRes.getData() == null) {
                return ResponseBody.create(-1, "获取资源列表失败", false);
            }
            GameResourceList gameResourceList = resourceRes.getData();

            // 3. 下载文件
            boolean downloadSuccess = downloadFiles(launcherResource.getUpdateData(), gameResourceList.getResource(), gameRootDir, sourceType);

            return ResponseBody.create(200, "下载完成", downloadSuccess);
        } catch (Exception e) {
            return ResponseBody.create(-1, "下载失败: " + e.getMessage(), false);
        }
    }

    private boolean downloadFiles(UpdateData updateData, List<FileInfo> fileInfoList, String gameRootDir, SourceType sourceType) {
        if (sourceType == SourceType.DEFAULT) {
            return downloadForDefault(updateData, fileInfoList, gameRootDir);
        }
        return false;
    }

    private boolean downloadForDefault(UpdateData updateData, List<FileInfo> fileInfoList, String gameRootDir) {
        List<String> targetPaths = List.of(
                "Client/Binaries/Win64/ThirdParty/KrPcSdk_Mainland",
                "Client/Binaries/Win64/Client-Win64-Shipping.exe",
                "Client/Binaries/Win64/Client-Win64-ShippingBase.dll"
        );

        List<FileInfo> requestDownFileInfoList = fileInfoList.stream()
                .filter(fileInfo -> targetPaths.stream().anyMatch(path -> fileInfo.getDest().startsWith(path)))
                .collect(Collectors.toList());

        List<DownloadTaskInfo> downloadTasks = requestDownFileInfoList.stream()
                .map(fileInfo -> {
                    String url = updateData.getCdnList().getFirst().getUrl() +
                            updateData.getResourcesBasePath() + "/" + fileInfo.getDest();
                    return new DownloadTaskInfo(url, gameRootDir + "/wwt/default", fileInfo);
                })
                .collect(Collectors.toList());

        LOG.info("待下载文件数: {}", downloadTasks.size());

        ExecutorService executorService = Executors.newFixedThreadPool(8);
        try {
            List<CompletableFuture<Boolean>> futures = downloadTasks.stream()
                    .map(taskInfo -> CompletableFuture.supplyAsync(() -> downloadFile(taskInfo), executorService))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.MINUTES);

            return futures.stream().allMatch(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (Exception e) {
            return false;
        } finally {
            executorService.shutdown();
        }
    }
}
