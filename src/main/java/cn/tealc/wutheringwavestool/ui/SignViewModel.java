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
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.SIGN_USER_UPDATE,((s, objects) -> {
            int index = (int) objects[0];
            SignUserInfo userInfo = (SignUserInfo) objects[1];
            updateUser(index,userInfo);
        }));
    }



    private SignUserInfo getMain(){
        List<SignUserInfo> list = userInfoList.filtered(SignUserInfo::getMain).stream().toList();
        if (!list.isEmpty()){
            return list.getFirst();
        }else {
            if (!userInfoList.isEmpty()){
                return userInfoList.getFirst();
            }else
                return null;
        }
    }

    private void getSignGoods(){
        SignUserInfo main = getMain();
        if (main != null){
            SignGoodsTask task=new SignGoodsTask(main);
            task.setOnSucceeded(workerStateEvent -> {
                goodsList.setAll(task.getValue().getValue());
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
            System.out.println(task.getValue());
            getSignGoods();
            logs.set(task.getValue());

        });
        Thread.startVirtualThread(task);
    }




    public boolean addUser(SignUserInfo userInfo) {
        long count = userInfoList.stream()
                .filter(signUserInfo -> signUserInfo.getUserId().equals(userInfo.getUserId()) && signUserInfo.getRoleId().equals(userInfo.getRoleId()))
                .count();
        if(count == 0){
            if (userInfo.getMain()){
                for (SignUserInfo signUserInfo : userInfoList) {
                    if (signUserInfo.getMain()){
                        signUserInfo.setMain(false);
                    }
                }
            }

            if (userInfoList.isEmpty()){
                userInfoList.add(userInfo);
            }else {
                userInfoList.add(userInfo);
                getSignGoods();
            }


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
    public boolean updateUser(int index,SignUserInfo userInfo) {
        if (userInfo.getMain()){
            for (SignUserInfo signUserInfo : userInfoList) {
                if (signUserInfo.getMain()){
                    signUserInfo.setMain(false);
                }
            }
        }
        userInfoList.set(index,userInfo);

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