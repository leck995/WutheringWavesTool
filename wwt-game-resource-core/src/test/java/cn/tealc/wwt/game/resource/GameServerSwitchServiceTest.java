package cn.tealc.wwt.game.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cn.tealc.wwt.game.resource.model.game.FileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServerSwitchServiceTest {
    private static final Path EXECUTABLE = Path.of("Client", "Binaries", "Win64", "Client-Win64-Shipping.exe");
    private static final Path SDK = Path.of("Client", "Binaries", "Win64", "ThirdParty", "KrPcSdk_Mainland");
    private static final Path GLOBAL_SDK = Path.of("Client", "Binaries", "Win64", "ThirdParty", "KrPcSdk_Global");
    private static final Path ANTI_CHEAT = Path.of("Client", "Binaries", "Win64", "AntiCheatExpert");

    @TempDir
    Path temporaryDirectory;

    @Test
    void downloadsOnlyChangedOrMissingFiles() throws Exception {
        Path game = temporaryDirectory.resolve("game");
        Path same = Path.of("Client", "Binaries", "Win64", "ThirdParty", "KrPcSdk_Mainland", "same.bin");
        Path changed = Path.of("Client", "Binaries", "Win64", "AntiCheatExpert", "changed.bin");
        Path missing = Path.of("Client", "Binaries", "Win64", "Client-Win64-Shipping.exe");
        write(game.resolve(same), "same-content");
        write(game.resolve(changed), "old-content");

        FileInfo sameFile = fileInfo(same, Files.size(game.resolve(same)), md5(game.resolve(same)));
        FileInfo changedFile = fileInfo(changed, Files.size(game.resolve(changed)), "00000000000000000000000000000000");
        FileInfo missingFile = fileInfo(missing, 12, "11111111111111111111111111111111");

        assertEquals(java.util.List.of(changed.toString().replace('\\', '/'), missing.toString().replace('\\', '/')),
                GameServerSwitchService.selectFilesToDownload(game, java.util.List.of(sameFile, changedFile, missingFile))
                        .stream().map(FileInfo::getDest).toList());
    }

    @Test
    void switchesBothDirectionsAndPreservesLauncherVersion() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GameServerSwitchService service = new GameServerSwitchService(null, mapper);
        Path game = temporaryDirectory.resolve("game");

        write(game.resolve(EXECUTABLE), "mainland-exe");
        write(game.resolve(SDK).resolve("KRSDKRes/KRSDKConfig.json"), "mainland-sdk");
        write(game.resolve(SDK).resolve("KRSDKRes/Common.dat"), "common-sdk-file");
        write(game.resolve(ANTI_CHEAT).resolve("AntiCheatExpert.dll"), "mainland-anti-cheat");
        Files.writeString(game.resolve("launcherDownloadConfig.json"),
                "{\"version\":\"3.5.3\",\"appId\":\"10003\",\"custom\":true}", StandardCharsets.UTF_8);

        Path bilibiliPayload = game.resolve("WwtBackup/server-switch/bilibili/payload");
        Path bilibiliExecutable = bilibiliPayload.resolve(EXECUTABLE);
        Path bilibiliConfig = bilibiliPayload.resolve(SDK).resolve("KRSDKRes/KRSDKConfig.json");
        Path bilibiliMarker = bilibiliPayload.resolve(SDK).resolve("KRSDKRes/Bilibili/PCGameSDK.dll");
        Path bilibiliAntiCheat = bilibiliPayload.resolve(ANTI_CHEAT).resolve("AntiCheatExpert.dll");
        Path bilibiliCommon = bilibiliPayload.resolve(SDK).resolve("KRSDKRes/Common.dat");
        write(bilibiliExecutable, "bilibili-exe");
        write(bilibiliConfig, "bilibili-sdk");
        write(bilibiliMarker, "bilibili-marker");
        write(bilibiliAntiCheat, "bilibili-anti-cheat");
        write(bilibiliCommon, "common-sdk-file");
        writeReadyCache(mapper, game, bilibiliPayload, bilibiliExecutable, bilibiliConfig, bilibiliMarker,
                bilibiliAntiCheat, bilibiliCommon);

        service.switchTo(game, GameDownloadSource.BILIBILI, ServerSwitchProgressListener.noop());

        assertEquals(GameDownloadSource.BILIBILI, service.detectActiveSource(game).orElseThrow());
        assertEquals("bilibili-exe", Files.readString(game.resolve(EXECUTABLE)));
        assertTrue(Files.isDirectory(game.resolve(SDK).resolve("KRSDKRes/Bilibili")));
        assertEquals("bilibili-anti-cheat", Files.readString(game.resolve(ANTI_CHEAT).resolve("AntiCheatExpert.dll")));
        assertEquals("common-sdk-file", Files.readString(
                game.resolve("WwtBackup/server-switch/cn/payload").resolve(SDK).resolve("KRSDKRes/Common.dat")));
        assertEquals("mainland-exe", Files.readString(game.resolve("WwtBackup/server-switch/cn/payload").resolve(EXECUTABLE)));
        assertEquals("10004", mapper.readTree(game.resolve("launcherDownloadConfig.json").toFile()).path("appId").asText());
        assertEquals("3.5.3", mapper.readTree(game.resolve("launcherDownloadConfig.json").toFile()).path("version").asText());
        assertTrue(service.inspect(game).mainlandCacheReady());
        assertFalse(service.inspect(game).bilibiliCacheReady());

        service.switchTo(game, GameDownloadSource.MAINLAND, ServerSwitchProgressListener.noop());

        assertEquals(GameDownloadSource.MAINLAND, service.detectActiveSource(game).orElseThrow());
        assertEquals("mainland-exe", Files.readString(game.resolve(EXECUTABLE)));
        assertFalse(Files.exists(game.resolve(SDK).resolve("KRSDKRes/Bilibili")));
        assertEquals("mainland-anti-cheat", Files.readString(game.resolve(ANTI_CHEAT).resolve("AntiCheatExpert.dll")));
        assertTrue(Files.isRegularFile(game.resolve(SDK).resolve("KRSDKRes/Common.dat")));
        assertEquals("bilibili-exe", Files.readString(game.resolve("WwtBackup/server-switch/bilibili/payload").resolve(EXECUTABLE)));
        assertEquals("10003", mapper.readTree(game.resolve("launcherDownloadConfig.json").toFile()).path("appId").asText());

        Files.delete(game.resolve("WwtBackup/server-switch/bilibili/payload").resolve(EXECUTABLE));
        assertFalse(service.inspect(game).bilibiliCacheReady());
    }

    @Test
    void detectsGlobalServerFromAppIdAndSdkDirectory() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GameServerSwitchService service = new GameServerSwitchService(null, mapper);
        Path game = temporaryDirectory.resolve("global-game");

        write(game.resolve(GLOBAL_SDK).resolve("KRSDK.dll"), "global-sdk");
        write(game.resolve("launcherDownloadConfig.json"), "{\"appId\":\"50004\"}");

        assertEquals(GameDownloadSource.GLOBAL, service.detectActiveSource(game).orElseThrow());
    }

    @Test
    void fallsBackToSdkDirectoryWhenLauncherConfigIsMissing() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GameServerSwitchService service = new GameServerSwitchService(null, mapper);
        Path game = temporaryDirectory.resolve("legacy-game");

        write(game.resolve(SDK).resolve("KRSDK.dll"), "mainland-sdk");

        assertEquals(GameDownloadSource.MAINLAND, service.detectActiveSource(game).orElseThrow());
    }

    @Test
    void rejectsConflictingLauncherConfigAndSdkDirectory() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GameServerSwitchService service = new GameServerSwitchService(null, mapper);
        Path game = temporaryDirectory.resolve("conflicting-game");

        write(game.resolve(GLOBAL_SDK).resolve("KRSDK.dll"), "global-sdk");
        write(game.resolve("launcherDownloadConfig.json"), "{\"appId\":\"10003\"}");

        assertTrue(service.detectActiveSource(game).isEmpty());
    }


    @Test
    void deletesOnlySelectedCacheAndKeepsActiveGameFiles() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GameServerSwitchService service = new GameServerSwitchService(null, mapper);
        Path game = temporaryDirectory.resolve("game");

        write(game.resolve(EXECUTABLE), "active-mainland");
        write(game.resolve(SDK).resolve("KRSDKRes/KRSDKConfig.json"), "active-sdk");
        write(game.resolve(ANTI_CHEAT).resolve("AntiCheatExpert.dll"), "active-anti-cheat");

        Path bilibiliPayload = game.resolve("WwtBackup/server-switch/bilibili/payload");
        Path bilibiliExecutable = bilibiliPayload.resolve(EXECUTABLE);
        Path bilibiliConfig = bilibiliPayload.resolve(SDK).resolve("KRSDKRes/KRSDKConfig.json");
        Path bilibiliAntiCheat = bilibiliPayload.resolve(ANTI_CHEAT).resolve("AntiCheatExpert.dll");
        write(bilibiliExecutable, "cached-bilibili");
        write(bilibiliConfig, "cached-sdk");
        write(bilibiliAntiCheat, "cached-anti-cheat");
        writeReadyCache(mapper, game, bilibiliPayload, bilibiliExecutable, bilibiliConfig, bilibiliAntiCheat);

        service.deleteCache(game, GameDownloadSource.BILIBILI, ServerSwitchProgressListener.noop());

        assertFalse(Files.exists(bilibiliPayload));
        assertEquals("active-mainland", Files.readString(game.resolve(EXECUTABLE)));
        assertEquals("active-sdk", Files.readString(game.resolve(SDK).resolve("KRSDKRes/KRSDKConfig.json")));
        assertFalse(service.inspect(game).bilibiliCacheReady());
    }

    private static FileInfo fileInfo(Path path, long size, String md5) {
        FileInfo file = new FileInfo();
        file.setDest(path.toString().replace('\\', '/'));
        file.setSize(size);
        file.setMd5(md5);
        return file;
    }

    private static void writeReadyCache(ObjectMapper mapper, Path game, Path payload, Path... files) throws Exception {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("schemaVersion", 1);
        ObjectNode caches = metadata.putObject("caches");
        ObjectNode entry = caches.putObject("bilibili");
        entry.put("ready", true);
        entry.put("source", "BILIBILI");
        entry.put("version", "3.5.3");
        entry.put("appId", "10004");
        entry.put("preparedAt", "2026-08-15T00:00:00Z");
        ArrayNode entries = entry.putArray("files");
        for (Path file : files) {
            ObjectNode item = entries.addObject();
            item.put("path", payload.relativize(file).toString().replace('\\', '/'));
            item.put("size", Files.size(file));
            item.put("md5", md5(file));
        }
        Path metadataPath = game.resolve("WwtBackup/server-switch/metadata.json");
        Files.createDirectories(metadataPath.getParent());
        mapper.writeValue(metadataPath.toFile(), metadata);
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String md5(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(path)));
    }
}
