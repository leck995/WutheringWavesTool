package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.LocalCachePlayerData;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.service.ConfigService;
import cn.tealc.wutheringwavestool.service.LauncherUserService;
import cn.tealc.wutheringwavestool.service.LocalCachePlayerDataService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.google.inject.Inject;
import com.kuro.launcher.model.LocalCacheUser;
import com.kuro.launcher.model.api.BaseData;
import com.kuro.launcher.model.api.BattlePassData;
import com.kuro.launcher.model.api.PlayerData;
import com.kuro.launcher.model.api.PlayerInfo;
import com.kuro.launcher.thread.api.QueryPlayerInfoTask;
import com.kuro.launcher.thread.api.QueryRoleTask;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.Image;
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
    private static final Logger LOG = LoggerFactory.getLogger(RoleBoardByLocalViewModel.class);
    public static final String LOCAL_CACHE_SELECTED_ROLE = "LOCAL_CACHE_SELECTED_ROLE";
    @Inject
    private LauncherUserService launcherUserService;
    @Inject
    private LocalCachePlayerDataService localCachePlayerDataService;
    @Inject
    private ConfigService configService;
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
    private SimpleStringProperty phantomBox1Text = new SimpleStringProperty();
    private SimpleStringProperty phantomBox2Text = new SimpleStringProperty();
    private SimpleStringProperty phantomBox3Text = new SimpleStringProperty();

    private SimpleStringProperty weeklyInstCountText = new SimpleStringProperty();
    private SimpleStringProperty weeklyInstCountTipText = new SimpleStringProperty(LanguageManager.getString("ui.home.label.weekly"));
    private SimpleObjectProperty<Image> headIcon = new SimpleObjectProperty<>();


    public void initialize() {
        updateRoleData();
        NotificationManager.subscribe(NotificationKey.HOME_ROLE_DATA_REFRESH, (s, objects) -> {
            updateRoleData();
        });

        NotificationManager.subscribe(NotificationKey.HOME_ROLE_LOCAL_CHANGE, (s, objects) -> {
            loadLocalCacheUser((LocalCacheUser) objects[0]);
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
                    MessageInfo.warning(LanguageManager.getString("ui.home.message.type04")));
            return;
        }

        Optional<String> optional = configService.get(LOCAL_CACHE_SELECTED_ROLE);
        if (optional.isPresent()){
            Optional<LocalCachePlayerData> userFormDB = localCachePlayerDataService.getByRoleId(optional.get());
            if (userFormDB.isPresent() && !isCacheExpired(userFormDB.get().getUpdateTime())){
                freshRoleData(userFormDB.get().getRoleId(), userFormDB.get().getOauthCode());
            }else {
                loadLocalCacheUser();
            }
        }else {
            loadLocalCacheUser();
        }
    }

    private static final long ONE_DAY_SECONDS = 24 * 60 * 60;

    private boolean isCacheExpired(long updateTime) {
        long now = System.currentTimeMillis() / 1000;
        return now - updateTime > ONE_DAY_SECONDS;
    }

    private void loadLocalCacheUser() {
        Optional<List<LocalCacheUser>> list = launcherUserService.readLocalCacheUser();
        if (list.isPresent()){
            LocalCacheUser user = list.get().getFirst();
            Optional<LocalCachePlayerData> userFormDB = localCachePlayerDataService.getByOauthCode(user.getOauthCode());
            if (userFormDB.isPresent() && !isCacheExpired(userFormDB.get().getUpdateTime())) {
                freshRoleData(userFormDB.get().getRoleId(), userFormDB.get().getOauthCode());
                configService.set(LOCAL_CACHE_SELECTED_ROLE, userFormDB.get().getRoleId());
            } else {
                queryPlayer(user);
            }
        }else {
            NotificationManager.message(MessageInfo.info(LanguageManager.getString("ui.home.label.no_data")));
        }

    }

    private void loadLocalCacheUser(LocalCacheUser user) {
        Optional<LocalCachePlayerData> userFormDB = localCachePlayerDataService.getByOauthCode(user.getOauthCode());
        if (userFormDB.isPresent() && !isCacheExpired(userFormDB.get().getUpdateTime())) {
            freshRoleData(userFormDB.get().getRoleId(), userFormDB.get().getOauthCode());
            configService.set(LOCAL_CACHE_SELECTED_ROLE, userFormDB.get().getRoleId());
        } else {
            queryPlayer(user);
        }
    }

    private void queryPlayer(LocalCacheUser user) {
        QueryPlayerInfoTask queryPlayerInfoTask = new QueryPlayerInfoTask(user.getOauthCode(), user.getType());
        queryPlayerInfoTask.setOnSucceeded(event -> {
            ResponseBody<PlayerInfo> body = queryPlayerInfoTask.getValue();
            if (body.getCode() == 200) {
                PlayerInfo data = body.getData();
                LocalCachePlayerData playerData = new LocalCachePlayerData(data);
                playerData.setOauthCode(user.getOauthCode());
                playerData.setCuid(user.getCuid());
                localCachePlayerDataService.saveOrUpdate(playerData);
                configService.set(LOCAL_CACHE_SELECTED_ROLE,data.getRoleId());
                freshRoleData(data.getRoleId(), user.getOauthCode());
            }else {
                NotificationManager.message(MessageInfo.error(body.getMsg()));
            }
        });
        queryPlayerInfoTask.setOnFailed(workerStateEvent -> {
            LOG.error("获取玩家信息失败", workerStateEvent.getSource().getException());
        });
        Thread.startVirtualThread(queryPlayerInfoTask);
    }

    private void freshRoleData(String roleId, String oauthCode) {
        QueryRoleTask task = new QueryRoleTask(roleId, oauthCode);
        task.setOnSucceeded(event -> {
            ResponseBody<PlayerData> data = (ResponseBody<PlayerData>) event.getSource().getValue();
            getBaseData(data.getData().getBaseData());
            getBoxData(data.getData().getBaseData());
            getBattlePassData(data.getData().getBattlePassData());

            Optional<LocalCachePlayerData> playerData = localCachePlayerDataService.getByRoleId(String.valueOf(data.getData().getBaseData().getId()));
            playerData.ifPresent(p -> {
                int headPhoto = Integer.parseInt(p.getHeadPhoto().substring(4));
                Image header = LocalResourcesManager.header(headPhoto, 60, 60);
                headIcon.set(header);
            });
        });
        task.setOnFailed(workerStateEvent -> {
            LOG.error("获取角色数据失败", workerStateEvent.getSource().getException());
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
        for (String key : baseData.getPhantomBoxes().keySet()) {
            switch (key) {
                case "1" -> phantomBox1Text.set(String.valueOf(baseData.getPhantomBoxes().get(key)));
                case "2" -> phantomBox2Text.set(String.valueOf(baseData.getPhantomBoxes().get(key)));
                case "3" -> phantomBox3Text.set(String.valueOf(baseData.getPhantomBoxes().get(key)));
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
            long timestamp = baseData.getEnergyRecoverTime();
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
        weeklyInstCountText.set(String.format("%d", baseData.getWeeklyInstCount()));
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

    public Image getHeadIcon() {
        return headIcon.get();
    }

    public SimpleObjectProperty<Image> headIconProperty() {
        return headIcon;
    }

    public String getPhantomBox1Text() {
        return phantomBox1Text.get();
    }

    public SimpleStringProperty phantomBox1TextProperty() {
        return phantomBox1Text;
    }

    public String getPhantomBox2Text() {
        return phantomBox2Text.get();
    }

    public SimpleStringProperty phantomBox2TextProperty() {
        return phantomBox2Text;
    }

    public String getPhantomBox3Text() {
        return phantomBox3Text.get();
    }

    public SimpleStringProperty phantomBox3TextProperty() {
        return phantomBox3Text;
    }
}
