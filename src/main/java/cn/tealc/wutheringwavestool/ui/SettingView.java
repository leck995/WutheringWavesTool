package cn.tealc.wutheringwavestool.ui;

import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.File;
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


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        gameDirField.textProperty().bindBidirectional(viewModel.gameDirProperty());
        startWithAnalysisView.selectedProperty().bindBidirectional(viewModel.startWithAnalysisProperty());
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

}