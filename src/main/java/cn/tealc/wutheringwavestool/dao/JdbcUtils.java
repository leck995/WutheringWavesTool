package cn.tealc.wutheringwavestool.dao;

import java.sql.*;


public class JdbcUtils {
    private static String url="jdbc:sqlite:sqlite.db?date_string_format=yyyy-MM-dd";
    private static String driver="org.sqlite.JDBC";
    private static Connection connection;
    static {
        try {
            // 加载驱动
            Class.forName(driver);
            connection=DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("找不到数据库驱动", e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static void exit(){
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static void init() {
        try {
            Connection con = DriverManager.getConnection(url);
            String createRoleList="""
                    CREATE TABLE IF NOT EXISTS game_time(
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      role_id VARCHAR(100),
                      game_date VARCHAR(100),
                      start_time BIGINT NOT NULL,
                      end_time BIGINT NOT NULL,
                      duration BIGINT NOT NULL);
                    """;

            String createUserInfo="""
                    CREATE TABLE IF NOT EXISTS user_info(
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id VARCHAR(20),
                      role_id VARCHAR(20) UNIQUE,
                      token VARCHAR(255),
                      is_main BOOL DEFAULT false,
                      last_sign_time INTEGER,
                      is_web BOOL DEFAULT false,
                      role_name VARCHAR,
                      role_url VARCHAR,
                      creat_time INTEGER);
                    """;
            String createSignHistory="""
                    CREATE TABLE IF NOT EXISTS sign_history(
                      id BIGINT PRIMARY KEY,
                      goods_name VARCHAR(30),
                      goods_num INTEGER,
                      goods_url VARCHAR,
                      sign_date VARCHAR(40),
                      good_type INTEGER,
                      role_id VARCHAR,
                      user_id VARCHAR);
                    """;
            Statement st = con.createStatement();
            st.execute(createRoleList);
            st.execute(createUserInfo);
            st.execute(createSignHistory);
            st.close();
            con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static Connection getConnection() {
        return connection;
    }
}