package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.dao.GameTowerDataDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.UserInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Difficulty;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.DifficultyTotal;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.TowerArea;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import cn.tealc.wutheringwavestool.thread.api.TowerDataDetailTask;
import cn.tealc.wutheringwavestool.thread.api.role.GameRoleDataSaveTask;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-15 23:40
 */
public class TowerViewModel implements ViewModel {

    private final UserInfo userInfo;

    private final ObservableList<Difficulty> difficultyList = FXCollections.observableArrayList();
    private final ObservableList<TowerArea> towerAreaList = FXCollections.observableArrayList();
    private final ObservableList<Pair<Long,Pair<String,String>>> towerHistoryList = FXCollections.observableArrayList();
    private SimpleStringProperty seasonEndTime = new SimpleStringProperty();


    public TowerViewModel() {
        UserInfoDao userInfoDao = new UserInfoDao();
        userInfo = userInfoDao.getMain();
        if (userInfo != null) {
            getData();
        }else {

        }


    }

    private void getData(){
        GameRoleDataSaveTask gameRoleDataSaveTask = new GameRoleDataSaveTask(userInfo);
        Thread.startVirtualThread(gameRoleDataSaveTask);

        TowerDataDetailTask task = new TowerDataDetailTask(userInfo);
        task.setOnSucceeded(event -> {
            //System.out.println(task.getValue().getData().get(0).getDifficultyName());
            ResponseBody<DifficultyTotal> responseBody = task.getValue();
            if (responseBody.getCode() == 200) {
                DifficultyTotal data = responseBody.getData();
                difficultyList.setAll(data.getDifficultyList());
                towerAreaList.setAll(data.getDifficultyList().getFirst().getTowerAreaList());

                long milliseconds = data.getSeasonEndTime();

                long millisecondsInADay = 24 * 60 * 60 * 1000;
                long millisecondsInAnHour = 60 * 60 * 1000;

                long days = milliseconds / millisecondsInADay;
                milliseconds %= millisecondsInADay; // 剩余毫秒数

                long hours = milliseconds / millisecondsInAnHour;

                // 输出结果
                seasonEndTime.set(String.format("%d天%d小时后刷新", days, hours));



                GameTowerDataDao dataDao = new GameTowerDataDao();
                Set<Long> endTimeList = dataDao.getEndTimeList();

                SimpleDateFormat endFormat = new SimpleDateFormat("yyyy.MM.dd");
                DateTimeFormatter startFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                endTimeList.stream().forEach(endTime -> {
                    Instant instant = Instant.ofEpochMilli(endTime);
                    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
                    ZonedDateTime fifteenDaysAgo = zonedDateTime.minusDays(15);
                    // 格式化日期
                    String startDay = startFormat.format(fifteenDaysAgo);
                    Date date =new Date(endTime);
                    String endDay = endFormat.format(date);
                    towerHistoryList.add(new Pair<>(endTime, new Pair<>(startDay, endDay)));
                });
            }else {

            }


        });
        Thread.startVirtualThread(task);



    }


    public void changeDifficulty(Difficulty difficulty){
        towerAreaList.setAll(difficulty.getTowerAreaList());
    }

    public ObservableList<Difficulty> getDifficultyList() {
        return difficultyList;
    }

    public ObservableList<TowerArea> getTowerAreaList() {
        return towerAreaList;
    }

    public String getSeasonEndTime() {
        return seasonEndTime.get();
    }

    public SimpleStringProperty seasonEndTimeProperty() {
        return seasonEndTime;
    }

    public ObservableList<Pair<Long, Pair<String, String>>> getTowerHistoryList() {
        return towerHistoryList;
    }
}