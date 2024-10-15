package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.SignHistoryDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.SignGood;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.SignRecord;
import cn.tealc.wutheringwavestool.model.kujiequ.sign.UserInfo;
import cn.tealc.wutheringwavestool.thread.api.SignGoodsTask;
import cn.tealc.wutheringwavestool.thread.api.SignTask;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.*;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-07 17:53
 */
public class SignViewModel implements ViewModel {
    private final ObservableList<UserInfo> userInfoList= FXCollections.observableArrayList();
    private final SimpleIntegerProperty userIndex = new SimpleIntegerProperty(-1);
    private final ObservableList<SignGood> goodsList= FXCollections.observableArrayList();
    private final SimpleStringProperty logs=new SimpleStringProperty();
    private final SimpleBooleanProperty isSign=new SimpleBooleanProperty(true);
    private final ObservableList<SignRecord> signHistoryList= FXCollections.observableArrayList();

    public SignViewModel() {
        UserInfoDao dao=new UserInfoDao();
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
            getSignGoods(main);
            getSignHistory(main);
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING,"当前不存在主用户信息，无法获取，请在账号界面添加用户信息"),false);
        }

        userIndex.addListener((observableValue, number, t1) -> {
            if (t1 != null) {
                getSignGoods(userInfoList.get(t1.intValue()));

            }
        });


    }

    private void getSignHistory(UserInfo userInfo){
        SignHistoryDao dao=new SignHistoryDao();
        List<SignRecord> histories = dao.getHistoriesByRoleId(userInfo.getRoleId());


        Map<String,SignRecord> map=new HashMap<>();
        for (SignRecord history : histories) {
            String key = history.getGoodsName();
            if (!map.containsKey(key)) {
                SignRecord record = new SignRecord();
                record.setGoodsName(history.getGoodsName());
                record.setGoodsNum(history.getGoodsNum());
                record.setGoodsUrl(history.getGoodsUrl());
                record.setType(history.getType());
                map.put(key,record);
            }else {
                SignRecord record = map.get(key);
                record.setGoodsNum(record.getGoodsNum()+history.getGoodsNum());
            }
        }
        signHistoryList.setAll(map.values());
        signHistoryList.sort(((o1, o2) -> {
            if (o2.getGoodsName().equals("星声")){
                return 1;
            }else {
                return Integer.compare(o2.getType(),o1.getType());
            }
        }));
    }

    private void getSignGoods(UserInfo userInfo){
        if (userInfo != null){
            SignGoodsTask task=new SignGoodsTask(userInfo);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<Pair<Boolean, List<SignGood>>> value = task.getValue();
                if (value.getCode() == 200){
                    Pair<Boolean, List<SignGood>> data = value.getData();
                    goodsList.setAll(data.getValue());
                    isSign.set(data.getKey());
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING, value.getMsg()),false);
                }
            });
            Thread.startVirtualThread(task);
        }
    }


    /**
     * @description: 签到
     * @param:
     * @return  void
     * @date:   2024/7/7
     */
    public void sign(){
        SignTask task=new SignTask();
        task.setOnSucceeded(workerStateEvent -> {
            logs.set(task.getValue());
            getSignGoods(userInfoList.get(userIndex.get()));
            getSignHistory(userInfoList.get(userIndex.get()));

        });
        Thread.startVirtualThread(task);
    }

    public int getUserIndex() {
        return userIndex.get();
    }

    public SimpleIntegerProperty userIndexProperty() {
        return userIndex;
    }

    public ObservableList<UserInfo> getUserInfoList() {
        return userInfoList;
    }

    public ObservableList<SignGood> getGoodsList() {
        return goodsList;
    }

    public String getLogs() {
        return logs.get();
    }

    public SimpleStringProperty logsProperty() {
        return logs;
    }

    public boolean isIsSign() {
        return isSign.get();
    }

    public SimpleBooleanProperty isSignProperty() {
        return isSign;
    }

    public ObservableList<SignRecord> getSignHistoryList() {
        return signHistoryList;
    }
}