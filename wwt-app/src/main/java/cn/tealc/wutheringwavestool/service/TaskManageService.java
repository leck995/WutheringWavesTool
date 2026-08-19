package cn.tealc.wutheringwavestool.service;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局后台任务管理器：持有并管理所有后台任务（含下载任务），
 * 支持按 id 获取指定任务、统一暂停 / 继续 / 取消 / 重试，并提供进度与活跃状态供 UI 绑定。
 */
public class TaskManageService {
    private final ObservableList<ManagedTask> tasks = FXCollections.observableArrayList();
    private final Map<String, ManagedTask> byId = new HashMap<>();
    private final SimpleBooleanProperty hasActiveTasks = new SimpleBooleanProperty(false);

    /** 提交并启动一个受管任务。id 用于后续获取与控制，重复 id 覆盖旧任务。 */
    public ManagedTask submit(String id, String name, Task<?> task, TaskControl control,
            ManagedTask.TaskCategory category) {
        ManagedTask managed = new ManagedTask(id, name, task, control, category);
        tasks.addFirst(managed);
        if (id != null && !id.isBlank()) {
            byId.put(id, managed);
        }
        hasActiveTasks.set(true);
        task.stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED
                    || state == Worker.State.FAILED
                    || state == Worker.State.CANCELLED) {
                remove(managed);
            }
        });
        Thread.startVirtualThread(task);
        return managed;
    }

    /** 提交普通后台任务（无控制能力、OTHER 分类）。 */
    public ManagedTask execute(Task<?> task) {
        return submit(task.getClass().getSimpleName(), task.getTitle(), task, null,
                ManagedTask.TaskCategory.OTHER);
    }

    /** 按 id 获取受管任务。 */
    public ManagedTask get(String id) {
        return byId.get(id);
    }

    /** 从列表中移除。 */
    public void remove(ManagedTask managed) {
        tasks.remove(managed);
        if (managed.getId() != null) {
            byId.remove(managed.getId());
        }
        refreshActiveState();
    }

    private void refreshActiveState() {
        boolean active = tasks.stream().anyMatch(t -> t.getTask().isRunning());
        hasActiveTasks.set(active);
    }

    // ---------------- 统一控制 ----------------

    public boolean pause(String id) {
        ManagedTask t = byId.get(id);
        return t != null && t.pause();
    }

    public boolean resume(String id) {
        ManagedTask t = byId.get(id);
        return t != null && t.resume();
    }

    public boolean cancel(String id) {
        ManagedTask t = byId.get(id);
        return t != null && t.cancel();
    }

    public boolean retry(String id) {
        ManagedTask t = byId.get(id);
        return t != null && t.retry();
    }

    public ObservableList<ManagedTask> getTasks() { return tasks; }
    public SimpleBooleanProperty hasActiveTasksProperty() { return hasActiveTasks; }
    public boolean isHasActiveTasks() { return hasActiveTasks.get(); }
}