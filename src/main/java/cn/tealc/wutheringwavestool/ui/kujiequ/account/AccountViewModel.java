package cn.tealc.wutheringwavestool.ui.kujiequ.account;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.service.UserInfoService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.PlayerBaseDataTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-04 00:26
 */
public class AccountViewModel extends BaseViewModel {
    @Inject
    private UserInfoService userInfoService;

    private final ObservableList<UserInfo> accountList = FXCollections.observableArrayList();

    public void initialize() {
        refreshUserList();
        NotificationManager.subscribe(NotificationKey.ACCOUNT_UPDATE, (s, objects) -> refreshUserList());
    }


    private void refreshUserList() {
        List<UserInfo> userInfos = userInfoService.getAllUsers();
        accountList.setAll(userInfos);
    }


    public boolean addUser(UserInfo userInfo) {
        if (userInfoService.existsByRoleId(userInfo.getRoleId())) {
            return false;
        }
        if (userInfo.getMain()) {
            userInfoService.changeMainUser(userInfo);
        }
        int id = userInfoService.addUser(userInfo);
        userInfo.setId(id);
        accountList.add(userInfo);
        return true;
    }

    public boolean updateUser(int index, UserInfo userInfo) {
        UserInfo oldUserInfo = accountList.get(index);
        UserInfo daoUserByRoleId = userInfoService.getUserByRoleId(oldUserInfo.getRoleId());
        if (daoUserByRoleId != null && Objects.equals(daoUserByRoleId.getId(), oldUserInfo.getId())) {
            if (userInfo.getMain()) {
                userInfoService.changeMainUser(userInfo);
            }
            userInfo.setId(oldUserInfo.getId());
            userInfo.setLastSignTime(oldUserInfo.getLastSignTime());
            userInfoService.updateUser(userInfo);
            accountList.set(index, userInfo);
            return true;
        }
        return false;
    }

    public boolean deleteUser(int index, UserInfo userInfo) {
        boolean i = userInfoService.deleteUser(userInfo.getId());
        if (i) {
            accountList.remove(index);
            return true;
        }
        return false;
    }

    public void getUserInfo(UserInfo userInfo) {
        PlayerBaseDataTask task = new PlayerBaseDataTask(userInfo);
        task.setOnSucceeded(event -> {
        });
    }


    public ObservableList<UserInfo> getAccountList() {
        return accountList;
    }

}