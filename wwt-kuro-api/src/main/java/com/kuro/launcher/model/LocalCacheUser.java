package com.kuro.launcher.model;

import com.kuro.model.SourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class LocalCacheUser {
    @JsonProperty("cuid")
    private String cuid;

    @JsonProperty("id")
    private Double id;

    @JsonProperty("loginType")
    private Integer loginType;

    @JsonProperty("oauthCode")
    private String oauthCode;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("thirdNickName")
    private String thirdNickName;

    @JsonProperty("username")
    private String username;

    private SourceType type;

    // 无参构造函数
    public LocalCacheUser() {
    }

    public String getPhoneOrEmail() {
        if (phone != null)
            return phone;
        return email;
    }

    // Getter和Setter方法
    public String getCuid() {
        return cuid;
    }

    public void setCuid(String cuid) {
        this.cuid = cuid;
    }

    public Double getId() {
        return id;
    }

    public void setId(Double id) {
        this.id = id;
    }

    public Integer getLoginType() {
        return loginType;
    }

    public void setLoginType(Integer loginType) {
        this.loginType = loginType;
    }

    public String getOauthCode() {
        return oauthCode;
    }

    public void setOauthCode(String oauthCode) {
        this.oauthCode = oauthCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getThirdNickName() {
        return thirdNickName;
    }

    public void setThirdNickName(String thirdNickName) {
        this.thirdNickName = thirdNickName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public SourceType getType() {
        return type;
    }

    public void setType(SourceType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "User{" +
                "cuid='" + cuid + '\'' +
                ", id=" + id +
                ", loginType=" + loginType +
                ", oauthCode='" + oauthCode + '\'' +
                ", phone='" + phone + '\'' +
                ", thirdNickName='" + thirdNickName + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}