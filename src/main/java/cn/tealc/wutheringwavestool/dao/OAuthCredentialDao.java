package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.OAuthCredential;
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
public class OAuthCredentialDao {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthCredentialDao.class);
    private final DataSource dataSource;

    private static final String COLUMNS = "id, role_id AS roleId, oauth_code AS oauthCode, update_time AS updateTime";

    @Inject
    public OAuthCredentialDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public OAuthCredentialDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    public OAuthCredential getByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT " + COLUMNS + " FROM oauth_credential WHERE role_id = ?";
        try {
            return qr.query(sql, new BeanHandler<>(OAuthCredential.class), roleId);
        } catch (SQLException e) {
            LOG.error("查询 OAuth 凭证失败, roleId: {}", roleId, e);
            return null;
        }
    }

    public OAuthCredential getByOauthCode(String oauthCode) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT " + COLUMNS + " FROM oauth_credential WHERE oauth_code = ?";
        try {
            return qr.query(sql, new BeanHandler<>(OAuthCredential.class), oauthCode);
        } catch (SQLException e) {
            LOG.error("查询 OAuth 凭证失败, oauthCode: {}", oauthCode, e);
            return null;
        }
    }

    public String getRoleIdByOauthCode(String oauthCode) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT role_id FROM oauth_credential WHERE oauth_code = ?";
        try {
            ResultSetHandler<String> rsh = new ScalarHandler<>();
            return qr.query(sql, rsh, oauthCode);
        } catch (SQLException e) {
            LOG.error("通过 oauthCode 查询 roleId 失败, oauthCode: {}", oauthCode, e);
            return null;
        }
    }

    public List<OAuthCredential> getAll() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT " + COLUMNS + " FROM oauth_credential";
        try {
            return qr.query(sql, new BeanListHandler<>(OAuthCredential.class));
        } catch (SQLException e) {
            LOG.error("查询所有 OAuth 凭证失败", e);
            return List.of();
        }
    }

    public int saveOrUpdate(String roleId, String oauthCode, long updateTime) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "INSERT INTO oauth_credential (role_id, oauth_code, update_time) VALUES (?, ?, ?)"
                + " ON CONFLICT(oauth_code) DO UPDATE SET role_id = excluded.role_id, update_time = excluded.update_time";
        try {
            int rows = qr.update(sql, roleId, oauthCode, updateTime);
            LOG.info("保存 OAuth 凭证成功, roleId: {}, oauthCode: {}, rows: {}", roleId, oauthCode, rows);
            return rows;
        } catch (SQLException e) {
            LOG.error("保存 OAuth 凭证失败, roleId: {}", roleId, e);
            return 0;
        }
    }

    public int updateOauthCode(String oldOauthCode, String newOauthCode, long updateTime) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "UPDATE oauth_credential SET oauth_code = ?, update_time = ? WHERE oauth_code = ?";
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
        String sql = "DELETE FROM oauth_credential WHERE role_id = ?";
        try {
            int rows = qr.update(sql, roleId);
            LOG.info("删除 OAuth 凭证, roleId: {}, rows: {}", roleId, rows);
            return rows;
        } catch (SQLException e) {
            LOG.error("删除 OAuth 凭证失败, roleId: {}", roleId, e);
            return 0;
        }
    }

    public int deleteByOauthCode(String oauthCode) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "DELETE FROM oauth_credential WHERE oauth_code = ?";
        try {
            int rows = qr.update(sql, oauthCode);
            LOG.info("删除 OAuth 凭证, oauthCode: {}, rows: {}", oauthCode, rows);
            return rows;
        } catch (SQLException e) {
            LOG.error("删除 OAuth 凭证失败, oauthCode: {}", oauthCode, e);
            return 0;
        }
    }

    public int count() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT COUNT(*) FROM oauth_credential";
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<>();
            return qr.query(sql, rsh);
        } catch (SQLException e) {
            LOG.error("统计 OAuth 凭证数量失败", e);
            return 0;
        }
    }
}
