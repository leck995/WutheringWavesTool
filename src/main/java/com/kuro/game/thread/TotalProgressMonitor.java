package com.kuro.game.thread;

import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyLongWrapper;

//public class TotalProgressMonitor {
//    private final long totalBytes; // 所有文件的总大小
//    private final ReadOnlyDoubleWrapper totalProgress = new ReadOnlyDoubleWrapper(0);
//    private final ReadOnlyLongWrapper downloadedBytes = new ReadOnlyLongWrapper(0);
//
//    // ... 构造函数，初始化 totalBytes
//
//    public void incrementDownloadedBytes(long bytes) {
//        // 确保在 JavaFX 线程上更新属性
//        Platform.runLater(() -> {
//            downloadedBytes.set(downloadedBytes.get() + bytes);
//            totalProgress.set((double) downloadedBytes.get() / totalBytes);
//        });
//    }
//
//    public ReadOnlyDoubleProperty totalProgressProperty() {
//        return totalProgress.getReadOnlyProperty();
//    }
//}