package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.Config;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
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
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 19:57
 */
public class HomeView implements Initializable, FxmlView<HomeViewModel> {
    @InjectViewModel
    private HomeViewModel viewModel;

    @FXML
    private ImageView battlePassIV;

    @FXML
    private Label battlePassLevelLabel;

    @FXML
    private Label battlePassNumLabel;

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
    private ImageView roleIV;

    @FXML
    private Label roleNameLabel;

    @FXML
    private AnchorPane rolePane;

    @FXML
    private AnchorPane root;

    @FXML
    private Label gameTimeLabel;
    @FXML
    private Label gameTimeTipLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        File roleIVFile=new File("assets/image/home-role.png");
        roleIV.setPreserveRatio(true);
        roleIV.setSmooth(true);
        roleIV.fitWidthProperty().bind(root.widthProperty().multiply(0.7));
        roleIV.fitHeightProperty().bind(root.heightProperty().multiply(0.7));
        roleIV.setImage(new Image(roleIVFile.toURI().toString(),true));


        energyTimeLabel.textProperty().bind(viewModel.energyTimeTextProperty());
        energyIv.setImage(new Image(MainApplication.class.getResource("image/energy.png").toExternalForm(),true));
        energyLabel.textProperty().bind(viewModel.energyTextProperty());
        livenessIV.setImage(new Image(MainApplication.class.getResource("image/liveness.png").toExternalForm(),true));
        livenessLabel.textProperty().bind(viewModel.livenessTextProperty());
        battlePassIV.setImage(new Image(MainApplication.class.getResource("image/battle-pass.png").toExternalForm(),true));
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

        gameTimeLabel.textProperty().bind(viewModel.gameTimeTextProperty());
        gameTimeTipLabel.textProperty().bind(viewModel.gameTimeTipTextProperty());
        headIV.setImage(new Image(MainApplication.class.getResource("image/icon.png").toExternalForm(),60,60,true,true,true));
        Circle circle = new Circle(30,30,30);
        headIV.setClip(circle);

    }



    @FXML
    void startGame(ActionEvent event) {
        viewModel.startGame();
    }

    @FXML
    void startUpdate(ActionEvent event) {
        viewModel.startUpdate();
    }
    @FXML
    void refreshRoleData(ActionEvent event) {
        Button button = (Button) event.getSource();
        Node graphic = button.getGraphic();
        RotateTransition transition=new RotateTransition(Duration.millis(300),graphic);
        transition.setByAngle(360);
        transition.play();
        viewModel.updateRoleData();
    }
    @FXML
    void toWiki01(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://wiki.kurobbs.com/mc/home"));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void toWiki02(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://www.gamekee.com/mc/"));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    @FXML
    void toAlbum(ActionEvent event) {
        try {
            File file=new File(Config.setting.getGameRootDir()+File.separator+"Wuthering Waves Game\\Client\\Saved\\ScreenShot");
            if (file.exists()){
                Desktop.getDesktop().open(file);
            }else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"截图保存目录不存在"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void toGameDir(ActionEvent event) {
        try {
            File file=new File(Config.setting.getGameRootDir());
            if (file.exists()){
                Desktop.getDesktop().open(file);
            }else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.WARNING,"未设置游戏安装目录，请前往设置进行设置"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}