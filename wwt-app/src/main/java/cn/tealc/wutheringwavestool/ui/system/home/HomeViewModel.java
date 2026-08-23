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
import cn.tealc.wutheringwavestool.service.ManagedTask;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.animation.PauseTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
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

    /** 首页资源更新状态枚举（CSS 伪类驱动）。从 Task 内置属性派生。 */
    public enum ResourceUpdateState {
        IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE,
        PREPARING, DOWNLOADING, PAUSED, APPLYING,
        COMPLETED, FAILED, CANCELED
    }

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
    private ResourceCheckResult checkResult;
    private final SimpleObjectProperty<ResourceUpdateState> resourceUpdateState =
            new SimpleObjectProperty<>(ResourceUpdateState.IDLE);
    private final SimpleStringProperty resourceStatusText = new SimpleStringProperty();
    private final SimpleStringProperty resourceVersionText = new SimpleStringProperty();
    private final SimpleStringProperty updateActionText = new SimpleStringProperty();
    private final SimpleBooleanProperty resourceStatusVisible = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty resourceRetryVisible = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty updateActionVisible = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty resourceProgress = new SimpleDoubleProperty(0);
    private final SimpleStringProperty resourceProgressText = new SimpleStringProperty("0%");
    private final SimpleBooleanProperty resourceProgressVisible = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty resourcePauseVisible = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty resourceResumeVisible = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty resourceCancelVisible = new SimpleBooleanProperty(false);
    /** 当前已绑定进度的更新任务，避免重复绑定 listener。 */
    private GameResourceUpdateTask boundUpdateTask;
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
        // 回到首页时，若已有进行中的更新任务（可能由资源管理页发起），直接接管其进度展示。
        GameResourceUpdateTask running = currentUpdateTask();
        if (running != null && running.isRunning()) {
            return;
        }
        checkGameResourceUpdate();
    }

    @Override
    public void onViewRemoved() {
        serverSwitchCoordinator.currentSourceProperty().removeListener(serverSourceListener);
    }

    /** 首页创建后后台检查远端资源版本，不阻塞页面和游戏启动。 */
    public void checkGameResourceUpdate() {
        GameResourceCheckTask task = new GameResourceCheckTask(updateService);
        resourceUpdateState.set(ResourceUpdateState.CHECKING);
        resourceStatusText.set(LanguageManager.getString("ui.home.resource.checking"));
        resourceStatusVisible.set(true);
        task.setOnSucceeded(event -> {
            ResourceCheckResult result = task.getValue();
            checkResult = result;
            if (result == null || !result.isSuccessful()) {
                resourceUpdateState.set(ResourceUpdateState.FAILED);
                resourceStatusText.set(LanguageManager.getString("ui.home.resource.check_failed"));
                return;
            }
            String current = hasText(result.installedVersion()) ? result.installedVersion() : "-";
            String latest = hasText(result.latestVersion()) ? result.latestVersion() : "-";
            boolean upToDate = result.state() == ResourceCheckState.UP_TO_DATE
                    || current.equalsIgnoreCase(latest);
            resourceUpdateState.set(upToDate
                    ? ResourceUpdateState.UP_TO_DATE
                    : ResourceUpdateState.UPDATE_AVAILABLE);
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
            resourceUpdateState.set(ResourceUpdateState.FAILED);
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

    public ReadOnlyBooleanProperty globalTargetReadyProperty() {
        return serverSwitchCoordinator.globalTargetReadyProperty();
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
        GameResourceUpdateTask task = currentUpdateTask();
        if (task == null) {
            task = new GameResourceUpdateTask(updateService);
            task.setCheckResult(checkResult);
            bindUpdateTask(task);
            taskManageService.submit(GameResourceUpdateTask.TASK_ID,
                    LanguageManager.getString("ui.home.resource.update_task"),
                    task, task, ManagedTask.TaskCategory.UPDATE);
        }
    }

    public void retryResourceUpdate() {
        checkGameResourceUpdate();
    }

    public void pauseResourceUpdate() {
        GameResourceUpdateTask task = currentUpdateTask();
        if (task != null) {
            task.pauseTask();
        }
    }

    public void resumeResourceUpdate() {
        GameResourceUpdateTask task = currentUpdateTask();
        if (task != null) {
            task.resumeTask();
        }
    }

    public void cancelResourceUpdate() {
        GameResourceUpdateTask task = currentUpdateTask();
        if (task != null) {
            task.cancelTask();
        }
    }

    /**
     * 从 {@link TaskManageService} 查询当前更新任务（通过固定 id 复用），
     * 首次遇到时绑定其进度/状态到本 VM 的属性，使两个界面共享同一任务。
     */
    private GameResourceUpdateTask currentUpdateTask() {
        ManagedTask managed = taskManageService.get(GameResourceUpdateTask.TASK_ID);
        if (managed == null) {
            return null;
        }
        GameResourceUpdateTask task = (GameResourceUpdateTask) managed.getTask();
        if (task != boundUpdateTask) {
            bindUpdateTask(task);
        }
        return task;
    }

    /**
     * 将 Task 内置属性绑定到 VM 属性，从 Task 的 state/title/message/paused/progress 派生所有展示状态。
     */
    private void bindUpdateTask(GameResourceUpdateTask task) {
        boundUpdateTask = task;
        // 状态派生：监听 state、title、paused 三个属性，统一派生 ResourceUpdateState
        ChangeListener<Object> stateDeriver = (obs, old, val) -> deriveUpdateState(task);
        task.stateProperty().addListener(stateDeriver);
        task.titleProperty().addListener(stateDeriver);
        task.pausedProperty().addListener(stateDeriver);
        // 进度、详情、按钮显隐：直接从 Task 属性派生
        task.progressProperty().addListener((o, a, n) -> {
            double v = n.doubleValue();
            if (v >= 0) {
                resourceProgress.set(v);
                resourceProgressText.set(String.format("%.1f%%", v * 100));
            }
        });
        task.messageProperty().addListener((o, a, n) -> {
            if (n != null && !n.isBlank()) {
                resourceVersionText.set(n);
            }
        });
        // 同步初始值
        deriveUpdateState(task);
        resourceProgress.set(task.getProgress() >= 0 ? task.getProgress() : 0);
        if (task.getMessage() != null && !task.getMessage().isBlank()) {
            resourceVersionText.set(task.getMessage());
        }
    }

    /** 从 Task 的 state + title + paused 派生 ResourceUpdateState。 */
    private void deriveUpdateState(GameResourceUpdateTask task) {
        Worker.State state = task.getState();
        boolean paused = task.isPaused();
        String title = task.getTitle();

        if (state == Worker.State.SUCCEEDED) {
            resourceUpdateState.set(ResourceUpdateState.COMPLETED);
            updateProgressVisibility(false);
            updateControlVisibility(false, false, false);
            return;
        }
        if (state == Worker.State.FAILED) {
            resourceUpdateState.set(ResourceUpdateState.FAILED);
            updateProgressVisibility(false);
            updateControlVisibility(false, false, false);
            resourceRetryVisible.set(true);
            resourceStatusVisible.set(true);
            resourceStatusText.set(title != null ? title : "");
            return;
        }
        if (state == Worker.State.CANCELLED) {
            resourceUpdateState.set(ResourceUpdateState.CANCELED);
            updateProgressVisibility(false);
            updateControlVisibility(false, false, false);
            resourceStatusVisible.set(true);
            resourceStatusText.set(title != null ? title : "");
            return;
        }

        // state == RUNNING（或 SCHEDULED/READY）
        if (paused) {
            resourceUpdateState.set(ResourceUpdateState.PAUSED);
            resourceStatusText.set(title != null ? title : "");
            updateControlVisibility(true, false, true);
            updateProgressVisibility(true);
            return;
        }

        // 根据 title 判断当前阶段
        ResourceUpdateState derived = ResourceUpdateState.PREPARING;
        if (title != null) {
            if (title.contains("检查")) derived = ResourceUpdateState.CHECKING;
            else if (title.contains("准备")) derived = ResourceUpdateState.PREPARING;
            else if (title.contains("下载")) derived = ResourceUpdateState.DOWNLOADING;
            else if (title.contains("合成")) derived = ResourceUpdateState.APPLYING;
        }
        resourceUpdateState.set(derived);
        resourceStatusText.set(title != null ? title : "");
        boolean isDownloading = derived == ResourceUpdateState.DOWNLOADING;
        boolean isApplying = derived == ResourceUpdateState.APPLYING;
        updateControlVisibility(isDownloading, false, !isApplying);
        updateProgressVisibility(true);
    }

    private void updateControlVisibility(boolean pause, boolean resume, boolean cancel) {
        resourcePauseVisible.set(pause);
        resourceResumeVisible.set(resume);
        resourceCancelVisible.set(cancel);
    }

    private void updateProgressVisibility(boolean visible) {
        resourceProgressVisible.set(visible);
        if (!visible) {
            resourceProgress.set(0);
            resourceProgressText.set("0%");
        }
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
                List<String> paramsList = new ArrayList<>(installationManager.activeStartUpParams());
                if (!paramsList.isEmpty()) {
                    paramsList.addFirst(exe.getAbsolutePath());
                    String[] newArray = new String[paramsList.size()];
                    paramsList.toArray(newArray);
                    runExeByCustom(newArray);
                } else { //无自定义参数时走默认启动
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


    private void hideMainWindow() {
        if (Config.setting().isHideWhenGameStart()) {
            WwtApp.getWindow().hide();
        }
    }

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

    public ReadOnlyObjectProperty<ResourceUpdateState> resourceUpdateStateProperty() {
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
        return resourceProgress;
    }

    public ReadOnlyStringProperty resourceProgressTextProperty() {
        return resourceProgressText;
    }

    public ReadOnlyBooleanProperty resourceProgressVisibleProperty() {
        return resourceProgressVisible;
    }

    public ReadOnlyBooleanProperty resourcePauseVisibleProperty() {
        return resourcePauseVisible;
    }

    public ReadOnlyBooleanProperty resourceResumeVisibleProperty() {
        return resourceResumeVisible;
    }

    public ReadOnlyBooleanProperty resourceCancelVisibleProperty() {
        return resourceCancelVisible;
    }
}