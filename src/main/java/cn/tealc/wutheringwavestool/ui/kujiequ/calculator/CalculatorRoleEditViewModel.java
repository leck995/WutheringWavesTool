package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.model.calculator.exist.RoleAim;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.result.CalculatorResult;
import com.kuro.kujiequ.model.calculator.result.Cost;
import com.kuro.kujiequ.model.roleData.RoleDetail;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.calculator.BatchRoleCostTask;
import com.kuro.kujiequ.thread.rolebox.role.GameRoleDetailTask;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 19:42
 */
public class CalculatorRoleEditViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(CalculatorRoleEditViewModel.class);
    private SimpleStringProperty roleName = new SimpleStringProperty();
    private SimpleObjectProperty<Image> icon = new SimpleObjectProperty<>();
    private SimpleDoubleProperty roleLevel = new SimpleDoubleProperty(90);
    private SimpleDoubleProperty skillLevel01 = new SimpleDoubleProperty(10);
    private SimpleDoubleProperty skillLevel02 = new SimpleDoubleProperty(10);
    private SimpleDoubleProperty skillLevel03 = new SimpleDoubleProperty(10);
    private SimpleDoubleProperty skillLevel04 = new SimpleDoubleProperty(10);
    private SimpleDoubleProperty skillLevel05 = new SimpleDoubleProperty(10);

    private int skill01 = 1;
    private int skill02 = 1;
    private int skill03 = 1;
    private int skill04 = 1;
    private int skill05 = 1;
    private int rolelevel = 1;


    private ObservableList<Cost> totalCostList = FXCollections.observableArrayList();
    private ObservableList<Cost> missingCostList = FXCollections.observableArrayList();

    private RoleForCalculator role;
    public CalculatorRoleEditViewModel(RoleForCalculator role,Image icon) {
        this.role = role;
        this.roleName.set(role.getRoleName());
        this.icon.set(icon);

        UserInfoDao dao = new UserInfoDao();
        UserInfo userInfo = dao.getMain();
        GameRoleDetailTask task1 = new GameRoleDetailTask(userInfo,role.getRoleId());
        task1.setOnSucceeded(e -> {
            ResponseBody<RoleDetail> value = task1.getValue();
            if (value.getCode() == 200) {
                RoleDetail data = value.getData();
                if(data.getSkillList() != null) {
                    skill01 = data.getSkillList().get(4).getLevel();
                    skill02 = data.getSkillList().get(3).getLevel();
                    skill03 = data.getSkillList().get(2).getLevel();
                    skill04 = data.getSkillList().get(1).getLevel();
                    skill05 = data.getSkillList().get(0).getLevel();
                    rolelevel = data.getRole().getLevel();
                }
            }
        });
        Thread.startVirtualThread(task1);
    }

    public void ready(){

    }



    public void calculate(List<String> otherSkills){
        UserInfoDao dao = new UserInfoDao();
        UserInfo userInfo = dao.getMain();
        if (userInfo != null) {
            RoleAim roleAim = new RoleAim();
            roleAim.setRoleId(role.getRoleId());
            roleAim.setRoleStartLevel(rolelevel);
            roleAim.setRoleEndLevel((int) getRoleLevel());

            roleAim.setAdvanceSkillList(otherSkills);
            List<RoleAim.SkillLevelUp> levelUpList = new ArrayList<>();
            roleAim.setSkillLevelUpList(levelUpList);
            levelUpList.add(getSkillLevelUp(skill01,getSkillLevel01()));
            levelUpList.add(getSkillLevelUp(skill02,getSkillLevel02()));
            levelUpList.add(getSkillLevelUp(skill03,getSkillLevel03()));
            levelUpList.add(getSkillLevelUp(skill04,getSkillLevel04()));
            levelUpList.add(getSkillLevelUp(skill05,getSkillLevel05()));

            BatchRoleCostTask task = new BatchRoleCostTask(userInfo,roleAim);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<CalculatorResult> responseBody = task.getValue();
                if (responseBody.getCode() == 200){
                    CalculatorResult data = responseBody.getData();
                    totalCostList.setAll(data.getPreview().getAllCost());
                    missingCostList.setAll(data.getPreview().getMissingCost());
                }else {
                    System.out.println(responseBody.getMsg());
                }
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

    public double getRoleLevel() {
        return roleLevel.get();
    }

    public SimpleDoubleProperty roleLevelProperty() {
        return roleLevel;
    }

    public double getSkillLevel01() {
        return skillLevel01.get();
    }

    public SimpleDoubleProperty skillLevel01Property() {
        return skillLevel01;
    }

    public double getSkillLevel02() {
        return skillLevel02.get();
    }

    public SimpleDoubleProperty skillLevel02Property() {
        return skillLevel02;
    }

    public double getSkillLevel03() {
        return skillLevel03.get();
    }

    public SimpleDoubleProperty skillLevel03Property() {
        return skillLevel03;
    }

    public double getSkillLevel04() {
        return skillLevel04.get();
    }

    public SimpleDoubleProperty skillLevel04Property() {
        return skillLevel04;
    }

    public double getSkillLevel05() {
        return skillLevel05.get();
    }

    public SimpleDoubleProperty skillLevel05Property() {
        return skillLevel05;
    }

    public ObservableList<Cost> getTotalCostList() {
        return totalCostList;
    }

    public ObservableList<Cost> getMissingCostList() {
        return missingCostList;
    }
}