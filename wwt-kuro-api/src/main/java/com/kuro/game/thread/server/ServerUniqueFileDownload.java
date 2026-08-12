package com.kuro.game.thread.server;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.SourceType;
import com.kuro.game.model.game.DownloadTaskInfo;
import com.kuro.game.model.game.FileInfo;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.UpdateData;
import com.kuro.game.thread.GameFileDownloadTask;
import com.kuro.game.thread.GameResourceListGetTask;
import com.kuro.game.thread.LauncherResourceTask;
import javafx.concurrent.Task;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ServerUniqueFileDownload extends Task<ResponseBody<Boolean>> {
    private SourceType sourceType = Config.setting().getGameRootDirSource();
    private AtomicInteger totalTasks = new AtomicInteger(0);
    private AtomicInteger completedTasks = new AtomicInteger(0);

    @Override
    protected ResponseBody<Boolean> call() throws Exception {
        if (sourceType == null) {
            return ResponseBody.create(-1, "游戏目录不存在", false);
        }

        try {
            // 1. 获取启动器资源
            LauncherResource launcherResource = getLauncherResource().get();
            if (launcherResource == null) {
                return ResponseBody.create(-1, "获取文件网址失败", false);
            }

            // 2. 获取游戏资源列表
            GameResourceList gameResourceList = getGameResourceList(launcherResource.getUpdateData()).get();
            if (gameResourceList == null) {
                return ResponseBody.create(-1, "获取资源列表失败", false);
            }

            // 3. 下载文件并更新进度
            boolean downloadSuccess = downloadFiles(launcherResource.getUpdateData(), gameResourceList.getResource());

            System.out.println("结束");
            return ResponseBody.create(200, "下载完成", downloadSuccess);

        } catch (Exception e) {
            return ResponseBody.create(-1, "下载失败: " + e.getMessage(), false);
        }
    }

    private CompletableFuture<LauncherResource> getLauncherResource() {
        LauncherResourceTask task = new LauncherResourceTask(LauncherResourceTask.Type.CN);
        CompletableFuture<LauncherResource> future = new CompletableFuture<>();

        task.setOnSucceeded(event -> {
            ResponseBody<LauncherResource> value = task.getValue();
            if (value.getCode() == 200) {
                future.complete(value.getData());
            } else {
                future.completeExceptionally(new RuntimeException("获取启动器资源失败"));
            }
        });

        task.setOnFailed(event -> {
            future.completeExceptionally(task.getException());
        });

        Thread.startVirtualThread(task);
        return future;
    }

    private CompletableFuture<GameResourceList> getGameResourceList(UpdateData updateData) {
        GameResourceListGetTask task = new GameResourceListGetTask(updateData.getResourceJsonUrl());
        CompletableFuture<GameResourceList> future = new CompletableFuture<>();

        task.setOnSucceeded(event -> {
            ResponseBody<GameResourceList> value = task.getValue();
            if (value.getCode() == 200) {
                future.complete(value.getData());
            } else {
                future.completeExceptionally(new RuntimeException("获取资源列表失败"));
            }
        });

        task.setOnFailed(event -> {
            future.completeExceptionally(task.getException());
        });

        Thread.startVirtualThread(task);
        return future;
    }

    private boolean downloadFiles(UpdateData updateData, List<FileInfo> fileInfoList) {
        if (sourceType == SourceType.DEFAULT) {
            return downloadForDefault(updateData, fileInfoList);
        }
        return false;
    }

    private boolean downloadForDefault(UpdateData updateData, List<FileInfo> fileInfoList) {
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
                    return new DownloadTaskInfo(url, Config.setting().getGameRootDir() + "/wwt/default", fileInfo);
                })
                .collect(Collectors.toList());

        System.out.println(downloadTasks.size());
        totalTasks.set(downloadTasks.size());
        completedTasks.set(0);

        HttpClient client = HttpClient.newBuilder().build();
        ExecutorService executorService = Executors.newFixedThreadPool(8);

        try {
            List<CompletableFuture<Boolean>> futures = downloadTasks.stream()
                    .map(taskInfo -> CompletableFuture.supplyAsync(() -> {
                        try {
                            GameFileDownloadTask downloadTask = new GameFileDownloadTask(client, taskInfo);
                            downloadTask.setOnSucceeded(event -> {
                                int completed = completedTasks.incrementAndGet();
                                updateProgress(completed, totalTasks.get());
                                updateMessage("已下载 " + completed + "/" + totalTasks.get() + " 个文件");
                                System.out.println("已下载 " + completed + "/" + totalTasks.get() + " 个文件");
                            });

                            // 监听子任务进度
                        /*    downloadTask.setOnSucceeded(event -> {
                                int completed = completedTasks.incrementAndGet();
                                updateProgress(completed, totalTasks.get());
                                updateMessage("已下载 " + completed + "/" + totalTasks.get() + " 个文件");
                            });

                            downloadTask.setOnFailed(event -> {
                                int completed = completedTasks.incrementAndGet();
                                updateProgress(completed, totalTasks.get());
                                updateMessage("下载失败: " + taskInfo.getFileName());
                            });*/

                            downloadTask.run(); // 同步执行
                            return downloadTask.getValue(); // 假设返回下载结果

                        } catch (Exception e) {
                            completedTasks.incrementAndGet();
                            return false;
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // 等待所有下载完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.MINUTES); // 设置超时

            // 检查所有下载结果
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