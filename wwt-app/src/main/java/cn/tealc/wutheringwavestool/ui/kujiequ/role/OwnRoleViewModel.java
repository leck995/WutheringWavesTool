package cn.tealc.wutheringwavestool.ui.kujiequ.role;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.service.UserInfoService;
import cn.tealc.wutheringwavestool.service.WebKujiequManager;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.google.inject.Inject;
import com.kuro.kujiequ.KujiequManager;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.roleData.RoleDetail;
import com.kuro.kujiequ.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.kuro.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
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
    private UserInfoService userInfoService;

    @Inject
    private WebKujiequManager webKujiequManager;

    @Inject
    private KujiequManager kujiequManager;

    private UserInfo userInfo;

    public void init() {
        userInfo= userInfoService.getMainUser();
        if (userInfo != null) {
            Thread.startVirtualThread(() -> {
                try {
                    ResponseBody<List<Role>> responseBody = kujiequManager.getGameRoleData(userInfo);
                    if (responseBody.getCode() == 200){
                        List<Role> list=responseBody.getData();
                        Platform.runLater(() -> roleList.setAll(list));
                    }else {
                        Platform.runLater(() ->
                                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                        MessageInfo.warning(responseBody.getMsg()),false));
                    }
                } catch (Exception e) {
                    LOG.error("获取角色数据失败", e);
                    Platform.runLater(() ->
                            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                                    MessageInfo.error("获取角色数据失败，请检查网络后重试"), false));
                }
            });
        }else {
            publish("EMPTY");
        }
    }

    public void openRoleBoxInWebView() {
        if (userInfo != null) {
            webKujiequManager.setUserInfo(userInfo);
        }
        webKujiequManager.openRoleBox();
    }


    public ObservableList<Role> getRoleList() {
        return roleList;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }
}