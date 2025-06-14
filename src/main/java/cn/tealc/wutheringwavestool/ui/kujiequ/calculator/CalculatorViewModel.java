package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.calculator.CalculatorDataRefreshTask;
import com.kuro.kujiequ.thread.rolebox.calculator.ListRoleTask;
import com.kuro.kujiequ.thread.rolebox.calculator.ListWeaponTask;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 17:00
 */
public class CalculatorViewModel implements ViewModel {
    private ObservableList<RoleForCalculator> roleList = FXCollections.observableArrayList();
    private ObservableList<WeaponForCalculator> weaponList = FXCollections.observableArrayList();
    public CalculatorViewModel() {

    }

    public void ready(){
        UserInfoDao userInfoDao = new UserInfoDao();
        UserInfo userInfo = userInfoDao.getMain();
        //刷新缓存数据后再获取
        CalculatorDataRefreshTask calculatorDataRefreshTask = new CalculatorDataRefreshTask(userInfo);
        calculatorDataRefreshTask.setOnSucceeded(event -> {
            syncRoleData(userInfo);
        });
        Thread.startVirtualThread(calculatorDataRefreshTask);
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
        Thread.startVirtualThread(roleTask);
    }


    public ObservableList<RoleForCalculator> getRoleList() {
        return roleList;
    }

    public ObservableList<WeaponForCalculator> getWeaponList() {
        return weaponList;
    }
}