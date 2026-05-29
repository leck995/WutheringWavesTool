package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.service.ConfigService;
import cn.tealc.wutheringwavestool.service.UserInfoService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kuro.kujiequ.model.sign.UserInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SelectedPlayerByKujiequViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(SelectedPlayerByKujiequViewModel.class);

    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private UserInfoService userInfoService;
    @Inject
    private ConfigService configService;

    private final ObservableList<UserInfo> userInfoList = FXCollections.observableArrayList();

    public SelectedPlayerByKujiequViewModel() {
        getUserInfo();
    }


    public void getUserInfo() {
        List<UserInfo> allUsers = userInfoService.getAllUsers();
        userInfoList.setAll(allUsers);
    }

    public void update(int index) {
        UserInfo userInfo = userInfoList.get(index);
        userInfoService.changeMainUser(userInfo);
        NotificationManager.publish(NotificationKey.HOME_ROLE_KUJIEQU_CHANGE, userInfo);
    }

    public ObservableList<UserInfo> getUserInfoList() {
        return userInfoList;
    }
}
