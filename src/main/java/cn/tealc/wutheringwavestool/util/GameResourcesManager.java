package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import de.saxsys.mvvmfx.MvvmFX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-19 01:35
 */
public class GameResourcesManager {
    private static final Logger LOG = LoggerFactory.getLogger(GameResourcesManager.class);


    public static File getGameDB(){
        String dir = Config.setting.getGameRootDir();
        File exe = null;
        if (dir != null) {
            if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
                exe = new File(dir + File.separator + "Client/Saved/LocalStorage/LocalStorage.db");
            } else {
                exe = new File(dir + File.separator + "Wuthering Waves Game/Client/Saved/LocalStorage/LocalStorage.db");

            }
            if (!exe.exists()) {
                return null;
            }
        }
        return exe;
    }

    public static File getGameExeClient(){
        String dir = Config.setting.getGameRootDir();
        File exe = null;
        if (dir != null) {
            if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
                exe = new File(dir + File.separator + "Client/Binaries/Win64/Client-Win64-Shipping.exe");
            } else {
                exe = new File(dir + File.separator + "Wuthering Waves Game/Client/Binaries/Win64/Client-Win64-Shipping.exe");
            }
            if (!exe.exists()) {
                return null;
            }
        }
        return exe;
    }
    public static File getGameExeBase(){
        String dir = Config.setting.getGameRootDir();
        File exe = null;
        if (dir != null) {
            if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
                exe = new File(dir + File.separator + "Wuthering Waves.exe");
            } else {
                exe = new File(dir + File.separator + "Wuthering Waves Game" + File.separator + "Wuthering Waves.exe");

            }
            if (!exe.exists()) {
                return null;
            }
        }
        return exe;
    }

    public static File getGameEngineIni(){
        String dir = Config.setting.getGameRootDir();
        File exe = null;
        if (dir != null) {
            if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
                exe = new File(dir + File.separator + "Client/Saved/Config/WindowsNoEditor/Engine.ini");
            } else {
                exe = new File(dir + File.separator + "Wuthering Waves Game/Client/Saved/Config/WindowsNoEditor/Engine.ini");
            }
            if (!exe.exists()) {
                return null;
            }
        }
        return exe;
    }
}