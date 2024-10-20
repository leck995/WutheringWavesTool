package cn.tealc.wutheringwavestool.dao;

import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class GameJdbcUtils {
    private static final Logger LOG = LoggerFactory.getLogger(GameJdbcUtils.class);
    private static Connection connection;


    public static void close(){
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            };
        } catch (SQLException e) {
            LOG.error("读取游戏DB文件失败", e);
        }
    }

    public static Connection getConnection() {
        if(connection == null){
            try {
                File gameDB = GameResourcesManager.getGameDB();
                if (gameDB != null) {
                    System.out.println(gameDB.getAbsolutePath());
                    String url = String.format("jdbc:sqlite:%s",gameDB.getAbsolutePath());
                    connection = DriverManager.getConnection(url);
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        return connection;
    }
}