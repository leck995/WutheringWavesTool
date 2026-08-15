package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.model.ui.ServerData;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.io.File;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 19:18
 */
public class GameManagerViewModel extends BaseViewModel {

    private final ObservableList<ServerData> serverList = FXCollections.observableArrayList();


    private SimpleObjectProperty<SourceType> gameRootDirSource = new SimpleObjectProperty<>();
    private SimpleStringProperty gameRootDir=new SimpleStringProperty();




    public GameManagerViewModel() {
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
        File gameDir = GameResourcesManager.getGameDir();
        if(gameDir != null){
            //判断国际服是否存在
            File globalFile = new File(gameDir, "/Client/Binaries/Win64/ThirdParty/KrPcSdk_Global");
            serverList.add(new ServerData(SourceType.GLOBAL,globalFile.exists()));
            //判断国内BILIBILI是否存在
            File mainLandBiliBiliFile = new File(gameDir, "/Client/Binaries/Win64/ThirdParty/KrPcSdk_Mainland-bil/KRSDKRes/Bilibili");
            serverList.add(new ServerData(SourceType.BILIBILI,mainLandBiliBiliFile.exists()));
            //判断国内官服是否存在

          /*  File globalFile = new File(gameDir, "/Client/Binaries/Win64/ThirdParty/KrPcSdk_Global");
            //判断BiLIBILI是否存在
            serverList.add(new Pair<>(SourceType.GLOBAL,globalFile.exists()));
            File globalFile = new File(gameDir, "/Client/Binaries/Win64/ThirdParty/KrPcSdk_Global");
            serverList.add(new Pair<>(SourceType.GLOBAL,globalFile.exists()));*/

        }
    }


    public void setGameDir(File gameDir) {
        File startApp = new File(gameDir.getAbsolutePath() + File.separator + "Wuthering Waves.exe");
        if (startApp.exists()) {
            gameRootDir.set(gameDir.getAbsolutePath());
            SourceType sourceType = checkCurrentServer();
            setGameRootDirSource(sourceType);
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,MessageInfo.warning(LanguageManager.getString("ui.setting.message.01")));
        }
    }
    /**
     * @description: 判断当前所处的服务器
     * @param:
     * @return  cn.tealc.wutheringwavestool.model.SourceType
     * @date:   2025/2/18
     */
    private SourceType checkCurrentServer(){
        File gameDir = GameResourcesManager.getGameDir();
        if(gameDir != null){
            File globalFile = new File(gameDir, "/Client/Binaries/Win64/ThirdParty/KrPcSdk_Global");
            if (globalFile.exists()) {
                return SourceType.GLOBAL;
            }
            File bilibiliFile = new File(gameDir, "/Client/Binaries/Win64/ThirdParty/KrPcSdk_Mainland/KRSDKRes/Bilibili");
            if (bilibiliFile.exists()) {
                return SourceType.BILIBILI;
            }
            File weGameFile = new File(gameDir, "/Client/Binaries/Win64/ThirdParty/KrPcSdk_Mainland/KRSDKRes/wegame");
            if (bilibiliFile.exists()) {
                return SourceType.WE_GAME;
            }

        }
        return SourceType.DEFAULT;
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
