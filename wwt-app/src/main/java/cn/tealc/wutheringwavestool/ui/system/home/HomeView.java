package cn.tealc.wutheringwavestool.ui.system.home;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.ui.item.HeaderImageSelectView;
import cn.tealc.wutheringwavestool.ui.item.PlayTimeAlertItemView;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.*;
import javafx.animation.RotateTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

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
    private static final PseudoClass CHECKING = PseudoClass.getPseudoClass("checking");
    private static final PseudoClass UP_TO_DATE = PseudoClass.getPseudoClass("up-to-date");
    private static final PseudoClass UPDATE_AVAILABLE = PseudoClass.getPseudoClass("update-available");
    private static final PseudoClass CHECK_FAILED = PseudoClass.getPseudoClass("check-failed");

    @InjectViewModel
    private HomeViewModel viewModel;

    @FXML
    private VBox roleBoardGroup;

    @FXML
    private BorderPane root;

    @FXML
    private Button gameTimeBtn;

    @FXML
    private Button startGameBtn;
    @FXML
    private Button startUpdateBtn;
    @FXML
    private HBox resourceStatusPane;
    @FXML
    private Label resourceStatusLabel;
    @FXML
    private Label resourceVersionLabel;
    @FXML
    private FontIcon resourceStatusIcon;
    @FXML
    private Button resourceRetryBtn;

    private RotateTransition resourceCheckRotation;




    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startGameBtn.disableProperty().bind(viewModel.startGameBtnDisabledProperty());
        startUpdateBtn.textProperty().bind(viewModel.updateActionTextProperty());
        resourceStatusLabel.textProperty().bind(viewModel.resourceStatusTextProperty());
        resourceVersionLabel.textProperty().bind(viewModel.resourceVersionTextProperty());
        resourceStatusPane.visibleProperty().bind(viewModel.resourceStatusVisibleProperty());
        resourceStatusPane.managedProperty().bind(viewModel.resourceStatusVisibleProperty());
        resourceRetryBtn.visibleProperty().bind(viewModel.resourceRetryVisibleProperty());
        resourceRetryBtn.managedProperty().bind(viewModel.resourceRetryVisibleProperty());

        resourceCheckRotation = new RotateTransition(Duration.seconds(1.2), resourceStatusIcon);
        resourceCheckRotation.setByAngle(360);
        resourceCheckRotation.setCycleCount(javafx.animation.Animation.INDEFINITE);
        resourceCheckRotation.setInterpolator(Interpolator.LINEAR);
        viewModel.resourceUpdateStateProperty().addListener((observable, oldState, newState) ->
                updateResourceStatusStyle(newState));
        updateResourceStatusStyle(viewModel.resourceUpdateStateProperty().get());

        Tooltip gameTimeTip = new Tooltip();
        gameTimeTip.textProperty().bind(viewModel.gameTimeTipTextProperty());
        gameTimeBtn.setTooltip(gameTimeTip);
        gameTimeBtn.textProperty().bind(viewModel.gameTimeTextProperty());

        if (Config.setting().isUseLocalCacheUser()){
            Platform.runLater(()->{
                ViewTuple<RoleBoardByLocalView, RoleBoardByLocalViewModel> viewTuple = FluentViewLoader.fxmlView(RoleBoardByLocalView.class).load();
                roleBoardGroup.getChildren().add(viewTuple.getView());
            });

        }else {
          Platform.runLater(()->{
              ViewTuple<RoleBoardByKujiequView, RoleBoardByKujiequViewModel> viewTuple = FluentViewLoader.fxmlView(RoleBoardByKujiequView.class).load();
              roleBoardGroup.getChildren().add(viewTuple.getView());
          });
        }

        setChangeBgEnable();
    }

    private void updateResourceStatusStyle(HomeViewModel.ResourceUpdateState state) {
        boolean checking = state == HomeViewModel.ResourceUpdateState.CHECKING;
        boolean upToDate = state == HomeViewModel.ResourceUpdateState.UP_TO_DATE;
        boolean updateAvailable = state == HomeViewModel.ResourceUpdateState.UPDATE_AVAILABLE;
        boolean failed = state == HomeViewModel.ResourceUpdateState.FAILED;

        resourceStatusPane.pseudoClassStateChanged(CHECKING, checking);
        resourceStatusPane.pseudoClassStateChanged(UP_TO_DATE, upToDate);
        resourceStatusPane.pseudoClassStateChanged(UPDATE_AVAILABLE, updateAvailable);
        resourceStatusPane.pseudoClassStateChanged(CHECK_FAILED, failed);
        startUpdateBtn.pseudoClassStateChanged(UPDATE_AVAILABLE, updateAvailable);

        if (checking) {
            resourceStatusIcon.setIconLiteral("mdomz-sync");
            resourceCheckRotation.play();
        } else {
            resourceCheckRotation.stop();
            resourceStatusIcon.setRotate(0);
            resourceStatusIcon.setIconLiteral(switch (state) {
                case UP_TO_DATE -> "mdoal-check_circle";
                case UPDATE_AVAILABLE -> "mdomz-system_update_alt";
                case FAILED -> "mdoal-error_outline";
                default -> "mdomz-sync";
            });
        }
    }


    /**
     * @description: 启动切换背景
     * @param:
     * @return  void
     * @date:   2025/2/18
     */
    private void setChangeBgEnable() {
        if (Config.setting().getDiyHomeBgType() == 2) {
            root.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    MvvmFX.getNotificationCenter().publish(NotificationKey.CHANGE_BG);
                }
            });
        }
    }




    @FXML
    void startGame(ActionEvent event) {
        viewModel.startGame();
    }

    @FXML
    void showGameTimerAlert(ActionEvent event) {
        NotificationManager.message(MessageInfo.info("请前往游玩统计修改时长"));
    }



    @FXML
    void startUpdate(ActionEvent event) {
        viewModel.startUpdate();
    }

    @FXML
    void retryResourceUpdateCheck(ActionEvent event) {
        viewModel.checkGameResourceUpdate();
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
    void toWikiMap01(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://www.kurobbs.com/mc/map/"));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void toWikiMap02(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://map.caimogu.cc/ww/main.html"));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void toAlbum(ActionEvent event) {
        try {
            File file = GameResourcesManager.getGameScreenShoot();
            if (file != null) {
                Desktop.getDesktop().open(file);
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.warning(LanguageManager.getString("ui.home.message.type06")));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void toGameDir(ActionEvent event) {
        try {
            File file = GameResourcesManager.getGameDir();
            if (file != null) {
                Desktop.getDesktop().open(file);
            } else {
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, MessageInfo.warning(LanguageManager.getString("ui.home.message.type04")));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
