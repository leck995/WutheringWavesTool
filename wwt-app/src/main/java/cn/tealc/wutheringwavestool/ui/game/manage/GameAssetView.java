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
 * 统一「游戏资源管理」视图：全量下载 + 增量更新 + 预下载 + 校验修复合一，
 * 单进度条。作为 GameManagerView 的第 5 个子 tab。
 */
public class GameAssetView implements FxmlView<GameAssetViewModel>, Initializable {
    @InjectViewModel
    private GameAssetViewModel viewModel;

    @FXML
    private Label currentVersionLabel;
    @FXML
    private Label latestVersionLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField downloadDirField;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressTextLabel;
    @FXML
    private Label tipLabel;

    @FXML
    private Button downloadBtn;
    @FXML
    private Button updateBtn;
    @FXML
    private Button repairBtn;
    @FXML
    private Button preDownloadBtn;
    @FXML
    private Button pauseBtn;
    @FXML
    private Button resumeBtn;
    @FXML
    private Button stopBtn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        viewModel.onViewAdded();

        currentVersionLabel.textProperty().bind(viewModel.currentVersionProperty());
        latestVersionLabel.textProperty().bind(viewModel.latestVersionProperty());
        statusLabel.textProperty().bind(viewModel.statusProperty());
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressTextLabel.textProperty().bind(viewModel.progressTextProperty());
        tipLabel.textProperty().bind(viewModel.tipProperty());
        downloadDirField.textProperty().bindBidirectional(viewModel.downloadDirProperty());

        // 按钮显隐
        bindButton(downloadBtn, viewModel.showDownloadProperty());
        bindButton(updateBtn, viewModel.showUpdateProperty());
        bindButton(repairBtn, viewModel.showRepairProperty());
        bindButton(preDownloadBtn, viewModel.showPreDownloadProperty());

        // 操作过程中禁用主操作按钮，暂停/继续/停止仅在有活动操作时可用
        downloadBtn.disableProperty().bind(viewModel.operatingProperty());
        updateBtn.disableProperty().bind(viewModel.operatingProperty());
        repairBtn.disableProperty().bind(viewModel.operatingProperty());
        preDownloadBtn.disableProperty().bind(viewModel.operatingProperty());
        pauseBtn.disableProperty().bind(viewModel.operatingProperty().not());
        resumeBtn.disableProperty().bind(viewModel.operatingProperty().not());
        stopBtn.disableProperty().bind(viewModel.operatingProperty().not());
    }

    private void bindButton(Button btn, javafx.beans.value.ObservableBooleanValue vis) {
        btn.visibleProperty().bind(vis);
        btn.managedProperty().bind(vis);
    }

    @FXML
    void chooseDownloadDir(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LanguageManager.getString("ui.game_manager.asset.dir"));
        String cwd = viewModel.downloadDirProperty().get();
        if (cwd != null && !cwd.isBlank()) {
            File f = new File(cwd);
            if (f.exists()) {
                chooser.setInitialDirectory(f);
            }
        }
        File selected = chooser.showDialog(downloadDirField.getScene().getWindow());
        if (selected != null) {
            viewModel.setDownloadDir(selected.getAbsolutePath());
        }
    }

    @FXML
    void download(ActionEvent event) {
        viewModel.download();
    }

    @FXML
    void update(ActionEvent event) {
        viewModel.update();
    }

    @FXML
    void repair(ActionEvent event) {
        viewModel.repair();
    }

    @FXML
    void preDownload(ActionEvent event) {
        viewModel.preDownload();
    }

    @FXML
    void pause(ActionEvent event) {
        viewModel.pause();
    }

    @FXML
    void resume(ActionEvent event) {
        viewModel.resume();
    }

    @FXML
    void stop(ActionEvent event) {
        viewModel.stop();
    }
}