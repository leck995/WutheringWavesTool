package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.roleData.Role;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class GameRoleDataDao {
    private static final Logger LOG = LoggerFactory.getLogger(GameRoleDataDao.class);
    private final DataSource dataSource;

    @Inject
    public GameRoleDataDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public GameRoleDataDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    private RowProcessor getRowProcessor() {
        Map<String, String> map = new HashMap<>();
        map.put("role_id", "roleId");
        map.put("role_name", "roleName");
        map.put("role_icon_url", "roleIconUrl");
        map.put("role_pic_url", "rolePicUrl");
        map.put("star_level", "starLevel");
        map.put("attribute_id", "attributeId");
        map.put("attribute_name", "attributeName");
        map.put("weapon_type_id", "weaponTypeId");
        map.put("weapon_type_name", "weaponTypeName");
        map.put("acronym", "acronym");
        map.put("breach", "breach");
        return new BasicRowProcessor(new BeanProcessor(map));
    }

    public Role getRoleDataById(int id) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM game_role WHERE role_id=?";
        try {
            return qr.query(sql, new BeanHandler<>(Role.class, getRowProcessor()), id);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public int add(Role role) {
        String sql = "INSERT OR IGNORE INTO game_role (role_id,role_name,role_icon_url,role_pic_url,star_level,attribute_id,attribute_name,weapon_type_id,weapon_type_name,acronym,breach) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<>();
            return qr.insert(sql, rsh,
                    role.getRoleId(), role.getRoleName(), role.getRoleIconUrl(), role.getRolePicUrl(), role.getStarLevel(), role.getAttributeId(), role.getAttributeName(), role.getWeaponTypeId(), role.getWeaponTypeName(), role.getAcronym(), role.getBreach());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
