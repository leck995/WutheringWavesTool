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

    public static File getGameDir(){
        if (Config.setting.getGameRootDir() != null) {
            File dir = new File(Config.setting.getGameRootDir());
            if (!dir.exists()) {
                return null;
            }
            return dir;
        }
        return null;
    }

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

    public static File getGameEngineIni() {
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


    public static File getGameScreenShoot(){
        String dir = Config.setting.getGameRootDir();
        File exe = null;
        if (dir != null) {
            if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
                exe =new File(Config.setting.getGameRootDir()+File.separator+"Client/Saved/ScreenShot");
            } else {
                exe =new File(Config.setting.getGameRootDir()+File.separator+"Wuthering Waves Game/Client/Saved/ScreenShot");
            }
            if (!exe.exists()) {
                return null;
            }
        }
        return exe;
    }

    /**
     * @description: 获取游戏日志文件夹
     * @param:
     * @return  java.io.File
     * @date:   2024/11/13
     */
    public static File getGameLogDir() {
        String dir = Config.setting.getGameRootDir();
        File exe = null;
        if (dir != null) {
            if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
                exe = new File(dir + File.separator + "Client/Saved/Logs");
            } else {
                exe = new File(dir + File.separator + "Wuthering Waves Game/Client/Saved/Logs");
            }
            if (!exe.exists()) {
                return null;
            }
        }
        return exe;
    }
    public static File getGameLogFile() {
        String dir = Config.setting.getGameRootDir();
        File file = null;
        if (dir != null) {
            if (Config.setting.getGameRootDirSource() == SourceType.WE_GAME) {
                file=new File(dir + File.separator + "Client/Saved/Logs/Client.log");
            } else {
                file=new File(dir + File.separator + "Wuthering Waves Game/Client/Saved/Logs/Client.log");
            }
        }
        return file;
    }
}