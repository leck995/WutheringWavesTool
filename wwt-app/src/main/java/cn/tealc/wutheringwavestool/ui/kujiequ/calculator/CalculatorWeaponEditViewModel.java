package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import com.kuro.kujiequ.model.calculator.exist.WeaponAim;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.calculator.result.CalculatorResult;
import com.kuro.kujiequ.model.calculator.result.Cost;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.calculator.BatchWeaponCostTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 22:33
 */
public class CalculatorWeaponEditViewModel extends BaseViewModel {
    private SimpleStringProperty weaponName = new SimpleStringProperty();
    private SimpleObjectProperty<Image> icon = new SimpleObjectProperty<>();
    private SimpleDoubleProperty startLevel = new SimpleDoubleProperty(1);
    private SimpleDoubleProperty endLevel = new SimpleDoubleProperty(90);
    private ObservableList<Cost> totalCostList = FXCollections.observableArrayList();
    private ObservableList<Cost> missingCostList = FXCollections.observableArrayList();
    private final ObservableList<Cost> relateCostList = FXCollections.observableArrayList();
    private WeaponForCalculator weapon;

    @Inject
    private UserInfoDao userInfoDao;

    public CalculatorWeaponEditViewModel(WeaponForCalculator weapon, Image icon) {
        this.weapon = weapon;
        this.weaponName.set(weapon.getWeaponName());
        this.icon.set(icon);
    }

    public void calculate(){
        if (getEndLevel() <= getStartLevel()){
            NotificationManager.message(MessageInfo.warning("开始等级必须小于结束等级"));
            return;
        }

        UserInfo userInfo = userInfoDao.getMain();
        if (userInfo != null) {
            WeaponAim weaponAim = new WeaponAim();
            weaponAim.setWeaponId(weapon.getWeaponId());
            weaponAim.setWeaponStartLevel((int)getStartLevel());
            weaponAim.setWeaponEndLevel( (int)getEndLevel());
            BatchWeaponCostTask task = new BatchWeaponCostTask(userInfo,weaponAim);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<CalculatorResult> responseBody = task.getValue();
                if (responseBody.getCode() == 200){
                    CalculatorResult data = responseBody.getData();
                    if (data.getPreview().getAllCost() != null){
                        totalCostList.setAll(data.getPreview().getAllCost());
                    }
                    if (data.getPreview().getMissingCost() != null){
                        missingCostList.setAll(data.getPreview().getMissingCost());
                    }
                    if (data.getPreview().getSynthetic() != null){
                        relateCostList.setAll(data.getPreview().getSynthetic());
                    }
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            MessageInfo.warning(responseBody.getMsg()));
                }
            });
            task.setOnFailed(workerStateEvent -> {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        MessageInfo.error("计算材料失败，请检查网络后重试"));
            });
            Thread.startVirtualThread(task);
        }



    }



    public String getWeaponName() {
        return weaponName.get();
    }

    public SimpleStringProperty weaponNameProperty() {
        return weaponName;
    }

    public Image getIcon() {
        return icon.get();
    }

    public SimpleObjectProperty<Image> iconProperty() {
        return icon;
    }

    public double getStartLevel() {
        return startLevel.get();
    }

    public SimpleDoubleProperty startLevelProperty() {
        return startLevel;
    }

    public double getEndLevel() {
        return endLevel.get();
    }

    public SimpleDoubleProperty endLevelProperty() {
        return endLevel;
    }

    public ObservableList<Cost> getTotalCostList() {
        return totalCostList;
    }

    public ObservableList<Cost> getMissingCostList() {
        return missingCostList;
    }

    public ObservableList<Cost> getRelateCostList() {
        return relateCostList;
    }
}