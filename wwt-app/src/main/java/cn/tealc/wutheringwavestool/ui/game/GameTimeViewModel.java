package cn.tealc.wutheringwavestool.ui.game;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.service.ConfigService;
import com.kuro.kujiequ.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-04 04:57
 */
public class GameTimeViewModel extends BaseViewModel {
    private final ObservableList<XYChart.Series<String, Double>> chartData = FXCollections.observableArrayList();
    private final ObservableList<GameTime> tableData = FXCollections.observableArrayList();
    private final ObservableList<String> userInfoList = FXCollections.observableArrayList();
    private final SimpleIntegerProperty userIndex = new SimpleIntegerProperty(-1);
    private final SimpleStringProperty allTotalTimeText = new SimpleStringProperty();
    private final SimpleStringProperty currentTotalTimeText = new SimpleStringProperty();
    private final SimpleStringProperty currentDayText = new SimpleStringProperty();
    private final SimpleStringProperty currentTimeText = new SimpleStringProperty();
    private final SimpleStringProperty currentUserName = new SimpleStringProperty();
    private final SimpleDoubleProperty currentProgressValue = new SimpleDoubleProperty();
    private final SimpleDoubleProperty totalProgressValue = new SimpleDoubleProperty();
    private final SimpleBooleanProperty empty = new SimpleBooleanProperty(false);

    @Inject
    private GameTimeDao gameTimeDao;
    @Inject
    private UserInfoDao userInfoDao;
    @Inject
    private ConfigService configService;

    public void initialize() {
        List<String> allRoleId = gameTimeDao.getAllRoleId();
        if (allRoleId == null || allRoleId.isEmpty()) {
            empty.set(true);
            return;
        }
        userInfoList.addAll(allRoleId);
        int index = 0;
        String roleId = null;
        if (!Config.setting().isUseLocalCacheUser()) {
            Optional<String> optional = configService.get("LOCAL_CACHE_SELECTED_ROLE");
            if (optional.isPresent()){
                roleId = optional.get();
            }
        } else {
            UserInfo main = userInfoDao.getMain();
            if (main != null) {
                roleId = main.getRoleId();
            }
        }
        if (roleId != null){
            for (int i = 0; i < userInfoList.size(); i++) {
                if (Objects.equals(userInfoList.get(i), roleId)) {
                    index = i;
                    break;
                }
            }
        }
        updateIndex(index);
    }

    /**
     * @return void
     * @description: 更新当前选择的用户
     * @param: index
     * @date: 2025/1/2
     */
    public void updateIndex(int index) {
        userIndex.set(index);
        freshWithAccount();
    }

    /**
     * @return void
     * @description: 更新用户数据
     * @param:
     * @date: 2024/10/8
     */
    public void freshWithAccount() {
        chartData.clear();
        //统计所有账号全部时长
        List<GameTime> gameTimeList = gameTimeDao.getAllTime();
        Map<String, List<GameTime>> allTimeMap = getMap(gameTimeList);
        double totalTime = sunTime(allTimeMap);
        allTotalTimeText.set(String.format("%.2f", totalTime));

        String roleId = userInfoList.get(userIndex.get());
        List<GameTime> timeListByRoleId = gameTimeDao.getTimeListByRoleId(roleId);
        Map<String, List<GameTime>> mainMap = getMap(timeListByRoleId);

        currentDayText.set(String.format(LanguageManager.getString("ui.game_time.account.days"), mainMap.keySet().size()));

        Map<String, List<GameTime>> mapInWeek = getMapInWeek(timeListByRoleId);
        //统计七日时长图表
        updateChartDate(mapInWeek, LanguageManager.getString("ui.game_time.total.charts.main_account"));
        //统计当前账号全部时长
        double currentTotalTime = sunTime(mainMap);
        currentTotalTimeText.set(String.format("%.2f", currentTotalTime));


        UserInfo userInfo = userInfoDao.getUserByRoleId(roleId);
        if (userInfo != null) {
            currentUserName.set(userInfo.getRoleName());
        } else {
            currentUserName.set(roleId);
        }


        totalProgressValue.set(currentTotalTime / totalTime);

        updateCurrentGameTime(roleId);
    }


    public void refreshTableData() {
        String roleId = userInfoList.get(userIndex.get());
        List<GameTime> list = gameTimeDao.getTimeListByRoleId(roleId);
        list.sort((a, b) -> {
            long t1 = a.getStartTime() != null ? a.getStartTime() : 0;
            long t2 = b.getStartTime() != null ? b.getStartTime() : 0;
            return Long.compare(t2, t1);
        });
        tableData.setAll(list);
    }

    public void addRecord(GameTime record) {
        record.setRoleId(userInfoList.get(userIndex.get()));
        gameTimeDao.addTime(record);
        refreshTableData();
        freshWithAccount();
    }

    public void updateRecord(GameTime record) {
        gameTimeDao.updateTime(record);
        refreshTableData();
        freshWithAccount();
    }

    public void deleteRecord(GameTime record) {
        if (record.getId() != null) {
            gameTimeDao.deleteTimeById(record.getId());
            refreshTableData();
            freshWithAccount();
        }
    }

    public ObservableList<GameTime> getTableData() {
        return tableData;
    }

    /**
     * @return java.util.Map<java.lang.String,java.util.List<cn.tealc.wutheringwavestool.model.game.GameTime>>
     * @description: 对数据库的记录按照日期分类
     * @param: list
     * @date: 2024/8/4
     */
    private Map<String, List<GameTime>> getMapInWeek(List<GameTime> list) {
        Map<String, List<GameTime>> map = new LinkedHashMap<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            GameTime gameTime = list.get(i);
            String key = gameTime.getGameDate();
            if (!map.containsKey(key)) {
                if (map.keySet().size() < 7) {
                    List<GameTime> temp = new ArrayList<>();
                    temp.add(gameTime);
                    map.put(key, temp);
                }
            } else {
                map.get(key).add(gameTime);
            }
        }
        return map;
    }

    private Map<String, List<GameTime>> getMap(List<GameTime> list) {
        Map<String, List<GameTime>> map = new LinkedHashMap<>();
        for (GameTime gameTime : list) {
            String key = gameTime.getGameDate();
            if (!map.containsKey(key)) {
                List<GameTime> temp = new ArrayList<>();
                temp.add(gameTime);
                map.put(key, temp);
            } else {
                map.get(key).add(gameTime);
            }
        }
        return map;
    }

    /**
     * @return void
     * @description: 更新图表
     * @param: map
     * @param: name
     * @date: 2024/8/4
     */
    private void updateChartDate(Map<String, List<GameTime>> map, String name) {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        series.setName(name);


        for (String key : map.keySet()) {
            List<GameTime> gameTimes = map.get(key);
            long count = gameTimes.stream().mapToLong(GameTime::getDuration).sum();
            double minute = count / 1000.0 / 60.0;
            series.getData().add(new XYChart.Data<>(key, minute));
        }
        FXCollections.reverse(series.getData());
        chartData.add(series);
    }


    /**
     * @return double
     * @description: 计算出全部时间，以小时为单位
     * @param: map
     * @date: 2024/8/4
     */
    private double sunTime(Map<String, List<GameTime>> map) {
        long total = 0;
        for (String key : map.keySet()) {
            List<GameTime> gameTimes = map.get(key);
            long count = gameTimes.stream().mapToLong(GameTime::getDuration).sum();
            total = total + count;
        }
        return total / 1000.0 / 60.0 / 60.0;
    }

    /**
     * @return void
     * @description: 获取当前账号游玩时间
     * @param:
     * @date: 2024/8/4
     */
    private void updateCurrentGameTime(String roleId) {
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = dateTimeFormatter.format(localDate);
        List<GameTime> list = gameTimeDao.getTimeListByDataAndRoleId(date, roleId);
        if (list != null) {
            long sum = list.stream().mapToLong(GameTime::getDuration).sum();
            int hour = (int) (sum / (1000 * 60 * 60));
            int minute = (int) ((sum % (1000 * 60 * 60)) / (1000 * 60));
            currentProgressValue.set(sum / (1000.0 * 60.0 * 60.0) / 24.0);
            currentTimeText.set(String.format(LanguageManager.getString("ui.game_time.account.duration"), hour, minute));
        }
    }

    public ObservableList<XYChart.Series<String, Double>> getChartData() {
        return chartData;
    }

    public int getUserIndex() {
        return userIndex.get();
    }

    public SimpleIntegerProperty userIndexProperty() {
        return userIndex;
    }

    public String getAllTotalTimeText() {
        return allTotalTimeText.get();
    }

    public SimpleStringProperty allTotalTimeTextProperty() {
        return allTotalTimeText;
    }

    public String getCurrentTotalTimeText() {
        return currentTotalTimeText.get();
    }

    public SimpleStringProperty currentTotalTimeTextProperty() {
        return currentTotalTimeText;
    }

    public String getCurrentDayText() {
        return currentDayText.get();
    }

    public SimpleStringProperty currentDayTextProperty() {
        return currentDayText;
    }

    public String getCurrentTimeText() {
        return currentTimeText.get();
    }

    public SimpleStringProperty currentTimeTextProperty() {
        return currentTimeText;
    }

    public double getCurrentProgressValue() {
        return currentProgressValue.get();
    }

    public SimpleDoubleProperty currentProgressValueProperty() {
        return currentProgressValue;
    }

    public double getTotalProgressValue() {
        return totalProgressValue.get();
    }

    public SimpleDoubleProperty totalProgressValueProperty() {
        return totalProgressValue;
    }

    public ObservableList<String> getUserInfoList() {
        return userInfoList;
    }

    public String getCurrentUserName() {
        return currentUserName.get();
    }

    public SimpleStringProperty currentUserNameProperty() {
        return currentUserName;
    }

    public boolean isEmpty() {
        return empty.get();
    }

    public SimpleBooleanProperty emptyProperty() {
        return empty;
    }
}