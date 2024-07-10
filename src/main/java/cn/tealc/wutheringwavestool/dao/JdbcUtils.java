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



    /**
     *@Description: 获取连接
     *@Param:
     *@return:
     *@Author: Leck
     *@date: 2022/6/30
     */
/*    public static Connection getConnection() throws SQLException
    {
        if (connection == null)
            connection=DriverManager.getConnection(url);
        return connection;
    }*/


    public static void close(Connection conn,Statement... sts) {
        try {
            for (Statement st : sts) {
                if (st != null){
                    st.close();
                }
            }
            if (conn != null){
                conn.close();
            }
        }catch (SQLException e){

        }

    }
    public static void close(Connection conn, ResultSet rs,Statement... sts) {
        try {
            if (rs !=null){
                rs.close();
            }
            for (Statement st : sts) {
                if (st != null){
                    st.close();
                }
            }
        }catch (SQLException e){

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
            Statement st = con.createStatement();
            st.execute(createRoleList);
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