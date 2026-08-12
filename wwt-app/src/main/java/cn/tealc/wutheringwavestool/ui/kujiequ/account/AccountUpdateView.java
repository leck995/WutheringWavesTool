package cn.tealc.wutheringwavestool.ui.kujiequ.account;

import atlantafx.base.theme.Styles;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.ui.component.BaseDialog;
import com.jfoenixN.controls.JFXDialogLayout;
import cn.tealc.wutheringwavestool.ui.kujiequ.web.GeetestCaptchaDialog;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.sms.SendSmsTask;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
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

    @FXML
    private Button getCodeBtn;


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
                () -> loginPhoneFiled.getText().isEmpty() || loginPhoneFiled.getText().length() != 11 || loginCodeField.getText().isEmpty(),
                loginPhoneFiled.textProperty(),
                loginCodeField.textProperty()
        );
        loginBtn.disableProperty().bind(isPhoneAndCodeEmpty);

        if (getCodeBtn != null) {
            BooleanBinding getCodeDisabled = Bindings.createBooleanBinding(
                    () -> {
                        String phone = loginPhoneFiled.getText() == null ? "" : loginPhoneFiled.getText().trim();
                        return !SendSmsTask.isValidCnMobile(phone)
                                || viewModel.isSmsSending()
                                || viewModel.getSmsCooldown() > 0;
                    },
                    loginPhoneFiled.textProperty(),
                    viewModel.smsSendingProperty(),
                    viewModel.smsCooldownProperty()
            );
            getCodeBtn.disableProperty().bind(getCodeDisabled);
            getCodeBtn.textProperty().bind(Bindings.createStringBinding(
                    () -> {
                        int left = viewModel.getSmsCooldown();
                        if (left > 0) {
                            return left + " 秒";
                        }
                        if (viewModel.isSmsSending()) {
                            return "发送中";
                        }
                        return "获取";
                    },
                    viewModel.smsCooldownProperty(),
                    viewModel.smsSendingProperty()
            ));
        }

        viewModel.subscribe(AccountUpdateViewModel.EVENT_CLOSE, (s, objects) -> closeDialog());

        viewModel.subscribe(AccountUpdateViewModel.EVENT_SELECT_ROLE, (s, objects) -> {
            initAndShowRoleSelectDialog((List<UserInfo>) objects[0]);
        });


    }

    @FXML
    void onSubmit(ActionEvent event) {
        viewModel.loginByToken();
    }

    @FXML
    void onCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void browserGuide(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(URI.create(AppConstants.URL_TOKEN_GUIDE));
        } catch (IOException e) {
            log.info("跳转错误", e);
        }
    }


    @FXML
    void onLogin(ActionEvent event) {
        viewModel.loginBySMS();
    }


    @FXML
    void sendLoginCode(ActionEvent event) {
        String phone = loginPhoneFiled.getText() == null ? "" : loginPhoneFiled.getText().trim();
        if (!SendSmsTask.isValidCnMobile(phone)) {
            NotificationManager.message(MessageInfo.warning("请输入正确的 11 位手机号"));
            return;
        }
        if (!viewModel.canRequestSms()) {
            return;
        }

        Window owner = getCodeBtn != null && getCodeBtn.getScene() != null
                ? getCodeBtn.getScene().getWindow()
                : null;

        GeetestCaptchaDialog captchaDialog = new GeetestCaptchaDialog();
        captchaDialog.setOnSuccess(viewModel::sendSMS);
        captchaDialog.setOnError(message -> {
            // 致命失败：关闭极验窗并提供人工获取验证码兜底
            if (message != null && (message.contains("加载失败")
                    || message.contains("桥接失败")
                    || message.contains("超时")
                    || message.contains("脚本")
                    || message.contains("无法弹出")
                    || message.contains("调用滑块失败"))) {
                captchaDialog.close();
                NotificationManager.message(MessageInfo.warning(message));
                showSmsFailDialog();
            }
        });
        captchaDialog.show(owner);
    }

    private void showSmsFailDialog() {
        Label titleLabel = new Label("获取验证码");
        titleLabel.getStyleClass().add("title-2");

        Label contentLabel = new Label("""
                助手无法发送验证码，请采用以下方法获取验证码。
                
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
        layout.setActions(submitBtn, cancelBtn);
        NotificationManager.publish(NotificationKey.DIALOG, layout);
    }
}
