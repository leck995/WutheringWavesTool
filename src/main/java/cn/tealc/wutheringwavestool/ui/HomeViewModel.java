package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.user.BoxInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.user.RoleDailyData;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.user.RoleInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.thread.api.SignTask;
import cn.tealc.wutheringwavestool.thread.api.UserDailyDataTask;
import cn.tealc.wutheringwavestool.thread.api.UserDataRefreshTask;
import cn.tealc.wutheringwavestool.thread.api.UserInfoDataTask;
import cn.tealc.wutheringwavestool.thread.api.role.GameRoleDataSaveTask;
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

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 19:57
 */
public class HomeViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(HomeViewModel.class);
    private final String[] gameTips = {"今日还没有开始冒险", "只是上去做个每日任务", "等找完这个宝箱，我就休息", "小肝不算肝", "肝佬"};
    private SignUserInfo userInfo;
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
        if (hour == 0 && minute == 0) {
            gameTimeTipText.set(gameTips[0]);
        } else if (hour < 1 && minute < 15) {
            gameTimeTipText.set(gameTips[1]);
        } else if (hour <= 2) {
            gameTimeTipText.set(gameTips[2]);
        } else if (hour <= 5) {
            gameTimeTipText.set(gameTips[3]);
        } else {
            gameTimeTipText.set(gameTips[4]);
        }
        gameTimeText.set(String.format("今日在线 %d 小时 %d 分钟", hour, minute));
    }



    /**
     * @description: 刷新角色数据
     * @param:
     * @return  void
     * @date:   2024/10/8
     */
    public void updateRoleData() {
        if (Config.setting.isNoKuJieQu()){
            hasSign.set(true);
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
        } else {
            UserInfoDao dao = new UserInfoDao();
            userInfo = dao.getMain();
            if (userInfo != null) {
                Config.currentRoleId = userInfo.getRoleId();
                updateRoleData();
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, "当前不存在主用户信息，无法获取，请在账号界面添加用户信息"));
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
                gameLifeText.set(String.format("已活跃%d天", roleInfo.getActiveDays()));
                levelText.set(String.format("LV.%d", roleInfo.getLevel()));
                for (BoxInfo boxInfo : roleInfo.getBoxList()) {
                    if (boxInfo.getBoxName().equals("朴素奇藏箱")) {
                        box1Text.set(String.valueOf(boxInfo.getNum()));
                    } else if (boxInfo.getBoxName().equals("基准奇藏箱")) {
                        box2Text.set(String.valueOf(boxInfo.getNum()));
                    } else if (boxInfo.getBoxName().equals("精密奇藏箱")) {
                        box3Text.set(String.valueOf(boxInfo.getNum()));
                    } else if (boxInfo.getBoxName().equals("辉光奇藏箱")) {
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
                    if (data.getEnergyData().getRefreshTimeStamp() == 0) {
                        energyTimeText.set("体力已满");
                    } else {
                        long timestamp = data.getEnergyData().getRefreshTimeStamp() * 1000; // 仅为示例，实际应替换为具体的时间戳
                        Date date = new Date(timestamp);
                        Instant instant = Instant.ofEpochMilli(timestamp);
                        LocalDate dateFromTimestamp = instant.atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalDate currentDate = LocalDate.now();
                        boolean isSameDay = dateFromTimestamp.equals(currentDate);
                        if (isSameDay) {
                            SimpleDateFormat formatter = new SimpleDateFormat("满: 今日HH:mm");
                            energyTimeText.set(formatter.format(date));
                        } else {
                            SimpleDateFormat formatter = new SimpleDateFormat("满: 明日HH:mm");
                            energyTimeText.set(formatter.format(date));
                        }
                    }
                    hasSign.set(data.isHasSignIn());
                    livenessText.set(String.valueOf(data.getLivenessData().getCur()));
                    battlePassLevelText.set(String.format("电台 LV.%02d", data.getBattlePassData().getFirst().getCur()));
                    battlePassNumText.set(String.format("经验:%d/%d", data.getBattlePassData().get(1).getCur(), data.getBattlePassData().get(1).getTotal()));
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
                    new MessageInfo(MessageType.WARNING, "请前往WeGame进行更新"), false);
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
                            new MessageInfo(MessageType.WARNING, String.format("无法找到%s，请确保游戏目录正确", exe.getPath()), false));
                }
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, "请在设置在先设置游戏目录"), false);
            }
        }
    }



    public void signAndGame() {
        SignTask task = new SignTask();
        task.setOnSucceeded(workerStateEvent -> hasSign.set(true));
        Thread.startVirtualThread(task);
        startGame();
    }


    public void startGame() {
        String dir = Config.setting.getGameRootDir();
        if (dir != null) {
            File exe = null;
            if (Config.setting.isGameStartAppCustom()){
                exe=new File(Config.setting.getGameStarAppPath());
                if (!exe.exists()) {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING, String.format("自定义启动程序: %s 不存在", exe.getPath())));
                    return;
                }
            }else {
                if (Config.setting.getGameRootDirSource() == SourceType.DEFAULT) {
                    exe = new File(dir + File.separator + "Wuthering Waves Game" + File.separator + "Wuthering Waves.exe");
                } else {
                    exe = new File(dir + File.separator + "Wuthering Waves.exe");
                }
            }

            if (exe.exists()) {
                try {
                    Desktop.getDesktop().open(exe);
                    if (Config.setting.isHideWhenGameStart()) {
                        MainApplication.window.hide();
                    }
                } catch (IOException e) {
                    LOG.info("启动游戏错误", e);
                    MainApplication.window.show();
                }
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, String.format("无法找到%s，请确保游戏目录正确", exe.getPath()), false));
            }
        } else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING, "请在设置在先设置游戏目录"), false);
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