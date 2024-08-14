package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.roleData.Phantom;
import cn.tealc.wutheringwavestool.model.roleData.Role;
import cn.tealc.wutheringwavestool.model.roleData.RoleDetail;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.thread.ImgColorBgTask;
import cn.tealc.wutheringwavestool.thread.role.GameRoleDetailTask;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Pair;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-30 17:05
 */
public class OwnRoleDetailViewModel implements ViewModel {
    private static final LinearGradient SSR = new LinearGradient(
            0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#fad9578c")),
            new Stop(1.0, Color.web("#f7d23be8")));
    private static final LinearGradient SR = new LinearGradient(
            0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#d7afffff")),
            new Stop(1.0, Color.web("#f7beffff")));
    private static final LinearGradient R = new LinearGradient(
            0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#fdfdfd")),
            new Stop(1.0, Color.web("#bab9b9")));



    private SimpleObjectProperty<Image> roleAttrImage=new SimpleObjectProperty<>();
    private SimpleStringProperty roleName = new SimpleStringProperty();
    private SimpleStringProperty roleLevel = new SimpleStringProperty();
    private SimpleObjectProperty<Image> roleImage=new SimpleObjectProperty<>();
    private SimpleStringProperty weaponName=new SimpleStringProperty();
    private SimpleStringProperty weaponLevel=new SimpleStringProperty();
    private SimpleStringProperty weaponResonLevel=new SimpleStringProperty();
    private SimpleObjectProperty<Image> weaponImage=new SimpleObjectProperty<>();
    private SimpleIntegerProperty weaponStarLevel=new SimpleIntegerProperty();
    private SimpleObjectProperty<Background> weaponBg=new SimpleObjectProperty();
    private SimpleStringProperty skill01=new SimpleStringProperty();
    private SimpleStringProperty skill02=new SimpleStringProperty();
    private SimpleStringProperty skill03=new SimpleStringProperty();
    private SimpleStringProperty skill04=new SimpleStringProperty();
    private SimpleStringProperty skill05=new SimpleStringProperty();
    private SimpleObjectProperty<Image> skillImg01=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> skillImg02=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> skillImg03=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> skillImg04=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> skillImg05=new SimpleObjectProperty<>();

    private SimpleObjectProperty<Image> chainImg01=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg02=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg03=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg04=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg05=new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg06=new SimpleObjectProperty<>();

    private SimpleBooleanProperty chainImgVisible01=new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible02=new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible03=new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible04=new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible05=new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible06=new SimpleBooleanProperty();

    private ObservableList<Pair<Role,Image>> rolePairList= FXCollections.observableArrayList();

    private SignUserInfo userInfo;

    private SimpleIntegerProperty selectIndex=new SimpleIntegerProperty();

    private SimpleObjectProperty<Background> roleBg=new SimpleObjectProperty<>();

    private ObservableList<Pair<Phantom, Image>> phantomList= FXCollections.observableArrayList();
    private SimpleStringProperty phantomCost=new SimpleStringProperty();



    public OwnRoleDetailViewModel(SignUserInfo userInfo, int selectIndex, List<Pair<Role,Image>> rolePairList) {
        this.userInfo = userInfo;
        this.rolePairList.setAll(rolePairList);
        this.selectIndex.set(selectIndex);
        load();
    }

    public void select(int index){
        selectIndex.set(index);
        load();
    }



    private void load(){
        Pair<Role, Image> pair = rolePairList.get(selectIndex.get());
        GameRoleDetailTask task=new GameRoleDetailTask(userInfo,pair.getKey().getRoleId());
        task.setOnSucceeded(e -> {
            ResponseBody<RoleDetail> value = task.getValue();
            if (value.getSuccess()){
                RoleDetail data = value.getData();
                roleName.set(data.getRole().getRoleName());
                roleLevel.set(String.format("LV.%d",data.getRole().getLevel()));
                roleImage.set(LocalResourcesManager.imageBuffer(data.getRole().getRolePicUrl(),500,380,true,true));

                roleAttrImage.set(
                        new Image(
                                FXResourcesLoader.load(
                                        String.format("image/attr/%d.png",data.getRole().getAttributeId())
                                ),true));

                weaponName.set(data.getWeaponData().getWeapon().getWeaponName());
                weaponResonLevel.set(String.format("突破%d",data.getWeaponData().getResonLevel()));
                weaponLevel.set(String.format("LV.%d",data.getWeaponData().getLevel()));
                weaponImage.set(new Image(data.getWeaponData().getWeapon().getWeaponIcon(),true));
                weaponStarLevel.set(data.getWeaponData().getWeapon().getWeaponStarLevel());



                weaponBg.set(
                        switch(weaponStarLevel.get()){
                            case 5 -> new Background(new BackgroundFill(SSR,new CornerRadii(8),null));
                            case 4 -> new Background(new BackgroundFill(SR,new CornerRadii(8),null));
                            default -> new Background(new BackgroundFill(R,new CornerRadii(8),null));
                });

                skill01.set(String.format("LV.%d",data.getSkillList().get(0).getLevel()));
                skill02.set(String.format("LV.%d",data.getSkillList().get(1).getLevel()));
                skill03.set(String.format("LV.%d",data.getSkillList().get(2).getLevel()));
                skill04.set(String.format("LV.%d",data.getSkillList().get(3).getLevel()));
                skill05.set(String.format("LV.%d",data.getSkillList().get(4).getLevel()));
                skillImg01.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(0).getSkill().getIconUrl()));
                skillImg02.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(1).getSkill().getIconUrl()));
                skillImg03.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(2).getSkill().getIconUrl()));
                skillImg04.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(3).getSkill().getIconUrl()));
                skillImg05.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(4).getSkill().getIconUrl()));


       /*         skillImg02.set(new Image(data.getSkillList().get(1).getSkill().getIconUrl(),true));
                skillImg03.set(new Image(data.getSkillList().get(2).getSkill().getIconUrl(),true));
                skillImg04.set(new Image(data.getSkillList().get(3).getSkill().getIconUrl(),true));
                skillImg05.set(new Image(data.getSkillList().get(4).getSkill().getIconUrl(),true));*/

                chainImg01.set(LocalResourcesManager.imageBuffer(data.getChainList().get(0).getIconUrl()));
                chainImg02.set(LocalResourcesManager.imageBuffer(data.getChainList().get(1).getIconUrl()));
                chainImg03.set(LocalResourcesManager.imageBuffer(data.getChainList().get(2).getIconUrl()));
                chainImg04.set(LocalResourcesManager.imageBuffer(data.getChainList().get(3).getIconUrl()));
                chainImg05.set(LocalResourcesManager.imageBuffer(data.getChainList().get(4).getIconUrl()));
                chainImg06.set(LocalResourcesManager.imageBuffer(data.getChainList().get(5).getIconUrl()));



                chainImgVisible01.set(data.getChainList().get(0).isUnlocked());
                chainImgVisible02.set(data.getChainList().get(1).isUnlocked());
                chainImgVisible03.set(data.getChainList().get(2).isUnlocked());
                chainImgVisible04.set(data.getChainList().get(3).isUnlocked());
                chainImgVisible05.set(data.getChainList().get(4).isUnlocked());
                chainImgVisible06.set(data.getChainList().get(5).isUnlocked());


                phantomCost.set(String.format(String.format("COST: %d",data.getPhantomData().getCost())));
                List<Phantom> equipPhantomList = data.getPhantomData().getEquipPhantomList();
                phantomList.clear();

                if (equipPhantomList != null){
                    for (Phantom phantom : equipPhantomList) {
                        if (phantom != null){
                            phantomList.add(new Pair<>(phantom,LocalResourcesManager.imageBuffer(phantom.getPhantomProp().getIconUrl(), 65, 65, true, true)));
                        }
                    }
                }



                ImgColorBgTask imgColorBgTask=new ImgColorBgTask(data.getRole().getRoleIconUrl());
                imgColorBgTask.setOnSucceeded(workerStateEvent -> {
                    roleBg.set(imgColorBgTask.getValue());
                });
                Thread.startVirtualThread(imgColorBgTask);

            }
        });
        Thread.startVirtualThread(task);
    }


    public String getRoleName() {
        return roleName.get();
    }

    public SimpleStringProperty roleNameProperty() {
        return roleName;
    }

    public String getRoleLevel() {
        return roleLevel.get();
    }

    public SimpleStringProperty roleLevelProperty() {
        return roleLevel;
    }

    public Image getRoleImage() {
        return roleImage.get();
    }

    public SimpleObjectProperty<Image> roleImageProperty() {
        return roleImage;
    }

    public String getWeaponLevel() {
        return weaponLevel.get();
    }

    public SimpleStringProperty weaponLevelProperty() {
        return weaponLevel;
    }

    public String getWeaponResonLevel() {
        return weaponResonLevel.get();
    }

    public SimpleStringProperty weaponResonLevelProperty() {
        return weaponResonLevel;
    }

    public Image getWeaponImage() {
        return weaponImage.get();
    }

    public SimpleObjectProperty<Image> weaponImageProperty() {
        return weaponImage;
    }

    public String getSkill01() {
        return skill01.get();
    }

    public SimpleStringProperty skill01Property() {
        return skill01;
    }

    public String getSkill02() {
        return skill02.get();
    }

    public SimpleStringProperty skill02Property() {
        return skill02;
    }

    public String getSkill03() {
        return skill03.get();
    }

    public SimpleStringProperty skill03Property() {
        return skill03;
    }

    public String getSkill04() {
        return skill04.get();
    }

    public SimpleStringProperty skill04Property() {
        return skill04;
    }

    public String getSkill05() {
        return skill05.get();
    }

    public SimpleStringProperty skill05Property() {
        return skill05;
    }

    public Image getSkillImg01() {
        return skillImg01.get();
    }

    public SimpleObjectProperty<Image> skillImg01Property() {
        return skillImg01;
    }

    public Image getSkillImg02() {
        return skillImg02.get();
    }

    public SimpleObjectProperty<Image> skillImg02Property() {
        return skillImg02;
    }

    public Image getSkillImg03() {
        return skillImg03.get();
    }

    public SimpleObjectProperty<Image> skillImg03Property() {
        return skillImg03;
    }

    public Image getSkillImg04() {
        return skillImg04.get();
    }

    public SimpleObjectProperty<Image> skillImg04Property() {
        return skillImg04;
    }

    public Image getSkillImg05() {
        return skillImg05.get();
    }

    public SimpleObjectProperty<Image> skillImg05Property() {
        return skillImg05;
    }

    public Image getChainImg01() {
        return chainImg01.get();
    }

    public SimpleObjectProperty<Image> chainImg01Property() {
        return chainImg01;
    }

    public Image getChainImg02() {
        return chainImg02.get();
    }

    public SimpleObjectProperty<Image> chainImg02Property() {
        return chainImg02;
    }

    public Image getChainImg03() {
        return chainImg03.get();
    }

    public SimpleObjectProperty<Image> chainImg03Property() {
        return chainImg03;
    }

    public Image getChainImg04() {
        return chainImg04.get();
    }

    public SimpleObjectProperty<Image> chainImg04Property() {
        return chainImg04;
    }

    public Image getChainImg05() {
        return chainImg05.get();
    }

    public SimpleObjectProperty<Image> chainImg05Property() {
        return chainImg05;
    }

    public Image getChainImg06() {
        return chainImg06.get();
    }

    public SimpleObjectProperty<Image> chainImg06Property() {
        return chainImg06;
    }

    public ObservableList<Pair<Role, Image>> getRolePairList() {
        return rolePairList;
    }

    public String getWeaponName() {
        return weaponName.get();
    }

    public SimpleStringProperty weaponNameProperty() {
        return weaponName;
    }

    public boolean isChainImgVisible01() {
        return chainImgVisible01.get();
    }

    public SimpleBooleanProperty chainImgVisible01Property() {
        return chainImgVisible01;
    }

    public boolean isChainImgVisible02() {
        return chainImgVisible02.get();
    }

    public SimpleBooleanProperty chainImgVisible02Property() {
        return chainImgVisible02;
    }

    public boolean isChainImgVisible03() {
        return chainImgVisible03.get();
    }

    public SimpleBooleanProperty chainImgVisible03Property() {
        return chainImgVisible03;
    }

    public boolean isChainImgVisible04() {
        return chainImgVisible04.get();
    }

    public SimpleBooleanProperty chainImgVisible04Property() {
        return chainImgVisible04;
    }

    public boolean isChainImgVisible05() {
        return chainImgVisible05.get();
    }

    public SimpleBooleanProperty chainImgVisible05Property() {
        return chainImgVisible05;
    }

    public boolean isChainImgVisible06() {
        return chainImgVisible06.get();
    }

    public SimpleBooleanProperty chainImgVisible06Property() {
        return chainImgVisible06;
    }

    public Background getRoleBg() {
        return roleBg.get();
    }

    public SimpleObjectProperty<Background> roleBgProperty() {
        return roleBg;
    }

    public int getWeaponStarLevel() {
        return weaponStarLevel.get();
    }

    public SimpleIntegerProperty weaponStarLevelProperty() {
        return weaponStarLevel;
    }

    public Background getWeaponBg() {
        return weaponBg.get();
    }

    public SimpleObjectProperty<Background> weaponBgProperty() {
        return weaponBg;
    }

    public ObservableList<Pair<Phantom, Image>> getPhantomList() {
        return phantomList;
    }

    public String getPhantomCost() {
        return phantomCost.get();
    }

    public SimpleStringProperty phantomCostProperty() {
        return phantomCost;
    }

    public Image getRoleAttrImage() {
        return roleAttrImage.get();
    }

    public SimpleObjectProperty<Image> roleAttrImageProperty() {
        return roleAttrImage;
    }
}