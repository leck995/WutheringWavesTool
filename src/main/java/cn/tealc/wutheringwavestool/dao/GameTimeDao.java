package cn.tealc.wutheringwavestool.dao;


import cn.tealc.wutheringwavestool.model.game.GameTime;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GameTimeDao {
    private Logger LOG= LoggerFactory.getLogger(GameTimeDao.class);
    private static Connection con= JdbcUtils.getConnection();

    private RowProcessor getRowProcessor(){
        Map<String,String> map=new HashMap<>();
        map.put("id","id");
        map.put("role_id","roleId");
        map.put("game_date","gameDate");
        map.put("start_time","startTime");
        map.put("end_time","endTime");
        map.put("duration","duration");
        BeanProcessor beanProcessor=new BeanProcessor(map);
        return new BasicRowProcessor(beanProcessor);
    }
    public List<GameTime> getTimeListByData(String date){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_time WHERE game_date=?";
        try {
            return qr.query(con,sql,new BeanListHandler<>(GameTime.class,getRowProcessor()),date);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }
    public List<GameTime> getTimeListByRoleId(String roleId){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_time WHERE role_id=?";
        try {
            return qr.query(con,sql,new BeanListHandler<>(GameTime.class,getRowProcessor()),roleId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }

    public boolean deleteTimeByData(String date) {
        QueryRunner qr = new QueryRunner();
        String sql = "DELETE FROM game_time WHERE game_date = ?";
        try {
            int update = qr.update(con, sql, date);
            LOG.info("成功删除指定时间的所有数据: {}", date);
            return true;
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteTimeByDataAndRoleId(String date, String roleId) {
        QueryRunner qr = new QueryRunner();
        String sql = "DELETE FROM game_time WHERE game_date = ? AND role_id = ?";
        try {
            int update = qr.update(con, sql, date, roleId);
            LOG.info("成功删除指定时间和用户的所有数据: {} 用户ID: {}", date, roleId);
            return true;
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * description: 获取所有用户ID
     *
     * @return
     */
    public List<String> getAllRoleId() {
        QueryRunner qr = new QueryRunner();
        String sql = "SELECT DISTINCT role_id FROM game_time WHERE role_id IS NOT NULL";
        try {
            return qr.query(con, sql, new org.apache.commons.dbutils.handlers.ColumnListHandler<>("role_id"));
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }


    public List<GameTime> getAllTime(){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_time";
        try {
            return qr.query(con,sql,new BeanListHandler<>(GameTime.class,getRowProcessor()));
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }

    public int addTime(GameTime gameTime){
        String sql="INSERT OR IGNORE INTO game_time (role_id,game_date,start_time,end_time,duration) VALUES (?,?,?,?,?)";
        QueryRunner qr=new QueryRunner();
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<Integer>();
            return qr.insert(con,sql,rsh,
                    gameTime.getRoleId(),gameTime.getGameDate(),gameTime.getStartTime(),gameTime.getEndTime(),gameTime.getDuration());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
