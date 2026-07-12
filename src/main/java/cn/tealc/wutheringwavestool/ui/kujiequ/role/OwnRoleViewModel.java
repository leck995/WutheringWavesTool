package cn.tealc.wutheringwavestool.ui.kujiequ.role;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.service.WebKujiequManager;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.roleData.RoleDetail;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.role.GameRoleDataTask;
import com.kuro.kujiequ.thread.rolebox.role.GameRoleDetailTask;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-29 22:14
 */
public class OwnRoleViewModel extends BaseViewModel {
    private static final Logger LOG= LoggerFactory.getLogger(OwnRoleViewModel.class);
    private final ObservableList<Role> roleList= FXCollections.observableArrayList();
    @Inject
    private UserInfoDao userInfoDao;

    @Inject
    private WebKujiequManager webKujiequManager;

    private UserInfo userInfo;

    public void init() {
        userInfo= userInfoDao.getMain();
        if (userInfo != null) {
            GameRoleDataTask task=new GameRoleDataTask(userInfo);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<List<Role>> responseBody = task.getValue();
                if (responseBody.getCode() == 200){
                    List<Role> list=responseBody.getData();
                    roleList.setAll(list);
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            MessageInfo.warning(responseBody.getMsg()),false);
                }
            });
            task.setOnFailed(workerStateEvent -> {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                        MessageInfo.error("获取角色数据失败，请检查网络后重试"), false);
            });
            Thread.startVirtualThread(task);
        }else {
            publish("EMPTY");
        }
    }

    public void openRoleBoxInWebView() {
        if (userInfo != null) {
            webKujiequManager.setUserInfo(userInfo);
        }
        try {
            webKujiequManager.openRoleBox();
        } catch (IOException e) {
            LOG.error("打开数据终端网页失败", e);
        }
    }


    public ObservableList<Role> getRoleList() {
        return roleList;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }
}