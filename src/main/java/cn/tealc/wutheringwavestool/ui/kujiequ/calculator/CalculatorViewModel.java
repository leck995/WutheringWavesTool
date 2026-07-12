package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.calculator.CalculatorDataRefreshTask;
import com.kuro.kujiequ.thread.rolebox.calculator.ListRoleTask;
import com.kuro.kujiequ.thread.rolebox.calculator.ListWeaponTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 17:00
 */
public class CalculatorViewModel extends BaseViewModel {
    private ObservableList<RoleForCalculator> roleList = FXCollections.observableArrayList();
    private ObservableList<WeaponForCalculator> weaponList = FXCollections.observableArrayList();
    @Inject
    private UserInfoDao userInfoDao;

    public CalculatorViewModel() {

    }

    public void init(){
        UserInfo userInfo = userInfoDao.getMain();
        if (userInfo != null){
            //刷新缓存数据后再获取
            CalculatorDataRefreshTask calculatorDataRefreshTask = new CalculatorDataRefreshTask(userInfo);
            calculatorDataRefreshTask.setOnSucceeded(event -> {
                syncRoleData(userInfo);
            });
            calculatorDataRefreshTask.setOnFailed(workerStateEvent -> {
                NotificationManager.message(MessageInfo.error("刷新缓存失败，请检查网络后重试"));
            });
            Thread.startVirtualThread(calculatorDataRefreshTask);
        }else {
            publish("EMPTY");
        }
    }

    /**
     * 获取角色与武器列表
     * @param userInfo
     */
    private void syncRoleData(UserInfo userInfo) {
        ListWeaponTask weaponTask = new ListWeaponTask(userInfo);
        weaponTask.setOnSucceeded(e -> {
            ResponseBody<List<WeaponForCalculator>> responseBody = weaponTask.getValue();
            if (responseBody.getCode() == 200){
                weaponList.setAll(responseBody.getData());
            }else {
                NotificationManager.message(MessageInfo.error(responseBody.getMsg()));
            }
        });
        weaponTask.setOnFailed(workerStateEvent -> {
            NotificationManager.message(MessageInfo.error("获取武器列表失败，请检查网络后重试"));
        });
        Thread.startVirtualThread(weaponTask);

        ListRoleTask roleTask = new ListRoleTask(userInfo);
        roleTask.setOnSucceeded(e -> {
            ResponseBody<List<RoleForCalculator>> responseBody = roleTask.getValue();
            if (responseBody.getCode() == 200){
                roleList.setAll(responseBody.getData());
            }else {
                NotificationManager.message(MessageInfo.error(responseBody.getMsg()));
            }
        });
        roleTask.setOnFailed(workerStateEvent -> {
            NotificationManager.message(MessageInfo.error("获取角色列表失败，请检查网络后重试"));
        });
        Thread.startVirtualThread(roleTask);
    }


    public ObservableList<RoleForCalculator> getRoleList() {
        return roleList;
    }

    public ObservableList<WeaponForCalculator> getWeaponList() {
        return weaponList;
    }
}