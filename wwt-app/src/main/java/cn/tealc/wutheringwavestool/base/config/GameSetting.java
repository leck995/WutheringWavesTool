package cn.tealc.wutheringwavestool.base.config;

import cn.tealc.wutheringwavestool.model.SourceType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * 游戏路径与行为相关配置：游戏根目录、启动文件、官方启动器目录、游戏来源类型，
 * 以及游戏启动/关闭时的自动行为。
 * <p>对应原 Setting 的"首选"与"游戏行为"分段。
 */
public class GameSetting {
    private SimpleObjectProperty<SourceType> gameRootDirSource = new SimpleObjectProperty<>(SourceType.DEFAULT); //游戏来源类型
    private SimpleStringProperty gameRootDir = new SimpleStringProperty();//游戏根目录
    private SimpleStringProperty gameStarAppPath = new SimpleStringProperty("Wuthering Waves.exe");//游戏启动文件
    private SimpleBooleanProperty gameStartAppCustom = new SimpleBooleanProperty(false); //自定义启动程序
    private SimpleStringProperty gameOfficialLauncherDir = new SimpleStringProperty();//游戏更新器目录
    private SimpleBooleanProperty exitWhenGameOver = new SimpleBooleanProperty(false); //检测到游戏关闭自动关闭程序
    private SimpleBooleanProperty hideWhenGameStart = new SimpleBooleanProperty(false); //检测到游戏启动自动隐藏程序至托盘
    private SimpleBooleanProperty autoStartGame = new SimpleBooleanProperty(false); //手动启动程序时自动启动游戏
    private SimpleStringProperty gameDownloadDir = new SimpleStringProperty(); //游戏下载保存目录（可自定义，留空则使用游戏根目录 WwtBackup）

    // ---------- gameRootDirSource ----------
    public SourceType getGameRootDirSource() { return gameRootDirSource.get(); }
    public SimpleObjectProperty<SourceType> gameRootDirSourceProperty() { return gameRootDirSource; }
    public void setGameRootDirSource(SourceType gameRootDirSource) { this.gameRootDirSource.set(gameRootDirSource); }

    // ---------- gameRootDir ----------
    public String getGameRootDir() { return gameRootDir.get(); }
    public SimpleStringProperty gameRootDirProperty() { return gameRootDir; }
    public void setGameRootDir(String gameRootDir) { this.gameRootDir.set(gameRootDir); }

    // ---------- gameStarAppPath ----------
    public String getGameStarAppPath() { return gameStarAppPath.get(); }
    public SimpleStringProperty gameStarAppPathProperty() { return gameStarAppPath; }
    public void setGameStarAppPath(String gameStarAppPath) { this.gameStarAppPath.set(gameStarAppPath); }

    // ---------- gameStartAppCustom ----------
    public boolean isGameStartAppCustom() { return gameStartAppCustom.get(); }
    public SimpleBooleanProperty gameStartAppCustomProperty() { return gameStartAppCustom; }
    public void setGameStartAppCustom(boolean gameStartAppCustom) { this.gameStartAppCustom.set(gameStartAppCustom); }

    // ---------- gameOfficialLauncherDir ----------
    public String getGameOfficialLauncherDir() { return gameOfficialLauncherDir.get(); }
    public SimpleStringProperty gameOfficialLauncherDirProperty() { return gameOfficialLauncherDir; }
    public void setGameOfficialLauncherDir(String gameOfficialLauncherDir) { this.gameOfficialLauncherDir.set(gameOfficialLauncherDir); }

    // ---------- exitWhenGameOver ----------
    public boolean isExitWhenGameOver() { return exitWhenGameOver.get(); }
    public SimpleBooleanProperty exitWhenGameOverProperty() { return exitWhenGameOver; }
    public void setExitWhenGameOver(boolean exitWhenGameOver) { this.exitWhenGameOver.set(exitWhenGameOver); }

    // ---------- hideWhenGameStart ----------
    public boolean isHideWhenGameStart() { return hideWhenGameStart.get(); }
    public SimpleBooleanProperty hideWhenGameStartProperty() { return hideWhenGameStart; }
    public void setHideWhenGameStart(boolean hideWhenGameStart) { this.hideWhenGameStart.set(hideWhenGameStart); }

    // ---------- autoStartGame ----------
    public boolean isAutoStartGame() { return autoStartGame.get(); }
    public SimpleBooleanProperty autoStartGameProperty() { return autoStartGame; }
    public void setAutoStartGame(boolean autoStartGame) { this.autoStartGame.set(autoStartGame); }

    // ---------- gameDownloadDir ----------
    public String getGameDownloadDir() { return gameDownloadDir.get(); }
    public SimpleStringProperty gameDownloadDirProperty() { return gameDownloadDir; }
    public void setGameDownloadDir(String gameDownloadDir) { this.gameDownloadDir.set(gameDownloadDir); }
}
