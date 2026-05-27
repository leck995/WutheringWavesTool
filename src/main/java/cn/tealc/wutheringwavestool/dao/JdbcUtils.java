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
                                UNIQUE (role_id, area_id, floor, endTime))
                    """;

            String createGameRecord="""
                    CREATE TABLE IF NOT EXISTS game_record (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        role_id VARCHAR,
                        create_date VARCHAR NOT NULL,
                        role_change INTEGER DEFAULT 0, -- 切换角色次数
                        role_death INTEGER DEFAULT 0, -- 死亡次数
                        battle INTEGER DEFAULT 0, -- 战斗次数
                        phantom_get INTEGER DEFAULT 0, -- 获取声骸次数
                        phantom_call_skill INTEGER DEFAULT 0, -- 召唤声骸技能次数
                        phantom_transform_skill INTEGER DEFAULT 0, -- 变身声骸召唤次数
                        paralysis INTEGER DEFAULT 0, -- 怪物瘫痪次数
                        transfer INTEGER DEFAULT 0, -- 传送次数
                        parry_front INTEGER DEFAULT 0, -- 极限闪避次数
                        parry_back INTEGER DEFAULT 0, -- 极限闪避次数
                        parry_attack INTEGER DEFAULT 0, -- 极限闪避并反击次数
                        used_strength INTEGER DEFAULT 0,
                        month_card BOOL DEFAULT false,
                        month_card_remain_days INTEGER DEFAULT 0,
                        UNIQUE (role_id, create_date)
                    );
                    """;

            String createGameSlash= """
                    CREATE TABLE IF NOT EXISTS game_slash (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        data TEXT NOT NULL,
                        end_time BIGINT NOT NULL,
                        role_id VARCHAR,
                        UNIQUE (role_id, end_time)
                    );
                    """;
            String createConfig= """
                    CREATE TABLE IF NOT EXISTS config (
                        key VARCHAR NOT NULL UNIQUE,
                        value VARCHAR
                    );
                    """;

            Statement st = con.createStatement();
            st.execute(createGameTime);
            st.execute(createGameRole);
            st.execute(createUserInfo);
            st.execute(createSignHistory);
            st.execute(createGameTower);
            st.execute(createGameRecord);
            st.execute(createGameSlash);
            st.execute(createConfig);
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