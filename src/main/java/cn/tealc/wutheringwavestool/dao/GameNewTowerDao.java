package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.RowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

@Singleton
public class GameNewTowerDao {
    private static final Logger LOG = LoggerFactory.getLogger(GameNewTowerDao.class);
    private final DataSource dataSource;

    @Inject
    public GameNewTowerDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GameNewTowerDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    private RowProcessor getRowProcessor() {
        Map<String, String> columnToPropertyOverrides = new HashMap<>();
        columnToPropertyOverrides.put("id", "id");
        columnToPropertyOverrides.put("data", "data");
        columnToPropertyOverrides.put("role_id", "roleId");
        columnToPropertyOverrides.put("end_time", "endTime");
        return new BasicRowProcessor(new BeanProcessor(columnToPropertyOverrides));
    }

    public List<SlashDataForDB> getAll() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_new_tower";
        try {
            return qr.query(sql, new BeanListHandler<>(SlashDataForDB.class, getRowProcessor()));
        } catch (SQLException e) {
            LOG.error("获取所有新深塔记录失败", e);
            return Collections.emptyList();
        }
    }

    public SlashDataForDB getById(int id) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_new_tower WHERE id = ?";
        try {
            return qr.query(sql, new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), id);
        } catch (SQLException e) {
            LOG.error("按ID查询新深塔失败, ID: {}", id, e);
            return null;
        }
    }

    public Optional<SlashDataForDB> getByEndTime(long endTime) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_new_tower WHERE end_time = ?";
        try {
            return Optional.ofNullable(qr.query(sql,
                    new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), endTime));
        } catch (SQLException e) {
            LOG.error("按endTime查询新深塔失败, endTime: {}", endTime, e);
            return Optional.empty();
        }
    }

    public Optional<SlashDataForDB> getByRoleIdAndEndTime(String roleId, long endTime) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_new_tower WHERE end_time = ? AND role_id = ?";
        try {
            return Optional.ofNullable(qr.query(sql,
                    new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), endTime, roleId));
        } catch (SQLException e) {
            LOG.error("按endTime和roleId查询新深塔失败, endTime: {}, roleId: {}", endTime, roleId, e);
            return Optional.empty();
        }
    }

    public List<Long> getAllEndTimes() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT DISTINCT end_time FROM game_new_tower ORDER BY end_time DESC";
        try {
            return qr.query(sql, new ColumnListHandler<>());
        } catch (SQLException e) {
            LOG.error("获取新深塔endTime列表失败", e);
            return Collections.emptyList();
        }
    }

    public List<Long> getEndTimesByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT DISTINCT end_time FROM game_new_tower WHERE role_id = ? ORDER BY end_time DESC";
        try {
            return qr.query(sql, new ColumnListHandler<>(), roleId);
        } catch (SQLException e) {
            LOG.error("获取新深塔endTime列表失败", e);
            return Collections.emptyList();
        }
    }

    public int add(SlashDataForDB data) {
        String sql = "INSERT INTO game_new_tower(data, end_time, role_id) VALUES (?,?,?) " +
                "ON CONFLICT(end_time, role_id) DO UPDATE SET data = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql,
                    data.getData(),
                    data.getEndTime(),
                    data.getRoleId(),
                    data.getData());
        } catch (SQLException e) {
            LOG.error("保存新深塔记录失败, ID: {}", data.getId(), e);
            throw new RuntimeException("数据库操作失败", e);
        }
    }

    public int updateData(int id, String newData) {
        String sql = "UPDATE game_new_tower SET data = ? WHERE id = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, newData, id);
        } catch (SQLException e) {
            LOG.error("更新新深塔数据失败, ID: {}", id, e);
            throw new RuntimeException("更新失败", e);
        }
    }

    public int delete(int id) {
        String sql = "DELETE FROM game_new_tower WHERE id = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, id);
        } catch (SQLException e) {
            LOG.error("删除新深塔记录失败, ID: {}", id, e);
            throw new RuntimeException("删除失败", e);
        }
    }

    public int deleteByEndTime(long endTime) {
        String sql = "DELETE FROM game_new_tower WHERE end_time = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, endTime);
        } catch (SQLException e) {
            LOG.error("按endTime删除新深塔记录失败, endTime: {}", endTime, e);
            throw new RuntimeException("删除失败", e);
        }
    }
}
