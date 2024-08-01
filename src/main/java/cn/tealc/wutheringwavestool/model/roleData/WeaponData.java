package cn.tealc.wutheringwavestool.model.roleData;

/**
 * @program: WutheringWavesTool
 * @description: 角色武器数据，包括等级
 * @author: Leck
 * @create: 2024-07-30 16:15
 */
public class WeaponData {
    private Weapon weapon;
    private int level;
    private int resonLevel;

    public Weapon getWeapon() {
        return weapon;
    }

    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getResonLevel() {
        return resonLevel;
    }

    public void setResonLevel(int resonLevel) {
        this.resonLevel = resonLevel;
    }
}