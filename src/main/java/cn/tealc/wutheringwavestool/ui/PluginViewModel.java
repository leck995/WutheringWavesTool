package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.plugin.FxPluginConfig;
import cn.tealc.wutheringwavestool.plugin.FxPluginManager;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2025-01-01 01:31
 */
public class PluginViewModel implements ViewModel {
    private ObservableList<FxPluginConfig> plugins = FXCollections.observableArrayList();
    public PluginViewModel() {
        FxPluginManager pluginManager = FxPluginManager.getInstance();
        Map<Integer, FxPluginConfig> pluginMap = pluginManager.getPlugins();
        plugins.addAll(pluginMap.values());
    }

    public ObservableList<FxPluginConfig> getPlugins() {
        return plugins;
    }
}