package cn.tealc.wutheringwavestool.ui.gacha;

import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import com.jfoenixN.controls.JFXDialogLayout;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * @description:
 * @author: Leck
 */
public class AccountUpdateView extends JFXDialogLayout {
    private final Button cancelBtn;

    public AccountUpdateView() {
        Label title = new Label("设置助手账户");
        title.getStyleClass().add(Styles.TITLE_2);


        Label usernameLabel = new Label("用户名(订单)");
        TextField usernameField = new TextField();
        InputGroup usernameGroup = new InputGroup(usernameLabel,usernameField);

        Label passwordLabel = new Label("密码(订单)");
        PasswordField passwordField = new PasswordField();
        InputGroup passwordGroup = new InputGroup(passwordLabel,passwordField);


        usernameField.textProperty().bindBidirectional(Config.setting().serverUsernameProperty());
        passwordField.textProperty().bindBidirectional(Config.setting().serverPasswordProperty());


        VBox box = new VBox(10.0,usernameGroup,passwordGroup);
        box.setAlignment(Pos.TOP_CENTER);



        Button okBtn =new Button("确认");
        okBtn.setOnAction(event -> {});

        cancelBtn = new Button("取消");
        cancelBtn.setCancelButton(true);



        setHeading(title);
        setBody(box);
        setActions(okBtn,cancelBtn);
        setPrefSize(600, 420);
    }


}