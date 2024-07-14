package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.Config;
import cn.tealc.wutheringwavestool.MainApplication;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Font;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 20:21
 */
public class SettingViewModel implements ViewModel {
    private SimpleStringProperty gameDir = new SimpleStringProperty();
    private SimpleBooleanProperty startWithAnalysis = new SimpleBooleanProperty();
    private ObservableList<String> fontFamilyList= FXCollections.observableArrayList();

    public SettingViewModel() {
        gameDir.bindBidirectional(Config.setting.gameRootDirProperty());
        startWithAnalysis.bindBidirectional(Config.setting.firstViewWithPoolAnalysisProperty());
        fontFamilyList.setAll(Font.getFamilies());

    }


    public void setFontFamily(String fontFamily) {
        MainApplication.window.getScene().getRoot().setStyle("-fx-font-family: \"" + fontFamily+"\"");
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

    public ObservableList<String> getFontFamilyList() {
        return fontFamilyList;
    }
}