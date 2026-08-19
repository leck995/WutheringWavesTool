package cn.tealc.wutheringwavestool.service;

/**
 * 可控制的后台任务契约。TaskManageService 通过它统一为 UI 提供暂停 / 继续 / 取消 / 重试。
 * 不实现某能力的方法保留默认空实现，UI 据此决定是否显示对应控制。
 */
public interface TaskControl {

    /** 暂停任务。默认不支持。 */
    default boolean pauseTask() { return false; }

    /** 继续任务。默认不支持。 */
    default boolean resumeTask() { return false; }

    /** 取消任务。默认不支持。 */
    default boolean cancelTask() { return false; }

    /** 重试任务。默认不支持。 */
    default boolean retryTask() { return false; }

    /** 是否支持暂停/继续。 */
    default boolean supportsPause() { return false; }

    default boolean supportsRetry() { return false; }
}