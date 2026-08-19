package cn.tealc.wutheringwavestool.ui.system.home;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.thread.game.download.GameResourceUpdateTask.UpdateState;
import cn.tealc.wutheringwavestool.model.SourceType;
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
import javafx.beans.binding.Bindings;
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
import javafx.stage.DirectoryChooser;
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
    private static final PseudoClass OPERATING = PseudoClass.getPseudoClass("operating");
    private static final PseudoClass PAUSED = PseudoClass.getPseudoClass("paused");

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
    private VBox resourceStatusPane;
    @FXML
    private Label resourceStatusLabel;
    @FXML
    private Label resourceVersionLabel;
    @FXML
    private FontIcon resourceStatusIcon;
    @FXML
    private Button resourceRetryBtn;
    @FXML
    private VBox resourceProgressPane;
    @FXML
    private ProgressBar resourceProgressBar;
    @FXML
    private Label resourceProgressLabel;
    @FXML
    private Button resourcePauseBtn;
    @FXML
    private Button resourceResumeBtn;
    @FXML
    private Button resourceCancelBtn;

    @FXML
    private MenuButton serverSwitchMenu;
    @FXML
    private RadioMenuItem serverSwitchMainlandItem;
    @FXML
    private RadioMenuItem serverSwitchBilibiliItem;
    @FXML
    private RadioMenuItem serverSwitchGlobalItem;
    private RotateTransition resourceCheckRotation;




    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startGameBtn.disableProperty().bind(viewModel.startGameBtnDisabledProperty().or(
                Bindings.equal(viewModel.resourceUpdateStateProperty(),
                        UpdateState.APPLYING)).or(viewModel.serverSwitchOperatingProperty()));
        startUpdateBtn.disableProperty().bind(viewModel.serverSwitchOperatingProperty());
        startUpdateBtn.textProperty().bind(viewModel.updateActionTextProperty());
        bindVisibility(startUpdateBtn, viewModel.updateActionVisibleProperty());
        resourceStatusLabel.textProperty().bind(viewModel.resourceStatusTextProperty());
        resourceVersionLabel.textProperty().bind(viewModel.resourceVersionTextProperty());
        bindVisibility(resourceStatusPane, viewModel.resourceStatusVisibleProperty());
        bindVisibility(resourceRetryBtn, viewModel.resourceRetryVisibleProperty());
        bindVisibility(resourceProgressPane, viewModel.resourceProgressVisibleProperty());
        bindVisibility(resourcePauseBtn, viewModel.resourcePauseVisibleProperty());
        bindVisibility(resourceResumeBtn, viewModel.resourceResumeVisibleProperty());
        bindVisibility(resourceCancelBtn, viewModel.resourceCancelVisibleProperty());
        resourceProgressBar.progressProperty().bind(viewModel.resourceProgressProperty());
        resourceProgressLabel.textProperty().bind(viewModel.resourceProgressTextProperty());

        serverSwitchMenu.textProperty().bind(Bindings.createStringBinding(() -> {
            if (viewModel.serverSwitchOperatingProperty().get()) {
                return LanguageManager.getString("ui.home.server_switch.switching");
            }
            SourceType source = viewModel.currentGameSourceProperty().get();
            if (source == null) {
                return LanguageManager.getString("ui.home.server_switch.default");
            }
            return switch (source) {
                case DEFAULT, WE_GAME -> LanguageManager.getString(
                        "ui.game_manager.base.server_switch.mainland");
                case BILIBILI -> LanguageManager.getString(
                        "ui.game_manager.base.server_switch.bilibili");
                case GLOBAL -> LanguageManager.getString("ui.game_manager.asset.server_global");
            };
        }, viewModel.serverSwitchOperatingProperty(), viewModel.currentGameSourceProperty()));
        Tooltip serverSwitchTooltip = new Tooltip();
        serverSwitchTooltip.textProperty().bind(Bindings.createStringBinding(() -> {
            String status = viewModel.serverSwitchStatusTextProperty().get();
            String detail = viewModel.serverSwitchDetailTextProperty().get();
            return detail == null || detail.isBlank() ? status : status + System.lineSeparator() + detail;
        }, viewModel.serverSwitchStatusTextProperty(), viewModel.serverSwitchDetailTextProperty()));
        serverSwitchMenu.setTooltip(serverSwitchTooltip);
        var resourceOperationRunning = Bindings.createBooleanBinding(() -> switch (
                viewModel.resourceUpdateStateProperty().get()) {
            case CHECKING, PREPARING, DOWNLOADING, PAUSED, APPLYING -> true;
            default -> false;
        }, viewModel.resourceUpdateStateProperty());
        serverSwitchMenu.disableProperty().bind(
                viewModel.serverSwitchOperatingProperty().or(resourceOperationRunning));
        ToggleGroup serverSwitchToggleGroup = new ToggleGroup();
        serverSwitchMainlandItem.setToggleGroup(serverSwitchToggleGroup);
        serverSwitchBilibiliItem.setToggleGroup(serverSwitchToggleGroup);
        serverSwitchGlobalItem.setToggleGroup(serverSwitchToggleGroup);
        serverSwitchMainlandItem.disableProperty().bind(viewModel.serverSwitchOperatingProperty()
                .or(Bindings.equal(viewModel.currentGameSourceProperty(), SourceType.DEFAULT))
                .or(viewModel.mainlandTargetReadyProperty().not()));
        serverSwitchBilibiliItem.disableProperty().bind(viewModel.serverSwitchOperatingProperty()
                .or(Bindings.equal(viewModel.currentGameSourceProperty(), SourceType.BILIBILI))
                .or(viewModel.bilibiliTargetReadyProperty().not()));
        serverSwitchGlobalItem.disableProperty().bind(viewModel.serverSwitchOperatingProperty()
                .or(Bindings.equal(viewModel.currentGameSourceProperty(), SourceType.GLOBAL)));
        serverSwitchMenu.setOnShowing(event -> updateServerSwitchSelection());
        updateServerSwitchSelection();

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

    private void bindVisibility(Node node, javafx.beans.value.ObservableBooleanValue visible) {
        node.visibleProperty().bind(visible);
        node.managedProperty().bind(visible);
    }

    private void updateServerSwitchSelection() {
        SourceType current = viewModel.currentGameSourceProperty().get();
        serverSwitchMainlandItem.setSelected(current == SourceType.DEFAULT);
        serverSwitchBilibiliItem.setSelected(current == SourceType.BILIBILI);
        serverSwitchGlobalItem.setSelected(current == SourceType.GLOBAL);
    }

    @FXML
    void switchToMainland(ActionEvent event) {
        switchOrConfigure(SourceType.DEFAULT);
    }

    @FXML
    void switchToBilibili(ActionEvent event) {
        switchOrConfigure(SourceType.BILIBILI);
    }

    @FXML
    void switchToGlobal(ActionEvent event) {
        switchOrConfigure(SourceType.GLOBAL);
    }

    private void switchOrConfigure(SourceType target) {
        updateServerSwitchSelection();
        if (viewModel.isServerInstallationConfigured(target)) {
            viewModel.switchServer(target);
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(target == SourceType.GLOBAL
                ? LanguageManager.getString("ui.home.server_switch.choose_global_dir")
                : LanguageManager.getString("ui.home.server_switch.choose_china_dir"));
        File selected = chooser.showDialog(serverSwitchMenu.getScene().getWindow());
        if (selected == null) {
            return;
        }
        String error = viewModel.configureAndSwitchServer(target, selected);
        if (error != null) {
            NotificationManager.message(MessageInfo.warning(error));
        }
    }

    private void updateResourceStatusStyle(UpdateState state) {
        boolean checking = state == UpdateState.CHECKING || state == UpdateState.PREPARING;
        boolean upToDate = state == UpdateState.UP_TO_DATE || state == UpdateState.COMPLETED;
        boolean updateAvailable = state == UpdateState.UPDATE_AVAILABLE;
        boolean failed = state == UpdateState.FAILED || state == UpdateState.CANCELED;
        boolean operating = state == UpdateState.PREPARING || state == UpdateState.DOWNLOADING
                || state == UpdateState.APPLYING;
        boolean paused = state == UpdateState.PAUSED;

        resourceStatusPane.pseudoClassStateChanged(CHECKING, checking);
        resourceStatusPane.pseudoClassStateChanged(UP_TO_DATE, upToDate);
        resourceStatusPane.pseudoClassStateChanged(UPDATE_AVAILABLE, updateAvailable);
        resourceStatusPane.pseudoClassStateChanged(CHECK_FAILED, failed);
        resourceStatusPane.pseudoClassStateChanged(OPERATING, operating);
        resourceStatusPane.pseudoClassStateChanged(PAUSED, paused);
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
                case DOWNLOADING -> "mdoal-cloud_download";
                case PAUSED -> "mdomz-pause";
                case APPLYING -> "mdoal-build";
                case COMPLETED -> "mdoal-check_circle";
                case FAILED, CANCELED -> "mdoal-error_outline";
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
        viewModel.retryResourceUpdate();
    }

    @FXML
    void pauseResourceUpdate(ActionEvent event) {
        viewModel.pauseResourceUpdate();
    }

    @FXML
    void resumeResourceUpdate(ActionEvent event) {
        viewModel.resumeResourceUpdate();
    }

    @FXML
    void cancelResourceUpdate(ActionEvent event) {
        viewModel.cancelResourceUpdate();
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
