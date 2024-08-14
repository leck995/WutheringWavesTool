package cn.tealc.wutheringwavestool.model.roleData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-30 16:31
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleDetail {
    private Role role;
    private int level;
    private List<Chain> chainList;
    private WeaponData weaponData;
    private PhantomData phantomData;
    private List<SkillData> skillList;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public List<Chain> getChainList() {
        return chainList;
    }

    public void setChainList(List<Chain> chainList) {
        this.chainList = chainList;
    }

    public WeaponData getWeaponData() {
        return weaponData;
    }

    public void setWeaponData(WeaponData weaponData) {
        this.weaponData = weaponData;
    }

    public PhantomData getPhantomData() {
        return phantomData;
    }

    public void setPhantomData(PhantomData phantomData) {
        this.phantomData = phantomData;
    }

    public List<SkillData> getSkillList() {
        return skillList;
    }

    public void setSkillList(List<SkillData> skillList) {
        this.skillList = skillList;
    }
}