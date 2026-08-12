package cn.tealc.wutheringwavestool.model.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: WutheringWavesToolResources
 * @description: 版本更新数据
 * @author: Leck
 * @create: 2024-12-22 14:54
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Release {
    private String name; //文件名称
    private String version; //版本
    private String description; //更新说明
    private String date;
    private boolean force; //强制升级
    private String[] urls; //下载链接，一般两个，0是github，1是cloudflare
    private String md5; //安装包的md5,用于验证
    private String warning;//用于警告，比如强制更新的注意点
    private boolean isPre;//预览版

    public Release() {
    }

    public Release(String name, String version, String description, String date, boolean force, String[] urls, String md5, String warning, boolean isPre) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.date = date;
        this.force = force;
        this.urls = urls;
        this.md5 = md5;
        this.warning = warning;
        this.isPre = isPre;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getUrls() {
        return urls;
    }

    public void setUrls(String[] urls) {
        this.urls = urls;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isPre() {
        return isPre;
    }

    public void setPre(boolean pre) {
        isPre = pre;
    }
}