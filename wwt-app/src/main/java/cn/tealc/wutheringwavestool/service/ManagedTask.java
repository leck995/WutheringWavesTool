package cn.tealc.wutheringwavestool.service;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/** 由 TaskManageService 统一托管的后台任务包装：包含标识、展示名、底层 Task 与控制能力。 */
public final class ManagedTask {
    private final String id;
    private final String name;
    private final Task<?> task;
    private final TaskControl control;
    private final TaskCategory category;

    public enum TaskCategory { DOWNLOAD, REPAIR, UPDATE, PREDOWNLOAD, SERVER_SWITCH, OTHER }

    public ManagedTask(String id, String name, Task<?> task, TaskControl control, TaskCategory category) {
        this.id = id;
        this.name = name;
        this.task = task;
        this.control = control != null ? control : new TaskControl() { };
        this.category = category;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Task<?> getTask() { return task; }
    public TaskCategory getCategory() { return category; }

    /** 若底层任务实现了 TaskControl 才提供控制能力，否则不可控或仅取消。 */
    public boolean supportsPause() { return control.supportsPause(); }
    public boolean supportsRetry() { return control.supportsRetry(); }

    public boolean pause() { return control.pauseTask(); }
    public boolean resume() { return control.resumeTask(); }
    public boolean cancel() { return control.cancelTask(); }
    public boolean retry() { return control.retryTask(); }

    public Worker.State getState() { return task.getState(); }
    public double getProgress() { return task.getProgress(); }
    public String getTitle() { return task.getTitle(); }
    public String getMessage() { return task.getMessage(); }
}