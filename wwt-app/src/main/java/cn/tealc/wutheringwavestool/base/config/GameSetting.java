package cn.tealc.wutheringwavestool.base.config;

import cn.tealc.wutheringwavestool.model.GameEdition;
import cn.tealc.wutheringwavestool.model.GameInstallation;
import cn.tealc.wutheringwavestool.model.SourceType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏路径与行为相关配置：游戏根目录、启动文件、官方启动器目录、游戏来源类型，
 * 以及游戏启动/关闭时的自动行为。
 * <p>对应原 Setting 的"首选"与"游戏行为"分段。
 */
public class GameSetting {
    public static final String CHINA_INSTALLATION_ID = "china";
    public static final String GLOBAL_INSTALLATION_ID = "global";

    private SimpleObjectProperty<SourceType> gameRootDirSource = new SimpleObjectProperty<>(SourceType.DEFAULT); //游戏来源类型
    private SimpleStringProperty gameRootDir = new SimpleStringProperty();//游戏根目录
    private SimpleStringProperty gameStarAppPath = new SimpleStringProperty("Wuthering Waves.exe");//游戏启动文件
    private SimpleBooleanProperty gameStartAppCustom = new SimpleBooleanProperty(false); //自定义启动程序
    private SimpleStringProperty gameOfficialLauncherDir = new SimpleStringProperty();//游戏更新器目录
    private SimpleBooleanProperty exitWhenGameOver = new SimpleBooleanProperty(false); //检测到游戏关闭自动关闭程序
    private SimpleBooleanProperty hideWhenGameStart = new SimpleBooleanProperty(false); //检测到游戏启动自动隐藏程序至托盘
    private SimpleBooleanProperty autoStartGame = new SimpleBooleanProperty(false); //手动启动程序时自动启动游戏
    private SimpleStringProperty gameDownloadDir = new SimpleStringProperty(); //游戏下载保存目录（可自定义，留空则使用游戏根目录 WwtBackup）
    private SimpleStringProperty gameInstalledVersion = new SimpleStringProperty(); //已安装的游戏版本号（用于更新检查）
    private SimpleIntegerProperty downloadParallelCount = new SimpleIntegerProperty(4); // 下载并发数
    private SimpleLongProperty downloadSpeedLimitBytesPerSecond = new SimpleLongProperty(0); // 0 表示不限速
    private List<GameInstallation> gameInstallations = new ArrayList<>();
    private String activeGameInstallationId;

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

    // ---------- gameInstalledVersion ----------
    public String getGameInstalledVersion() { return gameInstalledVersion.get(); }
    public SimpleStringProperty gameInstalledVersionProperty() { return gameInstalledVersion; }
    public void setGameInstalledVersion(String gameInstalledVersion) { this.gameInstalledVersion.set(gameInstalledVersion); }

    public int getDownloadParallelCount() { return downloadParallelCount.get(); }
    public SimpleIntegerProperty downloadParallelCountProperty() { return downloadParallelCount; }
    public void setDownloadParallelCount(int downloadParallelCount) {
        this.downloadParallelCount.set(Math.clamp(downloadParallelCount, 1, 16));
    }

    public long getDownloadSpeedLimitBytesPerSecond() { return downloadSpeedLimitBytesPerSecond.get(); }
    public SimpleLongProperty downloadSpeedLimitBytesPerSecondProperty() {
        return downloadSpeedLimitBytesPerSecond;
    }
    public void setDownloadSpeedLimitBytesPerSecond(long bytesPerSecond) {
        this.downloadSpeedLimitBytesPerSecond.set(Math.max(0, bytesPerSecond));
    }

    public List<GameInstallation> getGameInstallations() { return gameInstallations; }
    public void setGameInstallations(List<GameInstallation> gameInstallations) {
        this.gameInstallations = gameInstallations != null ? new ArrayList<>(gameInstallations) : new ArrayList<>();
    }

    public String getActiveGameInstallationId() { return activeGameInstallationId; }
    public void setActiveGameInstallationId(String activeGameInstallationId) {
        this.activeGameInstallationId = activeGameInstallationId;
    }

    /** Creates the two installation slots and migrates the legacy single-directory configuration. */
    public void ensureGameInstallations() {
        if (gameInstallations == null) {
            gameInstallations = new ArrayList<>();
        }
        GameInstallation china = findInstallation(GameEdition.CHINA);
        GameInstallation global = findInstallation(GameEdition.GLOBAL);
        SourceType legacySource = getGameRootDirSource() != null ? getGameRootDirSource() : SourceType.DEFAULT;

        if (china == null) {
            china = new GameInstallation(CHINA_INSTALLATION_ID, "国服", GameEdition.CHINA,
                    legacySource == SourceType.GLOBAL ? SourceType.DEFAULT : legacySource);
            if (legacySource != SourceType.GLOBAL) {
                copyLegacyValuesTo(china);
            }
            gameInstallations.add(china);
        }
        if (global == null) {
            global = new GameInstallation(GLOBAL_INSTALLATION_ID, "国际服", GameEdition.GLOBAL, SourceType.GLOBAL);
            if (legacySource == SourceType.GLOBAL) {
                copyLegacyValuesTo(global);
            }
            gameInstallations.add(global);
        }
        normalizeInstallation(china, CHINA_INSTALLATION_ID, "国服", GameEdition.CHINA, SourceType.DEFAULT);
        normalizeInstallation(global, GLOBAL_INSTALLATION_ID, "国际服", GameEdition.GLOBAL, SourceType.GLOBAL);

        if (findInstallation(activeGameInstallationId) == null) {
            activeGameInstallationId = legacySource == SourceType.GLOBAL
                    ? GLOBAL_INSTALLATION_ID : CHINA_INSTALLATION_ID;
        }
    }

    public GameInstallation findInstallation(GameEdition edition) {
        if (edition == null || gameInstallations == null) {
            return null;
        }
        return gameInstallations.stream()
                .filter(installation -> installation != null && edition == installation.getEdition())
                .findFirst()
                .orElse(null);
    }

    public GameInstallation findInstallation(String id) {
        if (id == null || gameInstallations == null) {
            return null;
        }
        return gameInstallations.stream()
                .filter(installation -> installation != null && id.equals(installation.getId()))
                .findFirst()
                .orElse(null);
    }

    private void copyLegacyValuesTo(GameInstallation installation) {
        installation.setGameDir(getGameRootDir());
        installation.setVersion(getGameInstalledVersion());
        installation.setStartAppPath(getGameStarAppPath());
        installation.setStartAppCustom(isGameStartAppCustom());
        installation.setOfficialLauncherDir(getGameOfficialLauncherDir());
    }

    private static void normalizeInstallation(GameInstallation installation, String id, String name,
            GameEdition edition, SourceType defaultSource) {
        if (installation.getId() == null || installation.getId().isBlank()) {
            installation.setId(id);
        }
        if (installation.getName() == null || installation.getName().isBlank()) {
            installation.setName(name);
        }
        installation.setEdition(edition);
        if (installation.getSource() == null
                || (edition == GameEdition.GLOBAL && installation.getSource() != SourceType.GLOBAL)
                || (edition == GameEdition.CHINA && installation.getSource() == SourceType.GLOBAL)) {
            installation.setSource(defaultSource);
        }
    }
}
