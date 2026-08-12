package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.game.pool.GameGacha;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class GameGachaDao {
    private static final Logger LOG = LoggerFactory.getLogger(GameGachaDao.class);
    private final DataSource dataSource;

    @Inject
    public GameGachaDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GameGachaDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    private RowProcessor getRowProcessor() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "id");
        map.put("player_id", "playerId");
        map.put("gacha_name", "gachaName");
        map.put("card_pool_type", "cardPoolType");
        map.put("resource_id", "resourceId");
        map.put("quality_level", "qualityLevel");
        map.put("resource_type", "resourceType");
        map.put("name", "name");
        map.put("count", "count");
        map.put("time", "time");
        return new BasicRowProcessor(new BeanProcessor(map));
    }

    public int add(GameGacha gacha) {
        String sql = """
                INSERT OR IGNORE INTO game_gacha(player_id, gacha_name, card_pool_type, resource_id, quality_level, resource_type, name, count, time)
                VALUES (?,?,?,?,?,?,?,?,?)""";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<>();
            return qr.insert(sql, rsh,
                    gacha.getPlayerId(), gacha.getGachaName(), gacha.getCardPoolType(),
                    gacha.getResourceId(), gacha.getQualityLevel(), gacha.getResourceType(),
                    gacha.getName(), gacha.getCount(), gacha.getTime());
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return 0;
        }
    }

    public int[] batchAdd(List<GameGacha> list) {
        String sql = """
                INSERT OR IGNORE INTO game_gacha(player_id, gacha_name, card_pool_type, resource_id, quality_level, resource_type, name, count, time)
                VALUES (?,?,?,?,?,?,?,?,?)""";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            Object[][] params = new Object[list.size()][9];
            for (int i = 0; i < list.size(); i++) {
                GameGacha gacha = list.get(i);
                params[i] = new Object[]{
                        gacha.getPlayerId(), gacha.getGachaName(), gacha.getCardPoolType(),
                        gacha.getResourceId(), gacha.getQualityLevel(), gacha.getResourceType(),
                        gacha.getName(), gacha.getCount(), gacha.getTime()
                };
            }
            return qr.batch(sql, params);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return new int[0];
        }
    }

    public List<GameGacha> getListByPlayerId(String playerId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_gacha WHERE player_id = ? ORDER BY time DESC";
        try {
            return qr.query(sql, new BeanListHandler<>(GameGacha.class, getRowProcessor()), playerId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return List.of();
        }
    }

    public List<GameGacha> getListByPlayerIdAndGachaName(String playerId, String gachaName) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_gacha WHERE player_id = ? AND gacha_name = ? ORDER BY time DESC";
        try {
            return qr.query(sql, new BeanListHandler<>(GameGacha.class, getRowProcessor()), playerId, gachaName);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return List.of();
        }
    }

    public List<String> getAllPlayerIds() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT DISTINCT player_id FROM game_gacha";
        try {
            return qr.query(sql, new ColumnListHandler<>());
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return List.of();
        }
    }

    public int deleteByPlayerId(String playerId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "DELETE FROM game_gacha WHERE player_id = ?";
        try {
            return qr.update(sql, playerId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return 0;
        }
    }
}
