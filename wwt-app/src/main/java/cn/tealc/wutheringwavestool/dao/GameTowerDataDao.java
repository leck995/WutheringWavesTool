package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

@Singleton
public class GameTowerDataDao {
    private static final Logger LOG = LoggerFactory.getLogger(GameTowerDataDao.class);
    private final DataSource dataSource;

    @Inject
    public GameTowerDataDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GameTowerDataDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    private RowProcessor getRowProcessor() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "id");
        map.put("floor", "floor");
        map.put("pic_url", "picUrl");
        map.put("role_list", "roleList");
        map.put("role_id", "roleId");
        map.put("star", "star");
        map.put("area_id", "areaId");
        map.put("area_name", "areaName");
        map.put("difficulty", "difficulty");
        map.put("difficulty_name", "difficultyName");
        map.put("endTime", "endTime");
        return new BasicRowProcessor(new BeanProcessor(map));
    }

    public List<TowerData> getAll() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_tower";
        try {
            return qr.query(sql, new BeanListHandler<>(TowerData.class, getRowProcessor()));
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public Set<TowerData> getListByRoleIdAndEndTime(String roleId, long endTime) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_tower where endTime = ? and role_id = ?";
        try {
            List<TowerData> query = qr.query(sql, new BeanListHandler<>(TowerData.class, getRowProcessor()), endTime, roleId);
            return new HashSet<>(query);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public List<Long> getEndTimeList() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT endTime FROM game_tower";
        try {
            List<Long> query = qr.query(sql, new ColumnListHandler<>());
            HashSet<Long> longs = new HashSet<>(query);
            return longs.stream().sorted(Comparator.reverseOrder()).toList();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public List<Long> getEndTimeListByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT endTime FROM game_tower where role_id = ?";
        try {
            List<Long> query = qr.query(sql, new ColumnListHandler<>(), roleId);
            HashSet<Long> longs = new HashSet<>(query);
            return longs.stream().sorted(Comparator.reverseOrder()).toList();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public int add(TowerData tower) {
        String sql = """
                INSERT INTO game_tower(role_id,floor,pic_url,role_list,star,area_id,area_name,difficulty,difficulty_name,endTime)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(role_id, area_id, floor, endTime)
                DO UPDATE SET star = ?,role_list = ?""";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<>();
            return qr.insert(sql,
                    rsh, tower.getRoleId(), tower.getFloor(), tower.getPicUrl(), tower.getRoleList(), tower.getStar(), tower.getAreaId(), tower.getAreaName(), tower.getDifficulty(), tower.getDifficultyName(), tower.getEndTime(),
                    tower.getStar(), tower.getRoleList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update(TowerData tower) {
        String sql = """
        UPDATE game_tower
        SET role_id = ?,
            floor = ?,
            pic_url = ?,
            role_list = ?,
            star = ?,
            area_name = ?,
            difficulty = ?,
            difficulty_name = ?,
            endTime = ?
        WHERE id =?
        """;
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql,
                    tower.getRoleId(),
                    tower.getFloor(),
                    tower.getPicUrl(),
                    tower.getRoleList(),
                    tower.getStar(),
                    tower.getAreaName(),
                    tower.getDifficulty(),
                    tower.getDifficultyName(),
                    tower.getEndTime(),
                    tower.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tower data", e);
        }
    }
}
