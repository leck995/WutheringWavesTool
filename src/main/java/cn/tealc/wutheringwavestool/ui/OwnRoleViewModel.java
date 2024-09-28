package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.roleData.Role;
import cn.tealc.wutheringwavestool.model.roleData.RoleDetail;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.thread.api.role.GameRoleDataTask;
import cn.tealc.wutheringwavestool.thread.api.role.GameRoleDetailTask;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
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
public class OwnRoleViewModel implements ViewModel {
    private static final Logger LOG= LoggerFactory.getLogger(OwnRoleViewModel.class);
    private final ObservableList<Role> roleList= FXCollections.observableArrayList();

    private UserInfo userInfo;
    public OwnRoleViewModel() {
        UserInfoDao dao=new UserInfoDao();
        userInfo= dao.getMain();
        if (userInfo != null) {
            GameRoleDataTask task=new GameRoleDataTask(userInfo);
            task.setOnSucceeded(workerStateEvent -> {
                ResponseBody<List<Role>> responseBody = task.getValue();
                if (responseBody.getSuccess()){
                    List<Role> list=responseBody.getData();
                    roleList.setAll(list);
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                            new MessageInfo(MessageType.WARNING,responseBody.getMsg()),false);
                }

            });
            Thread.startVirtualThread(task);
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING,"当前不存在主用户信息，无法获取，请在账号界面添加用户信息"),false);
        }

    }

    public void openDetail(String cardRoleId){
       /* ViewTuple<OwnRoleDetailView,OwnRoleDetailViewModel> viewTuple = FluentViewLoader
                .fxmlView(OwnRoleDetailView.class)
                .viewModel(new OwnRoleDetailViewModel(userInfo,cardRoleId))
                .load();

        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,viewTuple.getView());*/

    }

    public void saveRoleIconToPoolCard(int id){
        GameRoleDetailTask task=new GameRoleDetailTask(userInfo,id);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<RoleDetail> value = task.getValue();
            if (value.getSuccess()){
                RoleDetail roleDetail = value.getData();
                String weaponIcon = roleDetail.getWeaponData().getWeapon().getWeaponIcon();
                String roleIconUrl = roleDetail.getRole().getRoleIconUrl();
                try {
                    Image image1 = LocalResourcesManager.imageBuffer(roleIconUrl);
                    File file1=new File("assets/header/%s.png",roleDetail.getRole().getRoleName());
                    ImageIO.write(SwingFXUtils.fromFXImage(image1,null),"png",file1);

                    Image image2 = LocalResourcesManager.imageBuffer(weaponIcon);
                    File file2=new File("assets/header/%s.png",roleDetail.getWeaponData().getWeapon().getWeaponName());
                    ImageIO.write(SwingFXUtils.fromFXImage(image2,null),"png",file2);
                } catch (IOException e) {
                    LOG.error("写入图片错误",e);
                }

            }
        });


    }

    public ObservableList<Role> getRoleList() {
        return roleList;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }
}