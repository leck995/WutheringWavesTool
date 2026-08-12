package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.LocalCachePlayerData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Singleton
public class LocalCachePlayerDataDao {
    private static final Logger LOG = LoggerFactory.getLogger(LocalCachePlayerDataDao.class);
    private final DataSource dataSource;

    private static final String TABLE = "local_cache_player_data";
    private static final String COLUMNS = "id, role_id AS roleId, role_name AS roleName, level,"
            + " sex, head_photo AS headPhoto, region, oauth_code AS oauthCode, cuid, update_time AS updateTime";

    @Inject
    public LocalCachePlayerDataDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public LocalCachePlayerDataDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    public LocalCachePlayerData getByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE role_id = ?";
        try {
            return qr.query(sql, new BeanHandler<>(LocalCachePlayerData.class), roleId);
        } catch (SQLException e) {
            LOG.error("查询玩家缓存数据失败, roleId: {}", roleId, e);
            return null;
        }
    }

    public LocalCachePlayerData getByOauthCode(String oauthCode) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE oauth_code = ?";
        try {
            return qr.query(sql, new BeanHandler<>(LocalCachePlayerData.class), oauthCode);
        } catch (SQLException e) {
            LOG.error("查询玩家缓存数据失败, oauthCode: {}", oauthCode, e);
            return null;
        }
    }

    public LocalCachePlayerData getByCuid(String cuid) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE cuid = ?";
        try {
            return qr.query(sql, new BeanHandler<>(LocalCachePlayerData.class), cuid);
        } catch (SQLException e) {
            LOG.error("查询玩家缓存数据失败, oauthCode: {}", cuid, e);
            return null;
        }
    }

    public String getRoleIdByOauthCode(String oauthCode) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT role_id FROM " + TABLE + " WHERE oauth_code = ?";
        try {
            ResultSetHandler<String> rsh = new ScalarHandler<>();
            return qr.query(sql, rsh, oauthCode);
        } catch (SQLException e) {
            LOG.error("通过 oauthCode 查询 roleId 失败, oauthCode: {}", oauthCode, e);
            return null;
        }
    }

    public List<LocalCachePlayerData> getAll() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE;
        try {
            return qr.query(sql, new BeanListHandler<>(LocalCachePlayerData.class));
        } catch (SQLException e) {
            LOG.error("查询所有玩家缓存数据失败", e);
            return List.of();
        }
    }

    public int saveOrUpdate(LocalCachePlayerData data) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "INSERT INTO " + TABLE
                + " (role_id, oauth_code, role_name, level, sex, head_photo, region, cuid, update_time)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                + " ON CONFLICT(oauth_code) DO UPDATE SET"
                + " role_id = excluded.role_id,"
                + " role_name = excluded.role_name,"
                + " level = excluded.level,"
                + " sex = excluded.sex,"
                + " head_photo = excluded.head_photo,"
                + " region = excluded.region,"
                + " cuid = excluded.cuid,"
                + " update_time = excluded.update_time";
        try {
            int rows = qr.update(sql,
                    data.getRoleId(), data.getOauthCode(), data.getRoleName(),
                    data.getLevel(), data.getSex(), data.getHeadPhoto(),
                    data.getRegion(), data.getCuid(), data.getUpdateTime());
            LOG.info("保存玩家缓存数据成功, roleId: {}, roleName: {}, rows: {}",
                    data.getRoleId(), data.getRoleName(), rows);
            return rows;
        } catch (SQLException e) {
            LOG.error("保存玩家缓存数据失败, roleId: {}", data.getRoleId(), e);
            return 0;
        }
    }

    public int updateOauthCode(String oldOauthCode, String newOauthCode, long updateTime) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "UPDATE " + TABLE + " SET oauth_code = ?, update_time = ? WHERE oauth_code = ?";
        try {
            int rows = qr.update(sql, newOauthCode, updateTime, oldOauthCode);
            LOG.info("更新 oauthCode 成功, old: {}, new: {}, rows: {}", oldOauthCode, newOauthCode, rows);
            return rows;
        } catch (SQLException e) {
            LOG.error("更新 oauthCode 失败, old: {}, new: {}", oldOauthCode, newOauthCode, e);
            return 0;
        }
    }

    public int deleteByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "DELETE FROM " + TABLE + " WHERE role_id = ?";
        try {
            int rows = qr.update(sql, roleId);
            LOG.info("删除玩家缓存数据, roleId: {}, rows: {}", roleId, rows);
            return rows;
        } catch (SQLException e) {
            LOG.error("删除玩家缓存数据失败, roleId: {}", roleId, e);
            return 0;
        }
    }

    public int deleteByOauthCode(String oauthCode) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "DELETE FROM " + TABLE + " WHERE oauth_code = ?";
        try {
            int rows = qr.update(sql, oauthCode);
            LOG.info("删除玩家缓存数据, oauthCode: {}, rows: {}", oauthCode, rows);
            return rows;
        } catch (SQLException e) {
            LOG.error("删除玩家缓存数据失败, oauthCode: {}", oauthCode, e);
            return 0;
        }
    }

    public int count() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT COUNT(*) FROM " + TABLE;
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<>();
            return qr.query(sql, rsh);
        } catch (SQLException e) {
            LOG.error("统计玩家缓存数据数量失败", e);
            return 0;
        }
    }
}
