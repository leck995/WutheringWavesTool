package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import com.kuro.kujiequ.model.roleData.user.BoxInfo;
import com.kuro.kujiequ.model.roleData.user.RoleDailyData;
import com.kuro.kujiequ.model.roleData.user.RoleInfo;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.UserDataRefreshTask;
import com.kuro.kujiequ.thread.base.UserDailyDataTask;
import com.kuro.kujiequ.thread.base.sign.SignTask;
import com.kuro.kujiequ.thread.rolebox.PlayerBaseDataTask;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class RoleBoardByKujiequViewModel extends BaseViewModel {

    private static final Logger LOG = LoggerFactory.getLogger(RoleBoardByKujiequViewModel.class);
    @Inject
    private UserInfoDao userInfoDao;

    private SimpleStringProperty energyText = new SimpleStringProperty();
    private SimpleStringProperty energyTimeText = new SimpleStringProperty();
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
    private SimpleStringProperty weeklyRougeText = new SimpleStringProperty();
    private SimpleStringProperty weeklyRougeTipText = new SimpleStringProperty("肉鸽");
    private SimpleStringProperty weeklyInstCountText = new SimpleStringProperty();
    private SimpleStringProperty weeklyInstCountTipText = new SimpleStringProperty("周本");

    private SimpleBooleanProperty hasSign = new SimpleBooleanProperty(true);
    private SimpleStringProperty signText = new SimpleStringProperty();

    public RoleBoardByKujiequViewModel() {
        updateKujiequRoleData();
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.HOME_ROLE_DATA_REFRESH, (s, objects) -> {
            updateKujiequRoleData();
        });
    }

    /**
     * @return void
     * @description: 刷新库街区的角色数据
     * @param:
     * @date: 2024/10/8
     */
    public void updateKujiequRoleData() {
        if (Config.setting().isNoKuJieQu()) {
            hasSign.set(true);
            return;
        }

        UserInfo userInfo = userInfoDao.getMain();
        if (userInfo == null) {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type01")));
            return;
        }

        UserDataRefreshTask task = new UserDataRefreshTask(userInfo);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<String> responseBody = task.getValue();
            if (responseBody.getCode() == 200) {
                getDailyData(userInfo);
                getRoleData(userInfo);
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        new MessageInfo(MessageType.WARNING, responseBody.getMsg()));
                LOG.error(responseBody.getMsg());
            }
        });
        Thread.startVirtualThread(task);
    }


    /**
     * 获取角色的基本数据，如宝箱数量。鉴于getDailyData方法每次都是同时调用，故失败请求不再弹出消息显示。
     *
     * @param userInfo
     */
    private void getRoleData(UserInfo userInfo) {
        PlayerBaseDataTask playerBaseDataTask = new PlayerBaseDataTask(userInfo);
        playerBaseDataTask.setOnSucceeded(workerStateEvent -> {
            ResponseBody<RoleInfo> responseBody = playerBaseDataTask.getValue();
            if (responseBody.getCode() == 200) {
                RoleInfo roleInfo = responseBody.getData();
                //roleNameText.set(roleInfo.getName());
                String template = LanguageManager.getString("ui.home.label.role.day");
                gameLifeText.set(String.format(template, roleInfo.getActiveDays()));
                levelText.set(String.format("LV.%d", roleInfo.getLevel()));

                //energyText.set(String.format("%d/%d", roleInfo.getEnergy(), roleInfo.getMaxEnergy()));
                //weeklyInstCountText.set(String.format("%d/%d", roleInfo.getWeeklyInstCountLimit() - roleInfo.getWeeklyInstCount(), roleInfo.getWeeklyInstCountLimit()));
                //storeEnergyText.set(String.format("%d/%d", roleInfo.getStoreEnergy(), roleInfo.getStoreEnergyLimit()));

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
                weeklyRougeText.set(String.format("%d", roleInfo.getRougeScore()));
                rolePaneVisible.set(true);
                onWeekEnd(false, roleInfo);
            } else {
                rolePaneVisible.set(false);
            }
        });
        Thread.startVirtualThread(playerBaseDataTask);
    }

    /**
     * 获取角色的日常数据，如体力;
     *
     * @param userInfo
     */
    private void getDailyData(UserInfo userInfo) {
        UserDailyDataTask userDailyDataTask = new UserDailyDataTask(userInfo);
        userDailyDataTask.setOnSucceeded(workerStateEvent -> {
            ResponseBody<RoleDailyData> responseBody = userDailyDataTask.getValue();
            if (responseBody != null) {
                if (responseBody.getCode() == 200) {
                    RoleDailyData data = responseBody.getData();

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


                    //原本放在userInfo
                    roleNameText.set(data.getRoleName());
                    energyText.set(String.format("%d/%d", data.getEnergyData().getCur(), data.getEnergyData().getTotal()));
                    weeklyInstCountText.set(String.format("%d/%d", data.getWeeklyData().getTotal() - data.getWeeklyData().getCur(), data.getWeeklyData().getTotal()));
                    storeEnergyText.set(String.format("%d/%d", data.getStoreEnergyData().getCur(), data.getStoreEnergyData().getTotal()));


                } else {
                    rolePaneVisible.set(false);
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING, responseBody.getMsg()), false);
                }
            }
        });
        Thread.startVirtualThread(userDailyDataTask);
    }

    /**
     * 检查每周最后一天，并进行提醒完成周活动
     *
     * @param isGlobal
     * @param roleInfo
     */
    private void onWeekEnd(boolean isGlobal, RoleInfo roleInfo) {
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            if (isGlobal) {
                NotificationManager.publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.label.weekly.message01"), MessageInfo.LONG));
            } else {
                if (roleInfo == null) {
                    return;
                }
                if (roleInfo.getWeeklyInstCount() != 0) {
                    weeklyInstCountTipText.set(LanguageManager.getString("ui.home.label.weekly.tip"));
                } else {
                    weeklyInstCountTipText.set(LanguageManager.getString("ui.home.label.weekly"));
                }
                if (roleInfo.getRougeScore() < 6000) {
                    weeklyRougeTipText.set(LanguageManager.getString("ui.home.label.weekly.tip"));
                    NotificationManager.publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING,
                                    LanguageManager.getString("ui.home.label.weekly.message03"), MessageInfo.LONG));
                } else {
                    weeklyRougeTipText.set(LanguageManager.getString("ui.home.label.rouge"));
                }
            }
        }
    }

    /**
     * 开始进行库街区鸣潮签到
     */
    public void startKujiequDailySign() {
        SignTask task = new SignTask();
        task.setOnSucceeded(workerStateEvent -> {
            hasSign.set(true);
            signText.set(LanguageManager.getString("ui.home.label.sign.yes"));
        });
        Thread.startVirtualThread(task);
        signText.set(LanguageManager.getString("ui.home.label.sign.ing"));
    }


    public String getEnergyText() {
        return energyText.get();
    }

    public SimpleStringProperty energyTextProperty() {
        return energyText;
    }

    public String getEnergyTimeText() {
        return energyTimeText.get();
    }

    public SimpleStringProperty energyTimeTextProperty() {
        return energyTimeText;
    }

    public String getStoreEnergyText() {
        return storeEnergyText.get();
    }

    public SimpleStringProperty storeEnergyTextProperty() {
        return storeEnergyText;
    }

    public String getLivenessText() {
        return livenessText.get();
    }

    public SimpleStringProperty livenessTextProperty() {
        return livenessText;
    }

    public String getBattlePassLevelText() {
        return battlePassLevelText.get();
    }

    public SimpleStringProperty battlePassLevelTextProperty() {
        return battlePassLevelText;
    }

    public String getBattlePassNumText() {
        return battlePassNumText.get();
    }

    public SimpleStringProperty battlePassNumTextProperty() {
        return battlePassNumText;
    }

    public double getBattlePassProgress() {
        return battlePassProgress.get();
    }

    public SimpleDoubleProperty battlePassProgressProperty() {
        return battlePassProgress;
    }

    public boolean isRolePaneVisible() {
        return rolePaneVisible.get();
    }

    public SimpleBooleanProperty rolePaneVisibleProperty() {
        return rolePaneVisible;
    }

    public String getRoleNameText() {
        return roleNameText.get();
    }

    public SimpleStringProperty roleNameTextProperty() {
        return roleNameText;
    }

    public String getGameLifeText() {
        return gameLifeText.get();
    }

    public SimpleStringProperty gameLifeTextProperty() {
        return gameLifeText;
    }

    public String getLevelText() {
        return levelText.get();
    }

    public SimpleStringProperty levelTextProperty() {
        return levelText;
    }

    public String getBox1Text() {
        return box1Text.get();
    }

    public SimpleStringProperty box1TextProperty() {
        return box1Text;
    }

    public String getBox2Text() {
        return box2Text.get();
    }

    public SimpleStringProperty box2TextProperty() {
        return box2Text;
    }

    public String getBox3Text() {
        return box3Text.get();
    }

    public SimpleStringProperty box3TextProperty() {
        return box3Text;
    }

    public String getBox4Text() {
        return box4Text.get();
    }

    public SimpleStringProperty box4TextProperty() {
        return box4Text;
    }

    public String getWeeklyRougeText() {
        return weeklyRougeText.get();
    }

    public SimpleStringProperty weeklyRougeTextProperty() {
        return weeklyRougeText;
    }

    public String getWeeklyRougeTipText() {
        return weeklyRougeTipText.get();
    }

    public SimpleStringProperty weeklyRougeTipTextProperty() {
        return weeklyRougeTipText;
    }

    public String getWeeklyInstCountText() {
        return weeklyInstCountText.get();
    }

    public SimpleStringProperty weeklyInstCountTextProperty() {
        return weeklyInstCountText;
    }

    public String getWeeklyInstCountTipText() {
        return weeklyInstCountTipText.get();
    }

    public SimpleStringProperty weeklyInstCountTipTextProperty() {
        return weeklyInstCountTipText;
    }

    public boolean isHasSign() {
        return hasSign.get();
    }

    public SimpleBooleanProperty hasSignProperty() {
        return hasSign;
    }

    public String getSignText() {
        return signText.get();
    }

    public SimpleStringProperty signTextProperty() {
        return signText;
    }
}
