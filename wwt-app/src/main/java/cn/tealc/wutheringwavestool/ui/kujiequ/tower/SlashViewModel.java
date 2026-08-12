package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameSlashDataDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.Challenge;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.slash.SlashDifficulty;
import cn.tealc.wutheringwavestool.service.SlashDataService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import com.kuro.model.ResponseBody;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
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

public class SlashViewModel extends BaseViewModel {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SlashViewModel.class);
    private final ObservableList<SlashDifficulty> difficulties = FXCollections.observableArrayList();
    private final ObservableList<Challenge> challenges = FXCollections.observableArrayList();
    private final ObservableList<Pair<Long, Pair<String, String>>> historyList = FXCollections.observableArrayList();
    private final SimpleStringProperty title = new SimpleStringProperty();
    private final SimpleStringProperty endTime = new SimpleStringProperty();
    private final SimpleStringProperty score01 = new SimpleStringProperty(); //海隙总积分
    private final SimpleStringProperty score02 = new SimpleStringProperty();//湍渊总积分
    private final SimpleBooleanProperty endTimeVisible = new SimpleBooleanProperty(true);
    private final UserInfo userInfo;
    private List<SlashDifficulty> sourceDifficulties;
    private final Map<Integer, Role> roleMap = new HashMap<>();

    @Inject
    private UserInfoDao userInfoDao;
    @Inject
    private GameSlashDataDao gameSlashDataDao;
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private KujiequManager kujiequManager;
    @Inject
    private SlashDataService slashDataService;

    public SlashViewModel() {
        userInfo = userInfoDao.getMain();
    }

    /**
     * 初始化
     *
     * @return void
     * @description:
     * @param:
     * @date: 2025/5/29
     */
    public void initialize() {
        if (userInfo != null) {
            Thread.startVirtualThread(() -> {
                ResponseBody<List<Role>> roleResp;
                ResponseBody<SlashData> slashResp;
                try {
                    roleResp = kujiequManager.getGameRoleData(userInfo);
                } catch (Exception e) {
                    Platform.runLater(() ->
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    MessageInfo.error("获取角色数据失败，请检查网络后重试"), false));
                    return;
                }
                try {
                    slashResp = kujiequManager.getSlashData(userInfo);
                } catch (Exception e) {
                    Platform.runLater(() ->
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    MessageInfo.error("获取海墟数据失败，请检查网络后重试"), false));
                    return;
                }
                updateRoleMap(roleResp);
                if (slashResp.isSuccess() && slashResp.getData() != null) {
                    try {
                        slashDataService.saveToDB(slashResp.getData(), userInfo.getRoleId()); // 落库
                    } catch (JsonProcessingException e) {
                        LOG.error("海墟数据落库失败", e);
                    }
                    SlashData data = slashResp.getData();
                    Platform.runLater(() -> updateDate(data));
                }
            });
        }
        initHistory();
    }

    /**
     * 初始化历史列表
     *
     * @return void
     * @description:
     * @param:
     * @date: 2025/5/29
     */
    private void initHistory() {
        if (userInfo != null) {
            List<Long> endTimeList = gameSlashDataDao.getEndTimesByRoleId(userInfo.getRoleId());
            SimpleDateFormat endFormat = new SimpleDateFormat("yyyy.MM.dd");
            DateTimeFormatter startFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            endTimeList.forEach(endTime -> {
                Instant instant = Instant.ofEpochMilli(endTime);
                ZonedDateTime endDate = instant.atZone(ZoneId.systemDefault());
                ZonedDateTime startDate = endDate.minusDays(28);
                Date date = new Date(endTime);
                String startDay = startFormat.format(startDate);
                String endDay = endFormat.format(date);
                historyList.add(new Pair<>(endTime, new Pair<>(startDay, endDay)));
            });
        }

    }


    /**
     * 根据截止日期显示历史战绩
     *
     * @return void
     * @description:
     * @param: timestamp
     * @date: 2025/5/29
     */
    public void changHistory(long timestamp) {
        Optional<SlashDataForDB> data = gameSlashDataDao.getByRoleIdAndEndTime(userInfo.getRoleId(),timestamp);
        data.ifPresent(slashData -> {
            try {
                List<SlashDifficulty> list = objectMapper.readValue(slashData.getData(), new TypeReference<List<SlashDifficulty>>() {
                });
                updateHistoryDifficulty(list);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * @return void
     * @description: 更新历史战绩具体内容
     * @param: list
     * @date: 2025/5/29
     */
    public void updateHistoryDifficulty(List<SlashDifficulty> list) {
        title.set("历史-再生海域");
        Optional<SlashDifficulty> first = list.stream().filter(difficulty -> difficulty.getDifficulty() == 1).findFirst();
        if (first.isPresent()) {
            Optional<SlashDifficulty> second = list.stream().filter(difficulty -> difficulty.getDifficulty() == 2).findFirst();
            if (second.isPresent()) {
                List<Challenge> mergedList = new ArrayList<>();
                if (second.get().getChallengeList() != null && !second.get().getChallengeList().isEmpty() && second.get().getChallengeList().getFirst().getScore() != 0) {
                    mergedList.addAll(second.get().getChallengeList());
                }
                if (first.get().getChallengeList() != null) {
                    mergedList.addAll(first.get().getChallengeList());
                }
           /*     List<Challenge> mergedList = Stream.concat(second.get().getChallengeList().stream(), first.get().getChallengeList().stream())
                        .toList();*/
                challenges.setAll(mergedList);
            } else {
                challenges.setAll(first.get().getChallengeList());
            }
        }
        endTimeVisible.setValue(false);
    }

    public void changeDifficulty(int index) {
        changeDifficulty(difficulties.get(index));
    }

    /**
     * @return void
     * @description: 切换关卡
     * @param: slashDifficulty
     * @date: 2025/5/29
     */
    public void changeDifficulty(SlashDifficulty slashDifficulty) {
        title.set(slashDifficulty.getDifficultyName());
        if (slashDifficulty.getDifficulty() == 1) { //对"无尽湍渊"与"再生海域-海隙"进行合并
            Optional<SlashDifficulty> first = sourceDifficulties.stream().filter(difficulty -> difficulty.getDifficulty() == 2).findFirst();
            if (first.isPresent()) {
                List<Challenge> mergedList = new ArrayList<>();
                if (first.get().getChallengeList() != null && first.get().getChallengeList().isEmpty() && first.get().getChallengeList().getFirst().getScore() != 0) {
                    mergedList.addAll(first.get().getChallengeList());
                }
                if (slashDifficulty.getChallengeList() != null) {
                    mergedList.addAll(slashDifficulty.getChallengeList());
                }

          /*      List<Challenge> mergedList = Stream.concat(first.get().getChallengeList().stream(), slashDifficulty.getChallengeList().stream())
                        .toList();*/
                challenges.setAll(mergedList);
            } else {
                challenges.setAll(slashDifficulty.getChallengeList());
            }
            endTimeVisible.setValue(true);
        } else { //其他情况无需合并
            challenges.setAll(slashDifficulty.getChallengeList());
            endTimeVisible.setValue(false);
        }
    }


    /**
     * @return void
     * @description: 更新获取到的最新数据
     * @param: data
     * @date: 2025/5/29
     */
    private void updateDate(SlashData data) {
        sourceDifficulties = data.getDifficultyList();

        //getDifficulty == 0是禁忌海域，1是再生海域1-11,2是12
        List<SlashDifficulty> filterList = sourceDifficulties
                .stream()
                .filter(slashDifficulty -> slashDifficulty.getDifficulty() != 2)
                .sorted(Comparator.comparing(SlashDifficulty::getDifficulty).reversed())
                .peek(slashDifficulty -> {
                    if (slashDifficulty.getDifficulty() == 1) {
                        slashDifficulty.setDifficultyName("再生海域");
                    }
                })
                .toList(); //过滤掉"无尽湍渊"
        difficulties.setAll(filterList);
        Optional<SlashDifficulty> difficulty = filterList.stream().filter(slashDifficulty -> slashDifficulty.getDifficulty() == 1).findFirst();
        List<Challenge> list = sourceDifficulties.stream()
                .filter(slashDifficulty -> slashDifficulty.getDifficulty() == 2)
                .map(SlashDifficulty::getChallengeList)
                .flatMap(List::stream)
                .toList();
        difficulty.ifPresent(slashDifficulty -> {
            slashDifficulty.getChallengeList().addAll(0,list);
        });

        updateScore(data);
        updateSeasonEndTime(data.getSeasonEndTime());
        changeDifficulty(0);
    }


    private void updateRoleMap(ResponseBody<List<Role>> roles) {
        if (roles.getCode() == 200) {
            roles.getData().forEach(role -> {
                roleMap.put(role.getRoleId(), role);
            });
        }
    }

    /**
     * @return void
     * @description: 更新海墟的总分数，仅在每次获取请求后更新
     * @param: data
     * @date: 2025/5/29
     */
    private void updateScore(SlashData data) {
        Optional<SlashDifficulty> first = data.getDifficultyList().stream().filter(difficulty -> difficulty.getDifficulty() == 1).findFirst();
        if (first.isPresent()) {
            score01.set(String.format("%d/%d", first.get().getAllScore(), first.get().getMaxScore()));
        } else {
            score01.set("");
        }

        Optional<SlashDifficulty> second = data.getDifficultyList().stream().filter(difficulty -> difficulty.getDifficulty() == 2).findFirst();
        if (second.isPresent()) {
            score02.set(String.format("%d/%d", second.get().getAllScore(), second.get().getMaxScore()));
        } else {
            score02.set("");
        }
    }

    /**
     * @return void
     * @description: 更新每期结束时间
     * @param: milliseconds
     * @date: 2025/5/29
     */
    private void updateSeasonEndTime(long milliseconds) {
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