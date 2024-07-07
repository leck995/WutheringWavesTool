package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.model.sign.SignGood;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.thread.SignGoodsTask;
import cn.tealc.wutheringwavestool.thread.SignTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-07 17:53
 */
public class SignViewModel implements ViewModel {
    private final ObservableList<SignUserInfo> userInfoList= FXCollections.observableArrayList();
    private final ObservableList<SignGood> goodsList= FXCollections.observableArrayList();
    private final SimpleStringProperty logs=new SimpleStringProperty();

    public SignViewModel() {
        File signJson=new File("signInfo.json");
        if (signJson.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<SignUserInfo> list = mapper.readValue(signJson, new TypeReference<List<SignUserInfo>>() {
                });
                userInfoList.setAll(list);
                getSignGoods();



            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }





        MvvmFX.getNotificationCenter().subscribe(NotificationKey.SIGN_USER_DELETE,((s, objects) -> {
            SignUserInfo userInfo = (SignUserInfo) objects[0];
            deleteUser(userInfo);
        }));
    }




    private void getSignGoods(){
        SignGoodsTask task=new SignGoodsTask(userInfoList.getFirst());
        task.setOnSucceeded(workerStateEvent -> {
            goodsList.setAll(task.getValue().getValue());
        });
        Thread.startVirtualThread(task);
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
            System.out.println(task.getValue());
            getSignGoods();
            logs.set(task.getValue());

        });
        Thread.startVirtualThread(task);
    }




    public boolean addUser(SignUserInfo userInfo) {
        long count = userInfoList.stream()
                .filter(signUserInfo -> signUserInfo.userId().equals(userInfo.userId()) && signUserInfo.roleId().equals(userInfo.roleId()))
                .count();
        if(count == 0){
            userInfoList.add(userInfo);
            ObjectMapper mapper = new ObjectMapper();
            File signJson=new File("signInfo.json");
            Thread.startVirtualThread(()->{
                try {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(signJson,userInfoList);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        }else {
            return false;
        }
    }


    public boolean deleteUser(SignUserInfo userInfo) {
        userInfoList.remove(userInfo);
        ObjectMapper mapper = new ObjectMapper();
        File signJson=new File("signInfo.json");
        Thread.startVirtualThread(()->{
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(signJson,userInfoList);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return true;
    }


    public ObservableList<SignUserInfo> getUserInfoList() {
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
}