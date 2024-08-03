package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.ui.component.SignUserCell;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-04 00:26
 */
public class AccountView implements FxmlView<AccountViewModel>, Initializable {
    @InjectViewModel
    private AccountViewModel viewModel;
    @FXML
    private ListView<UserInfo> accountListView;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        accountListView.setItems(viewModel.getAccountList());
        accountListView.setCellFactory((ListView<UserInfo> listView) -> new SignUserCell());
    }

    @FXML
    void addUser(ActionEvent event) {
        Label userIdLabel = new Label("用户ID:");
        TextField userIdTextField = new TextField();
        userIdTextField.setPromptText("库街区的用户ID");
        InputGroup inputGroup1 = new InputGroup(userIdLabel,userIdTextField);
        inputGroup1.setAlignment(Pos.CENTER);
        Label roleIdLabel = new Label("游戏ID:");

        TextField roleIdTextField = new TextField();
        roleIdTextField.setPromptText("鸣潮的玩家ID");
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
                boolean status = viewModel.addUser(new UserInfo(userId,roleId,token,selected,false));
                if (status) {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.SUCCESS,"成功添加用户"));
                }else {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"重复添加，请先删除原有用户再添加"));
                }
            }

            cancelBtn.fireEvent(event1); //这里是为了触发cancelBtn的事件，从而关闭窗口，属实另辟途径（自夸）
        });
        Label title = new Label("添加签到用户");
        title.getStyleClass().add(Styles.TITLE_2);
        JFXDialogLayout dialogLayout=new JFXDialogLayout();
        dialogLayout.setHeading(title);
        dialogLayout.setBody(parentVBox);
        dialogLayout.setActions(saveBtn,cancelBtn);
        dialogLayout.setPrefSize(400,250);


        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,dialogLayout);



    }
}