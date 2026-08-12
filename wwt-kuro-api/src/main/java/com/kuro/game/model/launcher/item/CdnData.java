package com.kuro.game.model.launcher.item;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-27 22:54
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdnData {
    @JsonAlias("K1")
    private int K1;
    @JsonAlias("K2")
    private int K2;
    @JsonAlias("p")
    private int ping;
    private String url;

    public int getK1() {
        return K1;
    }

    public void setK1(int k1) {
        K1 = k1;
    }

    public int getK2() {
        return K2;
    }

    public void setK2(int k2) {
        K2 = k2;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}