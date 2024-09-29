package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 20:21
 */
public class SettingViewModel implements ViewModel {
    private static final Logger LOG= LoggerFactory.getLogger(SettingViewModel.class);
    private SimpleBooleanProperty changeTitlebar = new SimpleBooleanProperty();
    private SimpleStringProperty gameDir = new SimpleStringProperty();
    private SimpleBooleanProperty startWithAnalysis = new SimpleBooleanProperty();
    private SimpleBooleanProperty exitWhenGameOver = new SimpleBooleanProperty();
    private SimpleBooleanProperty hideWhenGameStart = new SimpleBooleanProperty();
    private ObservableList<String> fontFamilyList= FXCollections.observableArrayList();
    private SimpleBooleanProperty diyHomeBg=new SimpleBooleanProperty();
    private SimpleStringProperty diyHomeBgName=new SimpleStringProperty();
    private SimpleObjectProperty<SourceType> gameRootDirSource=new SimpleObjectProperty<>();

    public SettingViewModel() {
        changeTitlebar.bindBidirectional(Config.setting.changeTitlebarProperty());
        gameDir.bindBidirectional(Config.setting.gameRootDirProperty());
        startWithAnalysis.bindBidirectional(Config.setting.firstViewWithPoolAnalysisProperty());
        exitWhenGameOver.bindBidirectional(Config.setting.exitWhenGameOverProperty());
        hideWhenGameStart.bindBidirectional(Config.setting.hideWhenGameStartProperty());
        gameRootDirSource.bindBidirectional(Config.setting.gameRootDirSourceProperty());

        fontFamilyList.setAll(Font.getFamilies());
        diyHomeBg.bindBidirectional(Config.setting.diyHomeBgProperty());
        diyHomeBgName.bindBidirectional(Config.setting.diyHomeBgNameProperty());

        diyHomeBg.addListener((observableValue, aBoolean, t1) -> {
            if (t1){
              if (getDiyHomeBgName()!= null){
                  MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);
              }
            }else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);
            }
        });

    }


    public void setFontFamily(String fontFamily) {
        MainApplication.window.getScene().getRoot().setStyle("-fx-font-family: \"" + fontFamily+"\"");
    }

    public void setBgFile(File file){
        String suffix = LocalResourcesManager.getSuffix(file.getName());
        File newFile = new File(String.format("assets/image/bg/%d.%s",System.currentTimeMillis(),suffix));
        try {
            Files.copy(file.toPath(),newFile.toPath());
            if (diyHomeBgName.get() != null && !diyHomeBgName.get().isEmpty()){
                File oldFile=new File(String.format("assets/image/bg/%s",diyHomeBgName.get()));
                if (oldFile.exists()){
                    boolean delete = oldFile.delete();
                    LOG.info("旧背景删除:{}",delete);
                }
            }

            diyHomeBgName.set(newFile.getName());
            MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);



        } catch (IOException e) {
            LOG.error("IO ERROR",e);
        }

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

    public boolean isExitWhenGameOver() {
        return exitWhenGameOver.get();
    }

    public SimpleBooleanProperty exitWhenGameOverProperty() {
        return exitWhenGameOver;
    }

    public void setExitWhenGameOver(boolean exitWhenGameOver) {
        this.exitWhenGameOver.set(exitWhenGameOver);
    }

    public boolean isHideWhenGameStart() {
        return hideWhenGameStart.get();
    }

    public SimpleBooleanProperty hideWhenGameStartProperty() {
        return hideWhenGameStart;
    }

    public boolean isDiyHomeBg() {
        return diyHomeBg.get();
    }

    public SimpleBooleanProperty diyHomeBgProperty() {
        return diyHomeBg;
    }

    public String getDiyHomeBgName() {
        return diyHomeBgName.get();
    }

    public SimpleStringProperty diyHomeBgNameProperty() {
        return diyHomeBgName;
    }


    public SourceType getGameRootDirSource() {
        return gameRootDirSource.get();
    }

    public SimpleObjectProperty<SourceType> gameRootDirSourceProperty() {
        return gameRootDirSource;
    }

    public void setGameRootDirSource(SourceType gameRootDirSource) {
        this.gameRootDirSource.set(gameRootDirSource);
    }

    public boolean isChangeTitlebar() {
        return changeTitlebar.get();
    }

    public SimpleBooleanProperty changeTitlebarProperty() {
        return changeTitlebar;
    }
}