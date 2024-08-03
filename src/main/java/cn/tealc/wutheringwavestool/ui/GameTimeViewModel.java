package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
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
public class GameTimeViewModel implements ViewModel {
    private ObservableList<XYChart.Series<String,Double>> chartData= FXCollections.observableArrayList();
    private SimpleStringProperty todayGameTimeText=new SimpleStringProperty();
    private final ObservableList<UserInfo> userInfoList= FXCollections.observableArrayList();
    private final SimpleIntegerProperty userIndex = new SimpleIntegerProperty(-1);
    public GameTimeViewModel() {
        UserInfoDao dao = new UserInfoDao();
        List<UserInfo> userInfos = dao.getAll();
        userInfoList.setAll(userInfos);
        UserInfo main = dao.getMain();
        if (main != null) {
            for (int i = 0; i < userInfoList.size(); i++) {
                if (Objects.equals(userInfoList.get(i).getId(), main.getId())) {
                    userIndex.set(i);
                    break;
                }
            }

            updateGameTime();
            updateMainGameTime();

        }

    }

    private void updateMainGameTime() {
        GameTimeDao gameTimeDao = new GameTimeDao();


       /* List<GameTime> gameTimeList = gameTimeDao.getAllTime();
        Map<String, List<GameTime>> allTimeMap = getMap(gameTimeList);
        updateChartDate(allTimeMap,"总时长");*/
        List<GameTime> timeListByRoleId = gameTimeDao.getTimeListByRoleId(userInfoList.get(userIndex.get()).getRoleId());
        Map<String, List<GameTime>> mainMap = getMap(timeListByRoleId);
        updateChartDate(mainMap,"主账号时长");
    }


    private Map<String,List<GameTime>> getMap(List<GameTime> list){
        Map<String,List<GameTime>> map = new LinkedHashMap<>();
        if (list.size() > 7){
            list = list.subList(list.size()-7,list.size()-1);
        }
        for (GameTime gameTime : list) {
            String key=gameTime.getGameDate();
            if (!map.containsKey(key)) {
                List<GameTime> temp = new ArrayList<>();
                temp.add(gameTime);
                map.put(key,temp);
            }else {
                map.get(key).add(gameTime);
            }
        }
        return map;
    }
    private void updateChartDate(Map<String,List<GameTime>> map,String name){
        XYChart.Series<String,Double> series = new XYChart.Series<>();
        series.setName(name);

        for (String key : map.keySet()) {
            List<GameTime> gameTimes = map.get(key);
            long count = gameTimes.stream().mapToLong(GameTime::getDuration).sum();
            double minute = count / 1000.0 / 60.0;
            series.getData().add(new XYChart.Data<>(key, minute));
        }
        chartData.add(series);
    }



    private void updateGameTime(){
        List<GameTime> list = getGameTimes();
        if (list !=null){
            long sum = list.stream().mapToLong(GameTime::getDuration).sum();
            updateGameTimeText(sum);
        }
    }
    private List<GameTime> getGameTimes() {
        GameTimeDao gameTimeDao=new GameTimeDao();
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = dateTimeFormatter.format(localDate);
        return gameTimeDao.getTimeListByData(date);
    }

    private void updateGameTimeText(long sum) {
        int hour = (int) (sum / (1000 * 60 * 60));
        int minute = (int) ((sum % (1000 * 60 * 60)) / (1000 * 60));
        todayGameTimeText.set(String.format("今日在线 %d 小时 %d 分钟",hour,minute));
    }

    public ObservableList<XYChart.Series<String, Double>> getChartData() {
        return chartData;
    }

    public String getTodayGameTimeText() {
        return todayGameTimeText.get();
    }

    public SimpleStringProperty todayGameTimeTextProperty() {
        return todayGameTimeText;
    }

    public int getUserIndex() {
        return userIndex.get();
    }

    public SimpleIntegerProperty userIndexProperty() {
        return userIndex;
    }
}