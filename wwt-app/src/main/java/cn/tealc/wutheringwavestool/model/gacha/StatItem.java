package cn.tealc.wutheringwavestool.model.gacha;

/**
 * 抽取次数排行列表项：名称 + 抽数 + resourceId（头像） + 品质。
 *
 * @author Leck
 */
public class StatItem {
    private final String name;
    private final int count;
    private final int resourceId;
    private final int qualityLevel;

    public StatItem(String name, int count, int resourceId, int qualityLevel) {
        this.name = name;
        this.count = count;
        this.resourceId = resourceId;
        this.qualityLevel = qualityLevel;
    }

    public String getName() { return name; }
    public int getCount() { return count; }
    public int getResourceId() { return resourceId; }
    public int getQualityLevel() { return qualityLevel; }
}
