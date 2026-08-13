package cn.tealc.wutheringwavestool.ui.kujiequ.sign;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.service.SignService;
import cn.tealc.wutheringwavestool.service.UserInfoService;
import cn.tealc.wutheringwavestool.service.WebKujiequManager;
import cn.tealc.wutheringwavestool.thread.SignTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.kujiequ.api.KujiequSignApi;
import com.kuro.kujiequ.model.sign.SignGood;
import com.kuro.kujiequ.model.sign.SignRecord;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-07 17:53
 */
public class SignViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(SignViewModel.class);
    @Inject
    private UserInfoService userInfoService;

    @Inject
    private SignService signService;

    @Inject
    private WebKujiequManager webKujiequManager;

    @Inject
    private KujiequManager kujiequManager;

    private final ObservableList<UserInfo> userInfoList= FXCollections.observableArrayList();
    private final SimpleIntegerProperty userIndex = new SimpleIntegerProperty(-1);
    private final ObservableList<SignGood> goodsList= FXCollections.observableArrayList();
    private final SimpleStringProperty logs=new SimpleStringProperty();
    private final SimpleBooleanProperty isSign=new SimpleBooleanProperty(true);
    private final ObservableList<SignRecord> signHistoryList= FXCollections.observableArrayList();

    public void init() {
        List<UserInfo> userInfos = userInfoService.getAllUsers();
        userInfoList.setAll(userInfos);
        UserInfo main = userInfoService.getMainUser();
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
           publish("EMPTY");
        }

        userIndex.addListener((observableValue, number, t1) -> {
            if (t1 != null) {
                getSignGoods(userInfoList.get(t1.intValue()));

            }
        });


    }

    private void getSignHistory(UserInfo userInfo){
        List<SignRecord> histories = signService.getHistoriesByRoleId(userInfo.getRoleId());


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
            Thread.startVirtualThread(() -> {
                try {
                    ResponseBody<KujiequSignApi.SignGoodsResult> value = kujiequManager.getSignGoods(userInfo);
                    if (value.getCode() == 200 && value.getData() != null){
                        KujiequSignApi.SignGoodsResult data = value.getData();
                        Platform.runLater(() -> {
                            goodsList.setAll(data.getSignGoods());
                            isSign.set(data.getIsSign());
                        });
                    }else {
                        Platform.runLater(() ->
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    MessageInfo.warning(value.getMsg()),false));
                    }
                } catch (Exception e) {
                    LOG.error("获取签到物品失败", e);
                    Platform.runLater(() ->
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    MessageInfo.error("获取签到物品失败，请检查网络后重试"), false));
                }
            });
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
        task.setOnFailed(workerStateEvent -> {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    MessageInfo.error("签到失败，请检查网络后重试"), false);
        });
        Thread.startVirtualThread(task);
    }

    public void openMonthSignInWebView() {
        int index = userIndex.get();
        if (index >= 0 && index < userInfoList.size()) {
            UserInfo current = userInfoList.get(index);
            webKujiequManager.setUserInfo(current);
        }
        webKujiequManager.openMonthSign();
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