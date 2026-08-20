package cn.tealc.wutheringwavestool.ui.game.manage;

import atlantafx.base.controls.ToggleSwitch;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.ui.component.dialog.NewDialog;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;

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
    private Button downloadSettingsBtn;
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
        preDownloadBtn.disableProperty().bind(resourceOrCacheOperating
                .or(viewModel.preDownloadCompleteProperty()));
        preDownloadBtn.textProperty().bind(Bindings.when(viewModel.preDownloadCompleteProperty())
                .then(LanguageManager.getString("ui.game_manager.asset.predownload_done"))
                .otherwise(LanguageManager.getString("ui.game_manager.asset.predownload")));
        mainlandSourceRadio.disableProperty().bind(resourceOrCacheOperating);
        bilibiliSourceRadio.disableProperty().bind(resourceOrCacheOperating);
        globalSourceRadio.disableProperty().bind(resourceOrCacheOperating);
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

    @FXML
    void openDownloadSettings(ActionEvent event) {
        VBox content = new VBox(10.0);
        content.setPrefWidth(420.0);

        // 下载缓存目录
        Label cacheTitle = new Label(LanguageManager.getString("ui.game_manager.asset.cache_dir"));
        cacheTitle.getStyleClass().add("asset-form-label");

        TextField cacheField = new TextField();
        cacheField.setPromptText(LanguageManager.getString("ui.game_manager.asset.cache_dir_tip"));
        cacheField.textProperty().bindBidirectional(viewModel.customDownloadCacheDirProperty());
        cacheField.setMaxWidth(Double.MAX_VALUE);

        Button browse = new Button(LanguageManager.getString("ui.game_manager.asset.browse"),
                new FontIcon(Material2OutlinedAL.FOLDER_OPEN));
        HBox cacheRow = new HBox(8.0, cacheField, browse);
        cacheRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cacheField, Priority.ALWAYS);

        Label cacheHint = new Label(LanguageManager.getString("ui.game_manager.asset.cache_dir_hint"));
        cacheHint.getStyleClass().add("text-tip");
        cacheHint.setWrapText(true);

        Separator sep = new Separator();

        // 下载策略
        Label policyTitle = new Label(LanguageManager.getString("ui.game_manager.asset.download_policy"));
        policyTitle.getStyleClass().add("asset-form-label");

        int configuredParallel = Math.clamp(viewModel.downloadParallelCountProperty().get(), 1, 16);
        Spinner<Integer> parallelSpinner = new Spinner<>();
        parallelSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, configuredParallel));
        parallelSpinner.setEditable(false);
        parallelSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.setDownloadParallelCount(newValue);
            }
        });
        Label parallelLabel = new Label(LanguageManager.getString("ui.game_manager.asset.parallel"));
        parallelLabel.getStyleClass().add("download-policy-label");
        Label parallelUnit = new Label(LanguageManager.getString("ui.game_manager.asset.parallel_unit"));
        parallelUnit.getStyleClass().add("text-tip");
        HBox parallelRow = new HBox(6.0, parallelLabel, parallelSpinner, parallelUnit);
        parallelRow.setAlignment(Pos.CENTER_LEFT);

        long configuredLimit = viewModel.downloadSpeedLimitBytesPerSecondProperty().get();
        double initialLimit = configuredLimit > 0 ? configuredLimit / (1024D * 1024D) : 10D;
        ToggleSwitch switchOn = new ToggleSwitch();
        switchOn.setSelected(configuredLimit > 0);

        Spinner<Double> speedSpinner = new Spinner<>();
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
        speedSpinner.setValueFactory(speedValues);
        speedSpinner.setEditable(true);

        switchOn.selectedProperty().addListener((observable, oldValue, enabled) -> {
            long bytes = enabled && speedValues.getValue() != null
                    ? Math.max(1, Math.round(speedValues.getValue() * 1024 * 1024)) : 0;
            viewModel.setDownloadSpeedLimitBytesPerSecond(bytes);
        });
        speedSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (switchOn.isSelected() && newValue != null) {
                viewModel.setDownloadSpeedLimitBytesPerSecond(
                        Math.max(1, Math.round(newValue * 1024 * 1024)));
            }
        });

        Label speedLabel = new Label(LanguageManager.getString("ui.game_manager.asset.speed_limit"));
        speedLabel.getStyleClass().add("download-policy-label");
        Label mbLabel = new Label("MB/s");
        mbLabel.getStyleClass().add("text-tip");
        HBox speedRow = new HBox(6.0, speedLabel, switchOn, speedSpinner, mbLabel);
        speedRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(cacheTitle, cacheRow, cacheHint, sep, policyTitle, parallelRow, speedRow);

        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(LanguageManager.getString("ui.game_manager.asset.cache_dir"));
            String cwd = viewModel.customDownloadCacheDirProperty().get();
            if (cwd != null && !cwd.isBlank()) {
                File f = new File(cwd);
                if (f.isDirectory()) {
                    chooser.setInitialDirectory(f);
                }
            }
            File selected = chooser.showDialog(assetRoot.getScene().getWindow());
            if (selected != null) {
                viewModel.setCustomDownloadCacheDir(selected.getAbsolutePath());
            }
        });

        NewDialog<Void> dialog = new NewDialog<>(
                LanguageManager.getString("ui.game_manager.asset.download_settings"), content,
                ButtonType.OK);
        dialog.showAndWait();
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
    void download(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LanguageManager.getString("ui.game_manager.asset.dir"));
        File selected = chooser.showDialog(assetRoot.getScene().getWindow());
        if (selected != null) {
            viewModel.download(selected.getAbsolutePath());
        }
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
