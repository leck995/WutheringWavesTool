package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.GameEdition;
import cn.tealc.wutheringwavestool.model.GameInstallation;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameInstallationManager;
import cn.tealc.wutheringwavestool.service.GameServerSwitchCoordinator;
import cn.tealc.wutheringwavestool.thread.system.CheckGameConfigTask;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.SceneLifecycle;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.nio.file.Path;

/** 管理当前编辑的游戏安装实例及服务器切换入口。 */
public class GameBaseSettingViewModel extends BaseViewModel implements SceneLifecycle {
    private static final String DEFAULT_START_APP = "Wuthering Waves.exe";
    private static final String CUSTOM_START_APP = "Client/Binaries/Win64/Client-Win64-Shipping.exe";

    @Inject private GameInstallationManager installationManager;
    @Inject private GameServerSwitchCoordinator serverSwitchCoordinator;

    private final ObjectProperty<GameEdition> editingEdition = new SimpleObjectProperty<>(GameEdition.CHINA);
    private final StringProperty gameDir = new SimpleStringProperty();
    private final StringProperty startAppPath = new SimpleStringProperty();
    private final StringProperty launcherPath = new SimpleStringProperty();
    private final BooleanProperty startAppCustom = new SimpleBooleanProperty();
    private final ObjectProperty<SourceType> chinaSource = new SimpleObjectProperty<>(SourceType.DEFAULT);
    private final StringProperty installationStatus = new SimpleStringProperty();
    private final ObservableList<String> startUpParams = FXCollections.observableArrayList();

    private boolean initialized;
    private boolean synchronizing;

    public void init() {
        if (initialized) return;
        initialized = true;
        editingEdition.addListener((observable, oldValue, newValue) -> refreshEditingInstallation());
        startAppPath.addListener((observable, oldValue, newValue) -> updateLaunchSettings());
        launcherPath.addListener((observable, oldValue, newValue) -> updateLaunchSettings());
        startAppCustom.addListener((observable, oldValue, newValue) -> updateLaunchSettings());
        chinaSource.addListener((observable, oldValue, newValue) -> {
            if (!synchronizing && editingEdition.get() == GameEdition.CHINA && newValue != null) {
                GameInstallation active = installationManager.activeInstallation();
                if (active != null && active.getEdition() == GameEdition.CHINA
                        && active.getSource() != newValue) {
                    synchronizing = true;
                    try {
                        chinaSource.set(active.getSource());
                    } finally {
                        synchronizing = false;
                    }
                    NotificationManager.message(MessageInfo.warning(LanguageManager.getString(
                            "ui.game_manager.base.server_switch.use_switch")));
                } else {
                    installationManager.updateSource(GameEdition.CHINA, newValue);
                }
            }
        });
        startUpParams.addListener((ListChangeListener<String>) change -> updateStartUpParams());
        refreshEditingInstallation();
    }

    public void setEditingEdition(GameEdition edition) {
        if (edition != null) editingEdition.set(edition);
    }

    public void setGameDirectory(Path gameDirectory) {
        if (gameDirectory == null) return;
        SourceType source = editingEdition.get() == GameEdition.GLOBAL ? SourceType.GLOBAL : chinaSource.get();
        installationManager.configureInstallation(source, gameDirectory, false);
        refreshEditingInstallation();
    }

    public void setStartAppMode(boolean custom) {
        startAppCustom.set(custom);
        startAppPath.set(defaultStartPath(custom));
    }

    public void setStartAppPath(String path) {
        startAppPath.set(path);
        startAppCustom.set(true);
    }

    public void setLauncherPath(String path) { launcherPath.set(path); }
    public void addParam(String param) { startUpParams.add(param); }
    public void deleteParam(int index) { startUpParams.remove(index); }
    public boolean isDx11() { return startUpParams.contains("-dx11"); }
    public boolean isDx12() { return startUpParams.contains("-dx12"); }
    public void addDx11() { replaceDxParam("-dx12", "-dx11"); }
    public void addDx12() { replaceDxParam("-dx11", "-dx12"); }

    public void switchServer(SourceType target) {
        if (target == null) return;
        GameEdition targetEdition = GameInstallationManager.editionOf(target);
        if (!installationManager.isConfigured(targetEdition)) {
            String key = target == SourceType.GLOBAL
                    ? "ui.game_manager.base.server_switch.global_unconfigured"
                    : "ui.game_manager.base.server_switch.china_unconfigured";
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString(key)));
            return;
        }
        if (target != SourceType.GLOBAL) {
            serverSwitchCoordinator.refresh();
            boolean ready = target == SourceType.BILIBILI
                    ? serverSwitchCoordinator.bilibiliTargetReadyProperty().get()
                    : serverSwitchCoordinator.mainlandTargetReadyProperty().get();
            if (!ready) {
                String key = target == SourceType.BILIBILI
                        ? "ui.game_manager.base.server_switch.bilibili_unready"
                        : "ui.game_manager.base.server_switch.mainland_unready";
                NotificationManager.message(MessageInfo.warning(LanguageManager.getString(key)));
                return;
            }
        }
        serverSwitchCoordinator.switchTo(target);
    }

    @Override
    public void onViewAdded() {
        serverSwitchCoordinator.refresh();
        refreshEditingInstallation();
    }

    @Override
    public void onViewRemoved() {
        checkGameLogOpen();
        Config.setting().save();
    }

    private void refreshEditingInstallation() {
        synchronizing = true;
        try {
            GameEdition edition = editingEdition.get();
            GameInstallation installation = installationManager.installation(edition).orElse(null);
            gameDir.set(installation != null ? valueOrEmpty(installation.getGameDir()) : "");
            startAppPath.set(installation != null ? valueOrEmpty(installation.getStartAppPath()) : "");
            startAppCustom.set(installation != null && installation.isStartAppCustom());
            launcherPath.set(installation != null ? valueOrEmpty(installation.getOfficialLauncherDir()) : "");
            if (edition == GameEdition.CHINA) {
                chinaSource.set(installation != null ? installation.getSource() : SourceType.DEFAULT);
            }
            startUpParams.setAll(installation != null ? installation.getStartUpParams() : FXCollections.emptyObservableList());
            installationStatus.set(installationManager.isConfigured(edition)
                    ? LanguageManager.getString("ui.game_manager.base.configured")
                    : LanguageManager.getString("ui.game_manager.base.not_configured"));
        } finally {
            synchronizing = false;
        }
    }

    private void updateLaunchSettings() {
        if (!synchronizing) {
            installationManager.updateLaunchSettings(editingEdition.get(), startAppPath.get(),
                    startAppCustom.get(), launcherPath.get());
        }
    }

    private void updateStartUpParams() {
        if (!synchronizing) installationManager.updateStartUpParams(editingEdition.get(), startUpParams);
    }

    private String defaultStartPath(boolean custom) {
        String directory = gameDir.get();
        if (directory == null || directory.isBlank()) return custom ? CUSTOM_START_APP : DEFAULT_START_APP;
        return Path.of(directory).resolve(custom ? CUSTOM_START_APP : DEFAULT_START_APP).toString();
    }

    private void replaceDxParam(String previous, String replacement) {
        int index = startUpParams.indexOf(previous);
        if (index >= 0) startUpParams.set(index, replacement);
        else if (!startUpParams.contains(replacement)) startUpParams.add(replacement);
    }

    private void checkGameLogOpen() {
        CheckGameConfigTask task = new CheckGameConfigTask();
        task.setOnSucceeded(event -> {
            if (!task.getValue()) {
                Platform.runLater(() -> NotificationManager.message(MessageInfo.success(
                        LanguageManager.getString("ui.main.sync.message.log.close"))));
            }
        });
        Thread.startVirtualThread(task);
    }

    private static String valueOrEmpty(String value) { return value != null ? value : ""; }

    public ObjectProperty<GameEdition> editingEditionProperty() { return editingEdition; }
    public StringProperty gameDirProperty() { return gameDir; }
    public StringProperty startAppPathProperty() { return startAppPath; }
    public StringProperty launcherPathProperty() { return launcherPath; }
    public BooleanProperty startAppCustomProperty() { return startAppCustom; }
    public ObjectProperty<SourceType> chinaSourceProperty() { return chinaSource; }
    public StringProperty installationStatusProperty() { return installationStatus; }
    public ObservableList<String> getStartUpParams() { return startUpParams; }
    public ObjectProperty<SourceType> currentServerProperty() { return Config.setting().gameRootDirSourceProperty(); }
}
