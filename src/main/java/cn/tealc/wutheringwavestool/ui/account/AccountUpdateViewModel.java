package cn.tealc.wutheringwavestool.ui.account;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.base.user.LoginUserTask;
import com.kuro.kujiequ.thread.rolebox.role.GameRoleSeekTask;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class AccountUpdateViewModel implements ViewModel {
    public static final String EVENT_CLOSE = "close";

    private final SimpleBooleanProperty loginTabVisible = new SimpleBooleanProperty(true);

    private final SimpleStringProperty title = new SimpleStringProperty("");
    private final SimpleStringProperty token = new SimpleStringProperty("");
    private final SimpleStringProperty did = new SimpleStringProperty("");
    private final SimpleStringProperty phone = new SimpleStringProperty("");
    private final SimpleStringProperty code = new SimpleStringProperty("");
    private final SimpleBooleanProperty mobileSource = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty mainAccount = new SimpleBooleanProperty(true);
    private final boolean isAdd; //判断是添加还是修改
    private UserInfo oldUserInfo; //用于修改时保存的旧UserInfo

    //添加时的构造方法
    public AccountUpdateViewModel() {
        loginTabVisible.set(true);
        this.isAdd = true;
        title.set("添加库街区账号");
    }


    //更新时的构造方法
    public AccountUpdateViewModel(UserInfo userInfo) {
        loginTabVisible.set(false);
        this.isAdd = false;
        title.set("修改库街区账号");
        this.oldUserInfo = userInfo;
        token.set(userInfo.getToken());
        did.set(userInfo.getDevCode());
        mobileSource.set(!userInfo.getIsWeb());
        mainAccount.set(userInfo.getMain());

    }


    /**
     * 手动添加账号方法
     */
    public void submit() {
        if (isAdd) {
            addUser(token.get(), !mobileSource.get(), did.get());
        } else {
            updateUser(token.get(), !mobileSource.get(), did.get());
        }
    }


    /**
     * 验证码登陆的处理方法，与submit()区别在于多一条请求获取Token与生成did
     */
    public void login() {
        LoginUserTask task = new LoginUserTask(getPhone(), getCode(), false);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<UserInfo> value = task.getValue();
            if (value.getCode() == 200) {
                if (isAdd) {
                    addUser(value.getData().getToken(), false, value.getData().getDevCode());
                } else {
                    updateUser(value.getData().getToken(), false, value.getData().getDevCode());
                }
            } else {
                NotificationManager.message(MessageInfo.error("登录账号失败，原因：" + value.getMsg()));
            }
        });
        Thread.startVirtualThread(task);
    }



    public void addUser(String token, boolean isWeb, String did) {
        GameRoleSeekTask task = new GameRoleSeekTask(token, isWeb);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<UserInfo> value = task.getValue();
            if (value.getCode() == 200) {
                UserInfo user = value.getData();
                user.setMain(mainAccount.get());
                user.setDevCode(did);
                boolean status = addUserToDB(user);
                if (status) {
                    publish(EVENT_CLOSE);
                    NotificationManager.message(MessageInfo.success("成功添加账号，游戏昵称：" + user.getRoleName()));
                    NotificationManager.publish(NotificationKey.ACCOUNT_UPDATE);
                } else {
                    NotificationManager.message(MessageInfo.error("账号已存在，无法添加账号：" + user.getRoleName()));
                }
            } else {
                NotificationManager.message(MessageInfo.error("添加账号失败，原因：" + value.getMsg()));
            }
        });
        Thread.startVirtualThread(task);
    }




    private void updateUser(String token, boolean isWeb, String did) {
        GameRoleSeekTask task = new GameRoleSeekTask(token, isWeb);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<UserInfo> value = task.getValue();
            if (value.getCode() == 200) {
                UserInfo user = value.getData();
                user.setMain(mainAccount.get());
                user.setId(oldUserInfo.getId());
                user.setDevCode(did);
                boolean status = updateUserToDB(user);
                if (status) {
                    publish(EVENT_CLOSE);
                    NotificationManager.message(MessageInfo.success("成功修改账号，游戏昵称：" + user.getRoleName()));
                    NotificationManager.publish(NotificationKey.ACCOUNT_UPDATE);
                } else {
                    NotificationManager.message(MessageInfo.error("账号已存在，无法修改账号：" + user.getRoleName()));
                }
            } else {
                NotificationManager.message(MessageInfo.error("修改账号失败，原因：" + value.getMsg()));
            }
        });
        Thread.startVirtualThread(task);
    }

    private boolean addUserToDB(UserInfo userInfo) {
        UserInfoDao dao = new UserInfoDao();
        UserInfo daoUserByRoleId = dao.getUserByRoleId(userInfo.getRoleId());
        List<UserInfo> accountList = dao.getAll();
        if (daoUserByRoleId == null) {
            if (userInfo.getMain()) {
                for (UserInfo oldMainUser : accountList) {
                    if (oldMainUser.getMain()) {
                        oldMainUser.setMain(false);
                        dao.updateUser(oldMainUser);
                    }
                }
            }
            int id = dao.addUser(userInfo);
            userInfo.setId(id);
            return true;
        } else {
            return false;
        }
    }

    private boolean updateUserToDB(UserInfo newUser) {
        UserInfoDao dao = new UserInfoDao();
        return dao.updateUser(newUser) > 0;
    }



    public String getTitle() {
        return title.get();
    }

    public SimpleStringProperty titleProperty() {
        return title;
    }

    public String getToken() {
        return token.get();
    }

    public SimpleStringProperty tokenProperty() {
        return token;
    }

    public boolean isMobileSource() {
        return mobileSource.get();
    }

    public SimpleBooleanProperty mobileSourceProperty() {
        return mobileSource;
    }

    public boolean isMainAccount() {
        return mainAccount.get();
    }

    public SimpleBooleanProperty mainAccountProperty() {
        return mainAccount;
    }

    public String getPhone() {
        return phone.get();
    }

    public SimpleStringProperty phoneProperty() {
        return phone;
    }

    public String getCode() {
        return code.get();
    }

    public SimpleStringProperty codeProperty() {
        return code;
    }

    public void setOldUserInfo(UserInfo oldUserInfo) {
        this.oldUserInfo = oldUserInfo;
    }

    public String getDid() {
        return did.get();
    }

    public SimpleStringProperty didProperty() {
        return did;
    }

    public boolean isLoginTabVisible() {
        return loginTabVisible.get();
    }

    public SimpleBooleanProperty loginTabVisibleProperty() {
        return loginTabVisible;
    }
    public void setLoginTabVisible(boolean loginTabVisible) {
        this.loginTabVisible.set(loginTabVisible);
    }

}