package cn.tealc.wutheringwavestool.base;

import cn.tealc.wwt.game.resource.GameResourceDownloadService;
import cn.tealc.wwt.game.resource.GameResourceInstallService;
import cn.tealc.wwt.game.resource.GameServerSwitchService;
import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kuro.kujiequ.KujiequApiContext;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.launcher.LauncherManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class DataSourceModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(DataSourceModule.class);

    @Provides
    @Singleton
    DataSource provideDataSource() {
        JdbcUtils.init();
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:sqlite.db?date_string_format=yyyy-MM-dd");
        return ds;
    }

    @Provides
    @Singleton
    ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

    @Provides
    @Singleton
    HttpClient provideHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            }, new SecureRandom());
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
            return HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            LOG.error("创建 SSL 上下文失败，回退到默认配置", e);
            return HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
    }

    @Provides
    @Singleton
    Setting provideSetting(ObjectMapper mapper) {
        Setting setting = null;
        File settingFile = new File("settings.json");
        if (settingFile.exists()) {
            try {
                setting = mapper.readValue(settingFile, Setting.class);
                // 迁移旧版启动参数
                if (setting.getAppParams() != null) {
                    setting.getStartUpParams().addAll(setting.getAppParams().split(" "));
                    setting.setAppParams(null);
                }
            } catch (IOException e) {
                LOG.error("读取 settings.json 失败，使用默认设置", e);
            }
        }
        if (setting == null) {
            setting = new Setting();
        }
        setting.getGame().ensureGameInstallations();
        setting.getGame().migrateLegacyStartUpParams(setting.consumeLegacyStartUpParams());
        return setting;
    }

    @Provides
    @Singleton
    KujiequApiContext provideKujiequApiContext(HttpClient httpClient, ObjectMapper objectMapper) {
        return new KujiequApiContext(httpClient, objectMapper,
                (userInfo, msg) -> NotificationManager.publish(NotificationKey.TOKEN_EXPIRED, userInfo, msg));
    }

    @Provides
    @Singleton
    KujiequManager provideKujiequManager(KujiequApiContext ctx) {
        return new KujiequManager(ctx);
    }

    @Provides
    @Singleton
    LauncherManager provideLauncherManager(HttpClient httpClient, ObjectMapper objectMapper) {
        return new LauncherManager(httpClient, objectMapper);
    }

    @Provides
    @Singleton
    GameResourceDownloadService provideGameResourceDownloadService(
            HttpClient httpClient, ObjectMapper objectMapper) {
        return new GameResourceDownloadService(httpClient, objectMapper);
    }

    @Provides
    @Singleton
    GameResourceInstallService provideGameResourceInstallService(
            GameResourceDownloadService downloadService, ObjectMapper objectMapper) {
        return new GameResourceInstallService(downloadService, objectMapper);
    }

    @Provides
    @Singleton
    GameServerSwitchService provideGameServerSwitchService(ObjectMapper objectMapper) {
        return new GameServerSwitchService(objectMapper);
    }
}
