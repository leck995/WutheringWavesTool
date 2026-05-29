package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.ui.item.HeaderImageSelectView;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.*;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class RoleBoardByKujiequView implements FxmlView<RoleBoardByKujiequViewModel>, Initializable {
    @InjectViewModel
    private RoleBoardByKujiequViewModel viewModel;

    @FXML
    private ImageView battlePassIV;

    @FXML
    private Label battlePassLevelLabel;

    @FXML
    private Label battlePassNumLabel;

    @FXML
    private Label battlePassNumLabel1;

    @FXML
    private ProgressBar battlePassProgress;

    @FXML
    private Label box1Label;

    @FXML
    private Label box2Label;

    @FXML
    private Label box3Label;

    @FXML
    private Label box4Label;

    @FXML
    private ImageView energyIv;

    @FXML
    private Label energyLabel;

    @FXML
    private Label energyTimeLabel;

    @FXML
    private Label gameLifeLabel;

    @FXML
    private ImageView headIV;

    @FXML
    private Label levelLabel;

    @FXML
    private ImageView livenessIV;

    @FXML
    private Label livenessLabel;

    @FXML
    private Label roleNameLabel;

    @FXML
    private VBox rolePane;

    @FXML
    private HBox rolePane2;

    @FXML
    private Label storeEnergyLabel;

    @FXML
    private Label weeklyInstCountLabel;

    @FXML
    private Label weeklyInstCountTipLabel;

    @FXML
    private Label weeklyRougeLabel;

    @FXML
    private Label weeklyRougeTipLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        energyTimeLabel.textProperty().bind(viewModel.energyTimeTextProperty());
        energyLabel.textProperty().bind(viewModel.energyTextProperty());
        storeEnergyLabel.textProperty().bind(viewModel.storeEnergyTextProperty());
        weeklyInstCountLabel.textProperty().bind(viewModel.weeklyInstCountTextProperty());
        weeklyInstCountTipLabel.textProperty().bind(viewModel.weeklyInstCountTipTextProperty());
        livenessLabel.textProperty().bind(viewModel.livenessTextProperty());
        battlePassProgress.progressProperty().bind(viewModel.battlePassProgressProperty());
        battlePassLevelLabel.textProperty().bind(viewModel.battlePassLevelTextProperty());
        battlePassNumLabel.textProperty().bind(viewModel.battlePassNumTextProperty());

        rolePane.visibleProperty().bind(viewModel.rolePaneVisibleProperty());
        roleNameLabel.textProperty().bind(viewModel.roleNameTextProperty());
        levelLabel.textProperty().bind(viewModel.levelTextProperty());
        gameLifeLabel.textProperty().bind(viewModel.gameLifeTextProperty());
        box1Label.textProperty().bind(viewModel.box1TextProperty());
        box2Label.textProperty().bind(viewModel.box2TextProperty());
        box3Label.textProperty().bind(viewModel.box3TextProperty());
        box4Label.textProperty().bind(viewModel.box4TextProperty());

        weeklyRougeLabel.textProperty().bind(viewModel.weeklyRougeTextProperty());
        weeklyRougeTipLabel.textProperty().bind(viewModel.weeklyRougeTipTextProperty());

        Circle circle = new Circle(30, 30, 30);
        headIV.setClip(circle);
        changeHeaderIv();
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.CHANGE_HEADER,((s, objects) -> changeHeaderIv()));

    }


    /**
     * @description: 改变头像
     * @param:
     * @return  void
     * @date:   2025/2/18
     */
    private void changeHeaderIv(){
        if (Config.setting().getHomeViewIcon() != null) {
            File roleIVFile = LocalResourcesManager.homeIcon();
            if (roleIVFile.exists()) {
                headIV.setImage(new Image(roleIVFile.toURI().toString(), 60, 60, true, true, true));
            } else {
                headIV.setImage(new Image(FXResourcesLoader.load("image/icon.png"), 60, 60, true, true, true));
            }
        } else {
            headIV.setImage(new Image(FXResourcesLoader.load("image/icon.png"), 60, 60, true, true, true));
        }
    }
    @FXML
    void changeHeaderImage(MouseEvent event) {
        HeaderImageSelectView view = new HeaderImageSelectView();
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, view);
    }

    @FXML
    void changeRole(ActionEvent event) {
        ViewTuple<SelectedPlayerByKujiequView, SelectedPlayerByKujiequViewModel> viewTuple = FluentViewLoader.javaView(SelectedPlayerByKujiequView.class).load();
        NotificationManager.dialog((JFXDialogLayout) viewTuple.getView());
    }

    @FXML
    void refreshRoleData(ActionEvent event) {
        Button button = (Button) event.getSource();
        Node graphic = button.getGraphic();
        RotateTransition transition = new RotateTransition(Duration.millis(300), graphic);
        transition.setByAngle(360);
        transition.play();
        viewModel.updateKujiequRoleData();
    }
}