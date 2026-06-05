package cn.tealc.wutheringwavestool.ui.kujiequ.role;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.kujiequ.model.roleData.*;
import com.kuro.kujiequ.model.roleData.weight.PhantomWeight;
import cn.tealc.wutheringwavestool.thread.system.ui.ImgColorBgTask;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.role.GameRoleDetailTask;
import cn.tealc.wutheringwavestool.util.LocalDataManager;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.ViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-30 17:05
 */
public class OwnRoleDetailViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(OwnRoleDetailViewModel.class);
    public static final String EVENT_CHANGE_ROLE = "EVENT_CHANGE_ROLE";
    public static final String EVENT_UPDATE_PHANTOM = "EVENT_UPDATE_PHANTOM";

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

    private static final Map<String, Double> propMaxValueMap = new HashMap<>();




    static {
        propMaxValueMap.put("暴击伤害", 21.0);
        propMaxValueMap.put("暴击", 10.5);
        propMaxValueMap.put("攻击", 60.0);
        propMaxValueMap.put("攻击百分比", 11.6);
        propMaxValueMap.put("生命", 580.0);
        propMaxValueMap.put("生命百分比", 11.6);
        propMaxValueMap.put("防御", 60.0);
        propMaxValueMap.put("防御百分比", 14.7);
        propMaxValueMap.put("共鸣效率", 12.4);
        propMaxValueMap.put("普攻伤害加成", 11.6);
        propMaxValueMap.put("重击伤害加成", 11.6);
        propMaxValueMap.put("共鸣技能伤害加成", 11.6);
        propMaxValueMap.put("共鸣解放伤害加成", 11.6);
    }


    private SimpleObjectProperty<Image> roleAttrImage = new SimpleObjectProperty<>();
    private SimpleStringProperty roleName = new SimpleStringProperty();
    private SimpleIntegerProperty roleId = new SimpleIntegerProperty();
    private SimpleStringProperty roleLevel = new SimpleStringProperty();
    private SimpleObjectProperty<Image> roleImage = new SimpleObjectProperty<>();
    private SimpleStringProperty weaponName = new SimpleStringProperty();
    private SimpleStringProperty weaponLevel = new SimpleStringProperty();
    private SimpleStringProperty weaponResonLevel = new SimpleStringProperty();
    private SimpleObjectProperty<Image> weaponImage = new SimpleObjectProperty<>();
    private SimpleIntegerProperty weaponStarLevel = new SimpleIntegerProperty();
    private SimpleObjectProperty<Background> weaponBg = new SimpleObjectProperty();
    private SimpleStringProperty skill01 = new SimpleStringProperty();
    private SimpleStringProperty skill02 = new SimpleStringProperty();
    private SimpleStringProperty skill03 = new SimpleStringProperty();
    private SimpleStringProperty skill04 = new SimpleStringProperty();
    private SimpleStringProperty skill05 = new SimpleStringProperty();
    private SimpleObjectProperty<Image> skillImg01 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> skillImg02 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> skillImg03 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> skillImg04 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> skillImg05 = new SimpleObjectProperty<>();

    private SimpleObjectProperty<Image> chainImg01 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg02 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg03 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg04 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg05 = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> chainImg06 = new SimpleObjectProperty<>();

    private SimpleBooleanProperty chainImgVisible01 = new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible02 = new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible03 = new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible04 = new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible05 = new SimpleBooleanProperty();
    private SimpleBooleanProperty chainImgVisible06 = new SimpleBooleanProperty();

    private ObservableList<Pair<Role, Image>> rolePairList = FXCollections.observableArrayList();

    private UserInfo userInfo;

    private SimpleIntegerProperty selectIndex = new SimpleIntegerProperty();

    private SimpleObjectProperty<Background> roleBg = new SimpleObjectProperty<>();

    private ObservableList<Pair<Phantom, Image>> phantomList = FXCollections.observableArrayList();
    private SimpleStringProperty phantomCost = new SimpleStringProperty();
    private ObservableSet<FetterDetail> fetterDetails = FXCollections.observableSet(); //记录套装声骸效果
    private ObservableList<PhoantomMainProps> totalPhantomValueList = FXCollections.observableArrayList(); //记录声骸每个词条的总值


    private SimpleObjectProperty<Phantom.Status> phantomStatus = new SimpleObjectProperty<>();

    private ObservableList<RoleAttribute> roleAttributeList = FXCollections.observableArrayList();

    private SimpleBooleanProperty loading = new SimpleBooleanProperty(true);

    public OwnRoleDetailViewModel(UserInfo userInfo, int selectIndex, List<Pair<Role, Image>> rolePairList) {
        this.userInfo = userInfo;
        this.rolePairList.setAll(rolePairList);
        this.selectIndex.set(selectIndex);
    }

    public void initialize(){
        CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS)
                .execute(this::load);
    }


    public void select(int index) {
        selectIndex.set(index);
        load();
        publish(EVENT_CHANGE_ROLE);
    }


    private void load() {
        Pair<Role, Image> pair = rolePairList.get(selectIndex.get());
        GameRoleDetailTask task = new GameRoleDetailTask(userInfo, pair.getKey().getRoleId());
        task.setOnSucceeded(e -> {
            ResponseBody<RoleDetail> value = task.getValue();
            if (value.getCode() == 200) {
                RoleDetail data = value.getData();
                analysis(data);
            }
        });
        Thread.startVirtualThread(task);
    }

    private void analysis(RoleDetail data) {
        updateRoleInfo(data);
        updateWeapon(data);
        updateSkillInfo(data);
        updateChainInfo(data);

        updateBackground(data);
        updatePhantomInfo(data);
    }


    private void updateBackground(RoleDetail data) {
        ImgColorBgTask imgColorBgTask = new ImgColorBgTask(data.getRole().getRoleIconUrl());
        imgColorBgTask.setOnSucceeded(workerStateEvent -> {
            roleBg.set(imgColorBgTask.getValue());
        });
        Thread.startVirtualThread(imgColorBgTask);
    }

    private void updatePhantomInfo(RoleDetail data) {
        phantomCost.set(String.format("COST: %d", data.getPhantomData().getCost()));
        fetterDetails.clear();
        totalPhantomValueList.clear();
        phantomStatus.set(null);
        phantomList.clear();

        Thread.startVirtualThread(() -> {
            List<Phantom> equipPhantomList = data.getPhantomData().getEquipPhantomList();
            if (equipPhantomList == null) {
                Platform.runLater(() -> {
                    loading.set(false);
                    publish(EVENT_UPDATE_PHANTOM);
                });
                return;
            }

            PhantomWeight weight = LocalDataManager.getWeight(String.valueOf(roleId.get()));
            Map<String, Double> totalAccumulator = new HashMap<>();
            Map<String, PhoantomMainProps> totalPropTemplates = new HashMap<>();
            int totalScore = 0;

            if (weight != null) {
                Map<String, Integer> subPropWeights = weight.getSubPropWeights();
                for (Phantom phantom : equipPhantomList) {
                    if (phantom == null || phantom.getSubProps() == null) {
                        continue;
                    }
                    int level3 = 0, level2 = 0, level1 = 0;
                    double subCount = 0.0;
                    LOG.debug("================={}==================", phantom.getPhantomProp().getName());

                    for (PhoantomMainProps subProp : phantom.getSubProps()) {
                        String attributeName = subProp.getAttributeName();
                        String currentValueString = subProp.getAttributeValue();
                        if (attributeName.equals("攻击") || attributeName.equals("生命") || attributeName.equals("防御")) {
                            if (currentValueString.contains("%")) {
                                attributeName = attributeName + "百分比";
                                subProp.setAttributeName(attributeName);
                            }
                        }

                        double currentValue = Double.parseDouble(currentValueString.replace("%", ""));
                        double maxValue = propMaxValueMap.get(attributeName);
                        Integer level = subPropWeights.get(attributeName);

                        totalAccumulator.merge(attributeName, currentValue, Double::sum);
                        if (!totalPropTemplates.containsKey(attributeName)) {
                            PhoantomMainProps template = new PhoantomMainProps();
                            template.setAttributeName(attributeName);
                            template.setLevel(level);
                            template.setIconUrl(subProp.getIconUrl());
                            totalPropTemplates.put(attributeName, template);
                        }

                        if (level != null) {
                            subProp.setLevel(level);
                            double percent = currentValue / maxValue;
                            subProp.setPercent(percent);
                            subProp.setAttributeMaxValue(maxValue);
                            if (level == 3) {
                                level3++;
                                subCount += percent;
                            } else if (level == 2) {
                                level2++;
                                subCount += percent;
                            } else if (level == 1) {
                                level1++;
                                subCount += percent;
                            }
                        }
                    }

                    totalScore += scorePhantomStatus(level3, level2, level1, phantom);
                    totalScore += scorePropStatus(level3, level1, subCount, phantom);
                }
            }

            List<PhoantomMainProps> totalList = totalAccumulator.entrySet().stream()
                    .map(e -> {
                        PhoantomMainProps props = totalPropTemplates.get(e.getKey());
                        props.setAttributeValue(String.format("%.1f", e.getValue()));
                        return props;
                    })
                    .sorted((o1, o2) -> Integer.compare(o2.getLevel(), o1.getLevel()))
                    .toList();

            int finalScore = totalScore;
            Platform.runLater(() -> {
                phantomStatus.set(scoreToStatus(finalScore));
                totalPhantomValueList.setAll(totalList);
                for (Phantom phantom : equipPhantomList) {
                    if (phantom != null) {
                        fetterDetails.add(phantom.getFetterDetail());
                        phantomList.add(new Pair<>(phantom,
                                LocalResourcesManager.imageBuffer(phantom.getPhantomProp().getIconUrl(), 65, 65, true, true)));
                    }
                }
                loading.set(false);
                publish(EVENT_UPDATE_PHANTOM);
            });
        });
    }

    private static int scorePhantomStatus(int level3, int level2, int level1, Phantom phantom) {
        int sum = level2 + level1;
        if (level3 == 2 && sum == 3) {
            LOG.debug("声骸 {} 已完美", phantom.getPhantomProp().getName());
            phantom.setStatus(Phantom.Status.ACE);
            return 5;
        }
        if (level3 == 2 && sum == 2) {
            LOG.debug("声骸 {} 已大毕业", phantom.getPhantomProp().getName());
            phantom.setStatus(Phantom.Status.SSS);
            return 4;
        }
        if (level3 == 2 && sum == 1 || level3 == 1 && sum == 3) {
            LOG.debug("声骸 {} 已毕业", phantom.getPhantomProp().getName());
            phantom.setStatus(Phantom.Status.SS);
            return 3;
        }
        if (level3 == 2 || level3 == 1 && sum >= 2) {
            LOG.debug("声骸 {} 已小毕业", phantom.getPhantomProp().getName());
            phantom.setStatus(Phantom.Status.S);
            return 2;
        }
        LOG.debug("声骸 {} 已不太行", phantom.getPhantomProp().getName());
        phantom.setStatus(Phantom.Status.N);
        return 1;
    }

    private static int scorePropStatus(int level3, int level1, double subCount, Phantom phantom) {
        if (level3 == 2 && subCount > 3.5) {
            LOG.debug("声骸词条 {} 已完美，分数：{}", phantom.getPhantomProp().getName(), subCount);
            phantom.setPropStatus(Phantom.Status.ACE);
            return 5;
        }
        if (level3 == 2 && subCount > 2.8) {
            LOG.debug("声骸词条 {} 已大毕业，分数：{}", phantom.getPhantomProp().getName(), subCount);
            phantom.setPropStatus(Phantom.Status.SSS);
            return 4;
        }
        if (level3 == 2 && subCount > 2.1 || level3 == 1 && subCount > 2.4) {
            LOG.debug("声骸词条 {} 已毕业，分数：{}", phantom.getPhantomProp().getName(), subCount);
            phantom.setPropStatus(Phantom.Status.SS);
            return 3;
        }
        if (level3 == 1 && subCount > 1.6 || level3 == 2 && subCount > 1.2) {
            LOG.debug("声骸词条 {} 已小毕业，分数：{}", phantom.getPhantomProp().getName(), subCount);
            phantom.setPropStatus(Phantom.Status.S);
            return 2;
        }
        LOG.debug("声骸词条 {} 已不太行，分数：{}", phantom.getPhantomProp().getName(), subCount);
        phantom.setPropStatus(Phantom.Status.N);
        return 1;
    }

    private static Phantom.Status scoreToStatus(int score) {
        if (score == 45) return Phantom.Status.ACE;
        if (score >= 35) return Phantom.Status.SSS;
        if (score >= 25) return Phantom.Status.SS;
        if (score >= 18) return Phantom.Status.S;
        if (score > 0) return Phantom.Status.N;
        return null;
    }

    private void updateChainInfo(RoleDetail data) {
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
    }

    private void updateSkillInfo(RoleDetail data) {
        skill01.set(String.format("LV.%d", data.getSkillList().get(4).getLevel()));
        skill02.set(String.format("LV.%d", data.getSkillList().get(3).getLevel()));
        skill03.set(String.format("LV.%d", data.getSkillList().get(2).getLevel()));
        skill04.set(String.format("LV.%d", data.getSkillList().get(1).getLevel()));
        skill05.set(String.format("LV.%d", data.getSkillList().get(0).getLevel()));
        skillImg01.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(4).getSkill().getIconUrl()));
        skillImg02.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(3).getSkill().getIconUrl()));
        skillImg03.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(2).getSkill().getIconUrl()));
        skillImg04.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(1).getSkill().getIconUrl()));
        skillImg05.set(LocalResourcesManager.imageBuffer(data.getSkillList().get(0).getSkill().getIconUrl()));
    }

    private void updateWeapon(RoleDetail data) {
        weaponName.set(data.getWeaponData().getWeapon().getWeaponName());
        weaponResonLevel.set(String.format("突破%d", data.getWeaponData().getResonLevel()));
        weaponLevel.set(String.format("LV.%d", data.getWeaponData().getLevel()));
        weaponImage.set(LocalResourcesManager.imageBuffer(data.getWeaponData().getWeapon().getWeaponIcon()));
        weaponStarLevel.set(data.getWeaponData().getWeapon().getWeaponStarLevel());
        weaponBg.set(
                switch (weaponStarLevel.get()) {
                    case 5 -> new Background(new BackgroundFill(SSR, new CornerRadii(8), null));
                    case 4 -> new Background(new BackgroundFill(SR, new CornerRadii(8), null));
                    default -> new Background(new BackgroundFill(R, new CornerRadii(8), null));
                });
    }

    private void updateRoleInfo(RoleDetail data) {
        roleName.set(data.getRole().getRoleName());
        roleId.set(data.getRole().getRoleId());
        roleLevel.set(String.format("LV.%d -- %d", data.getRole().getLevel(), data.getRole().getChainUnlockNum()));
        roleImage.set(LocalResourcesManager.imageBuffer(data.getRole().getRolePicUrl(), 500, 380, true, true));

        roleAttrImage.set(
                new Image(
                        FXResourcesLoader.load(
                                String.format("image/attr/%d.png", data.getRole().getAttributeId())
                        ), true));

        roleAttributeList.setAll(data.getRoleAttributeList());
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

    public ObservableList<RoleAttribute> getRoleAttributeList() {
        return roleAttributeList;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }
}