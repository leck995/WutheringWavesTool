package cn.tealc.wutheringwavestool.ui.game;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.dao.GameRecordDao;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.game.GameRecord;
import cn.tealc.wutheringwavestool.service.GameRecordService;
import com.kuro.kujiequ.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-11-16 22:44
 */
public class GameRecordViewModel extends BaseViewModel {
    private final ObservableList<String> roleIdList= FXCollections.observableArrayList();
    private final SimpleIntegerProperty roleIdIndex= new SimpleIntegerProperty();
    private final SimpleIntegerProperty battle = new SimpleIntegerProperty();
    private final SimpleIntegerProperty paralysis = new SimpleIntegerProperty();
    private final SimpleIntegerProperty parry = new SimpleIntegerProperty();
    private final SimpleIntegerProperty parryAttack = new SimpleIntegerProperty();
    private final SimpleIntegerProperty phantomGet = new SimpleIntegerProperty();
    private final SimpleIntegerProperty phantomSkill = new SimpleIntegerProperty();
    private final SimpleIntegerProperty roleChange = new SimpleIntegerProperty();
    private final SimpleIntegerProperty roleDeath = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalBattle = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalParalysis = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalParry = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalParryAttack = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalPhantomGet = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalPhantomSkill = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalRoleChange = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalRoleDeath = new SimpleIntegerProperty();
    private final SimpleIntegerProperty totalTransfer = new SimpleIntegerProperty();
    private final SimpleIntegerProperty transfer = new SimpleIntegerProperty();

    @Inject
    private GameRecordDao gameRecordDao;
    @Inject
    private UserInfoDao userInfoDao;

    @Inject
    private GameRecordService gameRecordService;

    public void initialize() {
        List<String> roleIds = gameRecordDao.getAllRoleId();
        roleIdList.addAll(roleIds);
        if (!Config.setting().isNoKuJieQu()){
            UserInfo main = userInfoDao.getMain();
            if (main != null) {
                boolean hasUser =false;
                for (int i = 0; i < roleIdList.size(); i++) {
                    if (Objects.equals(roleIdList.get(i), main.getRoleId())) {
                        updateIndex(i);
                        hasUser = true;
                        break;
                    }
                }
                if (!hasUser && !roleIdList.isEmpty()) {
                    updateIndex(0);
                }
            }
        }else {
            if (!roleIdList.isEmpty()) {
                updateIndex(0);
            }
        }
    }

    /**
     * @description: 更新当前选择的用户
     * @param:	index
     * @return  void
     * @date:   2025/1/2
     */
    public void updateIndex(int index){
        roleIdIndex.set(index);
        String today = getToday();
        String roleId = roleIdList.get(getRoleIdIndex());
        List<GameRecord> list = gameRecordDao.getRecordListByRoleIdAndDate(roleId,today);
        updateTodayData(list);
        List<GameRecord> allRecordList = gameRecordDao.getRecordListByRoleId(roleId);
        updateTotalData(allRecordList);
    }


    private void updateTodayData(List<GameRecord> list) {
        GameRecordService.GameRecordAggregation agg = gameRecordService.aggregate(list);
        roleChange.set(agg.roleChange);
        roleDeath.set(agg.roleDeath);
        battle.set(agg.battle);
        phantomGet.set(agg.phantomGet);
        phantomSkill.set(agg.getPhantomSkillTotal());
        paralysis.set(agg.paralysis);
        transfer.set(agg.transfer);
        parry.set(agg.getParryTotal());
        parryAttack.set(agg.parryAttack);
    }


    private void updateTotalData(List<GameRecord> list) {
        GameRecordService.GameRecordAggregation agg = gameRecordService.aggregate(list);
        totalRoleChange.set(agg.roleChange);
        totalRoleDeath.set(agg.roleDeath);
        totalBattle.set(agg.battle);
        totalPhantomGet.set(agg.phantomGet);
        totalPhantomSkill.set(agg.getPhantomSkillTotal());
        totalParalysis.set(agg.paralysis);
        totalTransfer.set(agg.transfer);
        totalParry.set(agg.getParryTotal());
        totalParryAttack.set(agg.parryAttack);
    }



    private String getToday(){
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return currentDate.format(formatter);
    }

    public int getBattle() {
        return battle.get();
    }

    public SimpleIntegerProperty battleProperty() {
        return battle;
    }

    public void setBattle(int battle) {
        this.battle.set(battle);
    }

    public int getParalysis() {
        return paralysis.get();
    }

    public SimpleIntegerProperty paralysisProperty() {
        return paralysis;
    }

    public void setParalysis(int paralysis) {
        this.paralysis.set(paralysis);
    }

    public int getParry() {
        return parry.get();
    }

    public SimpleIntegerProperty parryProperty() {
        return parry;
    }

    public void setParry(int parry) {
        this.parry.set(parry);
    }

    public int getParryAttack() {
        return parryAttack.get();
    }

    public SimpleIntegerProperty parryAttackProperty() {
        return parryAttack;
    }

    public void setParryAttack(int parryAttack) {
        this.parryAttack.set(parryAttack);
    }

    public int getPhantomGet() {
        return phantomGet.get();
    }

    public SimpleIntegerProperty phantomGetProperty() {
        return phantomGet;
    }

    public void setPhantomGet(int phantomGet) {
        this.phantomGet.set(phantomGet);
    }

    public int getPhantomSkill() {
        return phantomSkill.get();
    }

    public SimpleIntegerProperty phantomSkillProperty() {
        return phantomSkill;
    }

    public void setPhantomSkill(int phantomSkill) {
        this.phantomSkill.set(phantomSkill);
    }

    public int getRoleChange() {
        return roleChange.get();
    }

    public SimpleIntegerProperty roleChangeProperty() {
        return roleChange;
    }

    public void setRoleChange(int roleChange) {
        this.roleChange.set(roleChange);
    }

    public int getRoleDeath() {
        return roleDeath.get();
    }

    public SimpleIntegerProperty roleDeathProperty() {
        return roleDeath;
    }

    public void setRoleDeath(int roleDeath) {
        this.roleDeath.set(roleDeath);
    }

    public int getTotalBattle() {
        return totalBattle.get();
    }

    public SimpleIntegerProperty totalBattleProperty() {
        return totalBattle;
    }

    public void setTotalBattle(int totalBattle) {
        this.totalBattle.set(totalBattle);
    }

    public int getTotalParalysis() {
        return totalParalysis.get();
    }

    public SimpleIntegerProperty totalParalysisProperty() {
        return totalParalysis;
    }

    public void setTotalParalysis(int totalParalysis) {
        this.totalParalysis.set(totalParalysis);
    }

    public int getTotalParry() {
        return totalParry.get();
    }

    public SimpleIntegerProperty totalParryProperty() {
        return totalParry;
    }

    public void setTotalParry(int totalParry) {
        this.totalParry.set(totalParry);
    }

    public int getTotalParryAttack() {
        return totalParryAttack.get();
    }

    public SimpleIntegerProperty totalParryAttackProperty() {
        return totalParryAttack;
    }

    public void setTotalParryAttack(int totalParryAttack) {
        this.totalParryAttack.set(totalParryAttack);
    }

    public int getTotalPhantomGet() {
        return totalPhantomGet.get();
    }

    public SimpleIntegerProperty totalPhantomGetProperty() {
        return totalPhantomGet;
    }

    public void setTotalPhantomGet(int totalPhantomGet) {
        this.totalPhantomGet.set(totalPhantomGet);
    }

    public int getTotalPhantomSkill() {
        return totalPhantomSkill.get();
    }

    public SimpleIntegerProperty totalPhantomSkillProperty() {
        return totalPhantomSkill;
    }

    public void setTotalPhantomSkill(int totalPhantomSkill) {
        this.totalPhantomSkill.set(totalPhantomSkill);
    }

    public int getTotalRoleChange() {
        return totalRoleChange.get();
    }

    public SimpleIntegerProperty totalRoleChangeProperty() {
        return totalRoleChange;
    }

    public void setTotalRoleChange(int totalRoleChange) {
        this.totalRoleChange.set(totalRoleChange);
    }

    public int getTotalRoleDeath() {
        return totalRoleDeath.get();
    }

    public SimpleIntegerProperty totalRoleDeathProperty() {
        return totalRoleDeath;
    }

    public void setTotalRoleDeath(int totalRoleDeath) {
        this.totalRoleDeath.set(totalRoleDeath);
    }

    public int getTotalTransfer() {
        return totalTransfer.get();
    }

    public SimpleIntegerProperty totalTransferProperty() {
        return totalTransfer;
    }

    public void setTotalTransfer(int totalTransfer) {
        this.totalTransfer.set(totalTransfer);
    }

    public int getTransfer() {
        return transfer.get();
    }

    public SimpleIntegerProperty transferProperty() {
        return transfer;
    }

    public void setTransfer(int transfer) {
        this.transfer.set(transfer);
    }

    public ObservableList<String> getRoleIdList() {
        return roleIdList;
    }

    public int getRoleIdIndex() {
        return roleIdIndex.get();
    }

    public SimpleIntegerProperty roleIdIndexProperty() {
        return roleIdIndex;
    }
}