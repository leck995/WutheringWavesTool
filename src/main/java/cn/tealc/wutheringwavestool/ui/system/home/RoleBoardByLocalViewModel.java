package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.OAuthCredential;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.service.LauncherUserService;
import cn.tealc.wutheringwavestool.service.OAuthCredentialService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import com.kuro.launcher.model.LocalCacheUser;
import com.kuro.launcher.model.api.BaseData;
import com.kuro.launcher.model.api.BattlePassData;
import com.kuro.launcher.model.api.PlayerInfo;
import com.kuro.launcher.thread.api.QueryPlayerInfoTask;
import com.kuro.launcher.thread.api.QueryRoleTask;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class RoleBoardByLocalViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(HomeViewModel2.class);
    @Inject
    private LauncherUserService launcherUserService;
    @Inject
    private OAuthCredentialService oAuthCredentialService;
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


    public RoleBoardByLocalViewModel() {
        updateRoleData();
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.HOME_ROLE_DATA_REFRESH, (s, objects) -> {
            updateRoleData();
        });
    }

    /**
     * @return void
     * @description: 刷新库街区的角色数据
     * @param:
     * @date: 2024/10/8
     */
    public void updateRoleData() {
        if (Config.setting().getGameRootDir() == null) {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type09")));
            return;
        }

        Optional<List<LocalCacheUser>> list = launcherUserService.readLocalCacheUser();
        list.ifPresent(localCacheUsers -> {
            LocalCacheUser user = localCacheUsers.getFirst();
            Optional<OAuthCredential> userFormDB = oAuthCredentialService.getByOauthCode(user.getOauthCode());
            if (userFormDB.isPresent()) {//如果数据库存在，则直接刷新数据
                freshRoleData(userFormDB.get().getRoleId(), user.getOauthCode());
            } else {//否则，调用接口查询当前角色ID
                QueryPlayerInfoTask queryPlayerInfoTask = new QueryPlayerInfoTask(user.getOauthCode(), user.getType());
                queryPlayerInfoTask.setOnSucceeded(event -> {
                    ResponseBody<PlayerInfo> body = queryPlayerInfoTask.getValue();
                    if (body.getCode() == 200) {
                        PlayerInfo data = body.getData();
                        oAuthCredentialService.saveOrUpdate(data.getRoleId(), user.getOauthCode());
                        freshRoleData(data.getRoleId(), user.getOauthCode());
                    }
                });
                Thread.startVirtualThread(queryPlayerInfoTask);
            }
        });

    }

    private void freshRoleData(String roleId, String oauthCode) {
        QueryRoleTask task = new QueryRoleTask(roleId, oauthCode);
        task.setOnSucceeded(event -> {
            System.out.println("获取到角色数据");
            System.out.println(task.getValue().getData().getBaseData().getActiveDays());

            getBaseData(task.getValue().getData().getBaseData());
            getBoxData(task.getValue().getData().getBaseData());
            getBattlePassData(task.getValue().getData().getBattlePassData());

        });

        Thread.startVirtualThread(task);

    }


    /**
     * 获取角色的基本数据，如宝箱数量。
     *
     * @param baseData
     */
    private void getBoxData(BaseData baseData) {
        for (String key : baseData.getBasicBoxes().keySet()) {
            switch (key) {
                case "1" -> box1Text.set(String.valueOf(baseData.getBasicBoxes().get(key)));
                case "2" -> box2Text.set(String.valueOf(baseData.getBasicBoxes().get(key)));
                case "3" -> box3Text.set(String.valueOf(baseData.getBasicBoxes().get(key)));
                case "4" -> box4Text.set(String.valueOf(baseData.getBasicBoxes().get(key)));
            }
        }

    }

    /**
     * 获取角色的日常数据，如体力;
     *
     */
    private void getBaseData(BaseData baseData) {
        String[] strengths = LanguageManager.getStringArray("ui.home.label.daily.strength");
        if (baseData.getEnergyRecoverTime() == 0) { //体力
            energyTimeText.set(strengths[2]);
        } else {
            long timestamp = baseData.getEnergyRecoverTime() * 1000;
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

        String template = LanguageManager.getString("ui.home.label.role.day");
        gameLifeText.set(String.format(template, baseData.getActiveDays()));

        levelText.set(String.format("LV.%d", baseData.getLevel()));
        livenessText.set(String.valueOf(baseData.getLiveness()));
        roleNameText.set(baseData.getName());
        energyText.set(String.format("%d/%d", baseData.getEnergy(), baseData.getMaxEnergy()));
        weeklyInstCountText.set(String.format("%d/%d", 3 - baseData.getWeeklyInstCount(), 3));
        storeEnergyText.set(String.format("%d/%d", baseData.getStoreEnergy(), baseData.getMaxStoreEnergy()));
        // weeklyRougeText.set(String.format("%d", roleInfo.getRougeScore())); //肉鸽数据没有提供
        rolePaneVisible.set(true);
    }


    /**
     * 电台数据
     *
     * @param data
     * @author leck
     * @date 2026/05/27
     */
    private void getBattlePassData(BattlePassData data) {
        battlePassLevelText.set(String.format(" LV.%02d", data.getLevel()));
        battlePassNumText.set(String.format("%d/%d", data.getWeekExp(), data.getWeekMaxExp()));
        double cur = data.getWeekExp();
        double total = data.getWeekMaxExp();
        battlePassProgress.set(cur / total);
        rolePaneVisible.set(true);

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
}
