package cn.tealc.wutheringwavestool.dao;


import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UserInfoDao {
    private Logger LOG= LoggerFactory.getLogger(UserInfoDao.class);
    private static Connection con= JdbcUtils.getConnection();

    private RowProcessor getRowProcessor(){
        Map<String,String> map=new HashMap<>();
        map.put("id","id");
        map.put("user_id","userId");
        map.put("role_id","roleId");
        map.put("token","token");
        map.put("is_main","main");
        map.put("last_sign_time","lastSignTime");
        map.put("is_web","web");
        BeanProcessor beanProcessor=new BeanProcessor(map);
        return new BasicRowProcessor(beanProcessor);
    }
    public List<UserInfo> getAll(){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM user_info";
        try {
            return qr.query(con,sql,new BeanListHandler<>(UserInfo.class,getRowProcessor()));
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }
    public UserInfo getUserByRoleId(String roleId){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM user_info where role_id = ?";
        try {
            return qr.query(con,sql,new BeanHandler<>(UserInfo.class,getRowProcessor()),roleId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }
    public boolean existUserByRoleId(String roleId){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM user_info where role_id = ?";
        try {
            UserInfo userInfo = qr.query(con, sql, new BeanHandler<>(UserInfo.class, getRowProcessor()), roleId);
            return userInfo !=null;
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return false;
        }
    }
    public UserInfo getMain(){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM user_info where is_main=1";
        try {
            return qr.query(con,sql,new BeanHandler<>(UserInfo.class,getRowProcessor()));
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }

    public int addUser(UserInfo userInfo){
        String sql="INSERT INTO user_info (user_id,role_id,token,is_main,is_web) VALUES (?,?,?,?,?)";
        QueryRunner qr=new QueryRunner();
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<Integer>();
            return qr.insert(con,sql,rsh,
                    userInfo.getUserId(),userInfo.getRoleId(),userInfo.getToken(),userInfo.getMain(),userInfo.getWeb());
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return 0;
        }
    }

    public int updateUser(UserInfo userInfo){
        String sql="UPDATE user_info set user_id=?,role_id=?,token=?,is_main=?,is_web=? WHERE id=?";
        QueryRunner qr=new QueryRunner();
        try {
            return qr.update(con,sql,userInfo.getUserId(),userInfo.getRoleId(),userInfo.getToken(),userInfo.getMain(),userInfo.getWeb(),userInfo.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int updateLastSignTime(Long lastSignTime,Integer id){
        String sql="UPDATE user_info SET last_sign_time=? WHERE id=?";
        QueryRunner qr=new QueryRunner();
        try {
            return qr.update(con,sql,lastSignTime,id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public int deleteUser(Integer id){
        String sql="DELETE FROM user_info WHERE id=?";
        QueryRunner qr=new QueryRunner();
        try {
            return qr.update(con,sql,id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
