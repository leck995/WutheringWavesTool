package cn.tealc.wutheringwavestool.ui.system.account;

import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.ui.kujiequ.account.AccountView;
import cn.tealc.wutheringwavestool.ui.kujiequ.account.AccountViewModel;
import cn.tealc.wutheringwavestool.ui.kujiequ.tower.*;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class AccountGroupView implements FxmlView<AccountGroupViewModel> {
    @InjectViewModel
    private AccountGroupViewModel viewModel;
    @FXML
    private ToggleGroup childSelectedToggle;
    @FXML
    private StackPane content;
    @FXML
    private HBox headerPane;

    private Node kujiequView;
    private Node appAccountView;

    public void initialize() {
        createAppAccountView();
        if (Config.setting().isNoKuJieQu()){
            ToggleButton toggle = (ToggleButton) childSelectedToggle.getToggles().get(1);
            toggle.setVisible(false);
            toggle.setDisable(true);
        }
    }



    @FXML
    void toAppAccount(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            createAppAccountView();
        }else {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    void toKujiequ(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            if (kujiequView == null) {
                ViewTuple<AccountView, AccountViewModel> viewTuple = FluentViewLoader.fxmlView(AccountView.class).load();
                kujiequView = viewTuple.getView();
            }
            content.getChildren().setAll(kujiequView);
            Animations.slideInUp(kujiequView, Duration.millis(300)).play();
        }else {
            toggleButton.setSelected(true);
        }
    }

    private void createAppAccountView(){
        if (appAccountView == null) {
            ViewTuple<AppAccountView, AppAccountViewModel> viewTuple = FluentViewLoader.fxmlView(AppAccountView.class).load();
            appAccountView = viewTuple.getView();
        }
        content.getChildren().setAll(appAccountView);
        Animations.slideInUp(appAccountView, Duration.millis(300)).play();
    }
}