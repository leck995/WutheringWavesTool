package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.Challenge;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.slash.SlashDifficulty;
import com.kuro.kujiequ.thread.slash.SlashDataDetailTask;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SlashViewModel implements ViewModel {
    private final ObservableList<SlashDifficulty> difficulties = FXCollections.observableArrayList();
    private final ObservableList<Challenge> challenges = FXCollections.observableArrayList();

    private final SimpleStringProperty title = new SimpleStringProperty();
    private final SimpleStringProperty endTime = new SimpleStringProperty();
    private List<SlashDifficulty> sourceDifficulties;
    private final SimpleBooleanProperty endTimeVisible = new SimpleBooleanProperty(true);

    public SlashViewModel() {
        initialize();
    }


    private void initialize(){
        UserInfoDao userInfoDao = new UserInfoDao();
        UserInfo userInfo = userInfoDao.getMain();
        if (userInfo != null) {
            SlashDataDetailTask task = new SlashDataDetailTask(userInfo);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<SlashData> value = task.getValue();
                if (value.isSuccess()) {
                    updateDate(value.getData());
                }
            });
            Thread.startVirtualThread(task);
        }
    }


    public void changeDifficulty(int index) {
        SlashDifficulty slashDifficulty = difficulties.get(index);
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
        updateSeasonEndTime(data.getSeasonEndTime());
        changeDifficulty(0);
    }




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
}