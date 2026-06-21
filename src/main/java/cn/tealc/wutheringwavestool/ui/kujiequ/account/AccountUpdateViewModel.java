package cn.tealc.wutheringwavestool.ui.kujiequ.account;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.service.UserInfoService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.base.user.LoginUserTask;
import com.kuro.kujiequ.thread.rolebox.role.GameRoleSeekTask;
import com.kuro.kujiequ.thread.sms.SendSmsTask;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public class AccountUpdateViewModel extends BaseViewModel {
    public static final String EVENT_CLOSE = "EVENT_CLOSE";
    public static final String EVENT_SELECT_ROLE = "EVENT_SELECT_ROLE";

    @Inject
    private UserInfoService userInfoService;

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
    public void loginByToken() {
        if (isAdd) {
            addUser(token.get(), !mobileSource.get(), did.get());
        } else {
            updateUser(token.get(), !mobileSource.get(), did.get());
        }
    }


    /**
     * 验证码登陆的处理方法，与submit()区别在于多一条请求获取Token与生成did
     */
    public void loginBySMS() {
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
        task.setOnFailed(workerStateEvent -> {
            NotificationManager.message(MessageInfo.error("登录失败，请检查网络后重试"));
        });
        Thread.startVirtualThread(task);
    }

    public void sendSMS(Runnable callback) {
        SendSmsTask task = new SendSmsTask(getPhone());
        task.setOnSucceeded(event -> {
            ResponseBody<Boolean> value = task.getValue();
            if (value.getCode() == 200) {
                NotificationManager.message(MessageInfo.success("验证码发送成功"));
            } else if (value.getCode() == 41000) {
                callback.run();
            } else {
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
            }
        });
        task.setOnFailed(workerStateEvent -> {
            NotificationManager.message(MessageInfo.error("验证码发送失败，请检查网络后重试"));
        });
        Thread.startVirtualThread(task);
    }


    /**
     * 添加用户时，请求获取账号列表
     *
     * @param token
     * @param isWeb
     * @param did
     */
    public void addUser(String token, boolean isWeb, String did) {
        GameRoleSeekTask task = new GameRoleSeekTask(token, isWeb);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<List<UserInfo>> value = task.getValue();
            if (value.getCode() == 200) {
                List<UserInfo> userInfoList = value.getData();
                userInfoList.forEach(userInfo -> {
                    userInfo.setMain(mainAccount.get());
                    userInfo.setDevCode(did);
                });
                checkUserList(userInfoList);
            } else {
                NotificationManager.message(MessageInfo.error("添加账号失败，原因：" + value.getMsg()));
            }
        });
        task.setOnFailed(workerStateEvent -> {
            NotificationManager.message(MessageInfo.error("添加账号失败，请检查网络后重试"));
        });
        Thread.startVirtualThread(task);
    }


    /**
     * 修改用户时，请求获取账号列表
     *
     * @param token
     * @param isWeb
     * @param did
     */
    private void updateUser(String token, boolean isWeb, String did) {
        GameRoleSeekTask task = new GameRoleSeekTask(token, isWeb);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<List<UserInfo>> value = task.getValue();
            if (value.getCode() == 200) {
                List<UserInfo> userInfoList = value.getData();
                userInfoList.forEach(userInfo -> {
                    userInfo.setMain(mainAccount.get());
                    userInfo.setId(oldUserInfo.getId());
                    userInfo.setDevCode(did);
                });
                checkUserList(userInfoList);
            } else {
                NotificationManager.message(MessageInfo.error("修改账号失败，原因：" + value.getMsg()));
            }
        });
        task.setOnFailed(workerStateEvent -> {
            NotificationManager.message(MessageInfo.error("修改账号失败，请检查网络后重试"));
        });
        Thread.startVirtualThread(task);
    }


    /**
     * 处理获取的账号列表
     *
     * @param userList
     */
    private void checkUserList(List<UserInfo> userList) {
        if (userList.isEmpty()) {
            NotificationManager.message(MessageInfo.warning("该账号没有绑定游戏账号"));
        } else {
            if (userList.size() == 1) { //如果只有一个游戏账号，直接添加
                addAndUpdateUser(userList.getFirst());
            } else { //多个则弹窗，让用户选择
                publish(EVENT_SELECT_ROLE, userList);
            }
        }
    }


    public void addAndUpdateUserList(List<UserInfo> userList) {
        userList.forEach(this::addAndUpdateUser);
    }

    /**
     * 添加更新用户，当用户选择时调用
     *
     * @param user
     */
    public void addAndUpdateUser(UserInfo user) {
        boolean status;
        if (isAdd) {
            status = addUserToDB(user);
        } else {
            status = updateUserToDB(user);
        }
        if (status) {
            publish(EVENT_CLOSE);
            NotificationManager.message(MessageInfo.success("成功更新账号，游戏昵称：" + user.getRoleName()));
            NotificationManager.publish(NotificationKey.ACCOUNT_UPDATE);
        } else {
            String message = isAdd ? "添加账号失败，账号可能已存在：" : "数据库操作出错，更新账号失败：";
            NotificationManager.message(MessageInfo.error(message + user.getRoleName()));
        }
    }

    private boolean addUserToDB(UserInfo userInfo) {
        if (userInfoService.existsByRoleId(userInfo.getRoleId())) {
            return false;
        }
        if (userInfo.getMain()) {
            userInfoService.changeMainUser(userInfo);
        }
        int id = userInfoService.addUser(userInfo);
        userInfo.setId(id);
        return true;
    }

    private boolean updateUserToDB(UserInfo newUser) {
        if (newUser.getMain()) {
            userInfoService.changeMainUser(newUser);
        }
        return userInfoService.updateUser(newUser);
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