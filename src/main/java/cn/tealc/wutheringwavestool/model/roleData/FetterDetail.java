package cn.tealc.wutheringwavestool.model.roleData;

/**
 * @program: WutheringWavesTool
 * @description: 声骸技能
 * @author: Leck
 * @create: 2024-07-30 16:18
 */
public class FetterDetail {
    private int groupId;
    private String name;
    private String iconUrl;
    private int num;
    private String firstDescription;
    private String secondDescription;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getFirstDescription() {
        return firstDescription;
    }

    public void setFirstDescription(String firstDescription) {
        this.firstDescription = firstDescription;
    }

    public String getSecondDescription() {
        return secondDescription;
    }

    public void setSecondDescription(String secondDescription) {
        this.secondDescription = secondDescription;
    }
}