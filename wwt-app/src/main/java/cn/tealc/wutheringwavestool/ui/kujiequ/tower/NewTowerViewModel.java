package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameNewTowerDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.kujiequ.model.newTowerData.NewTowerData;
import com.kuro.kujiequ.model.newTowerData.NewTowerModeDetail;
import com.kuro.kujiequ.model.newTowerData.NewTowerTeam;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NewTowerViewModel extends BaseViewModel {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(NewTowerViewModel.class);
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private UserInfoDao userInfoDao;
    @Inject
    private GameNewTowerDao gameNewTowerDao;
    @Inject
    private KujiequManager kujiequManager;

    private final ObservableList<NewTowerModeDetail> difficulties = FXCollections.observableArrayList();
    private final ObservableList<NewTowerTeam> teams = FXCollections.observableArrayList();
    private final ObservableList<Pair<Long, Pair<String, String>>> historyList = FXCollections.observableArrayList();
    private final SimpleStringProperty title = new SimpleStringProperty();
    private final SimpleStringProperty endTime = new SimpleStringProperty();
    private final SimpleStringProperty totalScore = new SimpleStringProperty();
    private final SimpleStringProperty progressInfo = new SimpleStringProperty();
    private final SimpleStringProperty rankText = new SimpleStringProperty();
    private final SimpleDateFormat endFormat = new SimpleDateFormat("yyyy.MM.dd");
    private final Map<Integer, Role> roleMap = new HashMap<>();
    private UserInfo userInfo;
    private List<NewTowerModeDetail> sourceModeDetails;
    private long sourceEndTimeMillis;
    private SimpleBooleanProperty isUnLock = new SimpleBooleanProperty(false);

    public void initialize() {
        userInfo = userInfoDao.getMain();
        loadData();
        initHistory();
    }

    public void loadData() {
        if (userInfo != null) {
            Thread.startVirtualThread(() -> {
                ResponseBody<List<Role>> roleResp;
                ResponseBody<NewTowerData> towerResp;
                try {
                    roleResp = kujiequManager.getGameRoleData(userInfo);
                } catch (Exception e) {
                    LOG.error("获取角色数据失败", e);
                    Platform.runLater(() ->
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    MessageInfo.error("获取角色数据失败，请检查网络后重试"), false));
                    return;
                }
                try {
                    towerResp = kujiequManager.getNewTowerData(userInfo);
                } catch (Exception e) {
                    LOG.error("获取终焉矩阵数据失败", e);
                    Platform.runLater(() ->
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    MessageInfo.error("获取终焉矩阵数据失败，请检查网络后重试"), false));
                    return;
                }
                // 一定要先处理 updateRoleMap
                updateRoleMap(roleResp);
                if (towerResp.getCode() == 200) {
                    NewTowerData newTowerData = towerResp.getData();
                    boolean unlocked = newTowerData != null && newTowerData.isUnlock();
                    Platform.runLater(() -> {
                        isUnLock.set(unlocked);
                        if (unlocked || newTowerData.getModeDetails() != null) {
                            updateData(newTowerData);
                        }
                    });
                }
            });
        }
    }

    private void updateData(NewTowerData newTowerData) {
        if (newTowerData == null)
            return;
        sourceEndTimeMillis = newTowerData.getEndTime();
        sourceModeDetails = newTowerData.getModeDetails();
        difficulties.setAll(sourceModeDetails);

        if (!difficulties.isEmpty()){
            NewTowerModeDetail first = difficulties.getFirst();
            title.set(modeName(first));
            teams.setAll(first.getTeams());
            updateSummary(first);
        }
        refreshEndTime();
    }

    private void initHistory() {
        if (userInfo != null) {
            List<Long> endTimeList = gameNewTowerDao.getEndTimesByRoleId(userInfo.getRoleId());
            endTimeList.forEach(endTime -> {
                Date date = new Date(endTime);
                String endDay = endFormat.format(date);
                historyList.add(new Pair<>(endTime, new Pair<>(endDay, "前的记录")));
            });
        }
    }

    private void updateRoleMap(ResponseBody<List<Role>> roles) {
        if (roles.getCode() == 200) {
            roles.getData().forEach(role -> {
                roleMap.put(role.getRoleId(), role);
            });
        }
    }

    public void changeDifficulty(NewTowerModeDetail detail) {
        title.set(modeName(detail));
        teams.setAll(detail.getTeams());
        updateSummary(detail);
        refreshEndTime();
    }

    private void refreshEndTime() {
        long milliseconds = sourceEndTimeMillis;
        long millisecondsInADay = 24 * 60 * 60 * 1000;
        long millisecondsInAnHour = 60 * 60 * 1000;
        long days = milliseconds / millisecondsInADay;
        milliseconds %= millisecondsInADay;
        long hours = milliseconds / millisecondsInAnHour;
        endTime.set(String.format("%d天%d小时后刷新", days, hours));
    }

    public void changHistory(long timestamp) {
        Optional<SlashDataForDB> data = gameNewTowerDao.getByRoleIdAndEndTime(userInfo.getRoleId(), timestamp);
        data.ifPresent(d -> {
            try {
                List<NewTowerModeDetail> list = objectMapper.readValue(d.getData(),
                        new TypeReference<List<NewTowerModeDetail>>() {});
                updateHistoryData(list);
                isUnLock.set(true);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void updateHistoryData(List<NewTowerModeDetail> list) {
        title.set("历史-终焉矩阵");
        if (!list.isEmpty()) {
            NewTowerModeDetail detail = list.getFirst();
            teams.setAll(detail.getTeams());
            updateSummary(detail);
        }
        endTime.set("");
    }

    private static String modeName(NewTowerModeDetail detail) {
        return detail.getModeId() == 0 ? "稳态协议" : "奇点扩张";
    }

    private static final String[] RANK_MAP = {"C", "B", "A", "S"};

    private void updateSummary(NewTowerModeDetail detail) {
        totalScore.set(String.format("%d", detail.getScore()));
        if (detail.getModeId() == 0) {
            progressInfo.set(String.format("%d/%d", detail.getPassBoss(), detail.getBossCount()));
        } else {
            progressInfo.set(String.format("第%d轮  %d/%d", detail.getRound(), detail.getPassBoss(), detail.getBossCount()));
        }
        int rank = detail.getRank();
        if (rank >= 0 && rank < RANK_MAP.length) {
            rankText.set(RANK_MAP[rank]);
        }
    }

    public ObservableList<NewTowerModeDetail> getDifficulties() {
        return difficulties;
    }

    public ObservableList<NewTowerTeam> getTeams() {
        return teams;
    }

    public ObservableList<Pair<Long, Pair<String, String>>> getHistoryList() {
        return historyList;
    }

    public String getTitle() {
        return title.get();
    }

    public SimpleStringProperty titleProperty() {
        return title;
    }

    public String getEndTime() {
        return endTime.get();
    }

    public SimpleStringProperty endTimeProperty() {
        return endTime;
    }

    public SimpleStringProperty totalScoreProperty() {
        return totalScore;
    }

    public SimpleStringProperty progressInfoProperty() {
        return progressInfo;
    }

    public SimpleStringProperty rankTextProperty() {
        return rankText;
    }

    public Map<Integer, Role> getRoleMap() {
        return roleMap;
    }

    public boolean isIsUnLock() {
        return isUnLock.get();
    }

    public SimpleBooleanProperty isUnLockProperty() {
        return isUnLock;
    }
}
