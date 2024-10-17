package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.dao.GameRoleDataDao;
import cn.tealc.wutheringwavestool.dao.GameTowerDataDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.Role;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.UserInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.*;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import cn.tealc.wutheringwavestool.thread.api.TowerDataDetailTask;
import cn.tealc.wutheringwavestool.thread.api.role.GameRoleDataSaveTask;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private final SimpleStringProperty title= new SimpleStringProperty();

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
                title.set("深境区");


                GameTowerDataDao dataDao = new GameTowerDataDao();
                Set<Long> endTimeList = dataDao.getEndTimeList();

                SimpleDateFormat endFormat = new SimpleDateFormat("yyyy.MM.dd");
                DateTimeFormatter startFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                endTimeList.forEach(endTime -> {
                    Instant instant = Instant.ofEpochMilli(endTime);
                    ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
                    ZonedDateTime fifteenDaysAgo = zonedDateTime.minusDays(14);
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

    
    public void changeHistory(long endTime){
        title.set("深境区");
        GameTowerDataDao dao = new GameTowerDataDao();
        System.out.println(endTime);
        Set<TowerData> list = dao.getListByEndTime(endTime);

        Map<String,TowerArea> towerAreaMap = new LinkedHashMap<>();
        for (TowerData data : list) {
            if (!towerAreaMap.containsKey(data.getAreaName())){
                TowerArea towerArea = new TowerArea();
                towerArea.setAreaId(data.getAreaId());
                towerArea.setAreaName(data.getAreaName());
                towerArea.setFloorList(new ArrayList<>());
                towerAreaMap.put(data.getAreaName(), towerArea);
            }
        }

        GameRoleDataDao gameRoleDataDao = new GameRoleDataDao();
        for (TowerData data : list) {
            Floor floor = new Floor();
            floor.setFloor(data.getFloor());
            floor.setStar(data.getStar());
            floor.setPicUrl(data.getPicUrl());
            List<SimpleRole> roleList =new ArrayList<>();
            if (data.getRoleList() != null){
                String[] roleIds = data.getRoleList().split(",");
                for (String roleId : roleIds) {
                    Role role = gameRoleDataDao.getRoleDataById(Integer.parseInt(roleId));
                    if (role != null){
                        SimpleRole aim = new SimpleRole(role.getRoleId(), role.getRoleIconUrl());
                        roleList.add(aim);
                    }

                }
            }
            floor.setRoleList(roleList);
            TowerArea towerArea = towerAreaMap.get(data.getAreaName());
            towerArea.getFloorList().add(floor);
        }


        List<TowerArea> areaList = new ArrayList<>(towerAreaMap.values().stream().toList());
        areaList.sort(Comparator.comparing(TowerArea::getAreaId));
        areaList.forEach(towerArea -> towerArea.getFloorList().sort(Comparator.comparing(Floor::getFloor)));
        this.towerAreaList.setAll(areaList);

    }




    public void changeDifficulty(Difficulty difficulty){
        towerAreaList.setAll(difficulty.getTowerAreaList());
        title.set(difficulty.getDifficultyName());
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

    public String getTitle() {
        return title.get();
    }

    public SimpleStringProperty titleProperty() {
        return title;
    }
}