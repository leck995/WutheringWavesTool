package cn.tealc.wutheringwavestool;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 版本变更操作
 * @author: Leck
 * @create: 2024-07-16 22:15
 */
public class VersionUpdateUtil {
    public static void update(){
        update01();
    }


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
}