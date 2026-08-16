package cn.tealc.wutheringwavestool.ui.game.manage;

import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.util.DialogBuilder;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.model.SourceType;
import com.jfoenixN.controls.JFXDialogLayout;
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
 * 单进度条，作为 GameManagerView 的资源管理子 tab。
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
    private RadioButton mainlandSourceRadio;
    @FXML
    private RadioButton bilibiliSourceRadio;
    @FXML
    private RadioButton globalSourceRadio;
    @FXML
    private ToggleGroup downloadSourceToggle;
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
    @FXML
    private ToggleGroup serverSwitchSourceToggle;
    @FXML
    private RadioButton serverSwitchMainlandRadio;
    @FXML
    private RadioButton serverSwitchBilibiliRadio;
    @FXML
    private Label serverSwitchStatusLabel;
    @FXML
    private Label serverSwitchDetailLabel;
    @FXML
    private ProgressBar serverSwitchProgress;
    @FXML
    private Button redownloadMainlandButton;
    @FXML
    private Button redownloadBilibiliButton;
    @FXML
    private Button deleteMainlandButton;
    @FXML
    private Button deleteBilibiliButton;

    private boolean serverSwitchSourceInitialized;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentVersionLabel.textProperty().bind(viewModel.currentVersionProperty());
        latestVersionLabel.textProperty().bind(viewModel.latestVersionProperty());
        statusLabel.textProperty().bind(viewModel.statusProperty());
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressTextLabel.textProperty().bind(viewModel.progressTextProperty());
        tipLabel.textProperty().bind(viewModel.tipProperty());
        downloadDirField.textProperty().bindBidirectional(viewModel.downloadDirProperty());
        viewModel.downloadSourceProperty().addListener((observable, oldSource, newSource) ->
                selectDownloadSource(newSource));
        downloadSourceToggle.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle != null && newToggle.getUserData() instanceof String sourceName) {
                viewModel.downloadSourceProperty().set(SourceType.valueOf(sourceName));
            }
        });
        selectDownloadSource(viewModel.downloadSourceProperty().get());

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
        mainlandSourceRadio.disableProperty().bind(viewModel.operatingProperty());
        bilibiliSourceRadio.disableProperty().bind(viewModel.operatingProperty());
        globalSourceRadio.disableProperty().bind(viewModel.operatingProperty());
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
        initServerSwitch();
    }

    private void initServerSwitch() {
        serverSwitchStatusLabel.textProperty().bind(viewModel.serverSwitchStatusTextProperty());
        serverSwitchDetailLabel.textProperty().bind(viewModel.serverSwitchDetailTextProperty());
        serverSwitchProgress.progressProperty().bind(viewModel.serverSwitchProgressProperty());
        redownloadMainlandButton.disableProperty().bind(viewModel.serverSwitchOperatingProperty());
        redownloadBilibiliButton.disableProperty().bind(viewModel.serverSwitchOperatingProperty());
        deleteMainlandButton.disableProperty().bind(viewModel.serverSwitchOperatingProperty());
        deleteBilibiliButton.disableProperty().bind(viewModel.serverSwitchOperatingProperty());
        var serverSwitchDisabled = viewModel.serverSwitchOperatingProperty()
                .or(viewModel.serverSwitchAvailableProperty().not());
        serverSwitchMainlandRadio.disableProperty().bind(serverSwitchDisabled);
        serverSwitchBilibiliRadio.disableProperty().bind(serverSwitchDisabled);
        viewModel.serverSwitchOperatingProperty().addListener((observable, oldValue, running) -> {
            if (!running) {
                selectServerSource(viewModel.currentGameSourceProperty().get());
            }
        });

        viewModel.currentGameSourceProperty().addListener((observable, oldSource, newSource) ->
                selectServerSource(newSource));
        serverSwitchSourceToggle.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (!serverSwitchSourceInitialized || newToggle == null
                    || !(newToggle.getUserData() instanceof String sourceName)) {
                return;
            }
            SourceType target = SourceType.valueOf(sourceName);
            if (target != viewModel.currentGameSourceProperty().get()) {
                confirmServerSwitch(target);
            }
        });
        selectServerSource(viewModel.currentGameSourceProperty().get());
        serverSwitchSourceInitialized = true;
    }

    private void selectServerSource(SourceType source) {
        if (source != SourceType.DEFAULT && source != SourceType.BILIBILI) {
            serverSwitchSourceToggle.selectToggle(null);
            return;
        }
        RadioButton target = source == SourceType.BILIBILI ? serverSwitchBilibiliRadio : serverSwitchMainlandRadio;
        if (serverSwitchSourceToggle.getSelectedToggle() != target) {
            serverSwitchSourceToggle.selectToggle(target);
        }
    }

    private void confirmServerSwitch(SourceType target) {
        JFXDialogLayout layout = DialogBuilder
                .create()
                .title(LanguageManager.getString("ui.game_manager.base.server_switch"))
                .message(LanguageManager.getString("ui.game_manager.base.server_switch.confirm"))
                .button("确定",event ->  viewModel.changeServer(target))
                .button("取消",event -> selectServerSource(viewModel.currentGameSourceProperty().get()))
                .build();
        NotificationManager.dialog(layout);

    }

    private void confirmRedownload(SourceType source) {
        JFXDialogLayout layout = DialogBuilder
                .create()
                .title(LanguageManager.getString("ui.game_manager.base.server_switch"))
                .message(LanguageManager.getString("ui.game_manager.base.server_switch.redownload_confirm"))
                .button("确定",event ->  viewModel.redownloadServerFiles(source))
                .cancel()
                .build();
        NotificationManager.dialog(layout);
    }

    private void confirmDeleteCache(SourceType source) {


        JFXDialogLayout layout = DialogBuilder
                .create()
                .title(LanguageManager.getString("ui.game_manager.base.server_switch"))
                .message(LanguageManager.getString("ui.game_manager.base.server_switch.delete_confirm"))
                .button("确定",event -> viewModel.deleteServerCache(source))
                .cancel()
                .build();
        NotificationManager.dialog(layout);
    }

    private void bindVisibility(Node node, javafx.beans.value.ObservableBooleanValue visible) {
        node.visibleProperty().bind(visible);
        node.managedProperty().bind(visible);
    }

    private void selectDownloadSource(SourceType source) {
        RadioButton radio = switch (source) {
            case BILIBILI -> bilibiliSourceRadio;
            case GLOBAL -> globalSourceRadio;
            case DEFAULT, WE_GAME -> mainlandSourceRadio;
        };
        if (downloadSourceToggle.getSelectedToggle() != radio) {
            downloadSourceToggle.selectToggle(radio);
        }
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

    @FXML
    void redownloadMainlandFiles(ActionEvent event) {
        confirmRedownload(SourceType.DEFAULT);
    }

    @FXML
    void redownloadBilibiliFiles(ActionEvent event) {
        confirmRedownload(SourceType.BILIBILI);
    }

    @FXML
    void deleteMainlandCache(ActionEvent event) {
        confirmDeleteCache(SourceType.DEFAULT);
    }

    @FXML
    void deleteBilibiliCache(ActionEvent event) {
        confirmDeleteCache(SourceType.BILIBILI);
    }
}
