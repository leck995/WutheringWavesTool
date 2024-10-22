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
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.user.BoxInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.user.RoleDailyData;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.user.RoleInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.api.SignTask;
import cn.tealc.wutheringwavestool.thread.api.UserDailyDataTask;
import cn.tealc.wutheringwavestool.thread.api.UserDataRefreshTask;
import cn.tealc.wutheringwavestool.thread.api.UserInfoDataTask;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 19:57
 */
public class HomeViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(HomeViewModel.class);private SignUserInfo userInfo;
    private SimpleStringProperty energyText = new SimpleStringProperty();
    private SimpleStringProperty energyTimeText = new SimpleStringProperty();
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
    private SimpleBooleanProperty hasSign = new SimpleBooleanProperty(false);
    private SimpleStringProperty signText = new SimpleStringProperty();
    public HomeViewModel() {
        updateRoleData();
        updateGameTime(GameAppListener.getInstance().getDuration());
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.HOME_GAME_TIME_UPDATE, (s, objects) -> {
            long playTime = (long) objects[0];
            updateGameTime(playTime);
            updateRoleData();
        });
    }

    /**
     * @description: 更新游玩时长
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
     * @description: 刷新角色数据
     * @param:
     * @return  void
     * @date:   2024/10/8
     */
    public void updateRoleData() {
        if (Config.setting.isNoKuJieQu()){
            hasSign.set(false);
            return;
        }

        if (userInfo != null) {
            UserDataRefreshTask task = new UserDataRefreshTask(userInfo);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<String> responseBody = task.getValue();
                if (responseBody.getCode() == 200) {
                    getDailyData();
                    getRoleData();
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING, responseBody.getMsg()));
                }
            });
            Thread.startVirtualThread(task);

            if (Config.setting.isAutoKujieQuSign()){
                sign();
            }

        } else {
            UserInfoDao dao = new UserInfoDao();
            userInfo = dao.getMain();
            if (userInfo != null) {
                Config.currentRoleId = userInfo.getRoleId();
                updateRoleData();
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type01")));
            }
        }
    }

    private void getRoleData() {
        UserInfoDataTask userInfoDataTask = new UserInfoDataTask(userInfo);
        userInfoDataTask.setOnSucceeded(workerStateEvent -> {
            ResponseBody<RoleInfo> responseBody = userInfoDataTask.getValue();
            if (responseBody.getCode() == 200) {
                RoleInfo roleInfo = responseBody.getData();
                roleNameText.set(roleInfo.getName());
                String template = LanguageManager.getString("ui.home.label.role.day");
                gameLifeText.set(String.format(template, roleInfo.getActiveDays()));
                levelText.set(String.format("LV.%d", roleInfo.getLevel()));
                String[] chests = LanguageManager.getStringArray("ui.home.label.chest.types");
                for (BoxInfo boxInfo : roleInfo.getBoxList()) {
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
                rolePaneVisible.set(true);
            } else {
                rolePaneVisible.set(false);
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, responseBody.getMsg()), false);
            }
        });
        Thread.startVirtualThread(userInfoDataTask);
    }

    private void getDailyData() {
        UserDailyDataTask userDailyDataTask = new UserDailyDataTask(userInfo);
        userDailyDataTask.setOnSucceeded(workerStateEvent -> {
            ResponseBody<RoleDailyData> responseBody = userDailyDataTask.getValue();
            if (responseBody != null) {
                if (responseBody.getCode() == 200){
                    RoleDailyData data = responseBody.getData();
                    energyText.set(String.format("%d/%d", data.getEnergyData().getCur(), data.getEnergyData().getTotal()));

                    String[] strengths = LanguageManager.getStringArray("ui.home.label.daily.strength");
                    if (data.getEnergyData().getRefreshTimeStamp() == 0) { //体力已
                        energyTimeText.set(strengths[2]);
                    } else {
                        long timestamp = data.getEnergyData().getRefreshTimeStamp() * 1000; // 仅为示例，实际应替换为具体的时间戳
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
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING, responseBody.getMsg()));
                }
            }
        });
        Thread.startVirtualThread(userDailyDataTask);


    }


    public void startUpdate() {
        if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type02")), false);
        } else {
            String dir = Config.setting.getGameRootDir();
            if (dir != null) {
                File exe = new File(dir + File.separator + "launcher.exe");
                if (exe.exists()) {
                    try {
                        Desktop.getDesktop().open(exe);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING, String.format(LanguageManager.getString("ui.home.message.type03"), exe.getPath()), false));
                }
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type04")), false);
            }
        }
    }





    public void signAndGame() {
        sign();
        startGame();
    }


    public void sign(){
        SignTask task = new SignTask();
        task.setOnSucceeded(workerStateEvent -> {
            hasSign.set(true);
            signText.set(LanguageManager.getString("ui.home.label.sign.yes"));
        });
        Thread.startVirtualThread(task);
        signText.set(LanguageManager.getString("ui.home.label.sign.ing"));
    }

    public void startGame() {
        String dir = Config.setting.getGameRootDir();
        if (dir != null) {
            File exe = null;
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
            }else {
                if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
                    exe = new File(dir + File.separator + "Wuthering Waves.exe");
                } else {
                    exe = new File(dir + File.separator + "Wuthering Waves Game" + File.separator + "Wuthering Waves.exe");
                }
            }

            if (exe.exists()) {
                if (Config.setting.isUserAdvanceGameSettings()){ //使用高级启动设置
                    String appParams = Config.setting.getAppParams();
                    if (appParams != null && !appParams.isEmpty()) {
                        String[] arrays = appParams.split(" ");
                        String[] newArray = new String[arrays.length + 1];
                        newArray[0] = exe.getAbsolutePath();
                        // 复制原数组的元素到新数组
                        System.arraycopy(arrays, 0, newArray, 1, arrays.length);
                        for (String s : newArray) {
                            System.out.println(s);
                        }
                        runExeByCustom(newArray);
                    }else {
                        runExeByCustom(exe.getAbsolutePath());
                    }
                    //runExeByCustom(exe.getAbsolutePath(),"-dx11","-SkipSplash");
                }else {
                    runExe(exe);
                }
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
     * @description: 第一个参数必须是启动器的路径
     * @param:	params
     * @return  void
     * @date:   2024/10/17
     */
    private void runExeByCustom(String... params) {
        Thread.startVirtualThread(()->{
            ProcessBuilder processBuilder = new ProcessBuilder(params);
            try {
                processBuilder.start();
                //process.waitFor();目前的时长统计已不再需要该方法，仅作念想
            } catch (IOException e) {
                LOG.info("无法启动程序,进行提权启动");
                String[] command2 = {"cmd.exe", "/c", "start", "\"\""}; //权限不够，提权
                String[] mergedArray = Stream.concat(Stream.of(command2), Stream.of(params))
                        .toArray(String[]::new);
                ProcessBuilder processBuilder2 = new ProcessBuilder(mergedArray);
                try {
                    processBuilder2.start();
                } catch (IOException ex) {
                    LOG.error("提权后依然无法启动程序",e);
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.ERROR,"自定义参数方式启动失败:"+e.getMessage()));
                }
            }

        });
    }


    private void runExe(File exe){
        try {
            Desktop.getDesktop().open(exe);
            if (Config.setting.isHideWhenGameStart()) {
                MainApplication.window.hide();
            }
        } catch (IOException e) {
            LOG.info("启动游戏错误", e);
            MainApplication.window.show();
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
}