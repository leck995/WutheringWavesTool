package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.base.Setting;
import cn.tealc.wutheringwavestool.base.config.GameSetting;
import cn.tealc.wutheringwavestool.model.GameEdition;
import cn.tealc.wutheringwavestool.model.GameInstallation;
import cn.tealc.wutheringwavestool.model.SourceType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;

/** Owns the active physical game installation and mirrors it to legacy settings properties. */
@Singleton
public final class GameInstallationManager {
    private static final String DEFAULT_START_APP = "Wuthering Waves.exe";

    private final Setting setting;
    private final GameSetting gameSetting;
    private final ReadOnlyObjectWrapper<GameInstallation> activeInstallation = new ReadOnlyObjectWrapper<>();
    private boolean synchronizing;

    @Inject
    public GameInstallationManager(Setting setting) {
        this.setting = setting;
        this.gameSetting = setting.getGame();
        gameSetting.ensureGameInstallations();
        installLegacyMirrorListeners();
        GameInstallation active = gameSetting.findInstallation(gameSetting.getActiveGameInstallationId());
        if (active == null) {
            active = gameSetting.findInstallation(GameEdition.CHINA);
        }
        loadLegacyMirror(active);
    }

    public Optional<GameInstallation> installation(GameEdition edition) {
        return Optional.ofNullable(gameSetting.findInstallation(edition));
    }

    public Optional<GameInstallation> installation(SourceType source) {
        return installation(editionOf(source));
    }

    public Optional<Path> gameDirectory(GameEdition edition) {
        return installation(edition)
                .map(GameInstallation::getGameDir)
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .map(path -> path.toAbsolutePath().normalize());
    }

    public boolean isConfigured(GameEdition edition) {
        return gameDirectory(edition)
                .map(path -> Files.isRegularFile(path.resolve(DEFAULT_START_APP)))
                .orElse(false);
    }

    public boolean isConfigured(SourceType source) {
        return isConfigured(editionOf(source));
    }

    public GameInstallation activeInstallation() {
        return activeInstallation.get();
    }

    public ReadOnlyObjectProperty<GameInstallation> activeInstallationProperty() {
        return activeInstallation.getReadOnlyProperty();
    }

    public synchronized void configureInstallation(SourceType detectedSource, Path gameDir, boolean activate) {
        if (detectedSource == null || gameDir == null) {
            throw new IllegalArgumentException("游戏来源和目录不能为空");
        }
        GameEdition edition = editionOf(detectedSource);
        GameInstallation installation = gameSetting.findInstallation(edition);
        if (installation == null) {
            throw new IllegalStateException("找不到对应的游戏安装实例");
        }
        String normalizedDir = gameDir.toAbsolutePath().normalize().toString();
        boolean directoryChanged = installation.getGameDir() == null
                || !installation.getGameDir().equalsIgnoreCase(normalizedDir);
        installation.setGameDir(normalizedDir);
        installation.setSource(normalizeSource(edition, detectedSource));
        if (installation.getStartAppPath() == null || installation.getStartAppPath().isBlank()) {
            installation.setStartAppPath(DEFAULT_START_APP);
        }
        if (directoryChanged) {
            installation.setVersion(null);
            installation.setStartAppPath(DEFAULT_START_APP);
            installation.setStartAppCustom(false);
            installation.setOfficialLauncherDir(null);
        }
        if (activeInstallation.get() == installation) {
            loadLegacyMirror(installation);
        } else if (activate) {
            activateInternal(installation);
        }
    }

    public synchronized boolean activate(GameEdition edition) {
        GameInstallation installation = gameSetting.findInstallation(edition);
        if (installation == null || installation.getGameDir() == null || installation.getGameDir().isBlank()) {
            return false;
        }
        activateInternal(installation);
        return true;
    }

    public synchronized void updateSource(GameEdition edition, SourceType source) {
        GameInstallation installation = gameSetting.findInstallation(edition);
        if (installation == null) {
            return;
        }
        installation.setSource(normalizeSource(edition, source));
        if (activeInstallation.get() == installation) {
            runSynchronized(() -> setting.setGameRootDirSource(installation.getSource()));
        }
    }

    private void activateInternal(GameInstallation installation) {
        persistLegacyMirror(activeInstallation.get());
        gameSetting.setActiveGameInstallationId(installation.getId());
        loadLegacyMirror(installation);
    }

    private void loadLegacyMirror(GameInstallation installation) {
        if (installation == null) {
            return;
        }
        runSynchronized(() -> {
            setting.setGameRootDirSource(installation.getSource());
            setting.setGameRootDir(installation.getGameDir());
            setting.setGameInstalledVersion(installation.getVersion());
            setting.setGameStarAppPath(valueOrDefault(installation.getStartAppPath(), DEFAULT_START_APP));
            setting.setGameStartAppCustom(installation.isStartAppCustom());
            setting.setGameOfficialLauncherDir(installation.getOfficialLauncherDir());
            activeInstallation.set(installation);
        });
    }

    private void persistLegacyMirror(GameInstallation installation) {
        if (installation == null) {
            return;
        }
        SourceType configuredSource = setting.getGameRootDirSource();
        if (isCompatible(installation.getEdition(), configuredSource)) {
            installation.setSource(configuredSource);
        }
        installation.setGameDir(setting.getGameRootDir());
        installation.setVersion(setting.getGameInstalledVersion());
        installation.setStartAppPath(setting.getGameStarAppPath());
        installation.setStartAppCustom(setting.isGameStartAppCustom());
        installation.setOfficialLauncherDir(setting.getGameOfficialLauncherDir());
    }

    private void installLegacyMirrorListeners() {
        setting.gameRootDirProperty().addListener((observable, oldValue, newValue) -> updateActive(installation ->
                installation.setGameDir(newValue)));
        setting.gameInstalledVersionProperty().addListener((observable, oldValue, newValue) -> updateActive(installation ->
                installation.setVersion(newValue)));
        setting.gameStarAppPathProperty().addListener((observable, oldValue, newValue) -> updateActive(installation ->
                installation.setStartAppPath(newValue)));
        setting.gameStartAppCustomProperty().addListener((observable, oldValue, newValue) -> updateActive(installation ->
                installation.setStartAppCustom(newValue)));
        setting.gameOfficialLauncherDirProperty().addListener((observable, oldValue, newValue) -> updateActive(installation ->
                installation.setOfficialLauncherDir(newValue)));
        setting.gameRootDirSourceProperty().addListener((observable, oldValue, newValue) -> {
            if (synchronizing || activeInstallation.get() == null) {
                return;
            }
            GameInstallation active = activeInstallation.get();
            if (isCompatible(active.getEdition(), newValue)) {
                active.setSource(newValue);
            } else {
                runSynchronized(() -> setting.setGameRootDirSource(active.getSource()));
            }
        });
    }

    private void updateActive(InstallationUpdater updater) {
        if (!synchronizing && activeInstallation.get() != null) {
            updater.update(activeInstallation.get());
        }
    }

    private void runSynchronized(Runnable action) {
        synchronizing = true;
        try {
            action.run();
        } finally {
            synchronizing = false;
        }
    }

    public static GameEdition editionOf(SourceType source) {
        return source == SourceType.GLOBAL ? GameEdition.GLOBAL : GameEdition.CHINA;
    }

    private static SourceType normalizeSource(GameEdition edition, SourceType source) {
        if (edition == GameEdition.GLOBAL) {
            return SourceType.GLOBAL;
        }
        return source == null || source == SourceType.GLOBAL ? SourceType.DEFAULT : source;
    }

    private static boolean isCompatible(GameEdition edition, SourceType source) {
        return source != null && (edition == GameEdition.GLOBAL
                ? source == SourceType.GLOBAL : source != SourceType.GLOBAL);
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    @FunctionalInterface
    private interface InstallationUpdater {
        void update(GameInstallation installation);
    }
}
