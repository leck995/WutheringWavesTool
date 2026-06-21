package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import com.kuro.kujiequ.model.calculator.exist.ExistedRoleDataForCalculator;
import com.kuro.kujiequ.model.calculator.exist.RoleAim;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.result.CalculatorResult;
import com.kuro.kujiequ.model.calculator.result.Cost;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.calculator.BatchRoleCostTask;
import com.kuro.kujiequ.thread.rolebox.calculator.RoleCultivateStatusTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 19:42
 */
public class CalculatorRoleEditViewModel extends BaseViewModel {
    private final SimpleStringProperty roleName = new SimpleStringProperty();
    private final SimpleObjectProperty<Image> icon = new SimpleObjectProperty<>();
    private final SimpleDoubleProperty roleHighLevel = new SimpleDoubleProperty(90);
    private final SimpleDoubleProperty roleLowLevel = new SimpleDoubleProperty(1);
    private final SimpleDoubleProperty skillHighLevel01 = new SimpleDoubleProperty(10);
    private final SimpleDoubleProperty skillHighLevel02 = new SimpleDoubleProperty(10);
    private final SimpleDoubleProperty skillHighLevel03 = new SimpleDoubleProperty(10);
    private final SimpleDoubleProperty skillHighLevel04 = new SimpleDoubleProperty(10);
    private final SimpleDoubleProperty skillHighLevel05 = new SimpleDoubleProperty(10);
    private final SimpleDoubleProperty skillLowLevel01 = new SimpleDoubleProperty(1);
    private final SimpleDoubleProperty skillLowLevel02 = new SimpleDoubleProperty(1);
    private final SimpleDoubleProperty skillLowLevel03 = new SimpleDoubleProperty(1);
    private final SimpleDoubleProperty skillLowLevel04 = new SimpleDoubleProperty(1);
    private final SimpleDoubleProperty skillLowLevel05 = new SimpleDoubleProperty(1);
    private final SimpleBooleanProperty skillBreak01 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak02 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak03 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak04 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak05 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak06 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak07 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak08 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak09 = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty skillBreak010= new SimpleBooleanProperty(true);
    private final ObservableList<Cost> totalCostList = FXCollections.observableArrayList();
    private final ObservableList<Cost> missingCostList = FXCollections.observableArrayList();
    private final ObservableList<Cost> relateCostList = FXCollections.observableArrayList();

    private final RoleForCalculator role;

    @Inject
    private UserInfoDao userInfoDao;

    public CalculatorRoleEditViewModel(RoleForCalculator role,Image icon) {
        this.role = role;
        this.roleName.set(role.getRoleName());
        this.icon.set(icon);
    }

    /**
     * 同步角色练度
     */
    public void ready(){
        UserInfo userInfo = userInfoDao.getMain();
        if (userInfo != null) {
            RoleCultivateStatusTask task = new RoleCultivateStatusTask(userInfo,role.getRoleId());
            task.setOnSucceeded(event -> {
                ResponseBody<List<ExistedRoleDataForCalculator>> value = task.getValue();
                if (value.getCode() == 200){
                    List<ExistedRoleDataForCalculator> data = value.getData();
                    if (data != null && !data.isEmpty()){ //当前账号已解锁该角色
                        ExistedRoleDataForCalculator roleStatus = data.getFirst();
                        roleLowLevel.set(roleStatus.getRoleLevel());
                        skillLowLevel01.set(roleStatus.getSkillLevelList().get(0).getLevel());
                        skillLowLevel02.set(roleStatus.getSkillLevelList().get(1).getLevel());
                        skillLowLevel03.set(roleStatus.getSkillLevelList().get(4).getLevel());
                        skillLowLevel04.set(roleStatus.getSkillLevelList().get(2).getLevel());
                        skillLowLevel05.set(roleStatus.getSkillLevelList().get(3).getLevel());

                        List<String> skillBreakList = roleStatus.getSkillBreakList();
                        skillBreak01.set(!skillBreakList.contains("2-1"));
                        skillBreak02.set(!skillBreakList.contains("3-1"));
                        skillBreak03.set(!skillBreakList.contains("2-2"));
                        skillBreak04.set(!skillBreakList.contains("3-2"));
                        skillBreak05.set(!skillBreakList.contains("2-3"));
                        skillBreak06.set(!skillBreakList.contains("3-3"));
                        skillBreak07.set(!skillBreakList.contains("2-4"));
                        skillBreak08.set(!skillBreakList.contains("3-4"));
                        skillBreak09.set(!skillBreakList.contains("2-5"));
                        skillBreak010.set(!skillBreakList.contains("3-5"));
                    }
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            MessageInfo.warning(value.getMsg()));
                }
            });
            task.setOnFailed(workerStateEvent -> {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        MessageInfo.error("同步角色练度失败，请检查网络后重试"));
            });
            Thread.startVirtualThread(task);
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    MessageInfo.warning("当前不存在主用户信息，无法获取，请在账号界面添加用户信息"));
        }
    }


    /**
     * 计算材料
     * @param otherSkills
     */
    public void calculate(List<String> otherSkills){
        UserInfo userInfo = userInfoDao.getMain();
        if (userInfo != null) {
            RoleAim roleAim = new RoleAim();
            roleAim.setRoleId(role.getRoleId());
            roleAim.setRoleStartLevel((int)getRoleLowLevel());
            roleAim.setRoleEndLevel((int) getRoleHighLevel());
            roleAim.setAdvanceSkillList(otherSkills);
            List<RoleAim.SkillLevelUp> levelUpList = new ArrayList<>();
            roleAim.setSkillLevelUpList(levelUpList);
            levelUpList.add(getSkillLevelUp(getSkillLowLevel01(),getSkillHighLevel01()));
            levelUpList.add(getSkillLevelUp(getSkillLowLevel02(),getSkillHighLevel02()));
            levelUpList.add(getSkillLevelUp(getSkillLowLevel03(),getSkillHighLevel03()));
            levelUpList.add(getSkillLevelUp(getSkillLowLevel04(),getSkillHighLevel04()));
            levelUpList.add(getSkillLevelUp(getSkillLowLevel05(),getSkillHighLevel05()));

            BatchRoleCostTask task = new BatchRoleCostTask(userInfo,roleAim);
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


    private RoleAim.SkillLevelUp getSkillLevelUp(double start,double end){
        RoleAim.SkillLevelUp roleAimSkillLevelUp = new RoleAim.SkillLevelUp();
        roleAimSkillLevelUp.setStartLevel((int)start);
        roleAimSkillLevelUp.setEndLevel((int)end);
        return roleAimSkillLevelUp;
    }

    public String getRoleName() {
        return roleName.get();
    }

    public SimpleStringProperty roleNameProperty() {
        return roleName;
    }

    public Image getIcon() {
        return icon.get();
    }

    public SimpleObjectProperty<Image> iconProperty() {
        return icon;
    }

    public double getRoleHighLevel() {
        return roleHighLevel.get();
    }

    public SimpleDoubleProperty roleHighLevelProperty() {
        return roleHighLevel;
    }

    public double getRoleLowLevel() {
        return roleLowLevel.get();
    }

    public SimpleDoubleProperty roleLowLevelProperty() {
        return roleLowLevel;
    }

    public ObservableList<Cost> getTotalCostList() {
        return totalCostList;
    }

    public ObservableList<Cost> getMissingCostList() {
        return missingCostList;
    }

    public double getSkillHighLevel01() {
        return skillHighLevel01.get();
    }

    public SimpleDoubleProperty skillHighLevel01Property() {
        return skillHighLevel01;
    }

    public double getSkillHighLevel02() {
        return skillHighLevel02.get();
    }

    public SimpleDoubleProperty skillHighLevel02Property() {
        return skillHighLevel02;
    }

    public double getSkillHighLevel03() {
        return skillHighLevel03.get();
    }

    public SimpleDoubleProperty skillHighLevel03Property() {
        return skillHighLevel03;
    }

    public double getSkillHighLevel04() {
        return skillHighLevel04.get();
    }

    public SimpleDoubleProperty skillHighLevel04Property() {
        return skillHighLevel04;
    }

    public double getSkillHighLevel05() {
        return skillHighLevel05.get();
    }

    public SimpleDoubleProperty skillHighLevel05Property() {
        return skillHighLevel05;
    }

    public double getSkillLowLevel01() {
        return skillLowLevel01.get();
    }

    public SimpleDoubleProperty skillLowLevel01Property() {
        return skillLowLevel01;
    }

    public double getSkillLowLevel02() {
        return skillLowLevel02.get();
    }

    public SimpleDoubleProperty skillLowLevel02Property() {
        return skillLowLevel02;
    }

    public double getSkillLowLevel03() {
        return skillLowLevel03.get();
    }

    public SimpleDoubleProperty skillLowLevel03Property() {
        return skillLowLevel03;
    }

    public double getSkillLowLevel04() {
        return skillLowLevel04.get();
    }

    public SimpleDoubleProperty skillLowLevel04Property() {
        return skillLowLevel04;
    }

    public double getSkillLowLevel05() {
        return skillLowLevel05.get();
    }

    public SimpleDoubleProperty skillLowLevel05Property() {
        return skillLowLevel05;
    }

    public boolean isSkillBreak01() {
        return skillBreak01.get();
    }

    public SimpleBooleanProperty skillBreak01Property() {
        return skillBreak01;
    }

    public boolean isSkillBreak02() {
        return skillBreak02.get();
    }

    public SimpleBooleanProperty skillBreak02Property() {
        return skillBreak02;
    }

    public boolean isSkillBreak03() {
        return skillBreak03.get();
    }

    public SimpleBooleanProperty skillBreak03Property() {
        return skillBreak03;
    }

    public boolean isSkillBreak04() {
        return skillBreak04.get();
    }

    public SimpleBooleanProperty skillBreak04Property() {
        return skillBreak04;
    }

    public boolean isSkillBreak05() {
        return skillBreak05.get();
    }

    public SimpleBooleanProperty skillBreak05Property() {
        return skillBreak05;
    }

    public boolean isSkillBreak06() {
        return skillBreak06.get();
    }

    public SimpleBooleanProperty skillBreak06Property() {
        return skillBreak06;
    }

    public boolean isSkillBreak07() {
        return skillBreak07.get();
    }

    public SimpleBooleanProperty skillBreak07Property() {
        return skillBreak07;
    }

    public boolean isSkillBreak08() {
        return skillBreak08.get();
    }

    public SimpleBooleanProperty skillBreak08Property() {
        return skillBreak08;
    }

    public boolean isSkillBreak09() {
        return skillBreak09.get();
    }

    public SimpleBooleanProperty skillBreak09Property() {
        return skillBreak09;
    }

    public boolean isSkillBreak010() {
        return skillBreak010.get();
    }

    public SimpleBooleanProperty skillBreak010Property() {
        return skillBreak010;
    }

    public ObservableList<Cost> getRelateCostList() {
        return relateCostList;
    }
}