package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kuro.kujiequ.model.newTowerData.NewTowerData;
import com.kuro.kujiequ.model.newTowerData.NewTowerModeDetail;
import com.kuro.kujiequ.model.newTowerData.NewTowerRole;
import com.kuro.kujiequ.model.newTowerData.NewTowerTeam;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.Challenge;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.slash.SlashDifficulty;
import com.kuro.kujiequ.thread.rolebox.role.GameRoleDataTask;
import com.kuro.kujiequ.thread.rolebox.slash.SlashDataDetailTask;
import com.kuro.kujiequ.thread.rolebox.tower.NewTowerDataDetailTask;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.util.Pair;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class NewTowerViewModel extends BaseViewModel {
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private UserInfoDao userInfoDao;
    private final ObservableList<NewTowerModeDetail> difficulties = FXCollections.observableArrayList();
    private final ObservableList<NewTowerTeam> teams = FXCollections.observableArrayList();
    private final ObservableList<Pair<Long, Pair<String, String>>> historyList = FXCollections.observableArrayList();
    private final SimpleStringProperty title = new SimpleStringProperty();
    private final SimpleStringProperty endTime = new SimpleStringProperty();
    private final SimpleStringProperty totalScore = new SimpleStringProperty();
    private final SimpleStringProperty progressInfo = new SimpleStringProperty();
    private final SimpleStringProperty rankText = new SimpleStringProperty();
    private final SimpleDateFormat endFormat = new SimpleDateFormat("yyyy.MM.dd");
    private final DateTimeFormatter startFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private final Map<Integer, Role> roleMap = new HashMap<>();
    private UserInfo userInfo;

    public void initialize(){
        userInfo = userInfoDao.getMain();
        loadData();
    }

    public void loadData(){
        if (userInfo != null) {
            NewTowerDataDetailTask towerDataDetailTask = new NewTowerDataDetailTask(userInfo);
            GameRoleDataTask roleDataTask = new GameRoleDataTask(userInfo);
            EventHandler<WorkerStateEvent> eventHandler = workerStateEvent -> {
                if (towerDataDetailTask.state() == Future.State.SUCCESS && roleDataTask.state() == Future.State.SUCCESS) {
                    updateRoleMap(roleDataTask.getValue());
                    ResponseBody<NewTowerData> value = towerDataDetailTask.getValue();
                    if (value.getCode() == 200){
                        NewTowerData newTowerData = value.getData();
                        updateData(newTowerData);
                    }
                }
            };
            towerDataDetailTask.setOnSucceeded(eventHandler);
            roleDataTask.setOnSucceeded(eventHandler);
            Thread.startVirtualThread(towerDataDetailTask);
            Thread.startVirtualThread(roleDataTask);
        }
    }


    private void updateData(NewTowerData newTowerData) {
        difficulties.setAll(newTowerData.getModeDetails());
        NewTowerModeDetail first = difficulties.getFirst();
        title.set(modeName(first));
        teams.setAll(first.getTeams());
        updateSummary(first);
        long milliseconds = newTowerData.getEndTime();
        long millisecondsInADay = 24 * 60 * 60 * 1000;
        long millisecondsInAnHour = 60 * 60 * 1000;
        long days = milliseconds / millisecondsInADay;
        milliseconds %= millisecondsInADay; // 剩余毫秒数
        long hours = milliseconds / millisecondsInAnHour;
        endTime.set(String.format("%d天%d小时后刷新", days, hours));
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
    }

    private static String modeName(NewTowerModeDetail detail) {
        return detail.getModeId() == 0 ? "稳态协议" : "奇点扩张";
    }

    private static final String[] RANK_MAP = {"C", "B", "A", "S"};

    private void updateSummary(NewTowerModeDetail detail) {
        totalScore.set(String.format("%d", detail.getScore()));
        if (detail.getModeId() == 0) {
            progressInfo.set(String.format("第1轮  %d/%d", detail.getPassBoss(), detail.getBossCount()));
        } else {
            progressInfo.set(String.format("第%d轮  %d/%d", detail.getRound(), detail.getPassBoss(), detail.getBossCount()));
        }
        int rank = detail.getRank();
        if (rank >= 0 && rank < RANK_MAP.length) {
            rankText.set(RANK_MAP[rank]);
        }
    }

    public void changHistory(Long key) {
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


}
