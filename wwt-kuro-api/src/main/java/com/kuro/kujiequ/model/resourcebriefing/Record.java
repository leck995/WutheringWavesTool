package com.kuro.kujiequ.model.resourcebriefing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-14 10:40
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Record {
    private long totalCoin;
    private long totalStar;
    private List<Item> coinList;
    private List<Item> starList;
    private String copyWriting;

    public long getTotalCoin() {
        return totalCoin;
    }

    public void setTotalCoin(long totalCoin) {
        this.totalCoin = totalCoin;
    }

    public long getTotalStar() {
        return totalStar;
    }

    public void setTotalStar(long totalStar) {
        this.totalStar = totalStar;
    }

    public List<Item> getCoinList() {
        return coinList;
    }

    public void setCoinList(List<Item> coinList) {
        this.coinList = coinList;
    }

    public List<Item> getStarList() {
        return starList;
    }

    public void setStarList(List<Item> starList) {
        this.starList = starList;
    }

    public String getCopyWriting() {
        return copyWriting;
    }

    public void setCopyWriting(String copyWriting) {
        this.copyWriting = copyWriting;
    }
}