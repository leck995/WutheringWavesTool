package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.internal.legacy.model.LauncherConfig;

import java.util.Map;

/**
 * 各发行渠道的启动器引导配置（KRApp.conf 解码后的关键字段）静态表。
 *
 * <p>旧引擎 {@link GameResourceUpdateService} 原本需要从磁盘提取并解码 KRApp.conf 才能得到
 * {@link LauncherConfig}；但资源更新流程只依赖其中的 {@code configUrl}/{@code backUpConfigUrl}/
 * {@code appId}/{@code appKey}/{@code gameId}/{@code gameExeName} 六个字段，且这些字段稳定、与
 * 发行渠道一一对应。这里按 {@link GameDownloadSource} 直接提供，避免在多服场景下维护多份
 * KRApp.conf 文件。</p>
 */
public final class LauncherConfigs {

    private static final String CN_APP_ID = "10003";
    private static final String BILIBILI_APP_ID = "10004";
    private static final String GLOBAL_APP_ID = "50004";

    private static final String EXE_NAME = "Wuthering Waves.exe";

    private static final String CN_HOST = "prod-cn-alicdn-gamestarter.kurogame.com";
    private static final String CN_BACKUP_HOST = "prod-volcdn-gamestarter.kurogame.xyz";
    private static final String GLOBAL_HOST = "prod-alicdn-gamestarter.kurogame.com";
    private static final String GLOBAL_BACKUP_HOST = "prod-volcdn-gamestarter.kurogame.net";

    private LauncherConfigs() {
    }

    /** 返回指定发行渠道的启动器配置，字段值恒非 null。 */
    public static LauncherConfig forSource(GameDownloadSource source) {
        LauncherConfig config = new LauncherConfig();
        config.gameExeName = EXE_NAME;
        switch (source) {
            case BILIBILI -> {
                config.gameId = "G152";
                config.appId = BILIBILI_APP_ID;
                config.appKey = "j5GWFuUFlb8N31Wi2uS3ZAVHcb7ZGN7y";
                config.configUrl = indexUrl(CN_HOST, config.gameId, config.appId, config.appKey);
                config.backUpConfigUrl = indexUrl(CN_BACKUP_HOST, config.gameId, config.appId, config.appKey);
            }
            case GLOBAL -> {
                config.gameId = "G153";
                config.appId = GLOBAL_APP_ID;
                config.appKey = "obOHXFrFanqsaIEOmuKroCcbZkQRBC7c";
                config.configUrl = indexUrl(GLOBAL_HOST, config.gameId, config.appId, config.appKey);
                config.backUpConfigUrl = indexUrl(GLOBAL_BACKUP_HOST, config.gameId, config.appId, config.appKey);
            }
            default -> {
                config.gameId = "G152";
                config.appId = CN_APP_ID;
                config.appKey = "Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5";
                config.configUrl = indexUrl(CN_HOST, config.gameId, config.appId, config.appKey);
                config.backUpConfigUrl = indexUrl(CN_BACKUP_HOST, config.gameId, config.appId, config.appKey);
            }
        }
        config.gameName = Map.of("zh-Hans", "鸣潮");
        return config;
    }

    private static String indexUrl(String host, String gameId, String appId, String appKey) {
        return "https://" + host + "/launcher/game/" + gameId + "/" + appId + "_" + appKey + "/index.json";
    }
}