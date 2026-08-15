package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.*;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 统一「游戏资源管理」视图：全量下载 + 增量更新 + 预下载 + 校验修复合一，
 * 单进度条。作为 GameManagerView 的第 5 个子 tab。
 */
public class GameAssetView implements FxmlView<GameAssetViewModel>, Initializable {
    private static final PseudoClass RUNNING_STATE = PseudoClass.getPseudoClass("running");
    private static final PseudoClass PAUSED_STATE = PseudoClass.getPseudoClass("paused");
    private static final PseudoClass STOPPING_STATE = PseudoClass.getPseudoClass("stopping");

    @InjectViewModel
    private GameAssetViewModel viewModel;

    @FXML
    private StackPane assetRoot;
    @FXML
    private VBox directorySection;
    @FXML
    private HBox operationControls;
    @FXML
    private Separator pauseSeparator;
    @FXML
    private StackPane pauseControls;
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
    @FXML
    private Button chooseDirBtn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentVersionLabel.textProperty().bind(viewModel.currentVersionProperty());
        latestVersionLabel.textProperty().bind(viewModel.latestVersionProperty());
        statusLabel.textProperty().bind(viewModel.statusProperty());
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressTextLabel.textProperty().bind(viewModel.progressTextProperty());
        tipLabel.textProperty().bind(viewModel.tipProperty());
        downloadDirField.textProperty().bindBidirectional(viewModel.downloadDirProperty());

        // 按钮显隐
        bindVisibility(downloadBtn, viewModel.showDownloadProperty());
        bindVisibility(updateBtn, viewModel.showUpdateProperty());
        bindVisibility(repairBtn, viewModel.showRepairProperty());
        bindVisibility(preDownloadBtn, viewModel.showPreDownloadProperty());
        bindVisibility(directorySection, viewModel.showDownloadProperty());
        bindVisibility(operationControls, viewModel.operatingProperty().and(
                viewModel.pauseAvailableProperty().or(viewModel.stopAvailableProperty())));
        bindVisibility(pauseSeparator, viewModel.pauseAvailableProperty());
        bindVisibility(pauseControls, viewModel.pauseAvailableProperty());
        bindVisibility(stopBtn, viewModel.stopAvailableProperty());
        bindVisibility(pauseBtn, Bindings.equal(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.RUNNING)
                .and(viewModel.pauseAvailableProperty()));
        bindVisibility(resumeBtn, Bindings.equal(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.PAUSED)
                .and(viewModel.pauseAvailableProperty()));

        // 操作过程中禁用主操作按钮，各控制按钮仅在对应状态下可用
        downloadBtn.disableProperty().bind(viewModel.operatingProperty());
        updateBtn.disableProperty().bind(viewModel.operatingProperty());
        repairBtn.disableProperty().bind(viewModel.operatingProperty());
        preDownloadBtn.disableProperty().bind(viewModel.operatingProperty());
        downloadDirField.disableProperty().bind(viewModel.operatingProperty());
        chooseDirBtn.disableProperty().bind(viewModel.operatingProperty());
        pauseBtn.disableProperty().bind(Bindings.notEqual(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.RUNNING)
                .or(viewModel.pauseAvailableProperty().not()));
        resumeBtn.disableProperty().bind(Bindings.notEqual(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.PAUSED)
                .or(viewModel.pauseAvailableProperty().not()));
        stopBtn.disableProperty().bind(
                Bindings.equal(viewModel.operationStateProperty(), GameAssetViewModel.OperationState.IDLE)
                        .or(Bindings.equal(
                                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.STOPPING)));

        viewModel.operationStateProperty().addListener((observable, oldState, newState) ->
                updateOperationStateStyle(newState));
        updateOperationStateStyle(viewModel.operationStateProperty().get());
    }

    private void bindVisibility(Node node, javafx.beans.value.ObservableBooleanValue visible) {
        node.visibleProperty().bind(visible);
        node.managedProperty().bind(visible);
    }

    private void updateOperationStateStyle(GameAssetViewModel.OperationState state) {
        assetRoot.pseudoClassStateChanged(RUNNING_STATE, state == GameAssetViewModel.OperationState.RUNNING);
        assetRoot.pseudoClassStateChanged(PAUSED_STATE, state == GameAssetViewModel.OperationState.PAUSED);
        assetRoot.pseudoClassStateChanged(STOPPING_STATE, state == GameAssetViewModel.OperationState.STOPPING);
    }

    @FXML
    void chooseDownloadDir(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LanguageManager.getString("ui.game_manager.asset.dir"));
        String cwd = viewModel.downloadDirProperty().get();
        if (cwd != null && !cwd.isBlank()) {
            File f = new File(cwd);
            if (f.isDirectory()) {
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
