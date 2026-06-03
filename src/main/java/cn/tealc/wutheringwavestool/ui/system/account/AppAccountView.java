package cn.tealc.wutheringwavestool.ui.system.account;

import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;


public class AppAccountView implements FxmlView<AppAccountViewModel>, Initializable {
    @InjectViewModel
    private AppAccountViewModel viewModel;
    @FXML
    private Button checkUserBtn;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField usernameField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());
        checkUserBtn.disableProperty().bind(viewModel.getVerifyCommand().runningProperty());

        FontIcon icon = (FontIcon) checkUserBtn.getGraphic();
        RotateTransition rt = new RotateTransition(Duration.seconds(1), icon);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        viewModel.getVerifyCommand().runningProperty().addListener((obs, o, n) -> {
            if (n) {
                rt.play();
            } else {
                rt.stop();
            }
        });
    }


    @FXML
    void checkUserEvent(ActionEvent event) {
        //viewModel.verify();
        viewModel.getVerifyCommand().execute();
    }
}
