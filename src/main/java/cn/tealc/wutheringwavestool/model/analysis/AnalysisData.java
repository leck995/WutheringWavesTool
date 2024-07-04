package cn.tealc.wutheringwavestool.model.analysis;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 11:37
 */
public class AnalysisData {
    private String poolName;
    private Integer totalCount; //总抽数
    private Integer currentCount; //当前未出货抽数
    private List<SsrData> ssrDataList; //五星
    private Integer ssrCount; //五星数量
    private Integer srCount;
    private Integer rCount;
    private Double ssrAvg; //五星平均抽数
    private Integer ssrMin;//五星最小抽数
    private Integer ssrMax;//五星最大抽数

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }

    public List<SsrData> getSsrDataList() {
        return ssrDataList;
    }

    public void setSsrDataList(List<SsrData> ssrDataList) {
        this.ssrDataList = ssrDataList;
    }

    public Integer getSsrCount() {
        return ssrCount;
    }

    public void setSsrCount(Integer ssrCount) {
        this.ssrCount = ssrCount;
    }

    public Integer getSrCount() {
        return srCount;
    }

    public void setSrCount(Integer srCount) {
        this.srCount = srCount;
    }

    public Integer getrCount() {
        return rCount;
    }

    public void setrCount(Integer rCount) {
        this.rCount = rCount;
    }

    public Double getSsrAvg() {
        return ssrAvg;
    }

    public void setSsrAvg(Double ssrAvg) {
        this.ssrAvg = ssrAvg;
    }

    public Integer getSsrMin() {
        return ssrMin;
    }

    public void setSsrMin(Integer ssrMin) {
        this.ssrMin = ssrMin;
    }

    public Integer getSsrMax() {
        return ssrMax;
    }

    public void setSsrMax(Integer ssrMax) {
        this.ssrMax = ssrMax;
    }
}