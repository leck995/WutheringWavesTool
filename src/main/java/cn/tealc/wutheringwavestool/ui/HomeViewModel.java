package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.kuro.kujiequ.model.roleData.user.BoxInfo;
import com.kuro.kujiequ.model.roleData.user.RoleDailyData;
import com.kuro.kujiequ.model.roleData.user.RoleInfo;
import com.kuro.kujiequ.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.SignTask;
import com.kuro.kujiequ.thread.UserDailyDataTask;
import com.kuro.kujiequ.thread.UserDataRefreshTask;
import com.kuro.kujiequ.thread.UserInfoDataTask;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 19:57
 */
public class HomeViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(HomeViewModel.class);
    private SimpleStringProperty energyText = new SimpleStringProperty();
    private SimpleStringProperty energyTimeText = new SimpleStringProperty();

    private SimpleStringProperty weeklyInstCountText= new SimpleStringProperty();
    private SimpleStringProperty storeEnergyText = new SimpleStringProperty();
    private SimpleStringProperty livenessText = new SimpleStringProperty();
    private SimpleStringProperty battlePassLevelText = new SimpleStringProperty();
    private SimpleStringProperty battlePassNumText = new SimpleStringProperty();
    private SimpleDoubleProperty battlePassProgress = new SimpleDoubleProperty();
    private SimpleBooleanProperty rolePaneVisible = new SimpleBooleanProperty(false);
    private SimpleStringProperty roleNameText = new SimpleStringProperty();
    private SimpleStringProperty gameLifeText = new SimpleStringProperty();
    private SimpleStringProperty levelText = new SimpleStringProperty();
    private SimpleStringProperty box1Text = new SimpleStringProperty();
    private SimpleStringProperty box2Text = new SimpleStringProperty();
    private SimpleStringProperty box3Text = new SimpleStringProperty();
    private SimpleStringProperty box4Text = new SimpleStringProperty();
    private SimpleStringProperty gameTimeText = new SimpleStringProperty();
    private SimpleStringProperty gameTimeTipText = new SimpleStringProperty();
    private SimpleObjectProperty<Image> headImg = new SimpleObjectProperty<>();
    private SimpleBooleanProperty hasSign = new SimpleBooleanProperty(true);
    private SimpleStringProperty signText = new SimpleStringProperty();

    private SimpleStringProperty weeklyRougeText = new SimpleStringProperty();

    private SimpleBooleanProperty startGameBtnDisabled = new SimpleBooleanProperty(false);

    public HomeViewModel() {
        updateKujiequRoleData();
        updateGameTime(GameAppListener.getInstance().getDuration());
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.HOME_GAME_TIME_UPDATE, (s, objects) -> {
            if (objects.length > 0){
                long playTime = (long) objects[0];
                updateGameTime(playTime);
                updateKujiequRoleData();
            }else {
                updateGameTime(0);
                updateKujiequRoleData();
            }
        });
    }






    /**
     * @description: 更新游玩时长；会对数据库与time进行相加处理，并显示
     * @param:	time	尚未保存到数据库中的时长
     * @return  void
     * @date:   2024/10/8
     */
    private void updateGameTime(long time) {
        List<GameTime> list = getGameTimes();
        if (list != null) {
            long sum = list.stream().mapToLong(GameTime::getDuration).sum() + time;
            updateGameTimeText(sum);
        }
    }

    /**
     * @description: 获取数据库中的当天游玩时长时间
     * @return  java.util.List<cn.tealc.wutheringwavestool.model.game.GameTime>
     * @date:   2024/10/8
     */
    private List<GameTime> getGameTimes() {
        GameTimeDao gameTimeDao = new GameTimeDao();
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = dateTimeFormatter.format(localDate);
        return gameTimeDao.getTimeListByData(date);
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






    /**
     * @description: 刷新库街区的角色数据
     * @param:
     * @return  void
     * @date:   2024/10/8
     */
    public void updateKujiequRoleData() {
        if (Config.setting.isNoKuJieQu()){
            hasSign.set(true);
            return;
        }

        UserInfoDao dao = new UserInfoDao();
        UserInfo userInfo = dao.getMain();
        if (userInfo == null){
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type01")));
            return;
        }

        Config.currentRoleId = userInfo.getRoleId();

        UserDataRefreshTask task = new UserDataRefreshTask(userInfo);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<String> responseBody = task.getValue();
            if (responseBody.getCode() == 200) {
                getDailyData(userInfo);
                getRoleData(userInfo);
            }else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, responseBody.getMsg()));
                LOG.error(responseBody.getMsg());
            }
        });
        Thread.startVirtualThread(task);

        if (Config.setting.isAutoKujieQuSign()){
            startKujiequDailySign();
        }
    }


    /**
     * 获取角色的基本数据，如宝箱数量
     * @param userInfo
     */
    private void getRoleData(UserInfo userInfo) {
        UserInfoDataTask userInfoDataTask = new UserInfoDataTask(userInfo);
        userInfoDataTask.setOnSucceeded(workerStateEvent -> {
            ResponseBody<RoleInfo> responseBody = userInfoDataTask.getValue();
            if (responseBody.getCode() == 200) {
                RoleInfo roleInfo = responseBody.getData();
                roleNameText.set(roleInfo.getName());
                String template = LanguageManager.getString("ui.home.label.role.day");
                gameLifeText.set(String.format(template, roleInfo.getActiveDays()));
                levelText.set(String.format("LV.%d", roleInfo.getLevel()));

                weeklyInstCountText.set(String.format("%d/%d",roleInfo.getWeeklyInstCount(),roleInfo.getWeeklyInstCountLimit()));
                storeEnergyText.set(String.format("%d/%d",roleInfo.getStoreEnergy(),roleInfo.getStoreEnergyLimit()));

                String[] chests = LanguageManager.getStringArray("ui.home.label.chest.types");
                for (BoxInfo boxInfo : roleInfo.getTreasureBoxList()) {
                    if (boxInfo.getBoxName().equals(chests[0])) {
                        box1Text.set(String.valueOf(boxInfo.getNum()));
                    } else if (boxInfo.getBoxName().equals(chests[1])) {
                        box2Text.set(String.valueOf(boxInfo.getNum()));
                    } else if (boxInfo.getBoxName().equals(chests[2])) {
                        box3Text.set(String.valueOf(boxInfo.getNum()));
                    } else if (boxInfo.getBoxName().equals(chests[3])) {
                        box4Text.set(String.valueOf(boxInfo.getNum()));
                    }
                }


                double rouge = (double) roleInfo.getRougeScore() / (double) roleInfo.getRougeScoreLimit();
                weeklyRougeText.set(String.format("%2.0f%%",rouge));

                rolePaneVisible.set(true);


                onWeekEnd(false,roleInfo);
            } else {
                rolePaneVisible.set(false);
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, responseBody.getMsg()), false);
            }
        });
        Thread.startVirtualThread(userInfoDataTask);
    }

    /**
     * 获取角色的日常数据，如体力;鉴于getRoleData方法每次都是同时调用，故失败请求不再弹出消息显示。
     * @param userInfo
     */
    private void getDailyData(UserInfo userInfo) {
        UserDailyDataTask userDailyDataTask = new UserDailyDataTask(userInfo);
        userDailyDataTask.setOnSucceeded(workerStateEvent -> {
            ResponseBody<RoleDailyData> responseBody = userDailyDataTask.getValue();
            if (responseBody != null) {
                if (responseBody.getCode() == 200){
                    RoleDailyData data = responseBody.getData();
                    energyText.set(String.format("%d/%d", data.getEnergyData().getCur(), data.getEnergyData().getTotal()));

                    String[] strengths = LanguageManager.getStringArray("ui.home.label.daily.strength");
                    if (data.getEnergyData().getRefreshTimeStamp() == 0) { //体力
                        energyTimeText.set(strengths[2]);
                    } else {
                        long timestamp = data.getEnergyData().getRefreshTimeStamp() * 1000;
                        Date date = new Date(timestamp);
                        Instant instant = Instant.ofEpochMilli(timestamp);
                        LocalDate dateFromTimestamp = instant.atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalDate currentDate = LocalDate.now();
                        boolean isSameDay = dateFromTimestamp.equals(currentDate);
                        if (isSameDay) { //今日体力满时间
                            SimpleDateFormat formatter = new SimpleDateFormat(strengths[0]);
                            energyTimeText.set(formatter.format(date));
                        } else { //明日体力满时间
                            SimpleDateFormat formatter = new SimpleDateFormat(strengths[1]);
                            energyTimeText.set(formatter.format(date));
                        }
                    }
                    hasSign.set(data.isHasSignIn());
                    livenessText.set(String.valueOf(data.getLivenessData().getCur()));
                    battlePassLevelText.set(String.format(" LV.%02d", data.getBattlePassData().getFirst().getCur()));
                    battlePassNumText.set(String.format("%d/%d", data.getBattlePassData().get(1).getCur(), data.getBattlePassData().get(1).getTotal()));
                    double cur = data.getBattlePassData().get(1).getCur();
                    double total = data.getBattlePassData().get(1).getTotal();
                    battlePassProgress.set(cur / total);
                    rolePaneVisible.set(true);
                }else {
                    rolePaneVisible.set(false);
                }
            }
        });
        Thread.startVirtualThread(userDailyDataTask);
    }



    public void checkIsWeekEnd(){
        if (Config.setting.getGameRootDirSource() == SourceType.GLOBAL){
            Platform.runLater(()->{
                onWeekEnd(true,null);
            });

        }
    }

    /**
     * 检查每周最后一天，并进行提醒完成周活动
     * @param isGlobal
     * @param roleInfo
     */
    private void onWeekEnd(boolean isGlobal,RoleInfo roleInfo){
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            if (isGlobal) {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.label.weekly.message01"),false));
            }else {
                if (roleInfo == null) {
                    return;
                }
                if (roleInfo.getWeeklyInstCount() < 3){
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.label.weekly.message02"),false));
                }
                if (roleInfo.getRougeScore() < 5000){
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.label.weekly.message03"),false));
                }
            }
        }
    }



    public void startUpdate() {
        if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type02")), false);
        } else {
            String dir = Config.setting.getGameRootDir();
            if (dir != null) {
                File gameDir = GameResourcesManager.getGameDir();
                if (gameDir != null) {
                    File parent = gameDir.getParentFile();
                    File exe = new File(parent,"launcher.exe");
                    if (exe.exists()) {
                        try {
                            Desktop.getDesktop().open(exe);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                new MessageInfo(MessageType.WARNING, String.format(LanguageManager.getString("ui.home.message.type08"), exe.getPath()), false));
                    }
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type08")), false);
                }
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type08")), false);
            }
        }
    }


    /**
     * 签到并启动鸣潮
     */
    public void signAndGame() {
        startKujiequDailySign();
        startGame();
    }


    /**
     * 开始进行库街区鸣潮签到
     */
    public void startKujiequDailySign(){
        SignTask task = new SignTask();
        task.setOnSucceeded(workerStateEvent -> {
            hasSign.set(true);
            signText.set(LanguageManager.getString("ui.home.label.sign.yes"));
        });
        Thread.startVirtualThread(task);
        signText.set(LanguageManager.getString("ui.home.label.sign.ing"));
    }


    /**
     * 启动鸣潮，先删除旧日志，然后判断是否启动参数，并进行启动
     */
    public void startGame() {
        //先设置1s的禁止点击，防止双击启动
        PauseTransition pauseTransition = new PauseTransition(Duration.seconds(1));
        startGameBtnDisabled.set(true);
        pauseTransition.setOnFinished(event -> {
            startGameBtnDisabled.set(false);
        });
        pauseTransition.play();

        //删除游戏过去的日志，避免数据污染
        deleteLogFiles();

        String dir = Config.setting.getGameRootDir();
        if (dir != null) {
            File exe = null;
            //当自定义启动程序时
            if (Config.setting.isGameStartAppCustom()){
                exe=new File(Config.setting.getGameStarAppPath());
                if (!exe.exists()) {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING,
                                    String.format(
                                            LanguageManager.getString("ui.home.message.type05"),
                                            exe.getPath()
                                    )));
                    return;
                }
            }else { //默认启动程序Wuthering Waves.exe
                exe = GameResourcesManager.getGameExeBase();
            }

            if (exe != null) {
                if (Config.setting.isUserAdvanceGameSettings()){ //使用高级启动设置
                    List<String> paramsList = new ArrayList<String>(Config.setting.getStartUpParams());
                    if (!paramsList.isEmpty()) {
                        paramsList.addFirst(exe.getAbsolutePath());
                        String[] newArray = new String[paramsList.size()];
                        paramsList.toArray(newArray);
                        runExeByCustom(newArray);
                    }else {
                        runExe(exe);
                    }
                }else { //默认启动
                    runExe(exe);
                }
                hideMainWindow();
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, String.format(LanguageManager.getString("ui.home.message.type03"), exe.getPath())));
            }
        } else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type04")));
        }
    }


    /**
     * @description: 启动时隐藏窗口
     * @param:
     * @return  void
     * @date:   2024/11/16
     */
    private void hideMainWindow() {
        if (Config.setting.isHideWhenGameStart()){
            MainApplication.window.hide();
        }
    }

    /**
     * @description: 第一个参数必须是启动器的路径
     * @param:	params
     * @return  void
     * @date:   2024/10/17
     */
    private void runExeByCustom(String... params) {
        Thread.startVirtualThread(()->{
            String[] command2 = {"cmd.exe", "/c", "start", "\"\""}; //权限不够，提权
            String[] mergedArray = Stream.concat(Stream.of(command2), Stream.of(params))
                    .toArray(String[]::new);
            ProcessBuilder processBuilder = new ProcessBuilder(mergedArray);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
            String path = params[0];
            if (path.contains("WWMI Loader.exe")){
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
                MainApplication.window.show();
                GameAppListener.getInstance().setStartFromApp(false);
                LOG.error("高级启动无法启动鸣潮",e);
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.ERROR,LanguageManager.getString("ui.home.message.type07")+e.getMessage()));
            }
        });
    }


    /**
     * @description: 默认启动
     * @param:	exe
     * @return  void
     * @date:   2024/11/16
     */
    private void runExe(File exe){
        try {
            GameAppListener.getInstance().setStartFromApp(true);
            Desktop.getDesktop().open(exe);
            if (Config.setting.isHideWhenGameStart()) {
                MainApplication.window.hide();
            }
        } catch (IOException e) {
            GameAppListener.getInstance().setStartFromApp(false);
            LOG.info("启动游戏错误:{}", e.getMessage());
            MainApplication.window.show();
        }
    }


    /**
     * @description: 删除游戏日志，用于保证每次启动日志都是最新的，不重复的
     * @param:
     * @return  void
     * @date:   2024/11/16
     */
    private void deleteLogFiles(){
        File dir = GameResourcesManager.getGameLogDir();
        if (dir != null) {
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.stream(files).forEach(File::delete);
            }
        }
    }



    public String getEnergyText() {
        return energyText.get();
    }

    public void setEnergyText(String energyText) {
        this.energyText.set(energyText);
    }

    public SimpleStringProperty energyTextProperty() {
        return energyText;
    }

    public String getLivenessText() {
        return livenessText.get();
    }

    public void setLivenessText(String livenessText) {
        this.livenessText.set(livenessText);
    }

    public SimpleStringProperty livenessTextProperty() {
        return livenessText;
    }

    public String getBattlePassLevelText() {
        return battlePassLevelText.get();
    }

    public void setBattlePassLevelText(String battlePassLevelText) {
        this.battlePassLevelText.set(battlePassLevelText);
    }

    public SimpleStringProperty battlePassLevelTextProperty() {
        return battlePassLevelText;
    }

    public String getBattlePassNumText() {
        return battlePassNumText.get();
    }

    public void setBattlePassNumText(String battlePassNumText) {
        this.battlePassNumText.set(battlePassNumText);
    }

    public SimpleStringProperty battlePassNumTextProperty() {
        return battlePassNumText;
    }

    public double getBattlePassProgress() {
        return battlePassProgress.get();
    }

    public void setBattlePassProgress(double battlePassProgress) {
        this.battlePassProgress.set(battlePassProgress);
    }

    public SimpleDoubleProperty battlePassProgressProperty() {
        return battlePassProgress;
    }

    public String getEnergyTimeText() {
        return energyTimeText.get();
    }

    public void setEnergyTimeText(String energyTimeText) {
        this.energyTimeText.set(energyTimeText);
    }

    public SimpleStringProperty energyTimeTextProperty() {
        return energyTimeText;
    }

    public String getRoleNameText() {
        return roleNameText.get();
    }

    public void setRoleNameText(String roleNameText) {
        this.roleNameText.set(roleNameText);
    }

    public SimpleStringProperty roleNameTextProperty() {
        return roleNameText;
    }

    public String getGameLifeText() {
        return gameLifeText.get();
    }

    public void setGameLifeText(String gameLifeText) {
        this.gameLifeText.set(gameLifeText);
    }

    public SimpleStringProperty gameLifeTextProperty() {
        return gameLifeText;
    }

    public String getLevelText() {
        return levelText.get();
    }

    public void setLevelText(String levelText) {
        this.levelText.set(levelText);
    }

    public SimpleStringProperty levelTextProperty() {
        return levelText;
    }

    public String getBox1Text() {
        return box1Text.get();
    }

    public void setBox1Text(String box1Text) {
        this.box1Text.set(box1Text);
    }

    public SimpleStringProperty box1TextProperty() {
        return box1Text;
    }

    public String getBox2Text() {
        return box2Text.get();
    }

    public void setBox2Text(String box2Text) {
        this.box2Text.set(box2Text);
    }

    public SimpleStringProperty box2TextProperty() {
        return box2Text;
    }

    public String getBox3Text() {
        return box3Text.get();
    }

    public void setBox3Text(String box3Text) {
        this.box3Text.set(box3Text);
    }

    public SimpleStringProperty box3TextProperty() {
        return box3Text;
    }

    public String getBox4Text() {
        return box4Text.get();
    }

    public void setBox4Text(String box4Text) {
        this.box4Text.set(box4Text);
    }

    public SimpleStringProperty box4TextProperty() {
        return box4Text;
    }

    public Image getHeadImg() {
        return headImg.get();
    }

    public void setHeadImg(Image headImg) {
        this.headImg.set(headImg);
    }

    public SimpleObjectProperty<Image> headImgProperty() {
        return headImg;
    }

    public boolean isRolePaneVisible() {
        return rolePaneVisible.get();
    }

    public SimpleBooleanProperty rolePaneVisibleProperty() {
        return rolePaneVisible;
    }

    public String getGameTimeText() {
        return gameTimeText.get();
    }

    public void setGameTimeText(String gameTimeText) {
        this.gameTimeText.set(gameTimeText);
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

    public boolean isHasSign() {
        return hasSign.get();
    }

    public void setHasSign(boolean hasSign) {
        this.hasSign.set(hasSign);
    }

    public SimpleBooleanProperty hasSignProperty() {
        return hasSign;
    }

    public String getStoreEnergyText() {
        return storeEnergyText.get();
    }

    public SimpleStringProperty storeEnergyTextProperty() {
        return storeEnergyText;
    }

    public String getWeeklyInstCountText() {
        return weeklyInstCountText.get();
    }

    public SimpleStringProperty weeklyInstCountTextProperty() {
        return weeklyInstCountText;
    }

    public boolean isStartGameBtnDisabled() {
        return startGameBtnDisabled.get();
    }

    public SimpleBooleanProperty startGameBtnDisabledProperty() {
        return startGameBtnDisabled;
    }

    public String getWeeklyRougeText() {
        return weeklyRougeText.get();
    }

    public SimpleStringProperty weeklyRougeTextProperty() {
        return weeklyRougeText;
    }

    public void setWeeklyRougeText(String weeklyRougeText) {
        this.weeklyRougeText.set(weeklyRougeText);
    }
}