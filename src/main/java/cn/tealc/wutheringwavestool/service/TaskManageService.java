package cn.tealc.wutheringwavestool.service;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/**
 * 后台任务进度服务中心，存储正在执行的 Task，供 UI 绑定显示
 */
public class TaskManageService {
    private final ObservableList<Task<?>> tasks = FXCollections.observableArrayList();
    private final SimpleBooleanProperty hasActiveTasks = new SimpleBooleanProperty(false);

    public void register(Task<?> task) {
        tasks.addFirst(task);
        hasActiveTasks.set(true);
        task.stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED
                    || state == Worker.State.FAILED
                    || state == Worker.State.CANCELLED) {
                remove(task);
            }
        });
    }

    /** 启动任务并注册到后台任务面板，完成后自动移除 */
    public void execute(Task<?> task) {
        register(task);
        Thread.startVirtualThread(task);
    }

    /** 从列表中移除 */
    public void remove(Task<?> task) {
        tasks.remove(task);
        refreshActiveState();
    }

    private void refreshActiveState() {
        boolean active = tasks.stream().anyMatch(Task::isRunning);
        hasActiveTasks.set(active);
    }

    public ObservableList<Task<?>> getTasks() { return tasks; }
    public SimpleBooleanProperty hasActiveTasksProperty() { return hasActiveTasks; }
    public boolean isHasActiveTasks() { return hasActiveTasks.get(); }
}
