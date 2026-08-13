package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.service.TowerDataService;
import cn.tealc.wutheringwavestool.service.UserInfoService;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import com.kuro.kujiequ.model.towerData.*;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.model.ResponseBody;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
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
public class TowerViewModel extends BaseViewModel {
    private UserInfo userInfo;
    private final ObservableList<Difficulty> difficultyList = FXCollections.observableArrayList();
    private final ObservableList<TowerArea> towerAreaList = FXCollections.observableArrayList();
    private final ObservableList<Pair<Long,Pair<String,String>>> towerHistoryList = FXCollections.observableArrayList();
    private final SimpleStringProperty seasonEndTime = new SimpleStringProperty();
    private final SimpleStringProperty title= new SimpleStringProperty();

    private final Map<Integer,Role> roleMap = new HashMap<>();

    @Inject
    private UserInfoService userInfoService;
    @Inject
    private TowerDataService towerDataService;
    @Inject
    private KujiequManager kujiequManager;

    public TowerViewModel() {

    }

    public void initialize(){
        userInfo = userInfoService.getMainUser();
        if (userInfo != null) {
            initData();
            initHistory();
        }
    }

    public void refresh() {
        userInfo = userInfoService.getMainUser();
        if (userInfo != null) {
            initData();
        }
    }

    private void initData(){
        Thread.startVirtualThread(() -> {
            ResponseBody<List<Role>> roleResp;
            ResponseBody<DifficultyTotal> towerResp;
            try {
                roleResp = kujiequManager.getGameRoleData(userInfo);
            } catch (Exception e) {
                Platform.runLater(() ->
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                MessageInfo.error("获取角色数据失败，请检查网络后重试"), false));
                return;
            }
            try {
                towerResp = kujiequManager.getTowerData(userInfo);
            } catch (Exception e) {
                Platform.runLater(() ->
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                MessageInfo.error("获取深塔数据失败，请检查网络后重试"), false));
                return;
            }
            // 一定要先处理 updateRoleMap
            updateRoleMap(roleResp);
            updateDifficulty(towerResp);
        });
    }


    private void updateDifficulty(ResponseBody<DifficultyTotal> tower){
        if (tower.getCode() == 200) {
            DifficultyTotal data = tower.getData();
            towerDataService.saveToDB(data, userInfo.getRoleId()); // 落库
            long milliseconds = data.getSeasonEndTime();
            long millisecondsInADay = 24 * 60 * 60 * 1000;
            long millisecondsInAnHour = 60 * 60 * 1000;
            long days = milliseconds / millisecondsInADay;
            milliseconds %= millisecondsInADay; // 剩余毫秒数
            long hours = milliseconds / millisecondsInAnHour;
            Platform.runLater(() ->{
                difficultyList.setAll(data.getDifficultyList());
                towerAreaList.setAll(data.getDifficultyList().getFirst().getTowerAreaList());
                seasonEndTime.set(String.format("%d天%d小时后刷新", days, hours));
                title.set("深境区");
            });
        }else {

        }

    }

    private void updateRoleMap(ResponseBody<List<Role>> roles){
        if (roles.getCode() == 200){
            roles.getData().forEach(role -> {
                roleMap.put(role.getRoleId(), role);
            });
        }
    }



    private void initHistory(){
        List<Long> endTimeList = towerDataService.getEndTimeListByRoleId(userInfo.getRoleId());
        SimpleDateFormat endFormat = new SimpleDateFormat("yyyy.MM.dd");
        DateTimeFormatter startFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        // 定义一个截止日期
        LocalDate cutoffDate = LocalDate.of(2025, 2, 3);
        endTimeList.forEach(endTime -> {
            Instant instant = Instant.ofEpochMilli(endTime);
            ZonedDateTime endDate = instant.atZone(ZoneId.systemDefault());
            ZonedDateTime startDate;
            // 根据截止日期选择时间间隔,2025-02-03后深塔刷新时间改了，由14天改为28天
            if (endDate.toLocalDate().isAfter(cutoffDate)) {
                startDate = endDate.minusDays(28);
            } else {
                startDate = endDate.minusDays(14);
            }
            Date date =new Date(endTime);
            String startDay = startFormat.format(startDate);
            String endDay = endFormat.format(date);
            towerHistoryList.add(new Pair<>(endTime, new Pair<>(startDay, endDay)));
        });
    }





    public void changeHistory(long endTime){
        title.set("深境区");
        Set<TowerData> list = towerDataService.getRecordsByRoleIdAndEndTime(userInfo.getRoleId(),endTime);

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


        for (TowerData data : list) {
            Floor floor = new Floor();
            floor.setFloor(data.getFloor());
            floor.setStar(data.getStar());
            floor.setPicUrl(data.getPicUrl());
            List<SimpleRole> roleList =new ArrayList<>();
            if (data.getRoleList() != null){
                String[] roleIds = data.getRoleList().split(",");
                for (String roleId : roleIds) {
                    SimpleRole aim = new SimpleRole(Integer.valueOf(roleId), null);
                    roleList.add(aim);
                }
            }
            floor.setRoleList(roleList);
            TowerArea towerArea = towerAreaMap.get(data.getAreaName());
            towerArea.getFloorList().add(floor);
        }


        List<TowerArea> areaList = new ArrayList<>(towerAreaMap.values().stream().toList());
        areaList.sort(Comparator.comparing(TowerArea::getAreaId));
        areaList.forEach(towerArea -> {
            towerArea.getFloorList().sort(Comparator.comparing(Floor::getFloor));
            int star = towerArea.getFloorList().stream().mapToInt(Floor::getStar).sum();
            int maxStar = towerArea.getFloorList().size() * 3;
            towerArea.setStar(star);
            towerArea.setMaxStar(maxStar);
        });
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

    public Map<Integer, Role> getRoleMap() {
        return roleMap;
    }
}