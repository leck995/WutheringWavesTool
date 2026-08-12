package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.service.SignService;
import javafx.concurrent.Task;

/**
 * 签到调度任务（壳子，委托给 SignService.signAll()）。
 * 保留在 app 模块，因依赖 app 的 SignService。
 *
 * @author Leck
 */
public class SignTask extends Task<String> {

    @Override
    protected String call() {
        SignService signService = AppInjector.getInstance(SignService.class);
        return signService.signAll();
    }
}
