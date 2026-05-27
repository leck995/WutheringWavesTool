package cn.tealc.wutheringwavestool.base;

import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;

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
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
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
        return setting;
    }
}
