package com.kuro.game.thread;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ConcurrentDownloader {
    private List<DownloadTask> downloadTasks = new ArrayList<>();
    private ExecutorService executor;
    private AtomicLong totalDownloadedBytes = new AtomicLong(0);
    private long startTime;
    private boolean running = false;
    
    // 添加下载任务
    public void addDownloadTask(String fileUrl, String savePath) {
        downloadTasks.add(new DownloadTask(fileUrl, savePath));
    }
    
    // 开始下载
    public void startDownload(int concurrentThreads) {
        if (downloadTasks.isEmpty()) {
            System.out.println("没有下载任务！");
            return;
        }
        
        executor = Executors.newFixedThreadPool(concurrentThreads);
        running = true;
        startTime = System.currentTimeMillis();
        
        // 启动下载速度显示线程
        Thread speedMonitorThread = new Thread(this::displayDownloadSpeed);
        speedMonitorThread.setDaemon(true);
        speedMonitorThread.start();
        
        // 提交所有下载任务
        for (DownloadTask task : downloadTasks) {
            executor.submit(() -> {
                try {
                    downloadFile(task);
                } catch (IOException e) {
                    System.err.println("下载失败: " + task.fileUrl + " - " + e.getMessage());
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        running = false;
        System.out.println("\n所有下载任务完成！");
    }
    
    // 下载文件
    private void downloadFile(DownloadTask task) throws IOException {
        URL url = new URL(task.fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        long fileSize = connection.getContentLengthLong();
        task.fileSize = fileSize;
        
        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(task.savePath)) {
            
            byte[] dataBuffer = new byte[8192];
            int bytesRead;
            long downloaded = 0;
            
            while ((bytesRead = in.read(dataBuffer, 0, 8192)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                downloaded += bytesRead;
                task.downloadedBytes = downloaded;
                totalDownloadedBytes.addAndGet(bytesRead);
                
                // 更新进度
                task.updateProgress();
                
                // 稍微减慢循环速度以便更好地显示进度
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        task.completed = true;
    }
    
    // 显示下载速度
    private void displayDownloadSpeed() {
        long lastTotalBytes = 0;
        long lastTime = System.currentTimeMillis();
        
        while (running) {
            try {
                Thread.sleep(1000); // 每秒更新一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            long currentTotalBytes = totalDownloadedBytes.get();
            long currentTime = System.currentTimeMillis();
            
            // 计算速度 (字节/秒)
            long bytesPerSecond = (currentTotalBytes - lastTotalBytes) * 1000 / (currentTime - lastTime);
            
            // 转换速度单位
            String speed;
            if (bytesPerSecond < 1024) {
                speed = bytesPerSecond + " B/s";
            } else if (bytesPerSecond < 1024 * 1024) {
                speed = String.format("%.2f KB/s", bytesPerSecond / 1024.0);
            } else {
                speed = String.format("%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0));
            }
            
            // 清屏并显示所有下载进度
            System.out.print("\033[H\033[2J"); // 清屏
            System.out.flush();
            
            System.out.println("总下载速度: " + speed);
            System.out.println("==================================================");
            
            for (int i = 0; i < downloadTasks.size(); i++) {
                DownloadTask task = downloadTasks.get(i);
                System.out.printf("%d. %s: %s\n", i + 1, 
                                 getFileNameFromUrl(task.fileUrl), 
                                 task.getProgressBar());
            }
            
            lastTotalBytes = currentTotalBytes;
            lastTime = currentTime;
        }
    }
    
    // 从URL提取文件名
    private String getFileNameFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    }
    
    // 下载任务类
    private static class DownloadTask {
        String fileUrl;
        String savePath;
        long fileSize = -1;
        long downloadedBytes = 0;
        boolean completed = false;
        int progressPercentage = 0;
        
        DownloadTask(String fileUrl, String savePath) {
            this.fileUrl = fileUrl;
            this.savePath = savePath;
        }
        
        void updateProgress() {
            if (fileSize > 0) {
                progressPercentage = (int) ((downloadedBytes * 100) / fileSize);
            }
        }
        
        String getProgressBar() {
            if (fileSize == -1) {
                return "连接中...";
            }
            
            if (completed) {
                return "已完成 100% [==================================================]";
            }
            
            int bars = progressPercentage / 2;
            StringBuilder progressBar = new StringBuilder("[");
            for (int i = 0; i < 50; i++) {
                if (i < bars) {
                    progressBar.append("=");
                } else {
                    progressBar.append(" ");
                }
            }
            progressBar.append("] ");
            progressBar.append(progressPercentage).append("%");
            
            // 添加下载速度信息
            String sizeInfo;
            if (fileSize < 1024) {
                sizeInfo = downloadedBytes + "B/" + fileSize + "B";
            } else if (fileSize < 1024 * 1024) {
                sizeInfo = String.format("%.2fKB/%.2fKB", downloadedBytes/1024.0, fileSize/1024.0);
            } else {
                sizeInfo = String.format("%.2fMB/%.2fMB", downloadedBytes/(1024.0*1024.0), fileSize/(1024.0*1024.0));
            }
            
            progressBar.append(" (").append(sizeInfo).append(")");
            
            return progressBar.toString();
        }
    }
    
    // 使用示例
    public static void main(String[] args) {
        ConcurrentDownloader downloader = new ConcurrentDownloader();
        
        // 添加一些示例下载任务（实际使用时请替换为真实URL）
        downloader.addDownloadTask("https://example.com/file1.zip", "file1.zip");
        downloader.addDownloadTask("https://example.com/file2.pdf", "file2.pdf");
        downloader.addDownloadTask("https://example.com/file3.jpg", "file3.jpg");
        
        // 开始下载，使用3个并发线程
        downloader.startDownload(3);
    }
}