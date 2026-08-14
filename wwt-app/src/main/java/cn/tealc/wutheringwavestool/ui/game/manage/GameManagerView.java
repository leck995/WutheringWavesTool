package cn.tealc.wutheringwavestool.ui.game.manage;

import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import de.saxsys.mvvmfx.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 19:17
 */
public class GameManagerView implements FxmlView<GameManagerViewModel>, Initializable {
    @InjectViewModel
    private GameManagerViewModel viewModel;
    @FXML
    private StackPane root;
    @FXML
    private HBox headerPane;
    @FXML
    private StackPane content;
    @FXML
    private ToggleGroup childSelectedToggle;

    private Parent advanceChild;
    private Parent baseChild;
    private Parent downloadChild;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //判断游戏是否安装，未安装则进入选择界面，否则进入设置界面
        if (!viewModel.installed()){
            createDirChooseView();
            headerPane.setDisable(true);
        }else {
            createBaseChild();
        }

        //接收GameDirChooseView传递过来的消息，移除GameDirChooseView
        NotificationManager.subscribe(NotificationKey.GAME_MANAGER_TO_BASE,((s, objects) -> {
            createBaseChild();
            headerPane.setDisable(false);
        }));

        //接收GameBaseSettingView传递过来的消息，显示GameDirChooseView
        NotificationManager.subscribe(NotificationKey.GAME_MANAGE_TO_CHOOSE,((s, objects) -> {
            createDirChooseView();
            headerPane.setDisable(true);
        }));

    }


    @FXML
    void toAdvanceChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
                if (advanceChild == null) {
                    ViewTuple<GameAdvanceSettingView, GameAdvanceSettingViewModel> viewTuple = FluentViewLoader.fxmlView(GameAdvanceSettingView.class).load();
                    advanceChild = viewTuple.getView();
                }
                content.getChildren().setAll(advanceChild);
                Animations.slideInUp(advanceChild, Duration.millis(300)).play();
            } else {
                toggleButton.setSelected(true);
            }
        }
    }



    @FXML
    void toBaseChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
                createBaseChild();
                Animations.slideInUp(baseChild, Duration.millis(300)).play();
            } else {
                toggleButton.setSelected(true);
            }
        }
    }

    @FXML
    void toDownloadChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
                if (downloadChild == null) {
                    ViewTuple<GameDownloadView, GameDownloadViewModel> viewTuple = FluentViewLoader.fxmlView(GameDownloadView.class).load();
                    downloadChild = viewTuple.getView();
                }
                content.getChildren().setAll(downloadChild);
                Animations.slideInUp(downloadChild, Duration.millis(300)).play();
            } else {
                toggleButton.setSelected(true);
            }
        }
    }

    private void createBaseChild(){
        if (baseChild == null) {
            ViewTuple<GameBaseSettingView, GameBaseSettingViewModel> viewTuple = FluentViewLoader.fxmlView(GameBaseSettingView.class).load();
            baseChild = viewTuple.getView();
        }
        content.getChildren().setAll(baseChild);
        baseChild.toFront();
    }

    private void createDirChooseView() {
        ViewTuple<GameDirChooseView, GameDirChooseViewModel> viewTuple = FluentViewLoader.fxmlView(GameDirChooseView.class).load();
        content.getChildren().setAll(viewTuple.getView());
    }
}