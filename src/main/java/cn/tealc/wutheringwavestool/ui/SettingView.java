package cn.tealc.wutheringwavestool.ui;

import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

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
 * @create: 2024-07-03 20:20
 */
public class SettingView implements Initializable, FxmlView<SettingViewModel> {
    @InjectViewModel
    private SettingViewModel viewModel;
    @FXML
    private TextField gameDirField;

    @FXML
    private CheckBox startWithAnalysisView;
    @FXML
    private CheckBox exitWhenGameOver;
    @FXML
    private CheckBox hideWhenGameStart;
    @FXML
    private ChoiceBox<String> fontFamilyChoiceBox;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        gameDirField.textProperty().bindBidirectional(viewModel.gameDirProperty());
        startWithAnalysisView.selectedProperty().bindBidirectional(viewModel.startWithAnalysisProperty());
        hideWhenGameStart.selectedProperty().bindBidirectional(viewModel.hideWhenGameStartProperty());
        exitWhenGameOver.selectedProperty().bindBidirectional(viewModel.exitWhenGameOverProperty());
        fontFamilyChoiceBox.setItems(viewModel.getFontFamilyList());

        fontFamilyChoiceBox.valueProperty().addListener((observableValue, s, t1) -> {
            if (t1 != null){
                viewModel.setFontFamily(t1);
            }
        });

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

}