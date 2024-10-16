package cn.tealc.wutheringwavestool.dao;


import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.model.kujiequ.roleData.Role;
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


public class GameRoleDataDao {
    private static final Logger LOG= LoggerFactory.getLogger(GameRoleDataDao.class);
    private static final Connection con= JdbcUtils.getConnection();

    private RowProcessor getRowProcessor(){
        Map<String,String> map=new HashMap<>();
        map.put("role_id","roleId");
        map.put("role_name","roleName");
        map.put("role_icon_url","roleIconUrl");
        map.put("role_pic_url","rolePicUrl");
        map.put("star_level","starLevel");
        map.put("attribute_id","attributeId");
        map.put("attribute_name","attributeName");
        map.put("weapon_type_id","weaponTypeId");
        map.put("weapon_type_name","weaponTypeName");
        map.put("acronym","acronym");
        map.put("breach","breach");
        BeanProcessor beanProcessor=new BeanProcessor(map);
        return new BasicRowProcessor(beanProcessor);
    }




    public Role getRoleDataById(int id){
        QueryRunner qr=new QueryRunner();
        String sql="SELECT * FROM game_role WHERE role_id=?";
        try {
            return qr.query(con,sql,new BeanHandler<>(Role.class,getRowProcessor()),id);
        } catch (SQLException e) {
            LOG.error(e.getMessage(),e);
            return null;
        }
    }


    public int add(Role role){
        String sql="INSERT OR IGNORE INTO game_role (role_id,role_name,role_icon_url,role_pic_url,star_level,attribute_id,attribute_name,weapon_type_id,weapon_type_name,acronym,breach) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        QueryRunner qr=new QueryRunner();
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<Integer>();
            return qr.insert(con,sql,rsh,
                    role.getRoleId(),role.getRoleName(),role.getRoleIconUrl(),role.getRolePicUrl(),role.getStarLevel(),role.getAttributeId(),role.getAttributeName(),role.getWeaponTypeId(),role.getWeaponTypeName(),role.getAcronym(),role.getBreach());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
