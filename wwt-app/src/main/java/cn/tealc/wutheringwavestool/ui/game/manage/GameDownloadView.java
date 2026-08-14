package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 游戏下载管理视图：设置游戏下载目录、展示总进度、启停下载。
 * 作为 GameManagerView 的第三个子 tab。
 */
public class GameDownloadView implements FxmlView<GameDownloadViewModel>, Initializable {
    @InjectViewModel
    private GameDownloadViewModel viewModel;

    @FXML
    private TextField downloadDirField;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressTextLabel;
    @FXML
    private Label statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        viewModel.init();

        downloadDirField.textProperty().bindBidirectional(viewModel.downloadDirProperty());
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressTextLabel.textProperty().bind(viewModel.progressTextProperty());
        statusLabel.textProperty().bind(viewModel.statusProperty());
    }

    @FXML
    void startDownload(ActionEvent event) {
        viewModel.startDownload();
    }

    @FXML
    void pauseDownload(ActionEvent event) {
        viewModel.pauseDownload();
    }

    @FXML
    void resumeDownload(ActionEvent event) {
        viewModel.resumeDownload();
    }

    @FXML
    void stopDownload(ActionEvent event) {
        viewModel.stopDownload();
    }

    @FXML
    void chooseDownloadDir(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LanguageManager.getString("ui.game_manager.download.dir"));
        String current = viewModel.getDownloadDir();
        if (current != null && !current.isBlank()) {
            File dir = new File(current);
            if (dir.exists()) {
                chooser.setInitialDirectory(dir);
            }
        }
        File selected = chooser.showDialog(downloadDirField.getScene().getWindow());
        if (selected != null) {
            viewModel.setDownloadDir(selected.getAbsolutePath());
        }
    }
}