package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import de.saxsys.mvvmfx.ViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-04 00:26
 */
public class AccountViewModel implements ViewModel {
    private final ObservableList<UserInfo> accountList= FXCollections.observableArrayList();
    public AccountViewModel() {
        UserInfoDao dao=new UserInfoDao();
        List<UserInfo> userInfos = dao.getAll();
        accountList.setAll(userInfos);
        getSignGoods();

    }




    private void getSignGoods(){
        UserInfoDao dao=new UserInfoDao();
        UserInfo main = dao.getMain();
        if (main != null){
           
        }

    }






    public boolean addUser(UserInfo userInfo) {
        UserInfoDao dao=new UserInfoDao();
        UserInfo daoUserByRoleId = dao.getUserByRoleId(userInfo.getRoleId());
        if (daoUserByRoleId == null){
            if (userInfo.getMain()){
                for (UserInfo oldMainUser : accountList) {
                    if (oldMainUser.getMain()){
                        oldMainUser.setMain(false);
                        dao.updateUser(oldMainUser);
                    }
                }
            }
            int id = dao.addUser(userInfo);
            userInfo.setId(id);
            accountList.add(userInfo);
            return true;
        }else {
            return false;
        }
    }
    
    public boolean updateUser(int index,UserInfo userInfo) {
        UserInfo oldUserInfo = accountList.get(index);
        UserInfoDao dao=new UserInfoDao();
        UserInfo daoUserByRoleId = dao.getUserByRoleId(oldUserInfo.getRoleId());
        if (daoUserByRoleId != null && Objects.equals(daoUserByRoleId.getId(), oldUserInfo.getId())){ //当roleid存在，且id是一个，允许更新
            if (userInfo.getMain()){
                for (UserInfo oldMainUser : accountList) {
                    if (oldMainUser.getMain()){
                        oldMainUser.setMain(false);
                        dao.updateUser(oldMainUser);
                    }
                }
            }
            userInfo.setId(oldUserInfo.getId());
            userInfo.setLastSignTime(oldUserInfo.getLastSignTime());
            dao.updateUser(userInfo);
            accountList.set(index,userInfo);
            return true;
        }

        return false;
    }
    
    public boolean deleteUser(int index,UserInfo userInfo) {
        UserInfoDao dao=new UserInfoDao();
        int i = dao.deleteUser(userInfo.getId());
        if (i > 0){
            accountList.remove(index);
            return true;
        }
        return false;
    }

    public ObservableList<UserInfo> getAccountList() {
        return accountList;
    }

}