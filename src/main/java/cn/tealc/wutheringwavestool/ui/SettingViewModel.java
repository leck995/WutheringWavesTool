package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.Config;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 20:21
 */
public class SettingViewModel implements ViewModel {
    private SimpleStringProperty gameDir = new SimpleStringProperty();
    private SimpleBooleanProperty startWithAnalysis = new SimpleBooleanProperty();
    public SettingViewModel() {
        gameDir.bindBidirectional(Config.setting.gameRootDirProperty());
        startWithAnalysis.bindBidirectional(Config.setting.firstViewWithPoolAnalysisProperty());
    }

    public String getGameDir() {
        return gameDir.get();
    }

    public SimpleStringProperty gameDirProperty() {
        return gameDir;
    }

    public boolean isStartWithAnalysis() {
        return startWithAnalysis.get();
    }

    public SimpleBooleanProperty startWithAnalysisProperty() {
        return startWithAnalysis;
    }

    public void setStartWithAnalysis(boolean startWithAnalysis) {
        this.startWithAnalysis.set(startWithAnalysis);
    }
}