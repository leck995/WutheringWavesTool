package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Launcher configuration loaded from KRApp.conf.
 * Corresponds to KRLauncherConfigInner (KRLauncherConfig.cs lines 13-266)
 */
public class LauncherConfig {
    @JsonProperty("gameId")
    public String gameId;

    @JsonProperty("appId")
    public String appId;

    @JsonProperty("appKey")
    public String appKey;

    @JsonProperty("pkgId")
    public String pkgId;

    @JsonProperty("gameIdentity")
    public String gameIdentity;

    @JsonProperty("gameDirName")
    public String gameDirName;

    @JsonProperty("gameExeName")
    public String gameExeName;

    @JsonProperty("gameName")
    public Map<String, String> gameName;

    @JsonProperty("configUrl")
    public String configUrl;

    @JsonProperty("backUpConfigUrl")
    public String backUpConfigUrl;

    @JsonProperty("gameLogPath")
    public String gameLogPath;

    @JsonProperty("gameScreenshotPath")
    public String gameScreenshotPath;

    @JsonProperty("defaultFingerPrints")
    public List<String> defaultFingerPrints;

    @JsonProperty("feappMD5")
    public String feappMD5;

    @JsonProperty("launcherConfigUrl")
    public String launcherConfigUrl;

    @JsonProperty("launcherBackUpConfigUrl")
    public String launcherBackUpConfigUrl;

    @JsonProperty("launcherSingleInstanceKey")
    public String launcherSingleInstanceKey;

    @JsonProperty("exitAfterGameStarted")
    public String exitAfterGameStarted;

    @JsonProperty("showStartupOption")
    public String showStartupOption;

    @JsonProperty("hideLauncherOptionVisible")
    public String hideLauncherOptionVisible;

    @JsonProperty("enableRoleComponent")
    public String enableRoleComponent;

    @JsonProperty("enableGameRestarting")
    public String enableGameRestarting;

    @JsonProperty("gameRestartingLockFilePath")
    public String gameRestartingLockFilePath;

    @JsonProperty("enableDetectRuntimeEnvironment")
    public String enableDetectRuntimeEnvironment;

    @JsonProperty("enableReportAdParameters")
    public String enableReportAdParameters;

    @JsonProperty("enableLauncherDeeplink")
    public String enableLauncherDeeplink;

    @JsonProperty("launcherDeeplinkProtocolName")
    public String launcherDeeplinkProtocolName;

    @JsonProperty("keyFileCheckList")
    public List<String> keyFileCheckList;

    @JsonProperty("launcherAutoStartRegName")
    public String launcherAutoStartRegName;

    @JsonProperty("uninstallRegName")
    public String uninstallRegName;

    @JsonProperty("supportLanguageArrays")
    public List<String> supportLanguageArrays;

    @JsonProperty("defaultLanguage")
    public String defaultLanguage;

    @JsonProperty("crashSightAppId")
    public String crashSightAppId;

    @JsonProperty("crashSightServerUrl")
    public String crashSightServerUrl;

    @JsonProperty("shushuAppId")
    public String shushuAppId;

    @JsonProperty("shushuServerUrl")
    public String shushuServerUrl;

    // Test environment overrides
    @JsonProperty("test_gameId")
    public String testGameId;

    @JsonProperty("test_appId")
    public String testAppId;

    @JsonProperty("test_appKey")
    public String testAppKey;

    @JsonProperty("test_configUrl")
    public String testConfigUrl;

    @JsonProperty("test_backUpConfigUrl")
    public String testBackUpConfigUrl;

    @JsonProperty("test_launcherConfigUrl")
    public String testLauncherConfigUrl;

    @JsonProperty("test_launcherBackUpConfigUrl")
    public String testLauncherBackUpConfigUrl;

    /**
     * Resolve effective value, preferring test override in test environment.
     * Corresponds to KRLauncherConfig.TestEnvHelper() (line 515-522)
     */
    public String resolve(boolean isTestEnv, String production, String testOverride) {
        if (isTestEnv && testOverride != null && !testOverride.isEmpty()) {
            return testOverride;
        }
        return production;
    }

    /**
     * Get effective configUrl (test override if in test env).
     */
    public String getEffectiveConfigUrl(boolean isTestEnv) {
        return resolve(isTestEnv, configUrl, testConfigUrl);
    }

    /**
     * Get effective backUpConfigUrl.
     */
    public String getEffectiveBackUpConfigUrl(boolean isTestEnv) {
        return resolve(isTestEnv, backUpConfigUrl, testBackUpConfigUrl);
    }

    /**
     * Get effective launcherConfigUrl.
     */
    public String getEffectiveLauncherConfigUrl(boolean isTestEnv) {
        return resolve(isTestEnv, launcherConfigUrl, testLauncherConfigUrl);
    }

    /**
     * Get effective launcherBackUpConfigUrl.
     */
    public String getEffectiveLauncherBackUpConfigUrl(boolean isTestEnv) {
        return resolve(isTestEnv, launcherBackUpConfigUrl, testLauncherBackUpConfigUrl);
    }
}
