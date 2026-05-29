package cn.tealc.wutheringwavestool.ui.kujiequ.account;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.ui.component.BaseDialog;
import com.jfoenixN.controls.JFXDialogLayout;
import com.kuro.kujiequ.model.sign.UserInfo;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class AccountUpdateView extends BaseDialog implements FxmlView<AccountUpdateViewModel> {
    private static final Logger log = LoggerFactory.getLogger(AccountUpdateView.class);
    @InjectViewModel
    private AccountUpdateViewModel viewModel;

    @FXML
    private VBox addTab;

    @FXML
    private Button cancelBtn;

    @FXML
    private Button loginBtn;

    @FXML
    private TextField loginCodeField;

    @FXML
    private CheckBox loginMainAccountCheckBox;

    @FXML
    private TextField loginPhoneFiled;

    @FXML
    private RadioButton loginSourceRadioBox;

    @FXML
    private VBox loginTab;

    @FXML
    private CheckBox mainAccountCheckBox;

    @FXML
    private RadioButton mobileRadioBox;

    @FXML
    private Button okBtn;

    @FXML
    private ToggleGroup sourcesToggleGroup;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField tokenField;
    @FXML
    private TextField didField;
    @FXML
    private RadioButton webRadioBox;


    public void initialize() {
        addTab.visibleProperty().bind(viewModel.loginTabVisibleProperty().not());
        loginTab.visibleProperty().bind(viewModel.loginTabVisibleProperty());


        titleLabel.textProperty().bind(viewModel.titleProperty());
        mainAccountCheckBox.selectedProperty().bindBidirectional(viewModel.mainAccountProperty());
        mobileRadioBox.selectedProperty().bindBidirectional(viewModel.mobileSourceProperty());
        webRadioBox.setSelected(!viewModel.isMobileSource());
        tokenField.textProperty().bindBidirectional(viewModel.tokenProperty());
        didField.textProperty().bindBidirectional(viewModel.didProperty());
        BooleanBinding isTokenAndDidEmpty = Bindings.createBooleanBinding(
                () -> tokenField.getText().isEmpty() || didField.getText().isEmpty(),
                tokenField.textProperty(),
                didField.textProperty()
        );
        okBtn.disableProperty().bind(isTokenAndDidEmpty);


        loginPhoneFiled.textProperty().bindBidirectional(viewModel.phoneProperty());
        loginCodeField.textProperty().bindBidirectional(viewModel.codeProperty());
        loginSourceRadioBox.selectedProperty().bindBidirectional(viewModel.mobileSourceProperty());
        loginMainAccountCheckBox.selectedProperty().bindBidirectional(viewModel.mainAccountProperty());
        BooleanBinding isPhoneAndCodeEmpty = Bindings.createBooleanBinding(
                () -> loginPhoneFiled.getText().isEmpty() || loginCodeField.getText().isEmpty(),
                loginPhoneFiled.textProperty(),
                loginCodeField.textProperty()
        );
        loginBtn.disableProperty().bind(isPhoneAndCodeEmpty);

        viewModel.subscribe(AccountUpdateViewModel.EVENT_CLOSE, (s, objects) -> closeDialog());

        viewModel.subscribe(AccountUpdateViewModel.EVENT_SELECT_ROLE, (s, objects) -> {
            initAndShowRoleSelectDialog((List<UserInfo>) objects[0]);
        });


    }

    @FXML
    void onSubmit(ActionEvent event) {
        viewModel.submit();
    }

    @FXML
    void onCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void browserGuide(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(URI.create("https://www.yuque.com/chashuisuipian/sm05lg/pyk5otkcfhd1dqmf"));
        } catch (IOException e) {
            log.info("跳转错误", e);
        }
    }


    @FXML
    void onLogin(ActionEvent event) {
        viewModel.login();
    }


    @FXML
    void sendLoginCode(ActionEvent event) {
        Label titleLabel = new Label("如何获取验证码");
        titleLabel.getStyleClass().add("title-2");

        Label contentLabel = new Label("""
                由于开发者能力有限，助手不支持发送验证码功能，故采用迂回的方式获取验证码。
                
                第一种方法：
                    点击下方按钮前往网页版库街区，输入手机号登录，获取到验证码(收到验证码即停止)；
                第二种方法：
                    打开库街区APP，输入手机号登录，获取到验证码(收到验证码即停止);
                
                将获取到验证码在助手中输入并登录。
                """);
        Button openBrowserBtn = new Button("前往库街区");
        openBrowserBtn.setOnAction(event1 -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://www.kurobbs.com/mc/home/9"));
            } catch (IOException e) {
                log.info("跳转错误", e);
            }
        });

        Button cancelBtn = new Button("关闭");
        cancelBtn.setCancelButton(true);
        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setHeading(titleLabel);
        layout.setBody(contentLabel);
        layout.setActions(openBrowserBtn, cancelBtn);
        NotificationManager.publish(NotificationKey.DIALOG, layout);
    }

    @FXML
    void toAdd(ActionEvent event) {
        viewModel.setLoginTabVisible(false);
        mobileRadioBox.setSelected(true);
    }

    @FXML
    void toLogin(ActionEvent event) {
        viewModel.setLoginTabVisible(true);
        loginMainAccountCheckBox.setSelected(true);
    }


    private void initAndShowRoleSelectDialog(List<UserInfo> userInfoList) {
        Label titleLabel = new Label("选择角色账号");
        titleLabel.getStyleClass().add("title-2");


        VBox content = new VBox();
        content.setSpacing(10);
        List<CheckBox> checkboxes = new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            CheckBox checkBox = new CheckBox(String.format("%s(%s)", userInfo.getRoleName(), userInfo.getRoleId()));
            content.getChildren().add(checkBox);
            checkboxes.add(checkBox);
        }

        Button cancelBtn = new Button("关闭");
        cancelBtn.setCancelButton(true);

        Button submitBtn = new Button("保存");
        submitBtn.getStyleClass().add(Styles.ACCENT);
        submitBtn.setOnAction(event1 -> {
            for (int i = 0; i < checkboxes.size(); i++) {
                if (checkboxes.get(i).isSelected()) {
                    viewModel.addAndUpdateUser(userInfoList.get(i));
                }
            }
            cancelBtn.fire();
        });


        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setHeading(titleLabel);
        layout.setBody(content);
        layout.setActions(submitBtn,cancelBtn);
        NotificationManager.publish(NotificationKey.DIALOG, layout);
    }
}