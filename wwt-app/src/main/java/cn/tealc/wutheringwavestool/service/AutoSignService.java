package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.base.Config;
import com.kuro.kujiequ.thread.base.sign.SignTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoSignService {
    private static final Logger LOG = LoggerFactory.getLogger(AutoSignService.class);

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auto-sign-scheduler");
                t.setDaemon(true);
                return t;
            });

    /**
     * 启动服务：立即签到一次，然后定时到次日 00:05 左右
     */
    public void start() {
        sign();
        scheduleNext();
    }

    /**
     * 执行签到，异步发起签到请求
     */
    private void sign() {
        if (Config.setting().isAutoKujieQuSign()) {
            SignTask signTask = new SignTask();
            Thread.startVirtualThread(signTask);
        }
    }

    /**
     * 计算距次日 00:05 的毫秒数，一次性定时；到点后签到并递归调度下一天。
     * 错开零点整点避免服务器签到接口的高峰拥塞。
     */
    private void scheduleNext() {
        if (!Config.setting().isAutoKujieQuSign()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        // 次日 00:05，错开零点整点高峰
        LocalDateTime nextTrigger = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(5);
        long delayMillis = ChronoUnit.MILLIS.between(now, nextTrigger);

        scheduler.schedule(() -> {
            try {
                if (Config.setting().isAutoKujieQuSign()) {
                    LOG.info("到达签到时间，执行自动签到");
                    sign();
                }
            } catch (Exception e) {
                LOG.error("自动签到异常", e);
            } finally {
                scheduleNext();
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
    }
}
