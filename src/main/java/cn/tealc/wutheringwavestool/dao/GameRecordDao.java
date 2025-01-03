package cn.tealc.wutheringwavestool.dao;


import cn.tealc.wutheringwavestool.model.game.GameRecord;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GameRecordDao {
    private static final Logger LOG= LoggerFactory.getLogger(GameRecordDao.class);
    private final Connection con= JdbcUtils.getConnection();

    private RowProcessor getRowProcessor(){
        Map<String,String> map=new HashMap<>();
        map.put("id","id");
        map.put("role_id","roleId");
        map.put("create_date","createDate");
        map.put("role_change","roleChange");
        map.put("role_death","roleDeath");
        map.put("battle","battle");
        map.put("phantom_get","phantomGet");
        map.put("phantom_call_skill","phantomCallSkill");
        map.put("phantom_transform_skill","phantomTransformSkill");
        map.put("paralysis","paralysis");
        map.put("transfer","transfer");
        map.put("parry_front","parryFront");
        map.put("parry_back","parryBack");
        map.put("parry_attack","parryAttack");
        map.put("used_strength","usedStrength");
        map.put("month_card","monthCard");
        map.put("month_card_remain_days","monthCardRemainDays");
        BeanProcessor beanProcessor=new BeanProcessor(map);
        return new BasicRowProcessor(beanProcessor);
    }



    public GameRecord getRecordByRoleIdAndDate(String roleId,String date){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_record WHERE create_date = ? and role_id = ?";
        try {
            return qr.query(con,sql,new BeanHandler<>(GameRecord.class,getRowProcessor()),date,roleId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }
    /**
     * description: 获取所有用户ID
     *
     * @return
     */
    public List<String> getAllRoleId() {
        QueryRunner qr = new QueryRunner();
        String sql = "SELECT DISTINCT role_id FROM game_record WHERE role_id IS NOT NULL";
        try {
            return qr.query(con, sql, new org.apache.commons.dbutils.handlers.ColumnListHandler<>("role_id"));
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public List<GameRecord> getRecordListByDate(String date){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_record WHERE create_date=?";
        try {
            return qr.query(con,sql,new BeanListHandler<>(GameRecord.class,getRowProcessor()),date);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }
    public List<GameRecord> getRecordListByRoleIdAndDate(String roleId,String date){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_record WHERE  role_id=? and create_date=?";
        try {
            return qr.query(con,sql,new BeanListHandler<>(GameRecord.class,getRowProcessor()),roleId,date);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }

    public List<GameRecord> getRecordListByRoleId(String roleId){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_record WHERE role_id=?";
        try {
            return qr.query(con,sql,new BeanListHandler<>(GameRecord.class,getRowProcessor()),roleId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }


    public List<GameRecord> getAllRecordList(){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_record";
        try {
            return qr.query(con,sql,new BeanListHandler<>(GameRecord.class,getRowProcessor()));
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }

    /**
     * @description: 对不存在的进行添加，存在的更新
     * @param:	record
     * @return  java.lang.Integer
     * @date:   2024/11/13
     */
    public Integer addOrUpdateRecord(GameRecord record){
        String sql= """
                INSERT INTO game_record (role_id,create_date,role_change,
                role_death,battle,phantom_get,phantom_call_skill,phantom_transform_skill,
                paralysis,transfer,parry_front,parry_back,parry_attack,used_strength,month_card,month_card_remain_days)
                VALUES
                (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(role_id,create_date) DO UPDATE SET
                    role_change = role_change + ?,
                    role_death = role_death + ?,
                    battle = battle + ?,
                    phantom_get = phantom_get + ?,
                    phantom_call_skill = phantom_call_skill + ?,
                    phantom_transform_skill = phantom_transform_skill + ?,
                    paralysis = paralysis + ?,
                    transfer = transfer + ?,
                    parry_front = parry_front + ?,
                    parry_back = parry_back + ?,
                    parry_attack = parry_attack + ?,
                    used_strength = used_strength + ?,
                    month_card = month_card + ?,
                    month_card_remain_days = month_card_remain_days + ?;
                """;
        QueryRunner qr=new QueryRunner();
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<Integer>();
            return qr.insert(con,sql,rsh,
                    record.getRoleId(),record.getCreateDate(),record.getRoleChange(),record.getRoleDeath(),
                    record.getBattle(),record.getPhantomGet(),record.getPhantomCallSkill(),
                    record.getPhantomTransformSkill(),record.getParalysis(),record.getTransfer(),
                    record.getParryFront(),record.getParryBack(),record.getParryAttack(),
                    record.getUsedStrength(),record.isMonthCard(),record.getMonthCardRemainDays(),//添加结束，后续更新
                    record.getRoleChange(),record.getRoleDeath(),record.getBattle(),record.getPhantomGet(),record.getPhantomCallSkill(),
                    record.getPhantomTransformSkill(),record.getParalysis(),record.getTransfer(),record.getParryFront(),record.getParryBack(),
                    record.getParryAttack(),record.getUsedStrength(),record.isMonthCard(),record.getMonthCardRemainDays());
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }
}
