package cn.tealc.wutheringwavestool.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @description: 单个下载任务的进度模型，供 UI 绑定
 * @author: Leck
 * @create: 2026-05-27
 */
public class DownloadProgressModel {
    public enum Status { RUNNING, COMPLETED, FAILED }

    private final String taskId;
    private final SimpleStringProperty taskName = new SimpleStringProperty();
    private final SimpleStringProperty message = new SimpleStringProperty();
    private final SimpleDoubleProperty progress = new SimpleDoubleProperty(-1);
    private final SimpleObjectProperty<Status> status = new SimpleObjectProperty<>(Status.RUNNING);

    public DownloadProgressModel(String taskId, String taskName) {
        this.taskId = taskId;
        this.taskName.set(taskName);
    }

    public String getTaskId() { return taskId; }

    public String getTaskName() { return taskName.get(); }
    public SimpleStringProperty taskNameProperty() { return taskName; }

    public String getMessage() { return message.get(); }
    public SimpleStringProperty messageProperty() { return message; }
    public void setMessage(String value) { message.set(value); }

    public double getProgress() { return progress.get(); }
    public SimpleDoubleProperty progressProperty() { return progress; }
    public void setProgress(double value) { progress.set(value); }

    public Status getStatus() { return status.get(); }
    public SimpleObjectProperty<Status> statusProperty() { return status; }
    public void setStatus(Status value) { status.set(value); }
}
