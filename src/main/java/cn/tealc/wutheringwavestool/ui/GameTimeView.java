package cn.tealc.wutheringwavestool.ui;

import cn.tealc.wutheringwavestool.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-08-04 04:57
 */
public class GameTimeView implements FxmlView<GameTimeViewModel>, Initializable {
    @InjectViewModel
    private GameTimeViewModel viewModel;
    @FXML
    private ComboBox<UserInfo> accountComboBox;

    @FXML
    private Label allTotalTimeLabel;

    @FXML
    private Label currentDayLabel;

    @FXML
    private ProgressBar currentProgress;

    @FXML
    private Label currentTimeLabel;

    @FXML
    private Label currentTotalTimeLabel;

    @FXML
    private ImageView headImageView;

    @FXML
    private ProgressBar totalProgress;
    @FXML
    private LineChart<String, Double> lineChart;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Circle circle = new Circle(40,40,40);
        headImageView.setClip(circle);
        headImageView.setFitWidth(80);
        headImageView.setFitHeight(80);
        if (Config.setting.getHomeViewIcon() != null){
            File roleIVFile = LocalResourcesManager.homeIcon();
            if (roleIVFile.exists()){
                headImageView.setImage(new Image(roleIVFile.toURI().toString(),80,80,true,true,true));
            }else {
                headImageView.setImage(new Image(FXResourcesLoader.load("image/icon.png"),80,80,true,true,true));
            }
        }else {
            headImageView.setImage(new Image(FXResourcesLoader.load("image/icon.png"),80,80,true,true,true));
        }

        accountComboBox.setItems(viewModel.getUserInfoList());
        accountComboBox.getSelectionModel().select(viewModel.getUserIndex());
        viewModel.userIndexProperty().bind(accountComboBox.getSelectionModel().selectedIndexProperty());
        lineChart.setData(viewModel.getChartData());
        currentDayLabel.textProperty().bind(viewModel.currentDayTextProperty());
        currentTimeLabel.textProperty().bind(viewModel.currentTimeTextProperty());
        currentTotalTimeLabel.textProperty().bind(viewModel.currentTotalTimeTextProperty());
        //currentDayLabel.textProperty().bind(viewModel.currentDayTextProperty());
        currentProgress.progressProperty().bind(viewModel.currentProgressValueProperty());
        allTotalTimeLabel.textProperty().bind(viewModel.allTotalTimeTextProperty());

        totalProgress.progressProperty().bind(viewModel.totalProgressValueProperty());



    }
}