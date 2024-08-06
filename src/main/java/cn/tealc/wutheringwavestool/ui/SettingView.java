package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.controls.ToggleSwitchSkin;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 20:20
 */
public class SettingView implements Initializable, FxmlView<SettingViewModel> {
    private static final Logger LOG= LoggerFactory.getLogger(SettingView.class);
    @InjectViewModel
    private SettingViewModel viewModel;
    @FXML
    private TextField gameDirField;

    @FXML
    private CheckBox startWithAnalysisView;
    @FXML
    private ToggleSwitch exitWhenGameOver;
    @FXML
    private ToggleSwitch hideWhenGameStart;

    @FXML
    private TextField diyBgField;

    @FXML
    private StackPane diyBgInputGroup;

    @FXML
    private ToggleSwitch diyBgSwitch;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        gameDirField.textProperty().bindBidirectional(viewModel.gameDirProperty());
        startWithAnalysisView.selectedProperty().bindBidirectional(viewModel.startWithAnalysisProperty());

        hideWhenGameStart.setSkin(new ToggleSwitchSkin(hideWhenGameStart));
        hideWhenGameStart.selectedProperty().bindBidirectional(viewModel.hideWhenGameStartProperty());
        exitWhenGameOver.selectedProperty().bindBidirectional(viewModel.exitWhenGameOverProperty());
        diyBgField.textProperty().bindBidirectional(viewModel.diyHomeBgNameProperty());
        diyBgSwitch.selectedProperty().bindBidirectional(viewModel.diyHomeBgProperty());
        diyBgInputGroup.managedProperty().bind(diyBgSwitch.selectedProperty());
        diyBgInputGroup.visibleProperty().bind(diyBgSwitch.selectedProperty());

    }
    @FXML
    void setGameDir(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择鸣潮安装根目录");
        File file = directoryChooser.showDialog(gameDirField.getScene().getWindow());
        if (file != null) {
            gameDirField.setText(file.getAbsolutePath());

        }
    }

    @FXML
    void toWeb(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/leck995/WutheringWavesTool"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void setBgFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择背景图片");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("jpg,jpeg,png,bmp","*.png","*.jpg","*.jpeg","*.bmp"));
        File file = fileChooser.showOpenDialog(gameDirField.getScene().getWindow());
        if (file != null) {
            viewModel.setBgFile(file);

        }
    }

}