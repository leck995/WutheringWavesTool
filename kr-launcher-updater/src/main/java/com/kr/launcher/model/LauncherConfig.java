package com.kr.launcher.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Launcher configuration loaded from KRApp.conf.
 * Corresponds to KRLauncherConfigInner (KRLauncherConfig.cs lines 13-266)
 */
public class LauncherConfig {
    @SerializedName("gameId")
    public String gameId;

    @SerializedName("appId")
    public String appId;

    @SerializedName("appKey")
    public String appKey;

    @SerializedName("pkgId")
    public String pkgId;

    @SerializedName("gameIdentity")
    public String gameIdentity;

    @SerializedName("gameDirName")
    public String gameDirName;

    @SerializedName("gameExeName")
    public String gameExeName;

    @SerializedName("gameName")
    public Map<String, String> gameName;

    @SerializedName("configUrl")
    public String configUrl;

    @SerializedName("backUpConfigUrl")
    public String backUpConfigUrl;

    @SerializedName("gameLogPath")
    public String gameLogPath;

    @SerializedName("gameScreenshotPath")
    public String gameScreenshotPath;

    @SerializedName("defaultFingerPrints")
    public List<String> defaultFingerPrints;

    @SerializedName("feappMD5")
    public String feappMD5;

    @SerializedName("launcherConfigUrl")
    public String launcherConfigUrl;

    @SerializedName("launcherBackUpConfigUrl")
    public String launcherBackUpConfigUrl;

    @SerializedName("launcherSingleInstanceKey")
    public String launcherSingleInstanceKey;

    @SerializedName("exitAfterGameStarted")
    public String exitAfterGameStarted;

    @SerializedName("showStartupOption")
    public String showStartupOption;

    @SerializedName("hideLauncherOptionVisible")
    public String hideLauncherOptionVisible;

    @SerializedName("enableRoleComponent")
    public String enableRoleComponent;

    @SerializedName("enableGameRestarting")
    public String enableGameRestarting;

    @SerializedName("gameRestartingLockFilePath")
    public String gameRestartingLockFilePath;

    @SerializedName("enableDetectRuntimeEnvironment")
    public String enableDetectRuntimeEnvironment;

    @SerializedName("enableReportAdParameters")
    public String enableReportAdParameters;

    @SerializedName("enableLauncherDeeplink")
    public String enableLauncherDeeplink;

    @SerializedName("launcherDeeplinkProtocolName")
    public String launcherDeeplinkProtocolName;

    @SerializedName("keyFileCheckList")
    public List<String> keyFileCheckList;

    @SerializedName("launcherAutoStartRegName")
    public String launcherAutoStartRegName;

    @SerializedName("uninstallRegName")
    public String uninstallRegName;

    @SerializedName("supportLanguageArrays")
    public List<String> supportLanguageArrays;

    @SerializedName("defaultLanguage")
    public String defaultLanguage;

    @SerializedName("crashSightAppId")
    public String crashSightAppId;

    @SerializedName("crashSightServerUrl")
    public String crashSightServerUrl;

    @SerializedName("shushuAppId")
    public String shushuAppId;

    @SerializedName("shushuServerUrl")
    public String shushuServerUrl;

    // Test environment overrides
    @SerializedName("test_gameId")
    public String testGameId;

    @SerializedName("test_appId")
    public String testAppId;

    @SerializedName("test_appKey")
    public String testAppKey;

    @SerializedName("test_configUrl")
    public String testConfigUrl;

    @SerializedName("test_backUpConfigUrl")
    public String testBackUpConfigUrl;

    @SerializedName("test_launcherConfigUrl")
    public String testLauncherConfigUrl;

    @SerializedName("test_launcherBackUpConfigUrl")
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
