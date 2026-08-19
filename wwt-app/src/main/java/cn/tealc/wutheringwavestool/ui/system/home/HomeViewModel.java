package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.WwtApp;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.service.GameInstallationManager;
import cn.tealc.wutheringwavestool.service.GameServerSwitchCoordinator;
import cn.tealc.wutheringwavestool.service.GameTimeService;
import cn.tealc.wutheringwavestool.service.GameUpdateService;
import cn.tealc.wutheringwavestool.thread.game.download.GameResourceCheckTask;
import cn.tealc.wutheringwavestool.thread.game.download.GameResourceUpdateTask;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wwt.game.resource.GameDownloadSource;
import cn.tealc.wwt.game.resource.model.ResourceCheckResult;
import cn.tealc.wwt.game.resource.model.ResourceCheckState;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 19:57
 */
public class HomeViewModel extends BaseViewModel implements SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(HomeViewModel.class);

    @Inject
    private GameTimeService gameTimeService;
    @Inject
    private GameUpdateService updateService;
    @Inject
    private GameServerSwitchCoordinator serverSwitchCoordinator;
    @Inject
    private GameInstallationManager installationManager;
    @Inject
    private TaskManageService taskManageService;
    private GameResourceUpdateTask resourceUpdateTask;
    private ResourceCheckResult checkResult;
    private final SimpleObjectProperty<GameResourceUpdateTask.UpdateState> resourceUpdateState =
            new SimpleObjectProperty<>(GameResourceUpdateTask.UpdateState.IDLE);
    private final SimpleStringProperty resourceStatusText = new SimpleStringProperty();
    private final SimpleStringProperty resourceVersionText = new SimpleStringProperty();
    private final SimpleStringProperty updateActionText = new SimpleStringProperty();
    private final SimpleBooleanProperty resourceStatusVisible = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty resourceRetryVisible = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty updateActionVisible = new SimpleBooleanProperty(false);
    private SimpleStringProperty gameTimeText = new SimpleStringProperty();
    private SimpleStringProperty gameTimeTipText = new SimpleStringProperty();
    private SimpleBooleanProperty startGameBtnDisabled = new SimpleBooleanProperty(false);
    private final ChangeListener<GameDownloadSource> serverSourceListener =
            (observable, oldSource, newSource) -> {
                if (newSource != null && oldSource != newSource) {
                    checkGameResourceUpdate();
                }
            };

    public void initialize() {
        updateGameTime(GameAppListener.getInstance().getDuration());
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.HOME_GAME_TIME_UPDATE, (s, objects) -> {
            if (objects.length > 0) {
                long playTime = (long) objects[0];
                updateGameTime(playTime);
            } else {
                updateGameTime(0);
            }
        });

        MvvmFX.getNotificationCenter().subscribe(NotificationKey.HOME_AUTO_START_GAME, (s, objects) -> {
            startGame();
        });
    }

    @Override
    public void onViewAdded() {
        serverSwitchCoordinator.currentSourceProperty().addListener(serverSourceListener);
        serverSwitchCoordinator.refresh();
        checkGameResourceUpdate();
    }

    @Override
    public void onViewRemoved() {
        serverSwitchCoordinator.currentSourceProperty().removeListener(serverSourceListener);
    }

    /** 首页创建后后台检查远端资源版本，不阻塞页面和游戏启动。 */
    public void checkGameResourceUpdate() {
        GameResourceCheckTask task = new GameResourceCheckTask(updateService);
        resourceUpdateState.set(GameResourceUpdateTask.UpdateState.CHECKING);
        resourceStatusText.set(LanguageManager.getString("ui.home.resource.checking"));
        resourceStatusVisible.set(true);
        task.setOnSucceeded(event -> {
            ResourceCheckResult result = task.getValue();
            checkResult = result;
            if (result == null || !result.isSuccessful()) {
                resourceUpdateState.set(GameResourceUpdateTask.UpdateState.FAILED);
                resourceStatusText.set(LanguageManager.getString("ui.home.resource.check_failed"));
                return;
            }
            String current = hasText(result.installedVersion()) ? result.installedVersion() : "-";
            String latest = hasText(result.latestVersion()) ? result.latestVersion() : "-";
            boolean upToDate = result.state() == ResourceCheckState.UP_TO_DATE
                    || current.equalsIgnoreCase(latest);
            resourceUpdateState.set(upToDate
                    ? GameResourceUpdateTask.UpdateState.UP_TO_DATE
                    : GameResourceUpdateTask.UpdateState.UPDATE_AVAILABLE);
            resourceStatusVisible.set(true);
            updateActionVisible.set(!upToDate);
            if (!upToDate) {
                updateActionText.set(String.format(
                        LanguageManager.getString("ui.home.button.update_to"), latest));
            }
            resourceVersionText.set(String.format(
                    LanguageManager.getString("ui.home.resource.current_version"),
                    upToDate ? current : latest));
            resourceStatusText.set(upToDate
                    ? LanguageManager.getString("ui.home.resource.up_to_date")
                    : LanguageManager.getString("ui.home.resource.update_available"));
        });
        task.setOnFailed(event -> {
            LOG.warn("检查游戏资源更新失败", task.getException());
            checkResult = null;
            resourceUpdateState.set(GameResourceUpdateTask.UpdateState.FAILED);
            resourceStatusText.set(LanguageManager.getString("ui.home.resource.check_failed"));
        });
        taskManageService.execute(task);
    }


    public void switchServer(SourceType target) {
        serverSwitchCoordinator.switchTo(target);
    }

    public boolean isServerInstallationConfigured(SourceType target) {
        return serverSwitchCoordinator.isInstallationConfigured(target);
    }

    public String configureAndSwitchServer(SourceType target, File gameDirectory) {
        return serverSwitchCoordinator.configureAndSwitch(target, gameDirectory.toPath());
    }

    public ReadOnlyObjectProperty<SourceType> currentGameSourceProperty() {
        return Config.setting().gameRootDirSourceProperty();
    }

    public ReadOnlyBooleanProperty serverSwitchOperatingProperty() {
        return serverSwitchCoordinator.operatingProperty();
    }

    public ReadOnlyBooleanProperty serverSwitchAvailableProperty() {
        return serverSwitchCoordinator.switchAvailableProperty();
    }

    public ReadOnlyBooleanProperty mainlandCacheReadyProperty() {
        return serverSwitchCoordinator.mainlandCacheReadyProperty();
    }

    public ReadOnlyBooleanProperty bilibiliCacheReadyProperty() {
        return serverSwitchCoordinator.bilibiliCacheReadyProperty();
    }

    public ReadOnlyBooleanProperty mainlandTargetReadyProperty() {
        return serverSwitchCoordinator.mainlandTargetReadyProperty();
    }

    public ReadOnlyBooleanProperty bilibiliTargetReadyProperty() {
        return serverSwitchCoordinator.bilibiliTargetReadyProperty();
    }

    public ReadOnlyStringProperty serverSwitchStatusTextProperty() {
        return serverSwitchCoordinator.statusTextProperty();
    }

    public ReadOnlyStringProperty serverSwitchDetailTextProperty() {
        return serverSwitchCoordinator.detailTextProperty();
    }


    /**
     * @return void
     * @description: 更新游玩时长；会对数据库与time进行相加处理，并显示
     * @param: time    尚未保存到数据库中的时长
     * @date: 2024/10/8
     */
    private void updateGameTime(long time) {
        List<GameTime> list = getGameTimes();
        if (list != null) {
            long sum = list.stream().mapToLong(GameTime::getDuration).sum() + time;
            updateGameTimeText(sum);
        }
    }

    /**
     * @return java.util.List<cn.tealc.wutheringwavestool.model.game.GameTime>
     * @description: 获取数据库中的当天游玩时长时间
     * @date: 2024/10/8
     */
    private List<GameTime> getGameTimes() {
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = dateTimeFormatter.format(localDate);
        return gameTimeService.getTimeListByData(date);
    }

    private void updateGameTimeText(long sum) {
        int hour = (int) (sum / (1000 * 60 * 60));
        int minute = (int) ((sum % (1000 * 60 * 60)) / (1000 * 60));
        String[] tips = LanguageManager.getStringArray("ui.home.label.time.others");
        if (hour == 0 && minute == 0) {
            gameTimeTipText.set(tips[0]);
        } else if (hour < 1 && minute < 15) {
            gameTimeTipText.set(tips[1]);
        } else if (hour <= 2) {
            gameTimeTipText.set(tips[2]);
        } else if (hour <= 5) {
            gameTimeTipText.set(tips[3]);
        } else {
            gameTimeTipText.set(tips[4]);
        }

        String total = LanguageManager.getString("ui.home.label.time.total");
        gameTimeText.set(String.format(total, hour, minute));
    }


    public void startUpdate() {
        if (checkResult == null) {
            return;
        }
        updateTask().setCheckResult(checkResult);
        updateTask().executeUpdate();
    }

    public void retryResourceUpdate() {
        runCheck();
    }

    public void pauseResourceUpdate() {
        updateTask().pauseUpdate();
    }

    public void resumeResourceUpdate() {
        updateTask().resumeUpdate();
    }

    public void cancelResourceUpdate() {
        updateTask().cancelUpdate();
    }

    private GameResourceUpdateTask updateTask() {
        if (resourceUpdateTask == null) {
            resourceUpdateTask = new GameResourceUpdateTask(updateService);
            resourceUpdateTask.phaseProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    resourceUpdateState.set(newValue);
                }
            });
            resourceUpdateTask.statusTextProperty().addListener((o, a, n) -> resourceStatusText.set(n));
            resourceUpdateTask.detailTextProperty().addListener((o, a, n) -> resourceVersionText.set(n));
            resourceUpdateTask.retryVisibleProperty().addListener((o, a, n) -> resourceRetryVisible.set(n));
            resourceUpdateTask.statusVisibleProperty().addListener((o, a, n) -> resourceStatusVisible.set(n));
            resourceUpdateState.set(resourceUpdateTask.getPhase());
        }
        return resourceUpdateTask;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
    /**
     * 签到并启动鸣潮
     */
    public void signAndGame() {
        //startKujiequDailySign();
        startGame();
    }




    /**
     * 启动鸣潮，先删除旧日志，然后判断是否启动参数，并进行启动
     */
    public void startGame() {
        //先设置3s的禁止点击，防止双击启动
        startGameBtnDisabled.set(true);
        PauseTransition pauseTransition = new PauseTransition(Duration.seconds(3));
        pauseTransition.setOnFinished(event -> {
            startGameBtnDisabled.set(false);
        });
        pauseTransition.play();

        //删除游戏过去的日志，避免数据污染
        deleteLogFiles();

        String dir = Config.setting().getGameRootDir();
        if (dir != null) {
            File exe = null;
            //当自定义启动程序时
            if (Config.setting().isGameStartAppCustom()) {
                exe = new File(Config.setting().getGameStarAppPath());
                if (!exe.exists()) {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            MessageInfo.warning(
                                    String.format(
                                            LanguageManager.getString("ui.home.message.type05"),
                                            exe.getPath()
                                    )));
                    return;
                }
            } else { //默认启动程序Wuthering Waves.exe
                exe = GameResourcesManager.getGameExeBase();
            }

            if (exe != null) {
                if (Config.setting().isUserAdvanceGameSettings()) { //使用高级启动设置
                    List<String> paramsList = new ArrayList<>(installationManager.activeStartUpParams());
                    if (!paramsList.isEmpty()) {
                        paramsList.addFirst(exe.getAbsolutePath());
                        String[] newArray = new String[paramsList.size()];
                        paramsList.toArray(newArray);
                        runExeByCustom(newArray);
                    } else {
                        runExe(exe);
                    }
                } else { //默认启动
                    runExe(exe);
                }
                hideMainWindow();
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        MessageInfo.warning(String.format(LanguageManager.getString("ui.home.message.type03"), exe.getPath())));
            }
        } else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    MessageInfo.warning(LanguageManager.getString("ui.home.message.type04")));
        }
    }


    /**
     * @return void
     * @description: 启动时隐藏窗口
     * @param:
     * @date: 2024/11/16
     */
    private void hideMainWindow() {
        if (Config.setting().isHideWhenGameStart()) {
            WwtApp.getWindow().hide();
        }
    }

    /**
     * @return void
     * @description: 第一个参数必须是启动器的路径
     * @param: params
     * @date: 2024/10/17
     */
    private void runExeByCustom(String... params) {
        Thread.startVirtualThread(() -> {
            String[] command2 = {"cmd.exe", "/c", "start", "\"\""}; //权限不够，提权
            String[] mergedArray = Stream.concat(Stream.of(command2), Stream.of(params))
                    .toArray(String[]::new);
            ProcessBuilder processBuilder = new ProcessBuilder(mergedArray);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
            String path = params[0];
            if (path.contains("WWMI Loader.exe")) {
                //设置工作目录，适配wwmi
                File file = new File(path);
                if (file.exists()) {
                    File workingDirectory = file.getParentFile();
                    if (workingDirectory.exists()) {
                        processBuilder.directory(workingDirectory);
                    }
                }
            }
            try {
                GameAppListener.getInstance().setStartFromApp(true);
                processBuilder.start();
            } catch (IOException e) {
                WwtApp.getWindow().show();
                GameAppListener.getInstance().setStartFromApp(false);
                LOG.error("高级启动无法启动鸣潮", e);
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.error(LanguageManager.getString("ui.home.message.type07") + e.getMessage()));
            }
        });
    }


    /**
     * @return void
     * @description: 默认启动
     * @param: exe
     * @date: 2024/11/16
     */
    private void runExe(File exe) {
        try {
            GameAppListener.getInstance().setStartFromApp(true);
            Desktop.getDesktop().open(exe);
            if (Config.setting().isHideWhenGameStart()) {
                WwtApp.getWindow().hide();
            }
        } catch (IOException e) {
            GameAppListener.getInstance().setStartFromApp(false);
            LOG.info("启动游戏错误:{}", e.getMessage());
            WwtApp.getWindow().show();
        }
    }


    /**
     * @return void
     * @description: 删除游戏日志，用于保证每次启动日志都是最新的，不重复的
     * @param:
     * @date: 2024/11/16
     */
    private void deleteLogFiles() {
        File dir = GameResourcesManager.getGameLogDir();
        if (dir != null) {
            LOG.info("");
            File[] files = dir.listFiles();
            if (files != null) {
                LOG.info("开始删除旧日志");
                for (File file : files) {
                    boolean delete = file.delete();
                    LOG.debug("删除日志文件{},状态：{}", file.getName(),delete);
                }
                //Arrays.stream(files).forEach(File::delete);
            }else {
                LOG.info("无旧日志，跳过");
            }
        }
    }


    public String getGameTimeText() {
        return gameTimeText.get();
    }

    public SimpleStringProperty gameTimeTextProperty() {
        return gameTimeText;
    }

    public String getGameTimeTipText() {
        return gameTimeTipText.get();
    }

    public SimpleStringProperty gameTimeTipTextProperty() {
        return gameTimeTipText;
    }

    public boolean isStartGameBtnDisabled() {
        return startGameBtnDisabled.get();
    }

    public SimpleBooleanProperty startGameBtnDisabledProperty() {
        return startGameBtnDisabled;
    }

    public ReadOnlyObjectProperty<GameResourceUpdateTask.UpdateState> resourceUpdateStateProperty() {
        return resourceUpdateState;
    }

    public ReadOnlyBooleanProperty resourceStatusVisibleProperty() {
        return resourceStatusVisible;
    }

    public ReadOnlyBooleanProperty resourceRetryVisibleProperty() {
        return resourceRetryVisible;
    }

    public ReadOnlyStringProperty resourceStatusTextProperty() {
        return resourceStatusText;
    }

    public ReadOnlyStringProperty resourceVersionTextProperty() {
        return resourceVersionText;
    }

    public ReadOnlyStringProperty updateActionTextProperty() {
        return updateActionText;
    }

    public ReadOnlyBooleanProperty updateActionVisibleProperty() {
        return updateActionVisible;
    }

    public ReadOnlyDoubleProperty resourceProgressProperty() {
        return updateTask().progressProperty();
    }

    public ReadOnlyStringProperty resourceProgressTextProperty() {
        return updateTask().progressTextProperty();
    }

    public ReadOnlyBooleanProperty resourceProgressVisibleProperty() {
        return updateTask().progressVisibleProperty();
    }

    public ReadOnlyBooleanProperty resourcePauseVisibleProperty() {
        return updateTask().pauseVisibleProperty();
    }

    public ReadOnlyBooleanProperty resourceResumeVisibleProperty() {
        return updateTask().resumeVisibleProperty();
    }

    public ReadOnlyBooleanProperty resourceCancelVisibleProperty() {
        return updateTask().cancelVisibleProperty();
    }
}
