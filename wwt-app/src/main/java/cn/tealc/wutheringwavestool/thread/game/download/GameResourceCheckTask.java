package cn.tealc.wutheringwavestool.thread.game.download;

import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 游戏资源检测任务（一次性）：调用 {@link GameUpdateService#checkUpdate} 检查远端版本。
 *
 * <p>结果经 {@link #valueProperty()} 承载 {@link ResourceCheckResult}；成功/失败由调用方通过
 * {@link #setOnSucceeded} / {@link #setOnFailed} 统一处理。检测与更新拆分，本任务只做检测。</p>
 */
public class GameResourceCheckTask extends Task<ResourceCheckResult> {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourceCheckTask.class);

    private final GameUpdateService updateService;

    public GameResourceCheckTask(GameUpdateService updateService) {
        this.updateService = updateService;
    }

    @Override
    protected ResourceCheckResult call() throws Exception {
        updateTitle("检查游戏资源更新");
        return updateService.checkUpdate();
    }
}