package cn.tealc.wutheringwavestool.plugin;


import cn.tealc.fxplugin.model.FxPluginType;
import cn.tealc.wutheringwavestool.base.Config;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description: 插件配置信息类
 * @author: Leck
 * @create: 2024-12-21 01:06
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FxPluginConfig {
    private int id; //插件id，不为空，建议从1100开始，前100个留给开发者
    private String author;
    private String version;
    private String icon; //图标,支持图片与ikonli,图片填写插件目录图片名称；ikonli则填写助手已使用的ikonli图标库的key
    private String path; //plugins下的路径，例如wwt-pool-export,不能以/开头
    private String jarName; //jar的名字
    private Level level;//插件等级，0为系统级插件，无法卸载，1为重要插件，不建议卸载。2为不重要插件
    private Map<Locale,FxPluginLanguage> languages;//国际化
    private FxPluginType pluginType; //插件类型

    /*以下无需填写*/
    @JsonIgnore
    private boolean ready;//标志已加载

    @JsonIgnore
    public String getTitle(){
        if (languages.containsKey(Config.setting().getLanguage())){
            return languages.get(Config.setting().getLanguage()).getTitle();
        }else {
            return "Unknown";
        }
    }
    @JsonIgnore
    public String getDescription(){
        if (languages.containsKey(Config.setting().getLanguage())){
            return languages.get(Config.setting().getLanguage()).getDescription();
        }else {
            return "Unknown";
        }
    }

    public File getIconFile(){
        return new File("plugins/"+path+"/"+icon);
    }
    public File getJarPath(){
        return new File(path+"/"+jarName);
    }


    public int getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public FxPluginType getPluginType() {
        return pluginType;
    }

    public void setPluginType(FxPluginType pluginType) {
        this.pluginType = pluginType;
    }

    public boolean isReady() {
        return ready;
    }

    public Map<Locale, FxPluginLanguage> getLanguages() {
        return languages;
    }

    public void setLanguages(Map<Locale, FxPluginLanguage> languages) {
        this.languages = languages;
    }

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public enum Level{
        SYSTEM,
        IMPORTANT,
        DEFAULT;
    }
}