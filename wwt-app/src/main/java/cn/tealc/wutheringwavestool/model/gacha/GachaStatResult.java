package cn.tealc.wutheringwavestool.model.gacha;

/**
 * 抽卡统计聚合结果（纯 DTO）。
 *
 * @author Leck
 */
public class GachaStatResult {
    // —— 总体统计 ——
    public int totalPulls;
    public long totalStones;
    public int ssrCount;
    public int srCount;
    public int rCount;
    public int upSsrCount;
    public double ssrAvg;
    public double nonBannerRate;
    public String startDate;
    public String endDate;

    // —— 最多抽取的角色/武器 ——
    public String topSsrName = "—";
    public int topSsrId;
    public int topSsrCount;
    public String topSrName = "—";
    public int topSrId;
    public int topSrCount;
    public String topUpSsrName = "—";
    public int topUpSsrId;
    public int topUpSsrCount;

    // —— 角色 vs 武器 ——
    public int rolePulls;
    public int roleSsr;
    public double roleAvg;
    public int weaponPulls;
    public int weaponSsr;
    public double weaponAvg;
}
