package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.release.Release;
import cn.tealc.wutheringwavestool.thread.system.CheckGameConfigTask;
import cn.tealc.wutheringwavestool.thread.system.CheckVersionTask;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.SceneLifecycle;
import de.saxsys.mvvmfx.ViewModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Font;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 20:21
 */
public class SettingViewModel implements ViewModel, SceneLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(SettingViewModel.class);
    private SimpleBooleanProperty startWithAnalysis = new SimpleBooleanProperty();
    private SimpleBooleanProperty exitWhenGameOver = new SimpleBooleanProperty();
    private SimpleBooleanProperty hideWhenGameStart = new SimpleBooleanProperty();
    private ObservableList<String> fontFamilyList = FXCollections.observableArrayList();
    private SimpleBooleanProperty checkNewVersion = new SimpleBooleanProperty();

    private SimpleBooleanProperty diyHomeBg = new SimpleBooleanProperty();
    private SimpleStringProperty diyHomeBgName = new SimpleStringProperty();
    private SimpleIntegerProperty homeBgType = new SimpleIntegerProperty();
    private SimpleStringProperty homeBgDir = new SimpleStringProperty();

    private ObservableList<Pair<String, Locale>> languages = FXCollections.observableArrayList();

    public SettingViewModel() {
        startWithAnalysis.bindBidirectional(Config.setting().firstViewWithPoolAnalysisProperty());
        exitWhenGameOver.bindBidirectional(Config.setting().exitWhenGameOverProperty());
        hideWhenGameStart.bindBidirectional(Config.setting().hideWhenGameStartProperty());
        fontFamilyList.setAll(Font.getFamilies());
        diyHomeBg.bindBidirectional(Config.setting().diyHomeBgProperty());
        diyHomeBgName.bindBidirectional(Config.setting().diyHomeBgNameProperty());
        checkNewVersion.bindBidirectional(Config.setting().checkNewVersionProperty());
        homeBgType.bindBidirectional(Config.setting().diyHomeBgTypeProperty());
        homeBgDir.bindBidirectional(Config.setting().diyHomeBgDirProperty());

        diyHomeBgName.addListener((observableValue, s1, s2) -> {
            if (getDiyHomeBgName() != null) {
                MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);
            }
        });

        homeBgDir.addListener((observableValue, s1, s2) -> {
            if (getHomeBgDir() != null) {
                MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);
            }
        });

        languages.setAll(
                List.of(
                        new Pair<>("简体中文", Locale.CHINA),
                        new Pair<>("English", Locale.ENGLISH)
                ));
    }

    public void changeBackground() {
        MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);
    }


    @Override
    public void onViewAdded() {

    }

    @Override
    public void onViewRemoved() {
        checkGameLogOpen();
        Config.setting().save();
    }


    /**
     * description: 检测游戏日志是否被关闭
     */
    private void checkGameLogOpen() {
        CheckGameConfigTask task = new CheckGameConfigTask();
        task.setOnSucceeded(workerStateEvent -> {
            Boolean value = task.getValue();
            if (!value) { //游戏日志可能被关闭了
                Platform.runLater(() -> {
                    NotificationManager.message(MessageInfo.success(LanguageManager.getString("ui.main.sync.message.log.close")));
                });
            }
        });
        Thread.startVirtualThread(task);
    }


    public void setFontFamily(String fontFamily) {
        MainApplication.window.getScene().getRoot().setStyle("-fx-font-family: \"" + fontFamily + "\"");
    }

    public void setBgFile(File file) {
        String suffix = LocalResourcesManager.getSuffix(file.getName());
        File newFile = new File(String.format("assets/image/bg/%d.%s", System.currentTimeMillis(), suffix));
        try {
            Files.copy(file.toPath(), newFile.toPath());
            if (diyHomeBgName.get() != null && !diyHomeBgName.get().isEmpty()) {
                File oldFile = new File(String.format("assets/image/bg/%s", diyHomeBgName.get()));
                if (oldFile.exists()) {
                    boolean delete = oldFile.delete();
                    LOG.info("旧背景删除:{}", delete);
                }
            }
            diyHomeBgName.set(newFile.getName());
            MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);

        } catch (IOException e) {
            LOG.error("IO ERROR", e);
        }
    }

    public void checkVersion() {
        CheckVersionTask task = new CheckVersionTask(false);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<Release> value = task.getValue();
            if (value.getCode() == 200) {
                MvvmFX.getNotificationCenter().publish(NotificationKey.NOTIFICATION_SHOW_UPDATE, value.getData());
            } else if (value.getCode() == 1) {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.setting.about.update.tip01")));
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.main.message.type01")));
            }
        });
        Thread.startVirtualThread(task);
    }


    public void setLanguages(Locale locale) {
        Config.setting().setLanguage(locale);
        Config.language = ResourceBundle.getBundle("cn.tealc/wutheringwavestool/language/local", Config.setting().getLanguage());
        MvvmFX.setGlobalResourceBundle(Config.language);
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

    public boolean isCheckNewVersion() {
        return checkNewVersion.get();
    }

    public SimpleBooleanProperty checkNewVersionProperty() {
        return checkNewVersion;
    }

    public ObservableList<Pair<String, Locale>> getLanguages() {
        return languages;
    }

    public int getHomeBgType() {
        return homeBgType.get();
    }

    public SimpleIntegerProperty homeBgTypeProperty() {
        return homeBgType;
    }

    public void setHomeBgType(int homeBgType) {
        this.homeBgType.set(homeBgType);
    }

    public String getHomeBgDir() {
        return homeBgDir.get();
    }

    public SimpleStringProperty homeBgDirProperty() {
        return homeBgDir;
    }

    public void setHomeBgDir(String homeBgDir) {
        this.homeBgDir.set(homeBgDir);
    }
}