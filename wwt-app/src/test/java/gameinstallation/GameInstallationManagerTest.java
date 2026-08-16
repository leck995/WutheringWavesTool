package gameinstallation;

import cn.tealc.wutheringwavestool.base.Setting;
import cn.tealc.wutheringwavestool.model.GameEdition;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.service.GameInstallationManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameInstallationManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyDirectoryIntoChinaInstallation() {
        Setting setting = new Setting();
        setting.setGameRootDir(tempDir.resolve("china").toString());
        setting.setGameRootDirSource(SourceType.BILIBILI);
        setting.setGameInstalledVersion("2.7.0");

        GameInstallationManager manager = new GameInstallationManager(setting);

        var china = manager.installation(GameEdition.CHINA).orElseThrow();
        var global = manager.installation(GameEdition.GLOBAL).orElseThrow();
        assertEquals(SourceType.BILIBILI, china.getSource());
        assertEquals(setting.getGameRootDir(), china.getGameDir());
        assertEquals("2.7.0", china.getVersion());
        assertEquals(SourceType.GLOBAL, global.getSource());
        assertEquals(china, manager.activeInstallation());
    }

    @Test
    void keepsInstallationSpecificValuesWhenSwitchingAndSerializing() throws Exception {
        Setting setting = new Setting();
        GameInstallationManager manager = new GameInstallationManager(setting);
        Path chinaDir = tempDir.resolve("china");
        Path globalDir = tempDir.resolve("global");

        manager.configureInstallation(SourceType.DEFAULT, chinaDir, true);
        setting.setGameInstalledVersion("3.5.3-cn");
        setting.setGameStarAppPath("china-launcher.exe");
        manager.configureInstallation(SourceType.GLOBAL, globalDir, true);
        setting.setGameInstalledVersion("3.5.3-global");
        setting.setGameStarAppPath("global-launcher.exe");

        assertEquals(chinaDir.toAbsolutePath().normalize().toString(),
                manager.installation(GameEdition.CHINA).orElseThrow().getGameDir());
        assertEquals(globalDir.toAbsolutePath().normalize().toString(),
                manager.installation(GameEdition.GLOBAL).orElseThrow().getGameDir());

        manager.activate(GameEdition.CHINA);
        assertEquals(chinaDir.toAbsolutePath().normalize().toString(), setting.getGameRootDir());
        assertEquals("3.5.3-cn", setting.getGameInstalledVersion());
        assertEquals("china-launcher.exe", setting.getGameStarAppPath());

        manager.activate(GameEdition.GLOBAL);
        assertEquals(globalDir.toAbsolutePath().normalize().toString(), setting.getGameRootDir());
        assertEquals("3.5.3-global", setting.getGameInstalledVersion());
        assertEquals("global-launcher.exe", setting.getGameStarAppPath());

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(setting);
        assertTrue(json.contains("\"gameDir\""));
        assertTrue(json.contains("\"version\""));
        Setting restored = mapper.readValue(json, Setting.class);
        GameInstallationManager restoredManager = new GameInstallationManager(restored);
        assertNotNull(restoredManager.activeInstallation());
        assertEquals(GameEdition.GLOBAL, restoredManager.activeInstallation().getEdition());
        assertEquals("3.5.3-global", restoredManager.activeInstallation().getVersion());
        assertNotNull(restoredManager.installation(GameEdition.CHINA).orElse(null));
        assertNotNull(restoredManager.installation(GameEdition.GLOBAL).orElse(null));
    }
}
