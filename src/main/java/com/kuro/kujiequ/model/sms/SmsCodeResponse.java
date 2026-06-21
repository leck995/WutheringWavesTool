package com.kuro.kujiequ.model.sms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * APP 端 getSmsCode API 响应中 data 字段的模型。
 * 当 geeTest=true 时，需要用户完成极验人机验证，
 * 此时 gt 和 challenge 用于初始化极验 SDK。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsCodeResponse {
    private boolean geeTest;
    private String gt;
    private String challenge;

    public SmsCodeResponse() {
    }

    public boolean isGeeTest() {
        return geeTest;
    }

    public void setGeeTest(boolean geeTest) {
        this.geeTest = geeTest;
    }

    public String getGt() {
        return gt;
    }

    public void setGt(String gt) {
        this.gt = gt;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }
}
