package cn.tealc.wutheringwavestool.ui.game;

import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.ui.HomeView;
import cn.tealc.wutheringwavestool.ui.HomeViewModel;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-11-16 20:47
 */
public class GameStatisticsView implements FxmlView<GameStatisticsViewModel>, Initializable {
    @FXML
    private StackPane content;

    @FXML
    private AnchorPane root;


    private Parent recordView;
    private Parent timeView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        createTime();
    }

    @FXML
    void toMain(ActionEvent event) {
        content.getChildren().clear();
    }

    @FXML
    void toRecord(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            if (recordView == null) {
                ViewTuple<GameRecordView, GameRecordViewModel> viewTuple = FluentViewLoader.fxmlView(GameRecordView.class).load();
                recordView = viewTuple.getView();
            }
            content.getChildren().setAll(recordView);
            Animations.slideInUp(recordView, Duration.millis(300)).play();
        }else {
            toggleButton.setSelected(true);
        }


    }

    @FXML
    void toTime(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            createTime();
            Animations.slideInUp(timeView, Duration.millis(300)).play();
        }else {
            toggleButton.setSelected(true);
        }
    }

    private void createTime(){
        if (timeView == null) {
            ViewTuple<GameTimeView, GameTimeViewModel> viewTuple = FluentViewLoader.fxmlView(GameTimeView.class).load();
            timeView = viewTuple.getView();
        }
        content.getChildren().setAll(timeView);
    }


}