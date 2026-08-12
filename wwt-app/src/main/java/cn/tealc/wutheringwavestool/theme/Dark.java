package cn.tealc.wutheringwavestool.theme;

import atlantafx.base.theme.Theme;
import cn.tealc.wutheringwavestool.FXResourcesLoader;

public final class Dark implements Theme {

    public Dark() {
        // Default constructor
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Dark";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserAgentStylesheet() {
        return FXResourcesLoader.load("css/dark.css");
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