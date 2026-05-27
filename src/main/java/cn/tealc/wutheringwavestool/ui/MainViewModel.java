package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.DownloadProgressService;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.release.Release;
import cn.tealc.wutheringwavestool.model.system.NavData;
import cn.tealc.wutheringwavestool.thread.system.CheckGameConfigTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.towerData.DifficultyTotal;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.system.CheckVersionTask;
import com.kuro.kujiequ.thread.base.sign.SignTask;
import com.kuro.kujiequ.thread.rolebox.slash.SlashDataDetailTask;
import com.kuro.kujiequ.thread.rolebox.tower.TowerDataDetailTask;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 18:59
 */
public class MainViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(MainViewModel.class);

    @Inject
    private UserInfoDao userInfoDao;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private DownloadProgressService downloadProgressService;

    private final AtomicBoolean warningTower = new AtomicBoolean(false);
    private final AtomicBoolean warningSlash = new AtomicBoolean(false);

    public MainViewModel() {
        checkVersion();
        checkGameLogOpen();
        updateKujiequ();
        autoSign();
    }

    public DownloadProgressService getDownloadProgressService() {
        return downloadProgressService;
    }

    public List<NavData> getNavList(){
        InputStream inputStream = FXResourcesLoader.loadStream("/cn/tealc/wutheringwavestool/data/nav.json");
        List<NavData> list = null;
        try {
            list = objectMapper.readValue(inputStream, new TypeReference<List<NavData>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }




    private void autoSign(){
        if (Config.setting().isAutoKujieQuSign()) {
            SignTask signTask = new SignTask();
            Thread.startVirtualThread(signTask);
        }
    }


    public void checkVersion() {
        if (Config.setting().isCheckNewVersion()) {
            Platform.runLater(() -> {
                CheckVersionTask task = new CheckVersionTask(true);
                task.setOnSucceeded(workerStateEvent -> {
                    ResponseBody<Release> value = task.getValue();
                    if (value.getCode() == 200) {
                        Platform.runLater(() -> {
                            MvvmFX.getNotificationCenter().publish(NotificationKey.NOTIFICATION_SHOW_UPDATE, value.getData());
                        });
                    } else if (value.getCode() == -1) {
                        MvvmFX.getNotificationCenter().publish(NotificationKey.NOTIFICATION_SHOW_UPDATE, value.getData());
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.main.message.type01")));
                    }
                });
                Thread.startVirtualThread(task);
            });
        }
    }


    private void checkGameLogOpen() {
        CheckGameConfigTask task = new CheckGameConfigTask();
        task.setOnSucceeded(workerStateEvent -> {
            Boolean value = task.getValue();
            if (!value) { //游戏日志可能被关闭了
                Platform.runLater(() -> {
                    NotificationManager.message(MessageInfo.success(LanguageManager.getString("ui.main.sync.message.log.close")));
                });
            }
        });
        Thread.startVirtualThread(task);
    }




    private void updateKujiequ() {
        if (!Config.setting().isNoKuJieQu()) {
            Thread.startVirtualThread(()->{
                //获取深塔刷新时间，同时更新深塔历史记录
                List<UserInfo> users = userInfoDao.getAll();
                for (int i = 0; i < users.size(); i++) {
                    syncSlash(users.get(i));
                    syncTower(users.get(i));
                    if (users.size() > 2){
                        try {
                            Thread.sleep(200); //用户数超过两个，等待200ms
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }
    }




    /**
     * 查询用户的深塔记录并保存，当距离结束只剩1天提醒
     * @param userInfo
     */
    private void syncTower(UserInfo userInfo) {
        Thread.startVirtualThread(()->{
            TowerDataDetailTask task = new TowerDataDetailTask(userInfo);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<DifficultyTotal> value = task.getValue();
                if (value.getCode() == 200) {
                    long milliseconds = value.getData().getSeasonEndTime();
                    long millisecondsInADay = 24 * 60 * 60 * 1000;
                    double days = (double) milliseconds / (double) millisecondsInADay;
                    if (days > 0 && days < 1 && warningTower.compareAndSet(false, true)) {//不足一天时,提醒
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.main.sync.message.tower")));
                    }
                }
            });
            Thread.startVirtualThread(task);
        });
    }

    /**
     * 查询用户的海虚记录并保存，当距离结束只剩1天提醒
     * @param userInfo
     */
    private void syncSlash(UserInfo userInfo) {
        SlashDataDetailTask slashDataDetailTask = new SlashDataDetailTask(userInfo);
        slashDataDetailTask.setOnSucceeded(workerStateEvent -> {
            ResponseBody<SlashData> value = slashDataDetailTask.getValue();
            if (value.getCode() == 200) {
                long milliseconds = value.getData().getSeasonEndTime();
                long millisecondsInADay = 24 * 60 * 60 * 1000;
                double days = (double) milliseconds / (double) millisecondsInADay;
                if (days > 0 && days < 1 &&  warningSlash.compareAndSet(false, true)) {//不足一天时,提醒
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.main.sync.message.slash")));
                }
            }
        });
        Thread.startVirtualThread(slashDataDetailTask);
    }


}