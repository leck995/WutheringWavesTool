package cn.tealc.wutheringwavestool.theme;

import atlantafx.base.theme.Theme;
import cn.tealc.wutheringwavestool.FXResourcesLoader;

public final class Light implements Theme {

    public Light() {
        // Default constructor
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Light";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserAgentStylesheet() {
        return FXResourcesLoader.load("css/light.css");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserAgentStylesheetBSS() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDarkMode() {
        return true;
    }
}