package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.Role;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-16 21:39
 */
public class GameTowerDataDao {
    private static final Logger LOG= LoggerFactory.getLogger(GameTowerDataDao.class);
    private static final Connection con= JdbcUtils.getConnection();

    private RowProcessor getRowProcessor(){
        Map<String,String> map=new HashMap<>();
        map.put("id","id");
        map.put("floor","floor");
        map.put("pic_url","picUrl");
        map.put("role_list","roleList");
        map.put("star","star");
        map.put("area_id","areaId");
        map.put("area_name","areaName");
        map.put("difficulty","difficulty");
        map.put("difficulty_name","difficultyName");
        map.put("endTime","endTime");
        BeanProcessor beanProcessor=new BeanProcessor(map);
        return new BasicRowProcessor(beanProcessor);
    }


    public List<TowerData> getAll(){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_tower";
        try {
            return qr.query(con,sql,new BeanListHandler<>(TowerData.class,getRowProcessor()));
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }

    public Set<TowerData> getListByEndTime(long endTime){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_tower where endTime = ?";
        try {
            List<TowerData> query = qr.query(con, sql, new BeanListHandler<>(TowerData.class,getRowProcessor()),endTime);
            return new HashSet<>(query);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }


    public Set<Long> getEndTimeList(){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT endTime FROM game_tower";
        try {
            List<Long> query = qr.query(con, sql, new ColumnListHandler<>());
            return new HashSet<>(query);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }


    public int add(TowerData tower){
        String sql=" INSERT INTO game_tower(floor,pic_url,role_list,star,area_id,area_name,difficulty,difficulty_name,endTime) VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT(area_id,floor,endTime) DO UPDATE SET star = ?,role_list = ?";
        QueryRunner qr=new QueryRunner();
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<Integer>();
            return qr.insert(con,sql,rsh,
                    tower.getFloor(),tower.getPicUrl(),tower.getRoleList(),tower.getStar(),tower.getAreaId(),tower.getAreaName(),tower.getDifficulty(),tower.getDifficultyName(),tower.getEndTime(),
                    tower.getStar(),tower.getRoleList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}