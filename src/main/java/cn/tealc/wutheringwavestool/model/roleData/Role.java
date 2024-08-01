package cn.tealc.wutheringwavestool.model.roleData;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-29 21:45
 */
public class Role {
    private int roleId;
    private int level;
    private String roleName;
    private String roleIconUrl;
    private String rolePicUrl;
    private int starLevel;
    private int attributeId;
    private String attributeName;
    private int weaponTypeId;
    private String weaponTypeName;
    private String acronym;
    private Integer mapRoleId;

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleIconUrl() {
        return roleIconUrl;
    }

    public void setRoleIconUrl(String roleIconUrl) {
        this.roleIconUrl = roleIconUrl;
    }

    public String getRolePicUrl() {
        return rolePicUrl;
    }

    public void setRolePicUrl(String rolePicUrl) {
        this.rolePicUrl = rolePicUrl;
    }

    public int getStarLevel() {
        return starLevel;
    }

    public void setStarLevel(int starLevel) {
        this.starLevel = starLevel;
    }

    public int getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(int attributeId) {
        this.attributeId = attributeId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public int getWeaponTypeId() {
        return weaponTypeId;
    }

    public void setWeaponTypeId(int weaponTypeId) {
        this.weaponTypeId = weaponTypeId;
    }

    public String getWeaponTypeName() {
        return weaponTypeName;
    }

    public void setWeaponTypeName(String weaponTypeName) {
        this.weaponTypeName = weaponTypeName;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public Integer getMapRoleId() {
        return mapRoleId;
    }

    public void setMapRoleId(Integer mapRoleId) {
        this.mapRoleId = mapRoleId;
    }
}