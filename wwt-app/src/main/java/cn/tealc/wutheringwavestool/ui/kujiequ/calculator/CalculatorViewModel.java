package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.service.UserInfoService;
import cn.tealc.wutheringwavestool.service.WebKujiequManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.model.ResponseBody;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 17:00
 */
public class CalculatorViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(CalculatorViewModel.class);
    private ObservableList<RoleForCalculator> roleList = FXCollections.observableArrayList();
    private ObservableList<WeaponForCalculator> weaponList = FXCollections.observableArrayList();
    @Inject
    private UserInfoService userInfoService;

    @Inject
    private WebKujiequManager webKujiequManager;

    @Inject
    private KujiequManager kujiequManager;

    private UserInfo userInfo;

    public CalculatorViewModel() {

    }

    public void init(){
        userInfo = userInfoService.getMainUser();
        if (userInfo != null){
            //刷新缓存数据后再获取
            Thread.startVirtualThread(() -> {
                try {
                    kujiequManager.refreshCalculatorData(userInfo);
                    syncRoleData(userInfo);
                } catch (Exception e) {
                    LOG.error("刷新缓存失败", e);
                    Platform.runLater(() ->
                            NotificationManager.message(MessageInfo.error("刷新缓存失败，请检查网络后重试")));
                }
            });
        }else {
            publish("EMPTY");
        }
    }

    /**
     * 获取角色与武器列表
     * @param userInfo
     */
    private void syncRoleData(UserInfo userInfo) {
        Thread.startVirtualThread(() -> {
            try {
                ResponseBody<List<WeaponForCalculator>> responseBody = kujiequManager.listCalculatorWeapon(userInfo);
                if (responseBody.getCode() == 200 && responseBody.getData() != null){
                    Platform.runLater(() -> weaponList.setAll(responseBody.getData()));
                }
            } catch (Exception e) {
                LOG.error("获取武器列表失败", e);
                Platform.runLater(() ->
                        NotificationManager.message(MessageInfo.error("获取武器列表失败，请检查网络后重试")));
            }
        });

        Thread.startVirtualThread(() -> {
            try {
                ResponseBody<List<RoleForCalculator>> responseBody = kujiequManager.listCalculatorRole(userInfo);
                if (responseBody.getCode() == 200 && responseBody.getData() != null){
                    Platform.runLater(() -> roleList.setAll(responseBody.getData()));
                }else {
                    Platform.runLater(() ->
                            NotificationManager.message(MessageInfo.warning(responseBody.getMsg())));
                }
            } catch (Exception e) {
                LOG.error("获取角色列表失败", e);
                Platform.runLater(() ->
                        NotificationManager.message(MessageInfo.error("获取角色列表失败，请检查网络后重试")));
            }
        });
    }


    public void openGrowthCalculatorInWebView() {
        if (userInfo != null) {
            webKujiequManager.setUserInfo(userInfo);
        }
        webKujiequManager.openGrowthCalculator();
    }


    public ObservableList<RoleForCalculator> getRoleList() {
        return roleList;
    }

    public ObservableList<WeaponForCalculator> getWeaponList() {
        return weaponList;
    }
}