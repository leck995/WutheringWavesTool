package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.base.AppInjector;
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
        update02();
        update03();
        update05();
        update06();
    }


    /*1.4版本*/
    private static void update02() {
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


    //助手1.3.0版本，更改游戏根目录
    public static void update05() {
        String dirPath = Config.setting().getGameRootDir();
        File dir = new File(dirPath, "Wuthering Waves Game");
        if (dir.exists() && dir.isDirectory()) {
            Config.setting().setGameRootDir(dir.getAbsolutePath());
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