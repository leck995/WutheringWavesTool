package cn.tealc.wutheringwavestool.ui.system.home;

import atlantafx.base.theme.Styles;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;
import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.ui.item.HeaderImageSelectView;
import cn.tealc.wutheringwavestool.ui.item.PlayTimeAlertItemView;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.*;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
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
    private VBox roleBoardGroup;

    @FXML
    private BorderPane root;

    @FXML
    private Button gameTimeBtn;

    @FXML
    private Button startGameBtn;




    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startGameBtn.disableProperty().bind(viewModel.startGameBtnDisabledProperty());

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
        viewModel.checkIsWeekEnd();
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
//        if (viewModel.isHasSign()) {
//            viewModel.startGame();
//        } else {
//            JFXDialogLayout dialogLayout = new JFXDialogLayout();
//            Label title = new Label("签到提醒");
//            title.getStyleClass().add(Styles.TITLE_2);
//            dialogLayout.setHeading(title);
//            dialogLayout.setBody(new Label("检测到还没有签到，是否签到并启动游戏?"));
//
//            Button okBtn = new Button("签到并启动");
//            Button directBtn = new Button("启动");
//            Button cancelBtn = new Button("取消");
//
//            okBtn.setOnAction(event1 -> {
//                viewModel.signAndGame();
//                cancelBtn.fireEvent(event1);
//            });
//            directBtn.setOnAction(event1 -> {
//                viewModel.startGame();
//                cancelBtn.fireEvent(event1);
//            });
//
//            cancelBtn.setCancelButton(true);
//            dialogLayout.setActions(okBtn, directBtn, cancelBtn);
//
//            MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, dialogLayout);
//        }
    }

    @FXML
    void showGameTimerAlert(ActionEvent event) {
        PlayTimeAlertItemView view = new PlayTimeAlertItemView();
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, view);
    }



    @FXML
    void startUpdate(ActionEvent event) {
        viewModel.startUpdate();
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
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type06")));
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
                MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.WARNING, LanguageManager.getString("ui.home.message.type04")));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}