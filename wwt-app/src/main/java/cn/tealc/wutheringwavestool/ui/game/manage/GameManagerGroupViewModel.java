package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wwt.game.resource.GameServerSwitchService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameInstallationManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.model.ui.ServerData;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.google.inject.Inject;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 19:18
 */
public class GameManagerGroupViewModel extends BaseViewModel {

    @Inject
    private GameServerSwitchService serverSwitchService;
    @Inject
    private GameInstallationManager installationManager;

    private final ObservableList<ServerData> serverList = FXCollections.observableArrayList();


    private SimpleObjectProperty<SourceType> gameRootDirSource = new SimpleObjectProperty<>();
    private SimpleStringProperty gameRootDir=new SimpleStringProperty();




    public GameManagerGroupViewModel() {
        gameRootDirSource.bindBidirectional(Config.setting().gameRootDirSourceProperty());
        gameRootDir.bindBidirectional(Config.setting().gameRootDirProperty());
        checkService();
    }


    /**
     * 判断游戏是否已安装
     * @return boolean
     */
    public boolean installed(){
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir == null)
            return false;

        File startApp = new File(gameDir.getAbsolutePath() + File.separator + "Wuthering Waves.exe");
        return startApp.exists();
    }


    private void checkService(){
        serverList.clear();
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir == null) {
            return;
        }
        Optional<SourceType> detected = serverSwitchService.detectActiveSource(gameDir.toPath())
                .map(SourceType::fromGameDownloadSource);
        for (SourceType source : SourceType.values()) {
            serverList.add(new ServerData(source, detected.filter(source::equals).isPresent()));
        }
    }


    public void setGameDir(File gameDir) {
        File startApp = new File(gameDir.getAbsolutePath() + File.separator + "Wuthering Waves.exe");
        if (startApp.exists()) {
            var detectedSource = serverSwitchService
                    .detectActiveSource(gameDir.toPath());
            detectedSource.map(SourceType::fromGameDownloadSource)
                    .ifPresent(source -> installationManager.configureInstallation(source, gameDir.toPath(), true));
            if (detectedSource.isEmpty()) {
                NotificationManager.message(MessageInfo.warning("无法根据 launcherDownloadConfig.json 和 SDK 目录识别服务器，请手动确认区服"));
            } else {
                Config.setting().save();
            }
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,MessageInfo.warning(LanguageManager.getString("ui.setting.message.01")));
        }
    }
    public void setGameRootDirSource(String type){
        switch (type) {
            case "default"-> {
                setGameRootDirSource(SourceType.DEFAULT);
            }
            case "bilibili"-> {
                setGameRootDirSource(SourceType.BILIBILI);
            }
            case "wegame" -> {
                setGameRootDirSource(SourceType.WE_GAME);
                NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.tip02")));
            }
            case "global" -> {
                setGameRootDirSource(SourceType.GLOBAL);
            }
        }
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

    public ObservableList<ServerData> getServerList() {
        return serverList;
    }
}
