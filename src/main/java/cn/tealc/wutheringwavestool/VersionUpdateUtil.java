package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.dao.JdbcUtils;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 版本变更操作,每个版本保存3个月，后续移除
 * @author: Leck
 * @create: 2024-07-16 22:15
 */
public class VersionUpdateUtil {
    public static void update(){
        update01();
        update02();
        update03();
    }


    /*1.3版本*/
    private static void update01(){
        File signJson=new File("signInfo.json");
        if (signJson.exists()){
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<SignUserInfo> list = mapper.readValue(signJson, new TypeReference<List<SignUserInfo>>() {
                });
                if (!list.isEmpty()){
                    UserInfoDao dao=new UserInfoDao();
                    UserInfo user;
                    for (SignUserInfo userInfo : list) {
                        user=new UserInfo();
                        user.setUserId(userInfo.getUserId());
                        user.setRoleId(userInfo.getRoleId());
                        user.setMain(userInfo.getMain());
                        user.setToken(userInfo.getToken());
                        user.setWeb(false);
                        int i = dao.addUser(user);

                    }
                    List<UserInfo> all = dao.getAll();
                    dao.updateLastSignTime(new Date().getTime(),all.getFirst().getId());
                    signJson.delete();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /*1.4版本*/
    private static void update02(){
        File file1=new File("assets/image/home-role.png");
        if (file1.exists()){
            file1.delete();
        }
        File file2=new File("assets/image/home-bg.png");
        if (file2.exists()){
            file2.delete();
        }
        File file3=new File("assets/image/icon.png");
        if (file3.exists()){
            file3.delete();
        }
        Connection connection = JdbcUtils.getConnection();
        try {
            Statement st = connection.createStatement();
            String checkSql="select count(*) from sqlite_master where name='user_info' and sql like '%has_info%'";
            ResultSet resultSet = st.executeQuery(checkSql);
            int anInt = resultSet.getInt(1);
            if (anInt == 0){
                st.execute("ALTER table user_info ADD  has_info BOOL DEFAULT 0");
            }
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*1.4.2版本*/
    private static void update03(){
        Connection connection = JdbcUtils.getConnection();
        try {
            Statement st = connection.createStatement();

            String checkSql="select count(*) from sqlite_master where name='user_info' and sql like '%role_name%'";
            ResultSet resultSet = st.executeQuery(checkSql);
            int anInt = resultSet.getInt(1);
            if (anInt == 0){
                st.execute("ALTER table user_info ADD role_name VARCHAR");
            }

            String checkSql2="select count(*) from sqlite_master where name='user_info' and sql like '%role_url%'";
            ResultSet resultSet2 = st.executeQuery(checkSql2);
            int anInt2 = resultSet2.getInt(1);
            if (anInt2 == 0){
                st.execute("ALTER TABLE user_info ADD role_url VARCHAR");
            }

            String checkSql3="select count(*) from sqlite_master where name='user_info' and sql like '%creat_time%'";
            ResultSet resultSet3 = st.executeQuery(checkSql3);
            int anInt3 = resultSet3.getInt(1);
            if (anInt3 == 0){
                st.execute("ALTER TABLE user_info ADD creat_time INTEGER");
            }
            resultSet.close();



        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}