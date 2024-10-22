package cn.tealc.wutheringwavestool.dao;

import javafx.util.Pair;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-19 01:43
 */
public class GameSettingDao {

    private static final Logger LOG= LoggerFactory.getLogger(GameSettingDao.class);

    public List<Pair<String,String>> getAllSetting(){
        String sql="select * from LocalStorage";
        QueryRunner qr=new QueryRunner();
        Connection conn = GameJdbcUtils.getConnection();
        try {
            List<Map<String, Object>> maps = qr.query(conn,sql, new MapListHandler());

            List<Pair<String,String>> list=new ArrayList<Pair<String,String>>();
            for (Map<String, Object> map : maps) {
                String key = (String) map.get("key");
                String value = (String) map.get("value");
                System.out.println(key + ":" + value);
                list.add(new Pair<>(key, value));
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
        }
        GameJdbcUtils.close();
        return null;
    }

    public Pair<String,String> getSettingValueByKey(String key){
        String sql="select * from LocalStorage where key = ?";
        QueryRunner qr=new QueryRunner();
        Connection conn = GameJdbcUtils.getConnection();
        Pair<String,String> pair=null;
        try {
            Map<String, Object> map = qr.query(conn,sql, new MapHandler(),key);
            if (map != null) {
                String keyRow = (String) map.get("key");
                String value = (String) map.get("value");
                System.out.println(keyRow + ":" + value);
                pair =new Pair<>(keyRow, value);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
        }finally{
            GameJdbcUtils.close();
        }
        return pair;
    }
    public boolean updateSettingValueByKey(String key,String value){
        String sql="insert or replace INTO LocalStorage (key,value) VALUES (?,?)";
        QueryRunner qr=new QueryRunner();
        Connection conn = GameJdbcUtils.getConnection();
        Pair<String,String> pair=null;
        boolean flag = false;
        try {
            int update = qr.update(conn, sql, key, value);
            flag = update > 0;
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
        }finally{
            GameJdbcUtils.close();
        }

        return flag;
    }

}