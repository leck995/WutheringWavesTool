package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.roleData.Role;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.thread.role.GameRoleDataTask;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-29 22:14
 */
public class OwnRoleViewModel implements ViewModel {
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
                }

            });
            Thread.startVirtualThread(task);
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    new MessageInfo(MessageType.WARNING,"当前不存在主用户信息，无法获取，请在签到界面添加用户信息"),false);
        }

    }

    public void openDetail(String cardRoleId){
       /* ViewTuple<OwnRoleDetailView,OwnRoleDetailViewModel> viewTuple = FluentViewLoader
                .fxmlView(OwnRoleDetailView.class)
                .viewModel(new OwnRoleDetailViewModel(userInfo,cardRoleId))
                .load();

        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,viewTuple.getView());*/

    }


    public ObservableList<Role> getRoleList() {
        return roleList;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }
}