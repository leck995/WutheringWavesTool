package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.dao.GameSlashDataDao;
import cn.tealc.wutheringwavestool.dao.GameTowerDataDao;
import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.tower.SlashDataForDB;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import com.kuro.kujiequ.model.sign.SignUserInfo;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 版本变更操作, 每个版本保存3个月，后续移除
 * @author: Leck
 * @create: 2024-07-16 22:15
 */
public class VersionUpdateUtil {
    private static final Logger LOG = LoggerFactory.getLogger(VersionUpdateUtil.class);

    public static void update() {
        update01();
        update02();
        update03();
        update04();
        update05();
        update06();
        update07();
        update08();
    }


    /*1.3版本*/
    private static void update01() {
        File signJson = new File("signInfo.json");
        if (signJson.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<SignUserInfo> list = mapper.readValue(signJson, new TypeReference<List<SignUserInfo>>() {
                });
                if (!list.isEmpty()) {
                    UserInfoDao dao = new UserInfoDao();
                    UserInfo user;
                    for (SignUserInfo userInfo : list) {
                        user = new UserInfo();
                        user.setUserId(userInfo.getUserId());
                        user.setRoleId(userInfo.getRoleId());
                        user.setMain(userInfo.getMain());
                        user.setToken(userInfo.getToken());
                        user.setWeb(false);
                        int i = dao.addUser(user);

                    }
                    List<UserInfo> all = dao.getAll();
                    dao.updateLastSignTime(new Date().getTime(), all.getFirst().getId());
                    signJson.delete();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /*1.4版本*/
    private static void update02() {
        File file1 = new File("assets/image/home-role.png");
        if (file1.exists()) {
            file1.delete();
        }
        File file2 = new File("assets/image/home-bg.png");
        if (file2.exists()) {
            file2.delete();
        }
        File file3 = new File("assets/image/icon.png");
        if (file3.exists()) {
            file3.delete();
        }
        Connection connection = JdbcUtils.getConnection();
        try {
            Statement st = connection.createStatement();
            String checkSql = "select count(*) from sqlite_master where name='user_info' and sql like '%has_info%'";
            ResultSet resultSet = st.executeQuery(checkSql);
            int anInt = resultSet.getInt(1);
            if (anInt == 0) {
                st.execute("ALTER table user_info ADD  has_info BOOL DEFAULT 0");
            }
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*1.4.6版本*/
    private static void update03() {
        Connection connection = JdbcUtils.getConnection();
        try {
            Statement st = connection.createStatement();

            String checkSql = "select count(*) from sqlite_master where name='user_info' and sql like '%role_name%'";
            ResultSet resultSet = st.executeQuery(checkSql);
            int anInt = resultSet.getInt(1);
            if (anInt == 0) {
                st.execute("ALTER table user_info ADD role_name VARCHAR");
            }

            String checkSql2 = "select count(*) from sqlite_master where name='user_info' and sql like '%role_url%'";
            ResultSet resultSet2 = st.executeQuery(checkSql2);
            int anInt2 = resultSet2.getInt(1);
            if (anInt2 == 0) {
                st.execute("ALTER TABLE user_info ADD role_url VARCHAR");
            }

            String checkSql3 = "select count(*) from sqlite_master where name='user_info' and sql like '%creat_time%'";
            ResultSet resultSet3 = st.executeQuery(checkSql3);
            int anInt3 = resultSet3.getInt(1);
            if (anInt3 == 0) {
                st.execute("ALTER TABLE user_info ADD creat_time INTEGER");
            }
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Thread.startVirtualThread(() -> {
            File binFile = new File("bin");
            File confFile = new File("conf");
            File legalFile = new File("legal");
            File libFile = new File("lib");
            if (binFile.exists() && binFile.isDirectory()) {
                deleteFile(binFile);
            }
            if (confFile.exists() && confFile.isDirectory()) {
                deleteFile(confFile);
            }
            if (legalFile.exists() && legalFile.isDirectory()) {
                deleteFile(legalFile);
            }
            if (libFile.exists() && libFile.isDirectory()) {
                deleteFile(libFile);
            }
        });

    }

    //1.2.0版本
    public static void update04() {
        Connection connection = JdbcUtils.getConnection();
        try {
            Statement st = connection.createStatement();
            String sql = "DELETE FROM game_time WHERE duration < 0";
            int rowsAffected = st.executeUpdate(sql);
            st.close(); // 关闭Statement
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //助手1.3.0版本，更改游戏根目录
    public static void update05() {
        String dirPath = Config.setting.getGameRootDir();
        File dir = new File(dirPath, "Wuthering Waves Game");
        if (dir.exists() && dir.isDirectory()) {
            Config.setting.setGameRootDir(dir.getAbsolutePath());
        }

    }

    private static void update06() {
        Connection connection = JdbcUtils.getConnection();
        try {
            Statement st = connection.createStatement();
            String checkSql = "select count(*) from sqlite_master where name='user_info' and sql like '%dev_code%'";
            ResultSet resultSet = st.executeQuery(checkSql);
            int anInt = resultSet.getInt(1);
            if (anInt == 0) {
                st.execute("ALTER table user_info ADD dev_code TEXT");
            }
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private static void update07() {
        Connection connection = null;
        try {
            connection = JdbcUtils.getConnection();

            QueryRunner qr = new QueryRunner();

            // 检查是否需要迁移
            String checkSql = "SELECT count(*) FROM sqlite_master WHERE name='game_tower' AND sql LIKE '%role_id%'";
            ResultSetHandler<Integer> countHandler = new ScalarHandler<>();
            Integer exists = qr.query(connection, checkSql, countHandler);

            if (exists == null || exists == 0) {
                LOG.info("开始迁移数据表game_tower");
                connection.setAutoCommit(false);
                // 执行迁移操作
                qr.update(connection, "ALTER TABLE game_tower RENAME TO game_tower_old");

                qr.update(connection, """
                CREATE TABLE IF NOT EXISTS game_tower(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    role_id VARCHAR,
                    floor INTEGER NOT NULL,
                    pic_url INTEGER,
                    role_list VARCHAR,
                    star INTEGER,
                    area_id INTEGER NOT NULL,
                    area_name VARCHAR NOT NULL,
                    difficulty INTEGER,
                    difficulty_name VARCHAR,
                    endTime INTEGER NOT NULL,
                    UNIQUE (role_id, area_id, floor, endTime)
                )""");

                qr.update(connection, """
                INSERT INTO game_tower (floor, pic_url, role_list, star, area_id, area_name, difficulty, difficulty_name, endTime)
                SELECT floor, pic_url, role_list, star, area_id, area_name, difficulty, difficulty_name, endTime
                FROM game_tower_old""");

                connection.commit();
                connection.setAutoCommit(true); // 恢复自动提交模式


                UserInfoDao userInfoDao = new UserInfoDao();
                UserInfo userInfo = userInfoDao.getMain();
                if (userInfo != null) {
                    GameTowerDataDao dao = new GameTowerDataDao();
                    List<TowerData> all = dao.getAll();
                    for (TowerData towerData : all) {
                        towerData.setRoleId(userInfo.getRoleId());
                        dao.update(towerData);
                    }
                }
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            LOG.info("game_tower数据库操作失败{}",e.getMessage());
            throw new RuntimeException("Database migration failed", e);
        }finally {
            try {
                connection.setAutoCommit(true); // 恢复自动提交模式
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private static void update08() {
        Connection connection = null;
        try {
            connection = JdbcUtils.getConnection();

            QueryRunner qr = new QueryRunner();

            // 检查是否需要迁移
            String checkSql = "SELECT count(*) FROM sqlite_master WHERE name='game_slash' AND sql LIKE '%role_id%'";
            ResultSetHandler<Integer> countHandler = new ScalarHandler<>();
            Integer exists = qr.query(connection, checkSql, countHandler);

            if (exists == null || exists == 0) {
                LOG.info("开始迁移数据表game_slash");
                connection.setAutoCommit(false);
                // 执行迁移操作
                qr.update(connection, "ALTER TABLE game_slash RENAME TO game_slash_old");

                qr.update(connection, """
                        CREATE TABLE IF NOT EXISTS game_slash (
                        id INTEGER PRIMARY KEY  AUTOINCREMENT,
                        data TEXT NOT NULL,
                        end_time BIGINT NOT NULL,
                        role_id VARCHAR,
                        UNIQUE (role_id, end_time)
                    );""");

                qr.update(connection, """
                INSERT INTO game_slash (data, end_time)
                SELECT data, end_time
                FROM game_slash_old""");

                connection.commit();
                UserInfoDao userInfoDao = new UserInfoDao();
                UserInfo userInfo = userInfoDao.getMain();
                if (userInfo != null) {
                    GameSlashDataDao dao = new GameSlashDataDao();
                    List<SlashDataForDB> all = dao.getAll();
                    for (SlashDataForDB data : all) {
                        data.setRoleId(userInfo.getRoleId());
                        dao.updateRoleId(data.getId(), userInfo.getRoleId());
                    }
                }
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            LOG.info("game_tower数据库操作失败{}",e.getMessage());
            throw new RuntimeException("Database migration failed", e);
        }finally {
            try {
                connection.setAutoCommit(true); // 恢复自动提交模式
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }




    public static void deleteFile(File file) {
        if (file.isFile()) {
            file.delete();
        } else {
            File[] childFilePaths = file.listFiles();//得到当前的路径
            for (File childFile : childFilePaths) {
                deleteFile(childFile);
            }
            file.delete();
        }
    }
}