package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.WwtApp;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.service.GameDownloadService;
import cn.tealc.wutheringwavestool.service.GameTimeService;
import cn.tealc.wutheringwavestool.service.TaskManageService;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.thread.SignTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import com.kr.launcher.config.LauncherDownloadConfigHelper;
import com.kr.launcher.config.ResourceConfigManager;
import com.kr.launcher.model.LauncherDownloadConfig;
import com.kuro.kujiequ.model.roleData.user.RoleInfo;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
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

    public enum ResourceUpdateState {
        HIDDEN,
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        FAILED
    }

    @Inject
    private GameTimeService gameTimeService;
    @Inject
    private GameDownloadService gameDownloadService;
    @Inject
    private TaskManageService taskManageService;
    private SimpleStringProperty gameTimeText = new SimpleStringProperty();
    private SimpleStringProperty gameTimeTipText = new SimpleStringProperty();
    private SimpleBooleanProperty startGameBtnDisabled = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<ResourceUpdateState> resourceUpdateState =
            new SimpleObjectProperty<>(ResourceUpdateState.HIDDEN);
    private final SimpleBooleanProperty resourceStatusVisible = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty resourceRetryVisible = new SimpleBooleanProperty(false);
    private final SimpleStringProperty resourceStatusText = new SimpleStringProperty();
    private final SimpleStringProperty resourceVersionText = new SimpleStringProperty();
    private final SimpleStringProperty updateActionText = new SimpleStringProperty();
    private Task<ResourceVersionCheck> resourceCheckTask;

    private record ResourceVersionCheck(String currentVersion, String latestVersion) {
        boolean updateAvailable() {
            return !currentVersion.equalsIgnoreCase(latestVersion);
        }
    }

    public void initialize() {
        updateActionText.set(LanguageManager.getString("ui.home.button.start_update"));
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
        checkGameResourceUpdate();
    }

    @Override
    public void onViewRemoved() {
        Task<ResourceVersionCheck> task = resourceCheckTask;
        resourceCheckTask = null;
        if (task != null) {
            task.cancel(true);
        }
    }

    /** 首页创建后后台检查远端资源版本，不阻塞页面和游戏启动。 */
    public void checkGameResourceUpdate() {
        if (resourceCheckTask != null) {
            return;
        }
        if (GameResourcesManager.getGameExeBase() == null) {
            setResourceUpdateState(ResourceUpdateState.HIDDEN);
            return;
        }

        setResourceUpdateState(ResourceUpdateState.CHECKING);
        resourceStatusText.set(LanguageManager.getString("ui.home.resource.checking"));
        resourceVersionText.set(LanguageManager.getString("ui.home.resource.checking_detail"));
        updateActionText.set(LanguageManager.getString("ui.home.button.start_update"));

        SourceType configuredSource = Config.setting().getGameRootDirSource();
        SourceType source = configuredSource != null ? configuredSource : SourceType.DEFAULT;
        Task<ResourceVersionCheck> task = new Task<>() {
            @Override
            protected ResourceVersionCheck call() {
                updateTitle(LanguageManager.getString("ui.home.resource.task"));
                var response = gameDownloadService.getLauncherResource(source);
                if (response == null || response.getCode() != 200 || response.getData() == null
                        || response.getData().getUpdateData() == null) {
                    throw new IllegalStateException("获取游戏资源版本失败");
                }
                String latestVersion = response.getData().getUpdateData().getVersion();
                String currentVersion = readInstalledVersion();
                if (!hasText(latestVersion)) {
                    throw new IllegalStateException("远端游戏资源版本为空");
                }
                if (!hasText(currentVersion)) {
                    throw new IllegalStateException("无法读取本地游戏资源版本");
                }
                return new ResourceVersionCheck(currentVersion, latestVersion);
            }
        };
        resourceCheckTask = task;
        task.setOnSucceeded(event -> {
            if (!finishResourceCheck(task)) {
                return;
            }
            applyResourceVersionCheck(task.getValue());
        });
        task.setOnFailed(event -> {
            if (!finishResourceCheck(task)) {
                return;
            }
            LOG.warn("首页检查游戏资源更新失败", task.getException());
            showResourceCheckFailure();
        });
        task.setOnCancelled(event -> finishResourceCheck(task));
        taskManageService.execute(task);
    }

    private boolean finishResourceCheck(Task<ResourceVersionCheck> task) {
        if (resourceCheckTask != task) {
            return false;
        }
        resourceCheckTask = null;
        return true;
    }

    private void applyResourceVersionCheck(ResourceVersionCheck check) {
        if (check.updateAvailable()) {
            setResourceUpdateState(ResourceUpdateState.UPDATE_AVAILABLE);
            resourceStatusText.set(LanguageManager.getString("ui.home.resource.update_available"));
            resourceVersionText.set(String.format(
                    LanguageManager.getString("ui.home.resource.version_diff"),
                    check.currentVersion(), check.latestVersion()));
            updateActionText.set(String.format(
                    LanguageManager.getString("ui.home.button.update_to"), check.latestVersion()));
        } else {
            setResourceUpdateState(ResourceUpdateState.UP_TO_DATE);
            resourceStatusText.set(LanguageManager.getString("ui.home.resource.up_to_date"));
            resourceVersionText.set(String.format(
                    LanguageManager.getString("ui.home.resource.current_version"),
                    check.currentVersion()));
            updateActionText.set(LanguageManager.getString("ui.home.button.start_update"));
        }
    }

    private void showResourceCheckFailure() {
        setResourceUpdateState(ResourceUpdateState.FAILED);
        resourceStatusText.set(LanguageManager.getString("ui.home.resource.check_failed"));
        resourceVersionText.set(LanguageManager.getString("ui.home.resource.check_failed_detail"));
        updateActionText.set(LanguageManager.getString("ui.home.button.start_update"));
    }

    private void setResourceUpdateState(ResourceUpdateState state) {
        resourceUpdateState.set(state);
        resourceStatusVisible.set(state != ResourceUpdateState.HIDDEN);
        resourceRetryVisible.set(state == ResourceUpdateState.FAILED);
    }

    private String readInstalledVersion() {
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir != null) {
            File configFile = new File(gameDir, ResourceConfigManager.LAUNCHER_DOWNLOAD_CONFIG);
            LauncherDownloadConfig localConfig = LauncherDownloadConfigHelper.get(configFile.getAbsolutePath());
            if (localConfig != null && hasText(localConfig.version)) {
                return localConfig.version;
            }
        }
        String cachedVersion = Config.setting().getGameInstalledVersion();
        return hasText(cachedVersion) ? cachedVersion : "";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
        if (Config.setting().getGameRootDirSource() == SourceType.WE_GAME) {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    MessageInfo.warning(LanguageManager.getString("ui.home.message.type02")), false);
            return;
        }
        String dir = Config.setting().getGameRootDir();
        if (dir == null) {
            warnLauncherNotFound(null);
            return;
        }

        // 优先使用自定义更新器
        String updaterPath = Config.setting().getGameOfficialLauncherDir();
        if (updaterPath != null && !updaterPath.isEmpty()) {
            File updater = new File(updaterPath);
            if (updater.exists()) {
                try {
                    launchExe(updater);
                } catch (IOException e) {
                    LOG.warn("启动自定义更新器失败: {}", e.getMessage());
                }
            }
            return;
        }

        // 回退到安装目录下的 launcher.exe
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir != null) {
            File exe = new File(gameDir.getParentFile(), "launcher.exe");
            if (exe.exists()) {
                try {
                    launchExe(exe);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                warnLauncherNotFound(exe.getPath());
            }
        } else {
            warnLauncherNotFound(null);
        }
    }

    private void warnLauncherNotFound(String path) {
        String msg = path != null
                ? String.format(LanguageManager.getString("ui.home.message.type08"), path)
                : LanguageManager.getString("ui.home.message.type08");
        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.warning(msg), false);
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
                    List<String> paramsList = new ArrayList<String>(Config.setting().getStartUpParams());
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


    private void launchExe(File exe) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(exe.getAbsolutePath());
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.start();
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

    public SimpleObjectProperty<ResourceUpdateState> resourceUpdateStateProperty() {
        return resourceUpdateState;
    }

    public SimpleBooleanProperty resourceStatusVisibleProperty() {
        return resourceStatusVisible;
    }

    public SimpleBooleanProperty resourceRetryVisibleProperty() {
        return resourceRetryVisible;
    }

    public SimpleStringProperty resourceStatusTextProperty() {
        return resourceStatusText;
    }

    public SimpleStringProperty resourceVersionTextProperty() {
        return resourceVersionText;
    }

    public SimpleStringProperty updateActionTextProperty() {
        return updateActionText;
    }
}
