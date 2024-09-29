package cn.tealc.wutheringwavestool.ui.component;

import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-07 21:19
 */
public class SignUserCell extends ListCell<UserInfo> {
    private final Label userId=new Label();
    private final Label roleId=new Label();
    private final Label index=new Label();
    private final Button delete=new Button(null,new FontIcon(Material2AL.DELETE));
    private final Button update=new Button(null,new FontIcon(Material2AL.EDIT));

    public SignUserCell() {
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
            MvvmFX.getNotificationCenter().publish(NotificationKey.SIGN_USER_DELETE,getItem());
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
        roleIdTextField.setPromptText("鸣潮的玩家ID");
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
                MvvmFX.getNotificationCenter().publish(NotificationKey.SIGN_USER_UPDATE,getIndex(), new UserInfo(userId,roleId,token,selected,false));
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.SUCCESS,"修改成功"));
            }
            cancelBtn.fireEvent(event1); //这里是为了触发cancelBtn的事件，从而关闭窗口，属实另辟途径（自夸）
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

}