package cn.tealc.wutheringwavestool.base;

import cn.tealc.wutheringwavestool.model.DownloadProgressModel;
import cn.tealc.wutheringwavestool.model.DownloadProgressModel.Status;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: 下载进度服务中心，管理所有下载任务的进度，供 UI 绑定
 * @author: Leck
 * @create: 2026-05-27
 */
public class DownloadProgressService {
    private final ObservableList<DownloadProgressModel> tasks = FXCollections.observableArrayList();
    private final SimpleBooleanProperty hasActiveTasks = new SimpleBooleanProperty(false);
    private final ConcurrentHashMap<String, DownloadProgressModel> taskMap = new ConcurrentHashMap<>();

    /** 创建一个新的下载任务并返回其模型 */
    public DownloadProgressModel createTask(String taskName) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        DownloadProgressModel model = new DownloadProgressModel(taskId, taskName);
        taskMap.put(taskId, model);
        Platform.runLater(() -> {
            tasks.add(0, model);
            hasActiveTasks.set(true);
        });
        return model;
    }

    /** 更新任务进度（可在任意线程调用） */
    public void updateProgress(String taskId, double progress, String message) {
        DownloadProgressModel model = taskMap.get(taskId);
        if (model != null) {
            Platform.runLater(() -> {
                model.setProgress(progress);
                if (message != null) {
                    model.setMessage(message);
                }
            });
        }
    }

    /** 标记任务完成 */
    public void completeTask(String taskId, String message) {
        DownloadProgressModel model = taskMap.get(taskId);
        if (model != null) {
            Platform.runLater(() -> {
                model.setStatus(Status.COMPLETED);
                model.setProgress(1.0);
                if (message != null) {
                    model.setMessage(message);
                }
                refreshActiveState();
            });
            scheduleRemoval(taskId);
        }
    }

    /** 标记任务失败 */
    public void failTask(String taskId, String message) {
        DownloadProgressModel model = taskMap.get(taskId);
        if (model != null) {
            Platform.runLater(() -> {
                model.setStatus(Status.FAILED);
                if (message != null) {
                    model.setMessage(message);
                }
                refreshActiveState();
            });
            scheduleRemoval(taskId);
        }
    }

    private void refreshActiveState() {
        boolean active = tasks.stream().anyMatch(t -> t.getStatus() == Status.RUNNING);
        hasActiveTasks.set(active);
    }

    /** 延迟从列表中移除已完成/失败的任务 */
    private void scheduleRemoval(String taskId) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> {
                DownloadProgressModel model = taskMap.remove(taskId);
                if (model != null) {
                    tasks.remove(model);
                }
            });
        }).start();
    }

    public ObservableList<DownloadProgressModel> getTasks() { return tasks; }
    public SimpleBooleanProperty hasActiveTasksProperty() { return hasActiveTasks; }
    public boolean isHasActiveTasks() { return hasActiveTasks.get(); }
}
