package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.sign.UserInfo;

import java.util.List;

@Singleton
public class UserInfoService {
    private final UserInfoDao userInfoDao;

    @Inject
    public UserInfoService(UserInfoDao userInfoDao) {
        this.userInfoDao = userInfoDao;
    }

    public UserInfo getMainUser() {
        return userInfoDao.getMain();
    }

    public List<UserInfo> getAllUsers() {
        return userInfoDao.getAll();
    }

    public UserInfo getUserByRoleId(String roleId) {
        return userInfoDao.getUserByRoleId(roleId);
    }

    /** 解决主账号冲突：将新用户设为主账号，清除其他用户的主账号标记 */
    public void resolveMainAccountConflict(UserInfo newMainUser, List<UserInfo> allUsers) {
        for (UserInfo user : allUsers) {
            if (user.getMain() && !user.getRoleId().equals(newMainUser.getRoleId())) {
                user.setMain(false);
                userInfoDao.updateUser(user);
            }
        }
        newMainUser.setMain(true);
        userInfoDao.updateUser(newMainUser);
    }

    public void changeMainUser(UserInfo newMainUser) {
        UserInfo oldMain = userInfoDao.getMain();
        if (oldMain != null && !oldMain.getRoleId().equals(newMainUser.getRoleId())) {
            oldMain.setMain(false);
            userInfoDao.updateUser(oldMain);
        }
        newMainUser.setMain(true);
        userInfoDao.updateUser(newMainUser);
    }

    public int addUser(UserInfo user) {
        return userInfoDao.addUser(user);
    }

    public boolean updateUser(UserInfo user) {
        return userInfoDao.updateUser(user) > 0;
    }

    public boolean deleteUser(int id) {
        return userInfoDao.deleteUser(id) > 0;
    }

    public boolean existsByRoleId(String roleId) {
        return userInfoDao.existUserByRoleId(roleId);
    }
}
