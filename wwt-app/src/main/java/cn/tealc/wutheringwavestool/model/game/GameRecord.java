package cn.tealc.wutheringwavestool.model.game;

/**
 * @program: WutheringWavesTool
 * @description: 记录游玩详细数据，包括获取声骸数量，死亡次数，闪避次数等等
 * @author: Leck
 * @create: 2024-11-13 15:29
 */
public class GameRecord {
    private int id;
    private String roleId;
    private String createDate; //日期，无时间


    private int roleChange; //切换角色次数
    private int roleDeath; //死亡次数
    private int battle; //战斗次数
    private int phantomGet; //获取声骸次数
    private int phantomCallSkill; //召唤声骸技能次数
    private int phantomTransformSkill; //变身声骸技能次数
    private int paralysis; //怪物瘫痪次数
    private int transfer; //传送次数
    private int parryFront; //极限闪避次数
    private int parryBack; //极限闪避次数
    private int parryAttack; //极限闪避并反击次数

    private int usedStrength;//消耗体力
    private boolean monthCard;//月卡
    private int monthCardRemainDays;//剩余天数

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public int getRoleChange() {
        return roleChange;
    }

    public void setRoleChange(int roleChange) {
        this.roleChange = roleChange;
    }

    public int getRoleDeath() {
        return roleDeath;
    }

    public void setRoleDeath(int roleDeath) {
        this.roleDeath = roleDeath;
    }

    public int getBattle() {
        return battle;
    }

    public void setBattle(int battle) {
        this.battle = battle;
    }

    public int getPhantomGet() {
        return phantomGet;
    }

    public void setPhantomGet(int phantomGet) {
        this.phantomGet = phantomGet;
    }

    public int getPhantomCallSkill() {
        return phantomCallSkill;
    }

    public void setPhantomCallSkill(int phantomCallSkill) {
        this.phantomCallSkill = phantomCallSkill;
    }

    public int getPhantomTransformSkill() {
        return phantomTransformSkill;
    }

    public void setPhantomTransformSkill(int phantomTransformSkill) {
        this.phantomTransformSkill = phantomTransformSkill;
    }

    public int getParalysis() {
        return paralysis;
    }

    public void setParalysis(int paralysis) {
        this.paralysis = paralysis;
    }

    public int getTransfer() {
        return transfer;
    }

    public void setTransfer(int transfer) {
        this.transfer = transfer;
    }

    public int getParryFront() {
        return parryFront;
    }

    public void setParryFront(int parryFront) {
        this.parryFront = parryFront;
    }

    public int getParryBack() {
        return parryBack;
    }

    public void setParryBack(int parryBack) {
        this.parryBack = parryBack;
    }

    public int getParryAttack() {
        return parryAttack;
    }

    public void setParryAttack(int parryAttack) {
        this.parryAttack = parryAttack;
    }

    public int getUsedStrength() {
        return usedStrength;
    }

    public void setUsedStrength(int usedStrength) {
        this.usedStrength = usedStrength;
    }

    public boolean isMonthCard() {
        return monthCard;
    }

    public void setMonthCard(boolean monthCard) {
        this.monthCard = monthCard;
    }

    public int getMonthCardRemainDays() {
        return monthCardRemainDays;
    }

    public void setMonthCardRemainDays(int monthCardRemainDays) {
        this.monthCardRemainDays = monthCardRemainDays;
    }


    @Override
    public String toString() {
        return "GameRecord{" +
                "id=" + id +
                ", roleId='" + roleId + '\'' +
                ", createDate='" + createDate + '\'' +
                ", roleChange=" + roleChange +
                ", roleDeath=" + roleDeath +
                ", battle=" + battle +
                ", phantomGet=" + phantomGet +
                ", phantomCallSkill=" + phantomCallSkill +
                ", phantomTransformSkill=" + phantomTransformSkill +
                ", paralysis=" + paralysis +
                ", transfer=" + transfer +
                ", parryFront=" + parryFront +
                ", parryBack=" + parryBack +
                ", parryAttack=" + parryAttack +
                ", usedStrength=" + usedStrength +
                ", monthCard=" + monthCard +
                ", monthCardRemainDays=" + monthCardRemainDays +
                '}';
    }
}