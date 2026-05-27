package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;

@Singleton
public class ConfigDao {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigDao.class);
    private final DataSource dataSource;

    @Inject
    public ConfigDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ConfigDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    public String getValue(String key) {
        String sql = "SELECT value FROM config WHERE key = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.query(sql, new ScalarHandler<>(), key);
        } catch (SQLException e) {
            LOG.error("获取配置值 {} 失败", key, e);
            return null;
        }
    }

    public void saveOrUpdate(String key, String value) {
        String sql = "INSERT INTO config (key, value) VALUES (?, ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            qr.update(sql, key, value);
        } catch (SQLException e) {
            LOG.error("保存配置 {} 失败", key, e);
        }
    }

    public int delete(String key) {
        String sql = "DELETE FROM config WHERE key = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, key);
        } catch (SQLException e) {
            LOG.error("删除配置 {} 失败", key, e);
            return 0;
        }
    }
}
