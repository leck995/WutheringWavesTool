package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-19 23:17
 */
public class GameDirChooseViewModel implements ViewModel {
    private SimpleStringProperty localDir = new SimpleStringProperty();
    private SimpleObjectProperty<SourceType> localSourceType = new SimpleObjectProperty<>();

    private SimpleStringProperty installDir = new SimpleStringProperty();
    private SimpleObjectProperty<SourceType> installSourceType = new SimpleObjectProperty<>();

    private SimpleBooleanProperty finishEnabled = new SimpleBooleanProperty(false);

    private SimpleBooleanProperty downloadEnable = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty hasInstalled = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty serverEnable = new SimpleBooleanProperty(false);
    public GameDirChooseViewModel() {

    }


    /**
     * 完成本地游戏目录设置
     * @return boolean 设置成功返回true
     */
    public boolean finishLocal(){
        boolean checked = checkLocalDirCurrent(new File(localDir.get()));
        if (checked) {
            Config.setting.setGameRootDir(localDir.get());
            Config.setting.setGameRootDirSource(localSourceType.get());
            return true;
        }
        return false;
    }



    public void setLocalDir(File dir) {
        boolean checked = checkLocalDirCurrent(dir);
        if (checked) {
            localDir.set(dir.getAbsolutePath());
            SourceType type = checkCurrentServer();
            localSourceType.set(type);
            finishEnabled.set(true);
        }else {
            finishEnabled.set(false);
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.message01")));
        }
    }


    /**
     * 判断本地目录是否正确，只适用于本地已存在游戏的设置，网络下载不符合
     * @param dir 游戏目录
     * @return boolean
     */
    private boolean checkLocalDirCurrent(File dir){
        File startApp = new File(dir.getAbsolutePath() + File.separator + "Wuthering Waves.exe");
        return startApp.exists();
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
            if (weGameFile.exists()) {
                return SourceType.WE_GAME;
            }

        }
        return SourceType.DEFAULT;
    }


    public String getLocalDir() {
        return localDir.get();
    }

    public SimpleStringProperty localDirProperty() {
        return localDir;
    }

    public SourceType getLocalSourceType() {
        return localSourceType.get();
    }

    public SimpleObjectProperty<SourceType> localSourceTypeProperty() {
        return localSourceType;
    }

    public void setLocalSourceType(SourceType localSourceType) {
        this.localSourceType.set(localSourceType);
    }

    public boolean isFinishEnabled() {
        return finishEnabled.get();
    }

    public SimpleBooleanProperty finishEnabledProperty() {
        return finishEnabled;
    }
}