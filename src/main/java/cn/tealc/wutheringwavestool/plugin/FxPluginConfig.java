package cn.tealc.wutheringwavestool.plugin;


import cn.tealc.fxplugin.model.FxPluginType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FxPluginConfig {
    private int id;
    private String title; //从国际化的文件获取，没有则显示默认
    private String author;
    private String version;
    private String description;
    private String icon; //图标
    private String path; //启动类
    private String resourceBundleName; //国际化文件
    private boolean ready;//标志已加载

    private FxPluginType pluginType;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getResourceBundleName() {
        return resourceBundleName;
    }

    public void setResourceBundleName(String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
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
}