package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.dao.GameSlashDataDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.Challenge;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.slash.SlashDifficulty;
import com.kuro.kujiequ.thread.rolebox.role.GameRoleDataTask;
import com.kuro.kujiequ.thread.rolebox.slash.SlashDataDetailTask;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class SlashViewModel implements ViewModel {
    private final ObservableList<SlashDifficulty> difficulties = FXCollections.observableArrayList();
    private final ObservableList<Challenge> challenges = FXCollections.observableArrayList();
    private final ObservableList<Pair<Long, Pair<String,String>>> historyList = FXCollections.observableArrayList();
    private final SimpleStringProperty title = new SimpleStringProperty();
    private final SimpleStringProperty endTime = new SimpleStringProperty();
    private final SimpleStringProperty score01 = new SimpleStringProperty(); //海隙总积分
    private final SimpleStringProperty score02 = new SimpleStringProperty();//湍渊总积分
    private final SimpleBooleanProperty endTimeVisible = new SimpleBooleanProperty(true);
    private List<SlashDifficulty> sourceDifficulties;
    private final Map<Integer,Role> roleMap = new HashMap<>();
    public SlashViewModel() {
        initialize();
    }

    /**
     * 初始化
     * @description:
     * @param:
     * @return  void
     * @date:   2025/5/29
     */
    private void initialize(){
        UserInfoDao userInfoDao = new UserInfoDao();
        UserInfo userInfo = userInfoDao.getMain();
        if (userInfo != null) {
            SlashDataDetailTask slashDataDetailTask = new SlashDataDetailTask(userInfo);
            GameRoleDataTask roleDataTask = new GameRoleDataTask(userInfo);
            EventHandler<WorkerStateEvent> eventHandler = workerStateEvent -> {
                if (slashDataDetailTask.state() == Future.State.SUCCESS && roleDataTask.state() == Future.State.SUCCESS){
                    updateRoleMap(roleDataTask.getValue());
                    ResponseBody<SlashData> value = slashDataDetailTask.getValue();
                    if (value.isSuccess()) {
                        updateDate(value.getData());
                    }
                }
            };
            slashDataDetailTask.setOnSucceeded(eventHandler);
            roleDataTask.setOnSucceeded(eventHandler);
            Thread.startVirtualThread(slashDataDetailTask);
            Thread.startVirtualThread(roleDataTask);
        }
        initHistory();
    }

    /**
     * 初始化历史列表
     * @description:
     * @param:
     * @return  void
     * @date:   2025/5/29
     */
    private void initHistory() {
        GameSlashDataDao dao = new GameSlashDataDao();
        List<Long> endTimeList = dao.getAllEndTimes();
        SimpleDateFormat endFormat = new SimpleDateFormat("yyyy.MM.dd");
        DateTimeFormatter startFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        endTimeList.forEach(endTime -> {
            Instant instant = Instant.ofEpochMilli(endTime);
            ZonedDateTime endDate = instant.atZone(ZoneId.systemDefault());
            ZonedDateTime startDate = endDate.minusDays(28);
            Date date =new Date(endTime);
            String startDay = startFormat.format(startDate);
            String endDay = endFormat.format(date);
            historyList.add(new Pair<>(endTime, new Pair<>(startDay, endDay)));
        });
    }


    /**
     * 根据截止日期显示历史战绩
     * @description:
     * @param:	timestamp
     * @return  void
     * @date:   2025/5/29
     */
    public void changHistory(long timestamp){
        GameSlashDataDao dao = new GameSlashDataDao();
        Optional<SlashDataForDB> data = dao.getByEndTime(timestamp);
        data.ifPresent(slashData -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<SlashDifficulty> list = mapper.readValue(slashData.getData(), new TypeReference<List<SlashDifficulty>>() {
                });
                updateHistoryDifficulty(list);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * @description: 更新历史战绩具体内容
     * @param:	list
     * @return  void
     * @date:   2025/5/29
     */
    public void updateHistoryDifficulty(List<SlashDifficulty> list) {
        title.set("历史-再生海域");
        Optional<SlashDifficulty> first = list.stream().filter(difficulty -> difficulty.getDifficulty() == 1).findFirst();
        if (first.isPresent()) {
            Optional<SlashDifficulty> second = list.stream().filter(difficulty -> difficulty.getDifficulty() == 2).findFirst();
            if (second.isPresent()){
                List<Challenge> mergedList = Stream.concat(second.get().getChallengeList().stream(), first.get().getChallengeList().stream())
                        .toList();
                challenges.setAll(mergedList);
            }else {
                challenges.setAll(first.get().getChallengeList());
            }
        }
        endTimeVisible.setValue(false);
    }

    public void changeDifficulty(int index) {
        changeDifficulty(difficulties.get(index));
    }

    /**
     * @description: 切换关卡
     * @param:	slashDifficulty
     * @return  void
     * @date:   2025/5/29
     */
    public void changeDifficulty(SlashDifficulty slashDifficulty) {
        title.set(slashDifficulty.getDifficultyName());
        if (slashDifficulty.getDifficulty() == 1){ //对"无尽湍渊"与"再生海域-海隙"进行合并
            Optional<SlashDifficulty> first = sourceDifficulties.stream().filter(difficulty -> difficulty.getDifficulty() == 2).findFirst();
            if (first.isPresent()){
                List<Challenge> mergedList = Stream.concat(first.get().getChallengeList().stream(), slashDifficulty.getChallengeList().stream())
                        .toList();
                challenges.setAll(mergedList);
            }else {
                challenges.setAll(slashDifficulty.getChallengeList());
            }
            endTimeVisible.setValue(true);
        }else { //其他情况无需合并
            challenges.setAll(slashDifficulty.getChallengeList());
            endTimeVisible.setValue(false);
        }
    }


    /**
     * @description: 更新获取到的最新数据
     * @param:	data
     * @return  void
     * @date:   2025/5/29
     */
    private void updateDate(SlashData data){
        sourceDifficulties = data.getDifficultyList();
        List<SlashDifficulty> filterList = sourceDifficulties
                .stream()
                .filter(slashDifficulty -> slashDifficulty.getDifficulty() != 2)
                .sorted(Comparator.comparing(SlashDifficulty::getDifficulty).reversed())
                .peek(slashDifficulty -> {
                    if (slashDifficulty.getDifficulty() == 1) {
                        slashDifficulty.setDifficultyName("再生海域");
                    }})
                .toList(); //过滤掉"无尽湍渊"
        difficulties.setAll(filterList);


        updateScore(data);
        updateSeasonEndTime(data.getSeasonEndTime());
        changeDifficulty(0);
    }


    private void updateRoleMap(ResponseBody<List<Role>> roles){
        if (roles.getCode() == 200){
            roles.getData().forEach(role -> {
                roleMap.put(role.getRoleId(), role);
            });
        }
    }

    /**
     * @description: 更新海墟的总分数，仅在每次获取请求后更新
     * @param:	data
     * @return  void
     * @date:   2025/5/29
     */
    private void updateScore(SlashData data){
        Optional<SlashDifficulty> first = data.getDifficultyList().stream().filter(difficulty -> difficulty.getDifficulty() == 1).findFirst();
        if (first.isPresent()){
            score01.set(String.format("%d/%d",first.get().getAllScore(),first.get().getMaxScore()));
        }else {
            score01.set("");
        }

        Optional<SlashDifficulty> second = data.getDifficultyList().stream().filter(difficulty -> difficulty.getDifficulty() == 2).findFirst();
        if (second.isPresent()){
            score02.set(String.format("%d/%d",second.get().getAllScore(),second.get().getMaxScore()));
        }else {
            score02.set("");
        }
    }

    /**
     * @description: 更新每期结束时间
     * @param:	milliseconds
     * @return  void
     * @date:   2025/5/29
     */
    private void updateSeasonEndTime(long milliseconds){
        long millisecondsInADay = 24 * 60 * 60 * 1000;
        long millisecondsInAnHour = 60 * 60 * 1000;
        long days = milliseconds / millisecondsInADay;
        milliseconds %= millisecondsInADay; // 剩余毫秒数
        long hours = milliseconds / millisecondsInAnHour;
        endTime.set(String.format("%d天%d小时后刷新", days, hours));
    }

    public ObservableList<SlashDifficulty> getDifficulties() {
        return difficulties;
    }

    public ObservableList<Challenge> getChallenges() {
        return challenges;
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

    public boolean isEndTimeVisible() {
        return endTimeVisible.get();
    }

    public SimpleBooleanProperty endTimeVisibleProperty() {
        return endTimeVisible;
    }

    public ObservableList<Pair<Long, Pair<String, String>>> getHistoryList() {
        return historyList;
    }

    public String getScore01() {
        return score01.get();
    }

    public SimpleStringProperty score01Property() {
        return score01;
    }

    public String getScore02() {
        return score02.get();
    }

    public SimpleStringProperty score02Property() {
        return score02;
    }

    public Map<Integer, Role> getRoleMap() {
        return roleMap;
    }
}