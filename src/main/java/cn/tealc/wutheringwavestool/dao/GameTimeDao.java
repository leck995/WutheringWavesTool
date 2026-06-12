package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class GameTimeDao {
    private static final Logger LOG = LoggerFactory.getLogger(GameTimeDao.class);
    private final DataSource dataSource;

    @Inject
    public GameTimeDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GameTimeDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    private RowProcessor getRowProcessor() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "id");
        map.put("role_id", "roleId");
        map.put("game_date", "gameDate");
        map.put("start_time", "startTime");
        map.put("end_time", "endTime");
        map.put("duration", "duration");
        return new BasicRowProcessor(new BeanProcessor(map));
    }

    public List<GameTime> getTimeListByData(String date) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_time WHERE game_date=?";
        try {
            return qr.query(sql, new BeanListHandler<>(GameTime.class, getRowProcessor()), date);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public List<GameTime> getTimeListByDataAndRoleId(String date, String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_time WHERE game_date=? AND role_id=?";
        try {
            return qr.query(sql, new BeanListHandler<>(GameTime.class, getRowProcessor()), date, roleId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public List<GameTime> getTimeListByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_time WHERE role_id=?";
        try {
            return qr.query(sql, new BeanListHandler<>(GameTime.class, getRowProcessor()), roleId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public boolean deleteTimeByData(String date) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "DELETE FROM game_time WHERE game_date = ?";
        try {
            qr.update(sql, date);
            LOG.info("成功删除指定时间的所有数据: {}", date);
            return true;
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteTimeByDataAndRoleId(String date, String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "DELETE FROM game_time WHERE game_date = ? AND role_id = ?";
        try {
            qr.update(sql, date, roleId);
            LOG.info("成功删除指定时间和用户的所有数据: {} 用户ID: {}", date, roleId);
            return true;
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public List<String> getAllRoleId() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT DISTINCT role_id FROM game_time WHERE role_id IS NOT NULL";
        try {
            return qr.query(sql, new ColumnListHandler<>("role_id"));
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public List<GameTime> getAllTime() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_time";
        try {
            return qr.query(sql, new BeanListHandler<>(GameTime.class, getRowProcessor()));
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public int addTime(GameTime gameTime) {
        String sql = "INSERT OR IGNORE INTO game_time (role_id,game_date,start_time,end_time,duration) VALUES (?,?,?,?,?)";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<>();
            return qr.insert(sql, rsh,
                    gameTime.getRoleId(), gameTime.getGameDate(), gameTime.getStartTime(), gameTime.getEndTime(), gameTime.getDuration());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
