package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.base.AppInjector;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.sign.SignRecord;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SignHistoryDao {
    private static final Logger LOG = LoggerFactory.getLogger(SignHistoryDao.class);
    private final DataSource dataSource;

    @Inject
    public SignHistoryDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SignHistoryDao() {
        this.dataSource = AppInjector.getInstance(DataSource.class);
    }

    private RowProcessor getRowProcessor() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "orderCode");
        map.put("user_id", "userId");
        map.put("role_id", "roleId");
        map.put("goods_name", "goodsName");
        map.put("goods_num", "goodsNum");
        map.put("goods_url", "goodsUrl");
        map.put("sign_date", "sigInDate");
        map.put("good_type", "type");
        return new BasicRowProcessor(new BeanProcessor(map));
    }

    public List<SignRecord> getAll() {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM sign_history";
        try {
            return qr.query(sql, new BeanListHandler<>(SignRecord.class, getRowProcessor()));
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public List<SignRecord> getHistoriesByRoleId(String roleId) {
        QueryRunner qr = new QueryRunner(dataSource);
        String sql = "SELECT * FROM sign_history where role_id = ?";
        try {
            return qr.query(sql, new BeanListHandler<>(SignRecord.class, getRowProcessor()), roleId);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public int addHistory(SignRecord signRecord) {
        String sql = "INSERT OR IGNORE INTO sign_history (id,goods_name,goods_num,goods_url,sign_date,good_type,user_id,role_id) VALUES (?,?,?,?,?,?,?,?)";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            ResultSetHandler<Integer> rsh = new ScalarHandler<>();
            return qr.insert(sql, rsh,
                    signRecord.getOrderCode(), signRecord.getGoodsName(), signRecord.getGoodsNum(),
                    signRecord.getGoodsUrl(), signRecord.getSigInDate(), signRecord.getType(),
                    signRecord.getUserId(), signRecord.getRoleId());
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return 0;
        }
    }

    public int deleteHistory(Integer id) {
        String sql = "DELETE FROM sign_history WHERE id=?";
        QueryRunner qr = new QueryRunner(dataSource);
        try {
            return qr.update(sql, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
