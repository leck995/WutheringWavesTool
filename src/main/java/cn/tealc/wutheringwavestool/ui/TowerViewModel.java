package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.UserInfo;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.Difficulty;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.DifficultyTotal;
import cn.tealc.wutheringwavestool.model.kujiequ.towerData.TowerArea;
import cn.tealc.wutheringwavestool.thread.api.TowerDataDetailTask;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.Serializable;

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


    public TowerViewModel() {
        UserInfoDao userInfoDao = new UserInfoDao();
        userInfo = userInfoDao.getMain();
        if (userInfo != null) {
            getData();
        }else {

        }


    }

    private void getData(){
        TowerDataDetailTask task = new TowerDataDetailTask(userInfo);
        task.setOnSucceeded(event -> {
            //System.out.println(task.getValue().getData().get(0).getDifficultyName());
            ResponseBody<DifficultyTotal> responseBody = task.getValue();
            if (responseBody.getCode() == 200) {
                DifficultyTotal data = responseBody.getData();
                difficultyList.setAll(data.getDifficultyList());
                towerAreaList.setAll(data.getDifficultyList().getFirst().getTowerAreaList());

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
}