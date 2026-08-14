package cn.tealc.wutheringwavestool.ui.game.manage;

import de.saxsys.mvvmfx.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 游戏更新视图：检查更新、显示当前/最新版本、开始更新、进度展示。
 * 作为 GameManagerView 的第 4 个子 tab。
 */
public class GameUpdateView implements FxmlView<GameUpdateViewModel>, Initializable {
    @InjectViewModel
    private GameUpdateViewModel viewModel;

    @FXML
    private Label currentVersionLabel;
    @FXML
    private Label latestVersionLabel;
    @FXML
    private Label stateLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressTextLabel;
    @FXML
    private Button checkBtn;
    @FXML
    private Button updateBtn;
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
        stateLabel.textProperty().bind(viewModel.stateDescProperty());
        statusLabel.textProperty().bind(viewModel.statusProperty());
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressTextLabel.textProperty().bind(viewModel.progressTextProperty());

        // 忙时禁用操作按钮，但保留停止可用
        updateBtn.disableProperty().bind(viewModel.hasUpdateProperty().not()
                .or(viewModel.busyProperty()));
        checkBtn.disableProperty().bind(viewModel.busyProperty());
        pauseBtn.disableProperty().bind(viewModel.busyProperty().not());
        resumeBtn.disableProperty().bind(viewModel.busyProperty().not());
        stopBtn.disableProperty().bind(viewModel.busyProperty().not());
    }

    @FXML
    void checkUpdate(ActionEvent event) {
        viewModel.checkUpdate();
    }

    @FXML
    void update(ActionEvent event) {
        viewModel.update();
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