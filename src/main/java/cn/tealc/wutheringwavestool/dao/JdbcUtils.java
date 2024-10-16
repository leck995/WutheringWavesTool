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
            String createGameTime="""
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

            String createGameRole="""
                    CREATE TABLE IF NOT EXISTS game_role(
                      role_id BIGINT PRIMARY KEY,
                      role_name VARCHAR,
                      role_icon_url INTEGER,
                      role_pic_url VARCHAR,
                      star_level INTEGER,
                      attribute_id INTEGER,
                      attribute_name VARCHAR,     
                      weapon_type_id INTEGER,
                      weapon_type_name VARCHAR,
                      acronym VARCHAR,
                      breach INTEGER);
                    """;
            String createGameTower="""
                    CREATE TABLE IF NOT EXISTS game_tower(
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      floor INTEGER  NOT NULL,
                      pic_url INTEGER,
                      role_list VARCHAR,
                      star INTEGER,
                      area_id INTEGER  NOT NULL,
                      area_name VARCHAR  NOT NULL,
                      difficulty INTEGER,
                      difficulty_name VARCHAR,
                      endTime INTEGER NOT NULL ,
                      UNIQUE (area_id, floor, endTime));
                    """;
            Statement st = con.createStatement();
            st.execute(createGameTime);
            st.execute(createGameRole);
            st.execute(createUserInfo);
            st.execute(createSignHistory);
            st.execute(createGameTower);
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