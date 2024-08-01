package cn.tealc.wutheringwavestool.model.roleData;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-30 16:13
 */
public class Weapon {
    private int weaponId;
    private String weaponName;
    private int weaponType;
    private int weaponStarLevel;
    private String weaponIcon;
    private String weaponEffectName;
    private String effectDescription;

    public int getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }

    public String getWeaponName() {
        return weaponName;
    }

    public void setWeaponName(String weaponName) {
        this.weaponName = weaponName;
    }

    public int getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(int weaponType) {
        this.weaponType = weaponType;
    }

    public int getWeaponStarLevel() {
        return weaponStarLevel;
    }

    public void setWeaponStarLevel(int weaponStarLevel) {
        this.weaponStarLevel = weaponStarLevel;
    }

    public String getWeaponIcon() {
        return weaponIcon;
    }

    public void setWeaponIcon(String weaponIcon) {
        this.weaponIcon = weaponIcon;
    }

    public String getWeaponEffectName() {
        return weaponEffectName;
    }

    public void setWeaponEffectName(String weaponEffectName) {
        this.weaponEffectName = weaponEffectName;
    }

    public String getEffectDescription() {
        return effectDescription;
    }

    public void setEffectDescription(String effectDescription) {
        this.effectDescription = effectDescription;
    }
}