package cn.tealc.wutheringwavestool.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Locale;

/**
 * @program: WutheringWavesTool
 * @description: 国际化支持，推荐添加中文与英文支持
 * @author: Leck
 * @create: 2024-12-21 01:06
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FxPluginLanguage {
    private Locale locale;
    private String title;
    private String description;

    public FxPluginLanguage() {
    }

    public FxPluginLanguage(Locale locale, String title, String description) {
        this.locale = locale;
        this.title = title;
        this.description = description;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}