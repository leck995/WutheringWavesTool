package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.RowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class GameSlashDataDao {
    private static final Logger LOG = LoggerFactory.getLogger(GameSlashDataDao.class);
    private static final Connection con = JdbcUtils.getConnection();

    private RowProcessor getRowProcessor() {
        Map<String, String> columnToPropertyOverrides = new HashMap<>();
        columnToPropertyOverrides.put("id", "id");
        columnToPropertyOverrides.put("data", "data");
        columnToPropertyOverrides.put("role_id", "roleId");
        columnToPropertyOverrides.put("end_time", "endTime"); // 假设数据库列名为end_time
        return new BasicRowProcessor(new BeanProcessor(columnToPropertyOverrides));
    }

    // 获取所有记录
    public List<SlashDataForDB> getAll() {
        QueryRunner qr = new QueryRunner();
        String sql = "SELECT * FROM game_slash";
        try {
            return qr.query(con, sql, new BeanListHandler<>(SlashDataForDB.class, getRowProcessor()));
        } catch (SQLException e) {
            LOG.error("获取所有记录失败", e);
            return Collections.emptyList();
        }
    }


    // 按ID查询
    public SlashDataForDB getById(int id) {
        QueryRunner qr = new QueryRunner();
        String sql = "SELECT * FROM game_slash WHERE id = ?";
        try {
            SlashDataForDB result = qr.query(con, sql,
                    new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), id);
            return result;
        } catch (SQLException e) {
            LOG.error("按ID查询失败, ID: {}", id, e);
            return null;
        }
    }

    // 按endTime查询
    public Optional<SlashDataForDB> getByEndTime(long endTime) {
        QueryRunner qr = new QueryRunner();
        String sql = "SELECT * FROM game_slash WHERE end_time = ?";
        try {
            SlashDataForDB slashDataForDB =  qr.query(con, sql,
                    new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), endTime);
            return Optional.ofNullable(slashDataForDB);
        } catch (SQLException e) {
            LOG.error("按endTime查询失败, endTime: {}", endTime, e);
            return Optional.empty();
        }
    }

    public Optional<SlashDataForDB> getByRoleIdAndEndTime(String roleId,long endTime) {
        QueryRunner qr = new QueryRunner();
        String sql = "SELECT * FROM game_slash WHERE end_time = ? AND role_id = ?";
        try {
            SlashDataForDB slashDataForDB =  qr.query(con, sql,
                    new BeanHandler<>(SlashDataForDB.class, getRowProcessor()), endTime,roleId);
            return Optional.ofNullable(slashDataForDB);
        } catch (SQLException e) {
            LOG.error("按endTime查询失败, endTime: {}", endTime, e);
            return Optional.empty();
        }
    }



    // 获取所有不重复的endTime列表（按倒序排列）
    public List<Long> getAllEndTimes() {
        QueryRunner qr = new QueryRunner();
        String sql = "SELECT DISTINCT end_time FROM game_slash ORDER BY end_time DESC";
        try {
            return qr.query(con, sql, new ColumnListHandler<>());
        } catch (SQLException e) {
            LOG.error("获取endTime列表失败", e);
            return Collections.emptyList();
        }
    }


    public List<Long> getEndTimesByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner();
        String sql = "SELECT DISTINCT end_time FROM game_slash where role_id = ? ORDER BY end_time DESC";
        try {
            return qr.query(con, sql, new ColumnListHandler<>(),roleId);
        } catch (SQLException e) {
            LOG.error("获取endTime列表失败", e);
            return Collections.emptyList();
        }
    }



    // 添加或更新记录（upsert操作）
    public int add(SlashDataForDB data) {
        String sql = "INSERT INTO game_slash(data, end_time,role_id) VALUES (?,?,?) " +
                "ON CONFLICT(end_time,role_id) DO UPDATE SET data = ?";
        QueryRunner qr = new QueryRunner();
        try {
            return qr.update(con, sql,
                    data.getData(),
                    data.getEndTime(),
                    data.getRoleId(),
                    data.getData());
        } catch (SQLException e) {
            LOG.error("保存记录失败, ID: {}", data.getId(), e);
            throw new RuntimeException("数据库操作失败", e);
        }
    }

    // 批量保存
    public int[] addList(List<SlashDataForDB> dataList) {
        String sql = "INSERT INTO game_slash(data, end_time,rold_id) VALUES (?,?,?,?) " +
                "ON CONFLICT(end_time,rold_id) DO UPDATE SET data = ?";
        QueryRunner qr = new QueryRunner();
        try {
            Object[][] params = dataList.stream()
                    .map(data -> new Object[]{
                            data.getData(),
                            data.getEndTime(),
                            data.getRoleId(),
                            data.getData()
                    })
                    .toArray(Object[][]::new);
            return qr.batch(con, sql, params);
        } catch (SQLException e) {
            LOG.error("批量保存记录失败", e);
            throw new RuntimeException("批量操作失败", e);
        }
    }

    // 更新数据内容
    public int updateData(int id, String newData) {
        String sql = "UPDATE game_slash SET data = ? WHERE id = ?";
        QueryRunner qr = new QueryRunner();
        try {
            return qr.update(con, sql, newData, id);
        } catch (SQLException e) {
            LOG.error("更新数据内容失败, ID: {}", id, e);
            throw new RuntimeException("更新失败", e);
        }
    }

    public int updateRoleId(int id,String roleId) {
        String sql = "UPDATE game_slash SET role_id = ? WHERE id = ?";
        QueryRunner qr = new QueryRunner();
        try {
            return qr.update(con, sql, roleId, id);
        } catch (SQLException e) {
            LOG.error("更新数据内容失败, ID: {}", id, e);
            throw new RuntimeException("更新失败", e);
        }
    }


    // 删除记录
    public int delete(int id) {
        String sql = "DELETE FROM game_slash WHERE id = ?";
        QueryRunner qr = new QueryRunner();
        try {
            return qr.update(con, sql, id);
        } catch (SQLException e) {
            LOG.error("删除记录失败, ID: {}", id, e);
            throw new RuntimeException("删除失败", e);
        }
    }

    // 按endTime删除记录
    public int deleteByEndTime(long endTime) {
        String sql = "DELETE FROM game_slash WHERE end_time = ?";
        QueryRunner qr = new QueryRunner();
        try {
            return qr.update(con, sql, endTime);
        } catch (SQLException e) {
            LOG.error("按endTime删除记录失败, endTime: {}", endTime, e);
            throw new RuntimeException("删除失败", e);
        }
    }
}