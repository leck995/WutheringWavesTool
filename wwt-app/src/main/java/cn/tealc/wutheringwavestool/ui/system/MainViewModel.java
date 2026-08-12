package cn.tealc.wutheringwavestool.ui.system;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.*;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.AnnouncementItem;
import cn.tealc.wutheringwavestool.model.RedemptionCodeItem;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.AutoSignService;
import cn.tealc.wutheringwavestool.service.GameNewTowerService;
import cn.tealc.wutheringwavestool.service.SlashDataService;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.service.TowerDataService;

import cn.tealc.wutheringwavestool.service.ConfigService;
import cn.tealc.wutheringwavestool.thread.system.AnnouncementGetTask;
import cn.tealc.wutheringwavestool.thread.system.RedemptionCodeGetTask;
import cn.tealc.wutheringwavestool.thread.system.ResourcesSyncTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.release.Release;
import cn.tealc.wutheringwavestool.model.system.NavData;
import cn.tealc.wutheringwavestool.thread.system.CheckGameConfigTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.kujiequ.model.newTowerData.NewTowerData;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.slash.SlashDifficulty;
import com.kuro.kujiequ.model.towerData.DifficultyTotal;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.thread.system.AppCheckVersionTask;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private TaskManageService taskManageService;

    @Inject
    private ConfigService configService;

    @Inject
    private KujiequManager kujiequManager;

    @Inject
    private TowerDataService towerDataService;

    @Inject
    private SlashDataService slashDataService;

    @Inject
    private GameNewTowerService gameNewTowerService;

    private static final String REDEMPTION_CODE = "REDEMPTION_CODE";
    private static final String ANNOUNCEMENTS = "ANNOUNCEMENTS";

    private final AtomicBoolean warningTower = new AtomicBoolean(false);
    private final AtomicBoolean warningSlash = new AtomicBoolean(false);
    private final AtomicBoolean warningNewTower = new AtomicBoolean(false);

    public MainViewModel() {

    }

    public void initialize(){
        checkVersion();
        checkGameLogOpen();
        checkRedemptionCodes();
        checkAnnouncements();
        updateKujiequ();
        syncAppResources();
        autoSign();
    }



    private void autoSign() {
        AppInjector.getInstance(AutoSignService.class).start();
    }

    private static void syncAppResources() {
        ResourcesSyncTask task = new ResourcesSyncTask();
        task.messageProperty().addListener((observableValue, s, t1) -> {
            if (t1 != null) {
                switch (t1) {
                    case "success" -> MvvmFX.getNotificationCenter().publish(
                            NotificationKey.MESSAGE,
                            MessageInfo.success(LanguageManager.getString("ui.main.sync.message.success")));
                    case "error" -> MvvmFX.getNotificationCenter().publish(
                            NotificationKey.MESSAGE,
                            MessageInfo.error(LanguageManager.getString("ui.main.sync.message.error")));
                    case "start" -> MvvmFX.getNotificationCenter().publish(
                            NotificationKey.MESSAGE,
                            MessageInfo.info(LanguageManager.getString("ui.main.sync.message.start")));
                }
            }
        });
        AppInjector.getInstance(TaskManageService.class).execute(task);
    }

    public TaskManageService getDownloadProgressService() {
        return taskManageService;
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




    public void checkVersion() {
        if (Config.setting().isCheckNewVersion()) {
            Platform.runLater(() -> {
                AppCheckVersionTask task = new AppCheckVersionTask(true);
                task.setOnSucceeded(workerStateEvent -> {
                    ResponseBody<Release> value = task.getValue();
                    if (value.getCode() == 200) {
                        Platform.runLater(() -> {
                            MvvmFX.getNotificationCenter().publish(NotificationKey.NOTIFICATION_SHOW_UPDATE, value.getData());
                        });
                    } else if (value.getCode() == -1) {
                        //MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.warning(LanguageManager.getString("ui.main.message.type01")));
                    }
                });
                task.setOnFailed(workerStateEvent -> {
                    LOG.error("检查版本更新失败", workerStateEvent.getSource().getException());
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
        task.setOnFailed(workerStateEvent -> {
            LOG.error("检测游戏日志状态失败", workerStateEvent.getSource().getException());
        });
        Thread.startVirtualThread(task);
    }

    private void checkRedemptionCodes() {
        RedemptionCodeGetTask task = new RedemptionCodeGetTask();
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<Map<String, List<RedemptionCodeItem>>> value = task.getValue();
            if (value.getCode() == 200 && value.getData() != null) {
                boolean isGlobal = Config.setting().getGameRootDirSource() == SourceType.GLOBAL;
                String targetServer = isGlobal ? "mc1002" : "mc1001";
                List<RedemptionCodeItem> items = value.getData().get(targetServer);
                Set<String> currentKeys = items != null
                        ? items.stream().filter(RedemptionCodeItem::isValid).map(RedemptionCodeItem::getKey).collect(Collectors.toSet())
                        : Collections.emptySet();
                Set<String> notifiedKeys = configService.getObject(REDEMPTION_CODE, new TypeReference<Set<String>>() {})
                        .orElse(new HashSet<>());
                Set<String> newKeys = new HashSet<>(currentKeys);
                newKeys.removeAll(notifiedKeys);
                if (!newKeys.isEmpty()) {
                    Platform.runLater(() -> {
                        NotificationManager.publish(NotificationKey.MESSAGE,
                                MessageInfo.info("发现 " + newKeys.size() + " 个新兑换码，请在兑换码页面查看"));
                    });
                    notifiedKeys.addAll(newKeys);
                    configService.setObject(REDEMPTION_CODE, notifiedKeys);
                }
            }
        });
        task.setOnFailed(workerStateEvent -> {
            LOG.error("检查兑换码失败", workerStateEvent.getSource().getException());
        });
        Thread.startVirtualThread(task);
    }

    private void checkAnnouncements() {
        String gameId = Config.setting().getGameRootDirSource() == SourceType.GLOBAL ? "mc1002" : "mc1001";
        AnnouncementGetTask task = new AnnouncementGetTask(gameId);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<List<AnnouncementItem>> value = task.getValue();
            if (value.getCode() == 200 && value.getData() != null) {
                Set<Integer> notifiedIds = configService.getObject(ANNOUNCEMENTS, new TypeReference<Set<Integer>>() {})
                        .orElse(new HashSet<>());
                for (AnnouncementItem item : value.getData()) {
                    if (!notifiedIds.contains(item.getId())) {
                        Platform.runLater(() -> {
                            NotificationManager.message(MessageInfo.info(
                                    item.getTitle() + "\n" + item.getContent()));
                        });
                        notifiedIds.add(item.getId());
                    }
                }
                configService.setObject(ANNOUNCEMENTS, notifiedIds);
            }
        });
        task.setOnFailed(workerStateEvent -> {
            LOG.error("检查公告失败", workerStateEvent.getSource().getException());
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
                    syncNewTower(users.get(i));
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
            try {
                com.kuro.model.ResponseBody<DifficultyTotal> value = kujiequManager.getTowerData(userInfo);
                if (value.getCode() == 200 && value.getData() != null) {
                    towerDataService.saveToDB(value.getData(), userInfo.getRoleId());
                    long milliseconds = value.getData().getSeasonEndTime();
                    long millisecondsInADay = 24 * 60 * 60 * 1000;
                    double days = (double) milliseconds / (double) millisecondsInADay;
                    if (days > 0 && days < 1 && warningTower.compareAndSet(false, true)) {//不足一天时,提醒
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.warning(LanguageManager.getString("ui.main.sync.message.tower")));
                    }
                }
            } catch (Exception e) {
                LOG.error("同步深塔数据失败", e);
            }
        });
    }
    private void syncNewTower(UserInfo userInfo) {
        Thread.startVirtualThread(()->{
            try {
                com.kuro.model.ResponseBody<NewTowerData> value = kujiequManager.getNewTowerData(userInfo);
                if (value.getCode() == 200 && value.getData() != null && value.getData().isUnlock()) {
                    gameNewTowerService.saveToDB(value.getData(), userInfo.getRoleId());
                    long milliseconds = value.getData().getEndTime();
                    long millisecondsInADay = 24 * 60 * 60 * 1000;
                    double days = (double) milliseconds / (double) millisecondsInADay;
                    if (days > 0 && days < 1) {//不足一天时,提醒
                        long sum = value.getData().getModeDetails().stream()
                                .mapToInt(d -> d.getRank())
                                .sum();
                        if (sum < 6 && warningNewTower.compareAndSet(false, true)){
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.warning(LanguageManager.getString("ui.main.sync.message.tower2")));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("同步新深塔数据失败", e);
            }
        });
    }


    /**
     * 查询用户的海虚记录并保存，当距离结束只剩1天提醒
     * @param userInfo
     */
    private void syncSlash(UserInfo userInfo) {
        Thread.startVirtualThread(()->{
            try {
                com.kuro.model.ResponseBody<SlashData> value = kujiequManager.getSlashData(userInfo);
                if (value.getCode() == 200 && value.getData() != null) {
                    try {
                        slashDataService.saveToDB(value.getData(), userInfo.getRoleId());
                    } catch (JsonProcessingException e) {
                        LOG.error("海墟数据落库失败", e);
                    }
                    long milliseconds = value.getData().getSeasonEndTime();
                    long millisecondsInADay = 24 * 60 * 60 * 1000;
                    double days = (double) milliseconds / (double) millisecondsInADay;
                    if (days > 0 && days < 1) {//不足一天时,提醒
                        if (value.getData().getDifficultyList() == null || value.getData().getDifficultyList().isEmpty())
                            return;
                        boolean warning = false;
                        for (SlashDifficulty slashDifficulty : value.getData().getDifficultyList()) {
                            if (slashDifficulty.getAllScore() >= slashDifficulty.getMaxScore())
                                continue;
                            else {
                                warning = true;
                                break;
                            }
                        }
                        if (warning && warningSlash.compareAndSet(false, true))
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.warning(LanguageManager.getString("ui.main.sync.message.slash")));
                    }
                }
            } catch (Exception e) {
                LOG.error("同步海墟数据失败", e);
            }
        });
    }


}