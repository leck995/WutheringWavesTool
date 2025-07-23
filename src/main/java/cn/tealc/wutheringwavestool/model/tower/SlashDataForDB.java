package cn.tealc.wutheringwavestool.model.tower;

/**
 * @description:
 * @author: Leck
 * @create: 2025-05-28 19:35
 */
public class SlashDataForDB {
    private int id;
    private String data;
    private long endTime;
    private String roleId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
}