package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.sign.SignGood;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
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
import java.util.Objects;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-07 17:53
 */
public class SignViewModel implements ViewModel {
    private final ObservableList<UserInfo> userInfoList= FXCollections.observableArrayList();
    private final ObservableList<SignGood> goodsList= FXCollections.observableArrayList();
    private final SimpleStringProperty logs=new SimpleStringProperty();

    public SignViewModel() {
        UserInfoDao dao=new UserInfoDao();
        List<UserInfo> userInfos = dao.getAll();
        userInfoList.setAll(userInfos);
        getSignGoods();

        MvvmFX.getNotificationCenter().subscribe(NotificationKey.SIGN_USER_DELETE,((s, objects) -> {
            UserInfo userInfo = (UserInfo) objects[0];
            deleteUser(userInfo);
        }));
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.SIGN_USER_UPDATE,((s, objects) -> {
            int index = (int) objects[0];
            UserInfo userInfo = (UserInfo) objects[1];
            updateUser(index,userInfo);
        }));
    }




    private void getSignGoods(){
        UserInfoDao dao=new UserInfoDao();
        UserInfo main = dao.getMain();
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




    public boolean addUser(UserInfo userInfo) {
        UserInfoDao dao=new UserInfoDao();
        UserInfo daoUserByRoleId = dao.getUserByRoleId(userInfo.getRoleId());
        if (daoUserByRoleId == null){
            if (userInfo.getMain()){
                for (UserInfo oldMainUser : userInfoList) {
                    if (oldMainUser.getMain()){
                        oldMainUser.setMain(false);
                        dao.updateUser(oldMainUser);
                    }
                }
            }
            dao.addUser(userInfo);
            userInfoList.add(userInfo);
            return true;
        }else {
            return false;
        }
    }
    public boolean updateUser(int index,UserInfo userInfo) {
        UserInfo oldUserInfo = userInfoList.get(index);
        UserInfoDao dao=new UserInfoDao();
        UserInfo daoUserByRoleId = dao.getUserByRoleId(userInfo.getRoleId());
        if (daoUserByRoleId != null && Objects.equals(daoUserByRoleId.getId(), oldUserInfo.getId())){ //当roleid存在，且id是一个，允许更新
            if (userInfo.getMain()){
                for (UserInfo oldMainUser : userInfoList) {
                    if (oldMainUser.getMain()){
                        oldMainUser.setMain(false);
                        dao.updateUser(oldMainUser);
                    }
                }
            }
            userInfo.setId(oldUserInfo.getId());
            userInfo.setLastSignTime(oldUserInfo.getLastSignTime());
            dao.updateUser(userInfo);

            userInfoList.set(index,userInfo);
            return true;

        }

        return false;
    }


    public boolean deleteUser(UserInfo userInfo) {
        UserInfoDao dao=new UserInfoDao();
        int i = dao.deleteUser(userInfo.getId());
        if (i > 0){
            userInfoList.remove(userInfo);
            return true;
        }

        return false;
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
}