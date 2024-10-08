package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.roleData.*;
import cn.tealc.wutheringwavestool.model.roleData.weight.PhantomWeight;
import cn.tealc.wutheringwavestool.model.roleData.weight.PropWeight;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.thread.ImgColorBgTask;
import cn.tealc.wutheringwavestool.thread.api.role.GameRoleDetailTask;
import cn.tealc.wutheringwavestool.util.LocalDataManager;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-30 17:05
 */
public class OwnRoleDetailViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(OwnRoleDetailViewModel.class);
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

    private static final Map<String,Double> propMaxValueMap = new HashMap<>();
    static {
        propMaxValueMap.put("暴击伤害",21.0);
        propMaxValueMap.put("暴击",10.5);
        propMaxValueMap.put("攻击",60.0);
        propMaxValueMap.put("攻击百分比",11.6);
        propMaxValueMap.put("生命",580.0);
        propMaxValueMap.put("生命百分比",11.6);
        propMaxValueMap.put("防御",60.0);
        propMaxValueMap.put("防御百分比",14.7);
        propMaxValueMap.put("共鸣效率",12.4);
        propMaxValueMap.put("普攻伤害加成",11.6);
        propMaxValueMap.put("重击伤害加成",11.6);
        propMaxValueMap.put("共鸣技能伤害加成",11.6);
        propMaxValueMap.put("共鸣解放伤害加成",11.6);
    }


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
    private ObservableSet<FetterDetail> fetterDetails= FXCollections.observableSet(); //记录套装声骸效果
    private ObservableList<PhoantomMainProps> totalPhantomValueList=FXCollections.observableArrayList(); //记录声骸每个词条的总值


    private SimpleObjectProperty<Phantom.Status> phantomStatus=new SimpleObjectProperty<>();
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

                skill01.set(String.format("LV.%d",data.getSkillList().get(4).getLevel()));
                skill02.set(String.format("LV.%d",data.getSkillList().get(3).getLevel()));
                skill03.set(String.format("LV.%d",data.getSkillList().get(2).getLevel()));
                skill04.set(String.format("LV.%d",data.getSkillList().get(1).getLevel()));
                skill05.set(String.format("LV.%d",data.getSkillList().get(0).getLevel()));
                skillImg01.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(4).getSkill().getIconUrl()));
                skillImg02.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(3).getSkill().getIconUrl()));
                skillImg03.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(2).getSkill().getIconUrl()));
                skillImg04.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(1).getSkill().getIconUrl()));
                skillImg05.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(0).getSkill().getIconUrl()));


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

                fetterDetails.clear();
                totalPhantomValueList.clear();
                phantomStatus.set(null);
                phantomList.clear();
                Map<String,PhoantomMainProps> totalPhantomValueMap = new HashMap<>();
                if (equipPhantomList != null){
                    PhantomWeight weight = LocalDataManager.getWeight(getRoleName());
                    if (weight != null){
                        Map<String, Integer> subPropWeights = weight.getSubPropWeights();
                        int count= 0;
                        for (Phantom phantom : equipPhantomList) {
                            if (phantom.getSubProps() == null){
                                continue;
                            }
                            int level3=0;
                            int level2=0;
                            int level1=0;
                            int level0=0;
                            LOG.debug("================="+phantom.getPhantomProp().getName()+"==================");
                            double subCount = 0.0;
                            for (PhoantomMainProps subProp : phantom.getSubProps()) {

                                String attributeName = subProp.getAttributeName();
                                String currentValueString = subProp.getAttributeValue();
                                if (attributeName.equals("攻击") || attributeName.equals("生命") || attributeName.equals("防御") ){
                                    if (currentValueString.contains("%")){
                                        attributeName = attributeName+"百分比";
                                        subProp.setAttributeName(attributeName);
                                    }
                                }


                                double currentValue = Double.parseDouble(currentValueString.replace("%",""));
                                double maxValue = propMaxValueMap.get(attributeName);
                                Integer level = subPropWeights.get(attributeName);

                                //统计声骸词条总值
                                PhoantomMainProps totalProps = totalPhantomValueMap.get(attributeName);
                                if (totalProps != null){
                                    double v = Double.parseDouble(totalProps.getAttributeValue()) + currentValue;
                                    totalProps.setAttributeValue(String.format("%.1f",v));
                                }else {
                                    totalProps = new PhoantomMainProps();
                                    totalProps.setAttributeValue(String.format("%.1f",currentValue));
                                    totalProps.setAttributeName(attributeName);
                                    totalProps.setLevel(level);
                                    totalProps.setIconUrl(subProp.getIconUrl());
                                    totalPhantomValueMap.put(attributeName, totalProps);
                                }


                                if (level != null){
                                    subProp.setLevel(level);
                                    double percent = currentValue / maxValue;
                                    subProp.setPercent(percent);
                                    subProp.setAttributeMaxValue(maxValue);
                                    if (level  == 3){
                                        level3+=1;
                                        subCount += percent;
                                    }else if (level == 2){
                                        level2+=1;
                                        subCount += percent;
                                    }else if (level == 1){
                                        level1+=1;
                                        subCount += percent;
                                    }else{
                                        level0+=1;
                                    }
                                }

                            }

                            if (level3 == 2 && level2 + level1 == 3){ //完美
                                LOG.debug("声骸 {} 已完美",phantom.getPhantomProp().getName());
                                phantom.setStatus(Phantom.Status.ACE);
                                count += 5;
                            }else if (level3 == 2 && level2 + level1 == 2){ //大毕业
                                LOG.debug("声骸 {} 已大毕业",phantom.getPhantomProp().getName());
                                phantom.setStatus(Phantom.Status.SSS);
                                count += 4;
                            }else if (level3 == 2 && level2 + level1 == 1 || level3 == 1 && level2 + level1 == 3 ){ //大毕业
                                LOG.debug("声骸 {} 已毕业",phantom.getPhantomProp().getName());
                                phantom.setStatus(Phantom.Status.SS);
                                count += 3;
                            }else if (level3 == 2 || level3 == 1 && level2 + level1 >= 2){ //小毕业
                                LOG.debug("声骸 {} 已小毕业",phantom.getPhantomProp().getName());
                                phantom.setStatus(Phantom.Status.S);
                                count += 2;
                            }else{//普通
                                LOG.debug("声骸 {} 已不太行",phantom.getPhantomProp().getName());
                                phantom.setStatus(Phantom.Status.N);
                                count += 1;
                            }

                            if (level3 == 2 && subCount > 3.5){
                                LOG.debug("声骸词条 {} 已完美，分数：{}",phantom.getPhantomProp().getName(),subCount);
                                phantom.setPropStatus(Phantom.Status.ACE);
                                count += 5;
                            } else if (level3 == 2 && subCount > 2.8){
                                LOG.debug("声骸词条 {} 已大毕业，分数：{}",phantom.getPhantomProp().getName(),subCount);
                                phantom.setPropStatus(Phantom.Status.SSS);
                                count += 4;
                            }else if (level3 == 2 && subCount > 2.1 || level3 == 1 && subCount > 2.4){
                                LOG.debug("声骸词条 {} 已毕业，分数：{}",phantom.getPhantomProp().getName(),subCount);
                                phantom.setPropStatus(Phantom.Status.SS);
                                count += 3;
                            }else if (level3 == 1 && subCount > 1.6 || level3 == 2 && subCount > 1.2){
                                LOG.debug("声骸词条 {} 已小毕业，分数：{}",phantom.getPhantomProp().getName(),subCount);
                                phantom.setPropStatus(Phantom.Status.S);
                                count += 2;
                            }else {
                                LOG.debug("声骸词条 {} 已不太行，分数：{}",phantom.getPhantomProp().getName(),subCount);
                                phantom.setPropStatus(Phantom.Status.N);
                                count += 1;
                            }
                        }

                        if (count == 45){
                            phantomStatus.set(Phantom.Status.ACE);
                        } else if (count >= 35) {
                            phantomStatus.set(Phantom.Status.SSS);
                        }else if (count >= 25) {
                            phantomStatus.set(Phantom.Status.SS);
                        }else if (count >= 18) {
                            phantomStatus.set(Phantom.Status.S);
                        }else{
                            phantomStatus.set(Phantom.Status.N);
                        }
                    }

                    totalPhantomValueList.setAll(totalPhantomValueMap.values());
                    totalPhantomValueList.sort((o1, o2) -> {
                        if (o1.getLevel() > o2.getLevel()){
                            return -1;
                        }else if (o1.getLevel() < o2.getLevel()){
                            return 1;
                        }
                        return 0;
                    });

                    for (Phantom phantom : equipPhantomList) {
                        if (phantom != null){
                            fetterDetails.add(phantom.getFetterDetail());
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

    public ObservableSet<FetterDetail> getFetterDetails() {
        return fetterDetails;
    }

    public ObservableList<PhoantomMainProps> getTotalPhantomValueList() {
        return totalPhantomValueList;
    }

    public Phantom.Status getPhantomStatus() {
        return phantomStatus.get();
    }

    public SimpleObjectProperty<Phantom.Status> phantomStatusProperty() {
        return phantomStatus;
    }
}