package cn.tealc.wutheringwavestool.model.roleData;

/**
 * @program: WutheringWavesTool
 * @description: 共鸣链
 * @author: Leck
 * @create: 2024-07-30 16:11
 */
public class Chain {
    private String name;
    private int order;
    private String description;
    private String iconUrl;
    private boolean unlocked;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
}