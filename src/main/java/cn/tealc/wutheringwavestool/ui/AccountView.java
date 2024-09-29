package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.roleData.user.RoleDailyData;
import cn.tealc.wutheringwavestool.model.roleData.user.RoleInfo;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.thread.api.UserDailyDataTask;
import cn.tealc.wutheringwavestool.thread.api.UserInfoDataTask;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-04 00:26
 */
public class AccountView implements FxmlView<AccountViewModel>, Initializable {
    private static final Logger LOG= LoggerFactory.getLogger(AccountView.class);
    @InjectViewModel
    private AccountViewModel viewModel;
    @FXML
    private ListView<UserInfo> accountListView;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        accountListView.setItems(viewModel.getAccountList());
        accountListView.setCellFactory((ListView<UserInfo> listView) -> new AccountCell());
    }

    @FXML
    void addUser(ActionEvent event) {
        AddAccountView addAccountView = new AddAccountView();
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,addAccountView);
    }

    class AccountCell extends ListCell<UserInfo> {
        private final Label userId=new Label();
        private final Label roleId=new Label();
        private final Label index=new Label();
        private final Button delete=new Button(null,new FontIcon(Material2AL.DELETE));
        private final Button update=new Button(null,new FontIcon(Material2AL.EDIT));

        public AccountCell() {
            userId.getStyleClass().add("user-label");
            roleId.getStyleClass().add("role-label");
            delete.setVisible(false);
            delete.getStyleClass().add("delete-btn");
            delete.setOnAction(event -> {
                if (getItem() != null){
                    delete();
                }
            });

            update.setVisible(false);
            update.getStyleClass().add("delete-btn");
            update.setOnAction(event -> {
                if (getItem() != null){
                    update();
                }
            });

            VBox vbox=new VBox(3.0,userId,roleId);
            vbox.setAlignment(Pos.CENTER_LEFT);

            HBox hbox=new HBox(10.0,index,vbox,update,delete);
            HBox.setHgrow(vbox, Priority.ALWAYS);
            hbox.getStyleClass().add("user");
            hbox.setPadding(new Insets(5.0,5.0,5.0,5.0));
            hbox.setAlignment(Pos.CENTER_LEFT);
            setGraphic(hbox);
        }

        @Override
        protected void updateItem(UserInfo signUserInfo, boolean b) {
            super.updateItem(signUserInfo, b);
            if (!b){
                index.setText(String.valueOf(getIndex()+1));
                userId.setText("用户ID: "+signUserInfo.getUserId());
                roleId.setText("游戏ID: "+signUserInfo.getRoleId());
                delete.setVisible(true);
                update.setVisible(true);
            }else {
                index.setText(null);
                roleId.setText(null);
                userId.setText(null);
                delete.setVisible(false);
                update.setVisible(false);
            }
        }

        private void delete(){
            System.out.println(getIndex());
            JFXDialogLayout dialogLayout = new JFXDialogLayout();
            Label title=new Label("确认");
            title.getStyleClass().add(Styles.TITLE_2);
            dialogLayout.setHeading(title);
            Label content=new Label(String.format("确认删除用户ID: %s 的数据吗",getItem().getUserId()));
            dialogLayout.setBody(content);
            Button saveBtn=new Button("确认");
            saveBtn.getStyleClass().add(Styles.ACCENT);
            Button cancelBtn=new Button("取消");
            cancelBtn.setCancelButton(true);

            saveBtn.setOnAction(event1 -> {
                viewModel.deleteUser(getIndex(),getItem());
                cancelBtn.fireEvent(event1); //这里是为了触发cancelBtn的事件，从而关闭窗口，属实另辟途径（自夸）
            });
            dialogLayout.setActions(saveBtn, cancelBtn);

            MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,dialogLayout);

        }

        private void update(){
            SignUserInfo item = getItem();
            Label userIdLabel = new Label("用户ID:");
            TextField userIdTextField = new TextField(item.getUserId());
            userIdTextField.setPromptText("库街区的用户ID");
            InputGroup inputGroup1 = new InputGroup(userIdLabel,userIdTextField);
            inputGroup1.setAlignment(Pos.CENTER);
            Label roleIdLabel = new Label("游戏ID:");

            TextField roleIdTextField = new TextField(item.getRoleId());
            roleIdTextField.setPromptText("鸣潮的玩家ID(特征码)");
            InputGroup inputGroup2 = new InputGroup(roleIdLabel,roleIdTextField);
            inputGroup2.setAlignment(Pos.CENTER);

            Label tokenLabel = new Label("Token:");
            TextField tokenTextField = new TextField(item.getToken());
            tokenTextField.setPromptText("库街区的登录Token");
            InputGroup inputGroup3 = new InputGroup(tokenLabel, tokenTextField);
            inputGroup3.setAlignment(Pos.CENTER);

            CheckBox mainCheckBox = new CheckBox("设为主账号");
            mainCheckBox.setSelected(item.getMain());
            VBox parentVBox = new VBox(10.0,inputGroup1,inputGroup2,inputGroup3,mainCheckBox);

            parentVBox.setAlignment(Pos.CENTER);

            Button saveBtn=new Button("保存");
            saveBtn.getStyleClass().add(Styles.ACCENT);
            Button cancelBtn=new Button("取消");
            cancelBtn.setCancelButton(true);


            saveBtn.setOnAction(event1 -> {
                String userId = userIdTextField.getText();
                String roleId = roleIdTextField.getText();
                String token = tokenTextField.getText();
                boolean selected = mainCheckBox.isSelected();
                if (!userId.isEmpty() && !roleId.isEmpty() && !token.isEmpty()) {
                    UserInfo userInfo = new UserInfo(userId, roleId, token, selected, true);

                    check(userInfo,saveBtn,cancelBtn);

                }
            });
            Label title = new Label("修改签到用户");
            title.getStyleClass().add(Styles.TITLE_2);
            JFXDialogLayout dialogLayout=new JFXDialogLayout();
            dialogLayout.setHeading(title);
            dialogLayout.setBody(parentVBox);
            dialogLayout.setActions(saveBtn,cancelBtn);
            dialogLayout.setPrefSize(400,250);
            MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,dialogLayout);
        }


        private void check(UserInfo userInfo,Button saveBtn,Button cancelBtn){
            UserInfoDataTask task =new UserInfoDataTask(userInfo);
            task.setOnSucceeded(event1 -> {
                ResponseBody<RoleInfo> value = task.getValue();
                if (value.getCode() == 200){
                    userInfo.setRoleName(value.getData().getName());
                    userInfo.setCreatTime(value.getData().getCreatTime());

                    boolean b = viewModel.updateUser(getIndex(), userInfo);
                    if (b){
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.SUCCESS,"成功修改用户:"+value.getData().getName()));

                        cancelBtn.fireEvent(new ActionEvent()); //这里是为了触发cancelBtn的事件，从而关闭窗口
                    }else {
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"修改失败,可能重复添加"));
                    }
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"获取用户失败，输入信息有误，无法添加，原因:"+value.getMsg()));
                }
            });
            Thread.startVirtualThread(task);
        }

    }

    class AddAccountView extends JFXDialogLayout{
        private final Button saveBtn;
        private final Button cancelBtn;

        public AddAccountView() {
            Label userIdLabel = new Label("用户ID:");
            TextField userIdTextField = new TextField();
            userIdTextField.setPromptText("库街区的用户ID");
            InputGroup inputGroup1 = new InputGroup(userIdLabel,userIdTextField);
            inputGroup1.setAlignment(Pos.CENTER);
            Label roleIdLabel = new Label("游戏ID:");

            TextField roleIdTextField = new TextField();
            roleIdTextField.setPromptText("鸣潮的玩家ID(特征码)");
            InputGroup inputGroup2 = new InputGroup(roleIdLabel,roleIdTextField);
            inputGroup2.setAlignment(Pos.CENTER);

            Label tokenLabel = new Label("Token: ");
            TextField tokenTextField = new TextField();
            tokenTextField.setPromptText("库街区的登录Token");
            InputGroup inputGroup3 = new InputGroup(tokenLabel, tokenTextField);
            inputGroup3.setAlignment(Pos.CENTER);

            CheckBox mainCheckBox = new CheckBox("设为主账号");
            VBox parentVBox = new VBox(10.0,inputGroup1,inputGroup2,inputGroup3,mainCheckBox);

            parentVBox.setAlignment(Pos.CENTER);
            saveBtn= new Button("保存");

            saveBtn.getStyleClass().add(Styles.ACCENT);
            cancelBtn = new Button("取消");
            cancelBtn.setCancelButton(true);

            Hyperlink guide=new Hyperlink("查看教程");
            guide.setOnAction(event1 -> {
                try {
                    Desktop.getDesktop().browse(URI.create("https://www.yuque.com/chashuisuipian/tc2ire/yh69bg99qcbdgic5"));
                } catch (IOException e) {
                    LOG.info("跳转错误",e);
                }
            });
            saveBtn.setOnAction(event1 -> {
                String userId = userIdTextField.getText();
                String roleId = roleIdTextField.getText();
                String token = tokenTextField.getText();
                boolean selected = mainCheckBox.isSelected();
                if (!userId.isEmpty() && !roleId.isEmpty() && !token.isEmpty()) {
                    UserInfo userInfo = new UserInfo(userId, roleId, token, selected, true);
                    check(userInfo);
                }

            });
            Label title = new Label("添加签到用户");
            title.getStyleClass().add(Styles.TITLE_2);
            setHeading(title);
            setBody(parentVBox);
            setActions(guide,saveBtn, cancelBtn);
            setPrefSize(400,250);
        }

        private void check(UserInfo userInfo){
            UserInfoDataTask task =new UserInfoDataTask(userInfo);
            task.setOnSucceeded(event1 -> {
                ResponseBody<RoleInfo> value = task.getValue();
                if (value.getCode() == 200){
                    userInfo.setRoleName(value.getData().getName());
                    userInfo.setCreatTime(value.getData().getCreatTime());

                    boolean status = viewModel.addUser(userInfo);
                    if (status) {
                        cancelBtn.fireEvent(new ActionEvent());//这里是为了触发cancelBtn的事件，从而关闭窗口，属实另辟途径（自夸）
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.SUCCESS,"成功添加用户:"+value.getData().getName()));
                    }else {
                        MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"重复添加，请先删除原有用户再添加"));
                    }
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"获取用户失败，输入信息有误，无法添加，原因:"+value.getMsg()));
                }
            });
            Thread.startVirtualThread(task);
        }


    }





}