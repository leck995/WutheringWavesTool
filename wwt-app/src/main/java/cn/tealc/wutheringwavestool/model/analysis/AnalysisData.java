package cn.tealc.wutheringwavestool.model.analysis;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 11:37
 */
public class AnalysisData {
    private boolean isEmpty;

    private String poolName;
    private Integer totalCount = 0; //总抽数
    private Integer noUpSsrCount = 0; //当前未出货抽数
    private Integer noUpSrCount = 0; //当前未出货抽数
    private Integer noUpRCount = 0; //当前未出货抽数

    private List<SsrData> ssrDataList; //五星
    private List<SsrData> srDataList; //四星
    private List<SsrData> rDataList; //三星

    private Integer ssrCount = 0; //五星数量
    private Integer srCount = 0;
    private Integer rCount = 0;

    private Double ssrAvg = 0.0; //五星平均抽数
    private Integer ssrMin = 0;//五星最小抽数
    private Integer ssrMax = 0;//五星最大抽数
    private Integer upSsrCount = 0; //UP五星数量
    private Double upSsrAvg = 0.0; //UP五星平均抽数
    private Double nonBannerRate = 0.0; //五星不歪率（50/50胜率）
    private Double upRate = 0.0; //五星UP率（UP角色占全部五星的比例）

    private Double srAvg = 0.0; //四星平均抽数
    private Integer srMin = 0;//四星最小抽数
    private Integer srMax = 0;//四星最大抽数
    private Double rAvg = 0.0; //三星平均抽数
    private Integer rMin = 0;//三星最小抽数
    private Integer rMax = 0;//三星最大抽数

    private String startDate;
    private String endDate;

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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getNoUpSsrCount() {
        return noUpSsrCount;
    }

    public void setNoUpSsrCount(Integer noUpSsrCount) {
        this.noUpSsrCount = noUpSsrCount;
    }

    public Integer getNoUpSrCount() {
        return noUpSrCount;
    }

    public void setNoUpSrCount(Integer noUpSrCount) {
        this.noUpSrCount = noUpSrCount;
    }

    public Integer getNoUpRCount() {
        return noUpRCount;
    }

    public void setNoUpRCount(Integer noUpRCount) {
        this.noUpRCount = noUpRCount;
    }

    public Integer getUpSsrCount() {
        return upSsrCount;
    }

    public void setUpSsrCount(Integer upSsrCount) {
        this.upSsrCount = upSsrCount;
    }

    public Double getUpSsrAvg() {
        return upSsrAvg;
    }

    public void setUpSsrAvg(Double upSsrAvg) {
        this.upSsrAvg = upSsrAvg;
    }

    public Double getNonBannerRate() {
        return nonBannerRate;
    }

    public void setNonBannerRate(Double nonBannerRate) {
        this.nonBannerRate = nonBannerRate;
    }

    public Double getUpRate() {
        return upRate;
    }

    public void setUpRate(Double upRate) {
        this.upRate = upRate;
    }

    public List<SsrData> getSrDataList() {
        return srDataList;
    }

    public void setSrDataList(List<SsrData> srDataList) {
        this.srDataList = srDataList;
    }

    public List<SsrData> getrDataList() {
        return rDataList;
    }

    public void setrDataList(List<SsrData> rDataList) {
        this.rDataList = rDataList;
    }

    public Double getSrAvg() {
        return srAvg;
    }

    public void setSrAvg(Double srAvg) {
        this.srAvg = srAvg;
    }

    public Integer getSrMin() {
        return srMin;
    }

    public void setSrMin(Integer srMin) {
        this.srMin = srMin;
    }

    public Integer getSrMax() {
        return srMax;
    }

    public void setSrMax(Integer srMax) {
        this.srMax = srMax;
    }

    public Double getrAvg() {
        return rAvg;
    }

    public void setrAvg(Double rAvg) {
        this.rAvg = rAvg;
    }

    public Integer getrMin() {
        return rMin;
    }

    public void setrMin(Integer rMin) {
        this.rMin = rMin;
    }

    public Integer getrMax() {
        return rMax;
    }

    public void setrMax(Integer rMax) {
        this.rMax = rMax;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }
}