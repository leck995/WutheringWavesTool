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
public class GameSlashDataDao {
    private static final Logger LOG = LoggerFactory.getLogger(GameSlashDataDao.class);
    private final DataSource dataSource;

    @Inject
    public GameSlashDataDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GameSlashDataDao() {
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
        String sql = "SELECT * FROM game_slash";
        try {
            return qr.query(sql, new BeanListHandler<>(SlashDataForDB.class, getRowProcessor()));
        } catch (SQLException e) {
            LOG.error("获取所有记录失败", e);
            return Collections.emptyList();
        }
    }

    public SlashDataForDB getById(int id) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_slash WHERE id = ?";
        try {
            return qr.query(sql, new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), id);
        } catch (SQLException e) {
            LOG.error("按ID查询失败, ID: {}", id, e);
            return null;
        }
    }

    public Optional<SlashDataForDB> getByEndTime(long endTime) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_slash WHERE end_time = ?";
        try {
            return Optional.ofNullable(qr.query(sql,
                    new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), endTime));
        } catch (SQLException e) {
            LOG.error("按endTime查询失败, endTime: {}", endTime, e);
            return Optional.empty();
        }
    }

    public Optional<SlashDataForDB> getByRoleIdAndEndTime(String roleId, long endTime) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_slash WHERE end_time = ? AND role_id = ?";
        try {
            return Optional.ofNullable(qr.query(sql,
                    new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), endTime, roleId));
        } catch (SQLException e) {
            LOG.error("按endTime查询失败, endTime: {}", endTime, e);
            return Optional.empty();
        }
    }

    public List<Long> getAllEndTimes() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT DISTINCT end_time FROM game_slash ORDER BY end_time DESC";
        try {
            return qr.query(sql, new ColumnListHandler<>());
        } catch (SQLException e) {
            LOG.error("获取endTime列表失败", e);
            return Collections.emptyList();
        }
    }

    public List<Long> getEndTimesByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT DISTINCT end_time FROM game_slash where role_id = ? ORDER BY end_time DESC";
        try {
            return qr.query(sql, new ColumnListHandler<>(), roleId);
        } catch (SQLException e) {
            LOG.error("获取endTime列表失败", e);
            return Collections.emptyList();
        }
    }

    public int add(SlashDataForDB data) {
        String sql = "INSERT INTO game_slash(data, end_time,role_id) VALUES (?,?,?) " +
                "ON CONFLICT(end_time,role_id) DO UPDATE SET data = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql,
                    data.getData(),
                    data.getEndTime(),
                    data.getRoleId(),
                    data.getData());
        } catch (SQLException e) {
            LOG.error("保存记录失败, ID: {}", data.getId(), e);
            throw new RuntimeException("数据库操作失败", e);
        }
    }

    public int[] addList(List<SlashDataForDB> dataList) {
        String sql = "INSERT INTO game_slash(data, end_time,rold_id) VALUES (?,?,?,?) " +
                "ON CONFLICT(end_time,rold_id) DO UPDATE SET data = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            Object[][] params = dataList.stream()
                    .map(data -> new Object[]{
                            data.getData(),
                            data.getEndTime(),
                            data.getRoleId(),
                            data.getData()
                    })
                    .toArray(Object[][]::new);
            return qr.batch(sql, params);
        } catch (SQLException e) {
            LOG.error("批量保存记录失败", e);
            throw new RuntimeException("批量操作失败", e);
        }
    }

    public int updateData(int id, String newData) {
        String sql = "UPDATE game_slash SET data = ? WHERE id = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, newData, id);
        } catch (SQLException e) {
            LOG.error("更新数据内容失败, ID: {}", id, e);
            throw new RuntimeException("更新失败", e);
        }
    }

    public int updateRoleId(int id, String roleId) {
        String sql = "UPDATE game_slash SET role_id = ? WHERE id = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, roleId, id);
        } catch (SQLException e) {
            LOG.error("更新数据内容失败, ID: {}", id, e);
            throw new RuntimeException("更新失败", e);
        }
    }

    public int delete(int id) {
        String sql = "DELETE FROM game_slash WHERE id = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, id);
        } catch (SQLException e) {
            LOG.error("删除记录失败, ID: {}", id, e);
            throw new RuntimeException("删除失败", e);
        }
    }

    public int deleteByEndTime(long endTime) {
        String sql = "DELETE FROM game_slash WHERE end_time = ?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, endTime);
        } catch (SQLException e) {
            LOG.error("按endTime删除记录失败, endTime: {}", endTime, e);
            throw new RuntimeException("删除失败", e);
        }
    }
}
