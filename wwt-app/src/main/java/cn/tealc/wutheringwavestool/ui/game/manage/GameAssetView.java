package cn.tealc.wutheringwavestool.ui.game.manage;

import atlantafx.base.controls.ToggleSwitch;
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
 * 按任务类型展示独立进度区域，作为 GameManagerGroupView 的资源管理子 tab。
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
    private Label downloadSourceHintLabel;
    @FXML
    private VBox resourceProgressSection;
    @FXML
    private VBox downloadProgressSection;
    @FXML
    private HBox resourceOperationControls;
    @FXML
    private HBox downloadOperationControls;
    @FXML
    private Separator resourcePauseSeparator;
    @FXML
    private Separator downloadPauseSeparator;
    @FXML
    private StackPane resourcePauseControls;
    @FXML
    private StackPane downloadPauseControls;
    @FXML
    private Label currentVersionLabel;
    @FXML
    private Label latestVersionLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label currentServerLabel;
    @FXML
    private TextField downloadDirField;
    @FXML
    private TextField downloadCacheDirField;
    @FXML
    private Button chooseCacheDirBtn;
    @FXML
    private RadioButton mainlandSourceRadio;
    @FXML
    private RadioButton bilibiliSourceRadio;
    @FXML
    private RadioButton globalSourceRadio;
    @FXML
    private ToggleGroup downloadSourceToggle;
    @FXML
    private ProgressBar resourceProgressBar;
    @FXML
    private ProgressBar downloadProgressBar;
    @FXML
    private Label resourceSpeedLabel;
    @FXML
    private Label downloadSpeedLabel;
    @FXML
    private Label resourceProgressTextLabel;
    @FXML
    private Label downloadProgressTextLabel;
    @FXML
    private Label resourceTipLabel;
    @FXML
    private Label downloadTipLabel;
    @FXML
    private Spinner<Integer> parallelSpinner;
    @FXML
    private ToggleSwitch speedLimitSwitch;
    @FXML
    private Spinner<Double> speedLimitSpinner;

    @FXML
    private Button downloadBtn;
    @FXML
    private Button updateBtn;
    @FXML
    private Button repairBtn;
    @FXML
    private Button preDownloadBtn;
    @FXML
    private Label assetActionTipLabel;
    @FXML
    private Button resourcePauseBtn;
    @FXML
    private Button resourceResumeBtn;
    @FXML
    private Button resourceStopBtn;
    @FXML
    private Button downloadPauseBtn;
    @FXML
    private Button downloadResumeBtn;
    @FXML
    private Button downloadStopBtn;
    @FXML
    private Button chooseDirBtn;
    @FXML
    private VBox cacheOperationFeedback;
    @FXML
    private Label cacheStatusLabel;
    @FXML
    private Label cacheDetailLabel;
    @FXML
    private ProgressBar cacheProgress;
    @FXML
    private Label mainlandCacheStatusLabel;
    @FXML
    private Label bilibiliCacheStatusLabel;
    @FXML
    private Button redownloadMainlandButton;
    @FXML
    private Button redownloadBilibiliButton;
    @FXML
    private Button deleteMainlandButton;
    @FXML
    private Button deleteBilibiliButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentVersionLabel.textProperty().bind(viewModel.currentVersionProperty());
        latestVersionLabel.textProperty().bind(viewModel.latestVersionProperty());
        statusLabel.textProperty().bind(viewModel.statusProperty());
        resourceProgressBar.progressProperty().bind(viewModel.progressProperty());
        resourceSpeedLabel.textProperty().bind(viewModel.downloadSpeedProperty());
        resourceProgressTextLabel.textProperty().bind(viewModel.progressTextProperty());
        resourceTipLabel.textProperty().bind(viewModel.tipProperty());
        assetActionTipLabel.textProperty().bind(viewModel.tipProperty());
        downloadProgressBar.progressProperty().bind(viewModel.downloadProgressProperty());
        downloadSpeedLabel.textProperty().bind(viewModel.downloadDownloadSpeedProperty());
        downloadProgressTextLabel.textProperty().bind(viewModel.downloadProgressTextProperty());
        downloadTipLabel.textProperty().bind(viewModel.downloadTipProperty());
        viewModel.currentGameSourceProperty().addListener((observable, oldSource, newSource) ->
                updateCurrentServerLabel(newSource));
        updateCurrentServerLabel(viewModel.currentGameSourceProperty().get());
        downloadDirField.textProperty().bindBidirectional(viewModel.downloadDirProperty());
        downloadCacheDirField.textProperty().bindBidirectional(viewModel.customDownloadCacheDirProperty());
        downloadCacheDirField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                viewModel.setCustomDownloadCacheDir(downloadCacheDirField.getText());
            }
        });
        viewModel.downloadSourceProperty().addListener((observable, oldSource, newSource) ->
                selectDownloadSource(newSource));
        downloadSourceToggle.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle != null && newToggle.getUserData() instanceof String sourceName) {
                viewModel.downloadSourceProperty().set(SourceType.valueOf(sourceName));
            }
        });
        selectDownloadSource(viewModel.downloadSourceProperty().get());
        initDownloadPolicy();

        // 按钮显隐
        bindVisibility(downloadBtn, viewModel.showDownloadProperty());
        bindVisibility(updateBtn, viewModel.showUpdateProperty());
        bindVisibility(repairBtn, viewModel.showRepairProperty());
        bindVisibility(preDownloadBtn, viewModel.showPreDownloadProperty());
        bindVisibility(directorySection, viewModel.showDownloadProperty());
        bindVisibility(downloadSourceHintLabel, viewModel.showDownloadSourceHintProperty());
        bindVisibility(resourceProgressSection, viewModel.resourceOperationOperatingProperty());
        bindVisibility(downloadProgressSection, viewModel.fullDownloadOperatingProperty());
        bindVisibility(resourceOperationControls, viewModel.resourceOperationOperatingProperty().and(
                viewModel.pauseAvailableProperty().or(viewModel.stopAvailableProperty())));
        bindVisibility(downloadOperationControls, viewModel.fullDownloadOperatingProperty().and(
                viewModel.pauseAvailableProperty().or(viewModel.stopAvailableProperty())));
        bindVisibility(resourcePauseSeparator, viewModel.pauseAvailableProperty());
        bindVisibility(resourcePauseControls, viewModel.pauseAvailableProperty());
        bindVisibility(resourceStopBtn, viewModel.stopAvailableProperty());
        bindVisibility(downloadPauseSeparator, viewModel.pauseAvailableProperty());
        bindVisibility(downloadPauseControls, viewModel.pauseAvailableProperty());
        bindVisibility(downloadStopBtn, viewModel.stopAvailableProperty());
        bindVisibility(resourcePauseBtn, Bindings.equal(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.RUNNING)
                .and(viewModel.pauseAvailableProperty()));
        bindVisibility(resourceResumeBtn, Bindings.equal(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.PAUSED)
                .and(viewModel.pauseAvailableProperty()));
        bindVisibility(downloadPauseBtn, Bindings.equal(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.RUNNING)
                .and(viewModel.pauseAvailableProperty()));
        bindVisibility(downloadResumeBtn, Bindings.equal(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.PAUSED)
                .and(viewModel.pauseAvailableProperty()));

        // 操作过程中禁用主操作按钮，各控制按钮仅在对应状态下可用
        var resourceOrCacheOperating = viewModel.operatingProperty().or(viewModel.serverSwitchOperatingProperty());
        downloadBtn.disableProperty().bind(resourceOrCacheOperating);
        updateBtn.disableProperty().bind(resourceOrCacheOperating);
        repairBtn.disableProperty().bind(resourceOrCacheOperating);
        preDownloadBtn.disableProperty().bind(resourceOrCacheOperating);
        mainlandSourceRadio.disableProperty().bind(resourceOrCacheOperating);
        bilibiliSourceRadio.disableProperty().bind(resourceOrCacheOperating);
        globalSourceRadio.disableProperty().bind(resourceOrCacheOperating);
        downloadDirField.disableProperty().bind(resourceOrCacheOperating);
        chooseDirBtn.disableProperty().bind(resourceOrCacheOperating);
        downloadCacheDirField.disableProperty().bind(resourceOrCacheOperating);
        chooseCacheDirBtn.disableProperty().bind(resourceOrCacheOperating);
        resourcePauseBtn.disableProperty().bind(Bindings.notEqual(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.RUNNING)
                .or(viewModel.pauseAvailableProperty().not()));
        resourceResumeBtn.disableProperty().bind(Bindings.notEqual(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.PAUSED)
                .or(viewModel.pauseAvailableProperty().not()));
        resourceStopBtn.disableProperty().bind(
                Bindings.equal(viewModel.operationStateProperty(), GameAssetViewModel.OperationState.IDLE)
                        .or(Bindings.equal(
                                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.STOPPING)));
        downloadPauseBtn.disableProperty().bind(Bindings.notEqual(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.RUNNING)
                .or(viewModel.pauseAvailableProperty().not()));
        downloadResumeBtn.disableProperty().bind(Bindings.notEqual(
                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.PAUSED)
                .or(viewModel.pauseAvailableProperty().not()));
        downloadStopBtn.disableProperty().bind(
                Bindings.equal(viewModel.operationStateProperty(), GameAssetViewModel.OperationState.IDLE)
                        .or(Bindings.equal(
                                viewModel.operationStateProperty(), GameAssetViewModel.OperationState.STOPPING)));

        viewModel.operationStateProperty().addListener((observable, oldState, newState) ->
                updateOperationStateStyle(newState));
        updateOperationStateStyle(viewModel.operationStateProperty().get());
        initDownloadCache();
    }

    private void initDownloadPolicy() {
        int configuredParallel = Math.clamp(viewModel.downloadParallelCountProperty().get(), 1, 16);
        SpinnerValueFactory.IntegerSpinnerValueFactory parallelValues =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, configuredParallel);
        parallelSpinner.setValueFactory(parallelValues);
        parallelValues.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.setDownloadParallelCount(newValue);
            }
        });

        long configuredLimit = viewModel.downloadSpeedLimitBytesPerSecondProperty().get();
        double initialLimit = configuredLimit > 0
                ? configuredLimit / (1024D * 1024D) : 10D;
        SpinnerValueFactory.DoubleSpinnerValueFactory speedValues =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1000, initialLimit, 0.1);
        speedValues.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Double value) {
                return value == null ? "" : String.format(java.util.Locale.ROOT, "%.1f", value);
            }

            @Override
            public Double fromString(String value) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return speedValues.getValue();
                }
            }
        });
        speedLimitSpinner.setValueFactory(speedValues);
        speedLimitSwitch.setSelected(configuredLimit > 0);
        speedLimitSwitch.selectedProperty().addListener((observable, oldValue, enabled) ->
                updateSpeedLimit(enabled, speedValues.getValue()));
        speedValues.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (speedLimitSwitch.isSelected() && newValue != null) {
                updateSpeedLimit(true, newValue);
            }
        });

        var resourceOrCacheOperating = viewModel.operatingProperty().or(viewModel.serverSwitchOperatingProperty());
        parallelSpinner.disableProperty().bind(resourceOrCacheOperating);
        speedLimitSwitch.disableProperty().bind(resourceOrCacheOperating);
        speedLimitSpinner.disableProperty().bind(resourceOrCacheOperating
                .or(speedLimitSwitch.selectedProperty().not()));
    }

    private void updateSpeedLimit(boolean enabled, Double megabytesPerSecond) {
        long bytesPerSecond = enabled && megabytesPerSecond != null
                ? Math.max(1, Math.round(megabytesPerSecond * 1024 * 1024)) : 0;
        viewModel.setDownloadSpeedLimitBytesPerSecond(bytesPerSecond);
    }

    private void initDownloadCache() {
        cacheStatusLabel.textProperty().bind(viewModel.serverSwitchStatusTextProperty());
        cacheDetailLabel.textProperty().bind(viewModel.serverSwitchDetailTextProperty());
        cacheProgress.progressProperty().bind(viewModel.serverSwitchProgressProperty());
        bindVisibility(cacheOperationFeedback, viewModel.serverSwitchOperatingProperty());
        var resourceOrCacheOperating = viewModel.operatingProperty().or(viewModel.serverSwitchOperatingProperty());
        redownloadMainlandButton.disableProperty().bind(resourceOrCacheOperating);
        redownloadBilibiliButton.disableProperty().bind(resourceOrCacheOperating);
        deleteMainlandButton.disableProperty().bind(resourceOrCacheOperating);
        deleteBilibiliButton.disableProperty().bind(resourceOrCacheOperating);
        mainlandCacheStatusLabel.textProperty().bind(Bindings.when(viewModel.mainlandServerSwitchReadyProperty())
                .then(LanguageManager.getString("ui.game_manager.asset.cache_ready"))
                .otherwise(LanguageManager.getString("ui.game_manager.asset.cache_not_ready")));
        bilibiliCacheStatusLabel.textProperty().bind(Bindings.when(viewModel.bilibiliServerSwitchReadyProperty())
                .then(LanguageManager.getString("ui.game_manager.asset.cache_ready"))
                .otherwise(LanguageManager.getString("ui.game_manager.asset.cache_not_ready")));
    }

    private void confirmRedownload(SourceType source) {
        JFXDialogLayout layout = DialogBuilder
                .create()
                .title(LanguageManager.getString("ui.game_manager.asset.download_cache"))
                .message(LanguageManager.getString("ui.game_manager.base.server_switch.redownload_confirm"))
                .button("确定",null,true,event ->  viewModel.redownloadServerFiles(source))
                .cancel()
                .build();
        NotificationManager.dialog(layout);
    }

    private void confirmDeleteCache(SourceType source) {


        JFXDialogLayout layout = DialogBuilder
                .create()
                .title(LanguageManager.getString("ui.game_manager.asset.download_cache"))
                .message(LanguageManager.getString("ui.game_manager.base.server_switch.delete_confirm"))
                .button("确定",null,true,event -> viewModel.deleteServerCache(source))
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

    private void updateCurrentServerLabel(SourceType source) {
        SourceType effectiveSource = source != null ? source : SourceType.DEFAULT;
        String key = switch (effectiveSource) {
            case BILIBILI -> "ui.game_manager.base.server_switch.bilibili";
            case GLOBAL -> "ui.game_manager.asset.server_global";
            case DEFAULT, WE_GAME -> "ui.game_manager.base.server_switch.mainland";
        };
        currentServerLabel.setText(LanguageManager.getString("ui.game_manager.base.server_switch.current")
                + LanguageManager.getString(key));
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
    void chooseDownloadCacheDir(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LanguageManager.getString("ui.game_manager.asset.cache_dir"));
        String cwd = viewModel.customDownloadCacheDirProperty().get();
        if (cwd != null && !cwd.isBlank()) {
            File f = new File(cwd);
            if (f.isDirectory()) {
                chooser.setInitialDirectory(f);
            }
        }
        File selected = chooser.showDialog(downloadCacheDirField.getScene().getWindow());
        if (selected != null) {
            viewModel.setCustomDownloadCacheDir(selected.getAbsolutePath());
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
